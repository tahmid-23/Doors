package com.github.tahmid_23.doors.game.generation.transform;

import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.tahmid_23.doors.block.DoorBlockHandler;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;

import java.util.Map;

@Model("block_transform.door")
public class DoorTransform implements BlockTransform {

    private static final BlockHandler DOOR_BLOCK_HANDLER = new DoorBlockHandler();

    private final Block targetBlock;

    private final Block doorBlock;

    @FactoryMethod
    public DoorTransform(Data data) {
        this.targetBlock = data.targetBlock();
        this.doorBlock = data.doorBlock();
    }

    @Override
    public void transform(Block.Setter setter, Block block, Point location, BlockFace currentFace, boolean shouldInvert, int roomNumber) {
        Block lower = doorBlock.withProperties(Map.of(
                "facing", block.getProperty("facing"),
                "half", "lower"
        )).withHandler(DOOR_BLOCK_HANDLER);
        setter.setBlock(location, lower);

        Block upper = doorBlock.withProperties(Map.of(
                "facing", block.getProperty("facing"),
                "half", "upper"
        )).withHandler(DOOR_BLOCK_HANDLER);
        setter.setBlock(location.withY(location.y() + 1), upper);
    }

    @Override
    public boolean isValidBlock(Block block) {
        return block.compare(targetBlock);
    }

    @DataObject
    public record Data(Block targetBlock, Block doorBlock) {

    }

}
