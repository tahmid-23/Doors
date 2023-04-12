package com.github.tahmid_23.doors.game.generation.transform;

import com.github.steanky.element.core.annotation.Model;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

@Model("doors:block_transform")
public interface BlockTransform {

    void transform(Block.Setter setter, Block block, Point location, BlockFace currentFace, boolean shouldInvert, int roomNumber);

    boolean isValidBlock(Block block);

}
