package com.github.tahmid_23.doors.game.generation.transform;

import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.tahmid_23.doors.game.event.DoorsPlayerInteractEntityEvent;
import com.github.tahmid_23.doors.game.event.DoorsPlayerLeaveEvent;
import com.github.tahmid_23.doors.game.event.DoorsPlayerSneakEvent;
import com.github.tahmid_23.doors.game.generation.BlockAdjustment;
import com.github.tahmid_23.doors.game.map.DoorsMap;
import com.github.tahmid_23.doors.game.object.closet.HidingSpotManager;
import com.github.tahmid_23.doors.game.object.closet.Closet;
import com.github.tahmid_23.doors.game.object.closet.animation.ClosetAnimation;
import com.github.tahmid_23.doors.structure.Structure;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.FallingBlockMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.ArrayList;
import java.util.Collection;

@Model("doors:block_transform.closet")
public class ClosetTransform implements BlockTransform {

    private final HidingSpotManager hidingSpotManager;

    private final Instance instance;

    private final EventNode<Event> gameNode;

    private final Structure structure;

    private final Block targetBlock;

    private final Point sourceDelta;

    private final Pos interiorDelta;

    private final int paletteIndex;

    public ClosetTransform(HidingSpotManager hidingSpotManager, Instance instance, EventNode<Event> gameNode, Structure structure, Block targetBlock, Point sourceDelta, Pos interiorDelta, int paletteIndex) {
        this.hidingSpotManager = hidingSpotManager;
        this.instance = instance;
        this.gameNode = gameNode;
        this.structure = structure;
        this.targetBlock = targetBlock;
        this.sourceDelta = sourceDelta;
        this.interiorDelta = interiorDelta;
        this.paletteIndex = paletteIndex;
    }

    @FactoryMethod
    public ClosetTransform(Data data, DoorsMap map, HidingSpotManager hidingSpotManager, Instance instance, EventNode<Event> gameNode) {
        this(hidingSpotManager, instance, gameNode, map.extraStructures().get(data.structureKey()), data.targetBlock(), data.sourceDelta(), data.interiorDelta(), map.mapConfig().paletteIndex());
    }

    @Override
    public void transform(Block.Setter setter, Block block, Point location, BlockFace currentFace, boolean shouldInvert, int roomNumber) {
        BlockFace facing = switch (block.getProperty("facing")) {
            case "north" -> BlockFace.NORTH;
            case "south" -> BlockFace.SOUTH;
            case "west" -> BlockFace.WEST;
            case "east" -> BlockFace.EAST;
            default -> null;
        };

        Collection<Entity> doorEntities = new ArrayList<>();
        Point[] blockPositions = structure.blockPositions();
        int[] blockStates = structure.blockStates();
        Block[] palette = structure.palettes()[paletteIndex];
        NBTCompound[] blockEntityNBT = structure.blockEntityNBT();
        for (int j = 0; j < blockStates.length; ++j) {
            Block newBlock = palette[blockStates[j]];
            if (newBlock.compare(Block.STRUCTURE_VOID) || block.compare(Block.AIR)) {
                continue;
            }

            Point delta = blockPositions[j];
            delta = BlockAdjustment.adjustPoint(delta, facing, shouldInvert);
            Point actualPos = location.add(delta);

            NBTCompound nbt = blockEntityNBT[j];
            if (nbt != null) {
                newBlock = newBlock.withNbt(nbt);
            }

            String facingProperty = newBlock.getProperty("facing");
            if (facingProperty != null) {
                newBlock = newBlock.withProperty("facing", facingProperty = BlockAdjustment.adjustFacingProperty(facingProperty, facing, shouldInvert));
            }
            String hingeProperty = newBlock.getProperty("hinge");
            if (hingeProperty != null) {
                boolean left = hingeProperty.equals("left") == !shouldInvert;
                if (shouldInvert) {
                    newBlock = newBlock.withProperty("hinge", left ? "left" : "right");
                }

                Point doorDelta = switch (facingProperty) {
                    case "north" -> new Vec(-0.1, 0, 0);
                    case "south" -> new Vec(0.1, 0, 0);
                    case "west" -> new Vec(0, 0, 0.1);
                    case "east" -> new Vec(0, 0, -0.1);
                    default -> null;
                };

                if (!left) {
                    doorDelta = doorDelta.mul(-1);
                }

                Point entitySpawn = actualPos.add(doorDelta).add(0.5, 0, 0.5);
                Entity entity = new Entity(EntityType.FALLING_BLOCK);
                entity.setNoGravity(true);
                FallingBlockMeta meta = (FallingBlockMeta) entity.getEntityMeta();
                meta.setBlock(newBlock);
                meta.setSpawnPosition(entitySpawn);
                entity.setInstance(instance, entitySpawn);
                doorEntities.add(entity);

                continue;
            }

            setter.setBlock(actualPos, newBlock);
        }

        Point adjustedOrigin = BlockAdjustment.adjustPoint(new Vec(-0.5, 0, -0.5), facing, shouldInvert).add(location.x() + 0.5, 0, location.z() + 0.5);
        Point source = BlockAdjustment.adjustPoint(sourceDelta, facing, shouldInvert).add(adjustedOrigin);
        Point interior = BlockAdjustment.adjustPoint(interiorDelta, facing, shouldInvert).add(adjustedOrigin);
        Closet closet = new Closet(doorEntities, new ClosetAnimation(doorEntities, source, interior), source);
        gameNode.addListener(DoorsPlayerLeaveEvent.class, closet::onPlayerLeave);
        gameNode.addListener(DoorsPlayerInteractEntityEvent.class, closet::onInteract);
        gameNode.addListener(DoorsPlayerSneakEvent.class, closet::onPlayerSneak);
        hidingSpotManager.addHidingSpot(closet);
    }

    @Override
    public boolean isValidBlock(Block block) {
        return block.compare(targetBlock);
    }

    @DataObject
    public record Data(Block targetBlock, Key structureKey, Point sourceDelta, Pos interiorDelta) {

    }

}
