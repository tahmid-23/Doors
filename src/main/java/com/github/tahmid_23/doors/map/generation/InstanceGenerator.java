package com.github.tahmid_23.doors.map.generation;

import com.github.tahmid_23.doors.block.DoorBlockHandler;
import com.github.tahmid_23.doors.block.SignBlockHandler;
import com.github.tahmid_23.doors.map.DoorsMap;
import com.github.tahmid_23.doors.map.room.Exit;
import com.github.tahmid_23.doors.map.room.RoomInfo;
import com.github.tahmid_23.doors.map.room.RoomType;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.Batch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTString;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class InstanceGenerator {

    private static final BlockHandler DOOR_BLOCK_HANDLER = new DoorBlockHandler();

    private static final BlockHandler SIGN_BLOCK_HANDLER = new SignBlockHandler();

    private final InstanceManager instanceManager;

    private final Random random;

    public InstanceGenerator(InstanceManager instanceManager, Random random) {
        this.instanceManager = instanceManager;
        this.random = random;
    }

    public CompletableFuture<Instance> generate(DoorsMap map) {
        CompletableFuture<Instance> future = new CompletableFuture<>();
        Instance instance = instanceManager.createInstanceContainer();

        Batch<Runnable> batch = new AbsoluteBlockBatch();
        LongSet chunkIndices = new LongArraySet();

        Map<RoomType, IntList> indices = map.roomList().indices();
        IntList hallwayIndices = indices.get(RoomType.HALLWAY);
        IntList cornerIndices = indices.get(RoomType.CORNER);
        IntList intersectionIndices = indices.get(RoomType.INTERSECTION);

        BlockFace currentFace = BlockFace.SOUTH;
        Point currentPos = Vec.ZERO.relative(currentFace.getOppositeFace());
        for (int i = 0; i < map.mapConfig().roomCount(); ++i) {
            int structureIndex = chooseStructureIndex(hallwayIndices, cornerIndices, intersectionIndices, currentFace, i == map.mapConfig().roomCount() - 1);
            RoomInfo room = map.roomList().rooms().get(structureIndex);
            boolean isCorner = room.roomType() == RoomType.CORNER;
            boolean shouldInvert = checkShouldInvert(currentFace, isCorner);

            Point alignmentPos = currentPos.relative(currentFace);

            Point[] blockPositions = room.structure().blockPositions();
            int[] blockStates = room.structure().blockStates();
            Block[] palette = room.structure().palettes()[map.mapConfig().paletteIndex()];
            NBTCompound[] blockEntityNBT = room.structure().blockEntityNBT();
            for (int j = 0; j < blockStates.length; ++j) {
                Block block = palette[blockStates[j]];
                if (block.compare(Block.STRUCTURE_VOID)) {
                    continue;
                }

                Point delta = blockPositions[j].sub(room.entrance());
                delta = adjustPoint(delta, currentFace, shouldInvert);
                Point actualPos = alignmentPos.add(delta);
                chunkIndices.add(ChunkUtils.getChunkIndex(actualPos));

                NBTCompound nbt = blockEntityNBT[j];
                if (nbt != null) {
                    block = block.withNbt(nbt);
                }

                if (block.compare(Block.DISPENSER)) {
                    Block lower = Block.OAK_DOOR.withProperties(Map.of(
                            "facing", block.getProperty("facing"),
                            "half", "lower"
                    )).withHandler(DOOR_BLOCK_HANDLER);
                    batch.setBlock(actualPos, lower);

                    Block upper = Block.OAK_DOOR.withProperties(Map.of(
                            "facing", block.getProperty("facing"),
                            "half", "upper"
                    )).withHandler(DOOR_BLOCK_HANDLER);
                    batch.setBlock(actualPos.withY(actualPos.y() + 1), upper);
                } else if (block.compare(Block.OAK_WALL_SIGN)) {
                    NBTCompound textNBT = new NBTCompound(Map.of("Text1", new NBTString("{\"text\":\"" + (i + 1) + "\"}")));
                    block = block.withHandler(SIGN_BLOCK_HANDLER).withNbt(textNBT);
                }

                String facingProperty = block.getProperty("facing");
                if (facingProperty != null) {
                    block = block.withProperty("facing", adjustFacingProperty(facingProperty, currentFace, shouldInvert));
                }

                if (block.compare(Block.DISPENSER)) {
                    Block lower = Block.OAK_DOOR.withProperties(Map.of(
                            "facing", block.getProperty("facing"),
                            "half", "lower"
                    )).withHandler(DOOR_BLOCK_HANDLER);
                    batch.setBlock(actualPos, lower);

                    Block upper = Block.OAK_DOOR.withProperties(Map.of(
                            "facing", block.getProperty("facing"),
                            "half", "upper"
                    )).withHandler(DOOR_BLOCK_HANDLER);
                    batch.setBlock(actualPos.withY(actualPos.y() + 1), upper);
                } else if (!block.compare(Block.REDSTONE_BLOCK)) {
                    batch.setBlock(actualPos, block);
                }
            }

            int exitIndex = random.nextInt(room.exits().size());
            Exit exit = room.exits().get(exitIndex);
            Point exitDelta = exit.location().sub(room.entrance());
            exitDelta = adjustPoint(exitDelta, currentFace, shouldInvert);

            currentPos = alignmentPos.add(exitDelta);
            currentFace = adjustFace(exit.face(), currentFace, shouldInvert);
        }

        ChunkUtils.optionalLoadAll(instance, chunkIndices.toLongArray(), null).thenRun(() -> batch.apply(instance, () -> {
            future.complete(instance);
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
            if (currentFace == BlockFace.EAST) {
                return true;
            } else if (currentFace == BlockFace.WEST) {
                return false;
            } else {
                return random.nextBoolean();
            }
        } else {
            return random.nextBoolean();
        }
    }

    private Point adjustPoint(Point point, BlockFace newOrientation, boolean shouldInvert) {
        double x = shouldInvert ? -point.x() : point.x(), y = point.y(), z = point.z();

        switch (newOrientation) {
            case NORTH -> {
                return new Vec(-x, y, -z);
            }
            case SOUTH -> {
                return new Vec(x, y, z);
            }
            case EAST -> {
                return new Vec(z, y, -x);
            }
            case WEST -> {
                return new Vec(-z, y, x);
            }
        }

        return null;
    }

    private BlockFace adjustFace(BlockFace previousFace, BlockFace newOrientation, boolean shouldInvert) {
        if (shouldInvert && (previousFace == BlockFace.WEST || previousFace == BlockFace.EAST)) {
            previousFace = previousFace.getOppositeFace();
        }

        switch (previousFace) {
            case SOUTH -> {
                return newOrientation;
            }
            case NORTH -> {
                return newOrientation.getOppositeFace();
            }
            case WEST -> {
                switch (newOrientation) {
                    case SOUTH -> {
                        return BlockFace.WEST;
                    }
                    case NORTH -> {
                        return BlockFace.EAST;
                    }
                    case WEST -> {
                        return BlockFace.NORTH;
                    }
                    case EAST -> {
                        return BlockFace.SOUTH;
                    }
                }
            }
            case EAST -> {
                switch (newOrientation) {
                    case SOUTH -> {
                        return BlockFace.EAST;
                    }
                    case NORTH -> {
                        return BlockFace.WEST;
                    }
                    case WEST -> {
                        return BlockFace.SOUTH;
                    }
                    case EAST -> {
                        return BlockFace.NORTH;
                    }
                }
            }
        }

        return null;
    }

    private String adjustFacingProperty(String facingProperty, BlockFace newOrientation, boolean shouldInvert) {
        if (shouldInvert) {
            facingProperty = switch (facingProperty) {
                case "west" -> "east";
                case "east" -> "west";
                default -> facingProperty;
            };
        }
        switch (newOrientation) {
            case SOUTH -> {
                return facingProperty;
            }
            case NORTH -> {
                switch (facingProperty) {
                    case "north" -> {
                        return "south";
                    }
                    case "south" -> {
                        return "north";
                    }
                    case "west" -> {
                        return "east";
                    }
                    case "east" -> {
                        return "west";
                    }
                }
            }
            case WEST -> {
                switch (facingProperty) {
                    case "north" -> {
                        return "east";
                    }
                    case "south" -> {
                        return "west";
                    }
                    case "west" -> {
                        return "north";
                    }
                    case "east" -> {
                        return "south";
                    }
                }
            }
            case EAST -> {
                switch (facingProperty) {
                    case "north" -> {
                        return "west";
                    }
                    case "south" -> {
                        return "east";
                    }
                    case "west" -> {
                        return "south";
                    }
                    case "east" -> {
                        return "north";
                    }
                }
            }
        }

        return null;
    }

}
