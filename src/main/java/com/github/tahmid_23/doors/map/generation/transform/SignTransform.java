package com.github.tahmid_23.doors.map.generation.transform;

import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.tahmid_23.doors.block.SignBlockHandler;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTString;

import java.util.Collection;
import java.util.Map;

@Model("doors:block_transform.sign")
public class SignTransform implements BlockTransform {

    private static final BlockHandler SIGN_BLOCK_HANDLER = new SignBlockHandler();

    private final Collection<Block> targetBlocks;

    @FactoryMethod
    public SignTransform(Data data) {
        this.targetBlocks = data.targetBlocks();
    }

    @Override
    public void transform(Block.Setter setter, Block block, Point location, int roomNumber) {
        NBTCompound textNBT = new NBTCompound(Map.of("Text1", new NBTString("{\"text\":\"" + (roomNumber + 1) + "\"}")));
        setter.setBlock(location, block.withHandler(SIGN_BLOCK_HANDLER).withNbt(textNBT));
    }

    @Override
    public boolean isValidBlock(Block block) {
        for (Block targetBlock : targetBlocks) {
            if (targetBlock.compare(block)) {
                return true;
            }
        }

        return false;
    }

    @DataObject
    public record Data(Collection<Block> targetBlocks) {

    }

}
