package com.github.tahmid_23.doors.game.generation;

import com.github.steanky.element.core.annotation.Depend;
import com.github.steanky.element.core.context.ContextManager;
import com.github.steanky.element.core.dependency.DependencyModule;
import com.github.steanky.element.core.dependency.DependencyProvider;
import com.github.steanky.element.core.dependency.ModuleDependencyProvider;
import com.github.steanky.element.core.key.KeyParser;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.tahmid_23.doors.bounds.Bounds;
import com.github.tahmid_23.doors.game.generation.transform.BlockTransform;
import com.github.tahmid_23.doors.game.map.DoorsInstance;
import com.github.tahmid_23.doors.game.map.DoorsMap;
import com.github.tahmid_23.doors.game.map.config.RequiredRoom;
import com.github.tahmid_23.doors.game.map.room.Exit;
import com.github.tahmid_23.doors.game.map.room.Room;
import com.github.tahmid_23.doors.game.map.room.RoomInfo;
import com.github.tahmid_23.doors.game.map.room.RoomType;
import com.github.tahmid_23.doors.game.object.closet.HidingSpotManager;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.Batch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RoomGenerator {

    private final ContextManager contextManager;

    private final KeyParser keyParser;

    private final Random random;

    public RoomGenerator(ContextManager contextManager, KeyParser keyParser, Random random) {
        this.contextManager = contextManager;
        this.keyParser = keyParser;
        this.random = random;
    }

    public CompletableFuture<DoorsInstance> generate(DoorsMap map, HidingSpotManager hidingSpotManager, Instance instance, EventNode<Event> gameNode) {
        CompletableFuture<DoorsInstance> future = new CompletableFuture<>();

        DependencyProvider dependencyProvider = new ModuleDependencyProvider(keyParser, new Module(map, hidingSpotManager, instance, gameNode));
        List<BlockTransform> blockTransforms = new ArrayList<>(map.mapConfig().transforms().elementCollection().size());
        for (ConfigElement transformElement : map.mapConfig().transforms().elementCollection()) {
            blockTransforms.add(contextManager.makeContext(transformElement.asContainer()).provide(dependencyProvider));
        }

        Batch<Runnable> batch = new AbsoluteBlockBatch();
        LongSet chunkIndices = new LongArraySet();

        Map<RoomType, IntList> indices = map.roomInfoContext().indices();
        IntList hallwayIndices = indices.get(RoomType.HALLWAY);
        IntList cornerIndices = indices.get(RoomType.CORNER);
        IntList intersectionIndices = indices.get(RoomType.INTERSECTION);

        List<RequiredRoom> requiredRooms = map.mapConfig().requiredRooms();
        RequiredRoom upcomingRequired = null;
        int requiredRoomIndex = 0;
        if (!requiredRooms.isEmpty()) {
            upcomingRequired = requiredRooms.get(0);
        }

        List<Room> rooms = new ArrayList<>(map.mapConfig().roomCount());
        BlockFace currentFace = BlockFace.SOUTH;
        Point currentPos = Vec.ZERO.relative(currentFace.getOppositeFace());
        for (int i = 0; i < map.mapConfig().roomCount(); ++i) {
            RoomInfo roomInfo;
            if (upcomingRequired != null && upcomingRequired.roomNumber() == i) {
                roomInfo = map.roomInfoContext().nameToRoom().get(upcomingRequired.roomKey());
                if (++requiredRoomIndex < requiredRooms.size()) {
                    upcomingRequired = requiredRooms.get(requiredRoomIndex);
                } else {
                    upcomingRequired = null;
                }
            } else {
                int structureIndex = chooseStructureIndex(hallwayIndices, cornerIndices, intersectionIndices, currentFace, upcomingRequired != null && i == upcomingRequired.roomNumber() - 1);
                roomInfo = map.roomInfoContext().randomRooms().get(structureIndex);
            }
            boolean isCorner = roomInfo.roomType() == RoomType.CORNER;
            boolean shouldInvert = checkShouldInvert(currentFace, isCorner);

            Point alignmentPos = currentPos.relative(currentFace);

            Point[] blockPositions = roomInfo.structure().blockPositions();
            int[] blockStates = roomInfo.structure().blockStates();
            Block[] palette = roomInfo.structure().palettes()[map.mapConfig().paletteIndex()];
            NBTCompound[] blockEntityNBT = roomInfo.structure().blockEntityNBT();
            for (int j = 0; j < blockStates.length; ++j) {
                Block block = palette[blockStates[j]];
                if (block.compare(Block.STRUCTURE_VOID) || block.compare(Block.AIR)) {
                    continue;
                }

                Point delta = blockPositions[j].sub(roomInfo.entrance());
                delta = BlockAdjustment.adjustPoint(delta, currentFace, shouldInvert);
                Point actualPos = alignmentPos.add(delta);
                chunkIndices.add(ChunkUtils.getChunkIndex(actualPos));

                NBTCompound nbt = blockEntityNBT[j];
                if (nbt != null) {
                    block = block.withNbt(nbt);
                }

                String facingProperty = block.getProperty("facing");
                if (facingProperty != null) {
                    block = block.withProperty("facing", BlockAdjustment.adjustFacingProperty(facingProperty, currentFace, shouldInvert));
                }
                if (shouldInvert) {
                    String hingeProperty = block.getProperty("hinge");
                    if (hingeProperty != null) {
                        block = block.withProperty("hinge", hingeProperty.equals("left") ? "right" : "left");
                    }
                }

                boolean transformed = false;
                for (BlockTransform transform : blockTransforms) {
                    if (transform.isValidBlock(block)) {
                        transform.transform(batch, block, actualPos, currentFace, shouldInvert, i);
                        transformed = true;
                        break;
                    }
                }

                if (!transformed) {
                    batch.setBlock(actualPos, block);
                }
            }

            Point origin = roomInfo.entrance().mul(-1);
            if (shouldInvert) {
                switch (currentFace) {
                    case NORTH -> origin = origin.withZ(roomInfo.structure().length() - roomInfo.entrance().z() - 1);
                    case WEST ->
                            origin = new Vec(-(roomInfo.structure().length() - roomInfo.entrance().z() - 1), origin.y(), -(roomInfo.structure().width() - roomInfo.entrance().x() - 1));
                    case EAST -> origin = new Vec(origin.z(), origin.y(), -roomInfo.entrance().x());
                }

            } else {
                switch (currentFace) {
                    case NORTH -> origin = origin.withZ(roomInfo.structure().length() - roomInfo.entrance().z() - 1);
                    case WEST ->
                            origin = new Vec(-(roomInfo.structure().length() - roomInfo.entrance().z() - 1), origin.y(), origin.x());
                    case EAST ->
                            origin = new Vec(origin.z(), origin.y(), -(roomInfo.structure().width() - roomInfo.entrance().x() - 1));
                }
            }

            double width, height = roomInfo.structure().height(), length;
            if (currentFace == BlockFace.WEST || currentFace == BlockFace.EAST) {
                width = roomInfo.structure().length();
                length = roomInfo.structure().width();
            } else {
                width = roomInfo.structure().width();
                length = roomInfo.structure().length();
            }

            rooms.add(new Room(roomInfo, Bounds.fromLengths(origin.add(alignmentPos), width, height, length)));

            if (roomInfo.exits().isEmpty()) {
                break;
            }

            int exitIndex = random.nextInt(roomInfo.exits().size());
            Exit exit = roomInfo.exits().get(exitIndex);
            Point exitDelta = exit.location().sub(roomInfo.entrance());
            exitDelta = BlockAdjustment.adjustPoint(exitDelta, currentFace, shouldInvert);

            currentPos = alignmentPos.add(exitDelta);
            currentFace = BlockAdjustment.adjustFace(exit.face(), currentFace, shouldInvert);
        }

        ChunkUtils.optionalLoadAll(instance, chunkIndices.toLongArray(), null).thenRun(() -> batch.apply(instance, () -> {
            future.complete(new DoorsInstance(instance, rooms));
        }));

        return future;
    }

    private int chooseStructureIndex(IntList hallwayIndices, IntList cornerIndices, IntList intersectionIndices, BlockFace currentFace, boolean finalRoom) {
        if (finalRoom) {
            if (currentFace == BlockFace.SOUTH) {
                return hallwayIndices.getInt(random.nextInt(hallwayIndices.size()));
            }

            return cornerIndices.getInt(random.nextInt(cornerIndices.size()));
        }

        int bound = currentFace == BlockFace.SOUTH ? hallwayIndices.size() + cornerIndices.size() + intersectionIndices.size() : hallwayIndices.size() + cornerIndices.size();

        int choiceIndex = random.nextInt(bound);
        if (choiceIndex < hallwayIndices.size()) {
            return hallwayIndices.getInt(choiceIndex);
        } else if (choiceIndex < hallwayIndices.size() + cornerIndices.size()) {
            return cornerIndices.getInt(choiceIndex - hallwayIndices.size());
        } else {
            return intersectionIndices.getInt(choiceIndex - hallwayIndices.size() - cornerIndices.size());
        }
    }

    private boolean checkShouldInvert(BlockFace currentFace, boolean isCorner) {
        if (isCorner) {
            if (currentFace == BlockFace.WEST) {
                return false;
            } else if (currentFace == BlockFace.EAST) {
                return true;
            } else {
                return random.nextBoolean();
            }
        } else {
            return random.nextBoolean();
        }
    }

    @Depend
    public static class Module implements DependencyModule {

        private final DoorsMap map;

        private final HidingSpotManager hidingSpotManager;

        private final Instance instance;

        private final EventNode<Event> gameNode;

        public Module(DoorsMap map, HidingSpotManager hidingSpotManager, Instance instance, EventNode<Event> gameNode) {
            this.map = map;
            this.hidingSpotManager = hidingSpotManager;
            this.instance = instance;
            this.gameNode = gameNode;
        }

        public DoorsMap getMap() {
            return map;
        }

        public HidingSpotManager getClosetManager() {
            return hidingSpotManager;
        }

        public Instance getInstance() {
            return instance;
        }

        public EventNode<Event> getGameNode() {
            return gameNode;
        }
    }

}
