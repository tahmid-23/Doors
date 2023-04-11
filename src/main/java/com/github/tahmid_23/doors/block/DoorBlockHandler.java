package com.github.tahmid_23.doors.block;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.Batch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public class DoorBlockHandler implements BlockHandler {

    public static final NamespaceID HANDLER_ID = NamespaceID.from("doors", "block_handler.door");

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        Block block = interaction.getBlock();
        String newPropertyValue = block.getProperty("open").equals("true") ? "false" : "true";

        Instance instance = interaction.getInstance();
        Point firstPoint = interaction.getBlockPosition();
        instance.setBlock(firstPoint, block.withProperty("open", newPropertyValue));
        Point secondPoint;
        if (block.getProperty("half").equals("lower")) {
            secondPoint = firstPoint.withY(firstPoint.y() + 1);
        } else {
            secondPoint = firstPoint.withY(firstPoint.y() - 1);
        }
        Block secondBlock = instance.getBlock(secondPoint);
        if (secondBlock.compare(block)) {
            instance.setBlock(secondPoint, instance.getBlock(secondPoint).withProperty("open", newPropertyValue));
        }

        return true;
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return HANDLER_ID;
    }
}
