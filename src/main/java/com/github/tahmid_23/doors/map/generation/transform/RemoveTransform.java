package com.github.tahmid_23.doors.map.generation.transform;

import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;

@Model("doors:block_transform.remove")
public class RemoveTransform implements BlockTransform {

    private final Block targetBlock;

    @FactoryMethod
    public RemoveTransform(Data data) {
        this.targetBlock = data.targetBlock();
    }

    @Override
    public void transform(Block.Setter setter, Block block, Point location, int roomNumber) {

    }

    @Override
    public boolean isValidBlock(Block block) {
        return block.compare(targetBlock);
    }

    @DataObject
    public record Data(Block targetBlock) {

    }

}
