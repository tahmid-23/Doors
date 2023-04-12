package com.github.tahmid_23.doors.map.generation.transform;

import com.github.steanky.element.core.annotation.Model;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;

@Model("doors:block_transform")
public interface BlockTransform {

    void transform(Block.Setter setter, Block block, Point location, int roomNumber);

    boolean isValidBlock(Block block);

}
