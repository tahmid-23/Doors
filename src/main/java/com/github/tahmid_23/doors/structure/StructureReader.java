package com.github.tahmid_23.doors.structure;

import com.github.tahmid_23.doors.bounds.Bounds;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.jglrxavpok.hephaistos.nbt.*;

import java.util.HashMap;
import java.util.Map;

public class StructureReader {

    public Structure readStructure(NBTCompound structureNBT) throws StructureFormatException {
        NBTList<NBTList<NBTCompound>> palettesNBT = null;

        NBT potentialPalettesNBT = structureNBT.get("palettes");
        if (potentialPalettesNBT != null) {
            if (!(potentialPalettesNBT instanceof NBTList<?> newPalettesNBT)) {
                throw new StructureFormatException("Expected \"palettes\" to be " + NBTType.TAG_List.getReadableName() + ", was " + potentialPalettesNBT.getID().getReadableName());
            }

            palettesNBT = newPalettesNBT.asListOf();
        } else {
            NBT potentialPaletteNBT = structureNBT.get("palette");
            if (potentialPaletteNBT != null) {
                if (!(potentialPaletteNBT instanceof NBTList<?> newPaletteNBT)) {
                    throw new StructureFormatException("Expected \"palette\" to be " + NBTType.TAG_List.getReadableName() + ", was " + potentialPaletteNBT.getID().getReadableName());
                }

                palettesNBT = NBT.List(NBTType.TAG_List, newPaletteNBT).asListOf();
            }
        }
        if (palettesNBT == null) {
            throw new StructureFormatException("Missing \"palettes\" and \"palette\" NBT");
        }

        NBTList<NBTInt> sizeNBT = structureNBT.getList("size");
        if (sizeNBT == null) {
            throw new StructureFormatException("Missing \"size\" NBT");
        }
        if (sizeNBT.getSize() != 3) {
            throw new StructureFormatException("Expected \"size\" NBT to have length 3, was " + sizeNBT.getSize());
        }
        int width = sizeNBT.get(0).getValue();
        int height = sizeNBT.get(1).getValue();
        int length = sizeNBT.get(2).getValue();

        NBTList<NBTCompound> blocksNBT = structureNBT.getList("blocks");
        if (blocksNBT == null) {
            throw new StructureFormatException("Missing \"blocks\" NBT");
        }
        int blockCount = blocksNBT.getSize();
        Point[] blockPositions = new Vec[blockCount];
        int[] blockStates = new int[blockCount];
        NBTCompound[] blockEntityNBT = new NBTCompound[blockCount];

        for (int i = 0; i < blockCount; ++i) {
            NBTCompound block = blocksNBT.get(i);

            NBTList<NBTInt> pos = block.getList("pos");
            if (pos == null) {
                throw new StructureFormatException("Missing \"pos\" NBT");
            }
            if (pos.getSize() != 3) {
                throw new StructureFormatException("Expected \"size\" NBT to have length 3, was " + pos.getSize());
            }
            blockPositions[i] = new Vec(pos.get(0).getValue(), pos.get(1).getValue(), pos.get(2).getValue());

            Integer state = block.getAsInt("state");
            if (state == null) {
                throw new StructureFormatException("Missing \"state\" NBT");
            }
            blockStates[i] = state;

            if (block.containsKey("nbt")) {
                blockEntityNBT[i] = block.getCompound("nbt");
            }
        }

        Block[][] palettes = new Block[palettesNBT.getSize()][];
        for (int i = 0; i < palettesNBT.getSize(); ++i) {
            NBTList<NBTCompound> paletteNBT = palettesNBT.get(i);
            Block[] palette = palettes[i] = new Block[paletteNBT.getSize()];

            for (int j = 0; j < paletteNBT.getSize(); j++) {
                NBTCompound swatch = paletteNBT.get(j);
                String name = swatch.getString("Name");
                if (name == null) {
                    throw new StructureFormatException("Missing \"Name\" NBT");
                }

                Block block = Block.fromNamespaceId(name);
                if (block == null) {
                    throw new StructureFormatException("No block matches name \"" + name + '\"');
                }

                NBT potentialPropertiesNBT = swatch.get("Properties");
                if (potentialPropertiesNBT != null) {
                    if (!(potentialPropertiesNBT instanceof NBTCompound propertiesNBT)) {
                        throw new StructureFormatException("Expected \"Properties\" NBT to be " + NBTType.TAG_Compound.getReadableName() + ", was " + potentialPropertiesNBT.getID().getReadableName());
                    }
                    Map<String, String> propertyMap = new HashMap<>(propertiesNBT.getSize());
                    for (Map.Entry<? extends String, ? extends NBT> property : propertiesNBT) {
                        propertyMap.put(property.getKey(), ((NBTString) property.getValue()).getValue());
                    }

                    block = block.withProperties(propertyMap);
                }

                palette[j] = block;
            }
        }

        return new Structure(width, height, length, blockPositions, blockStates, blockEntityNBT, palettes);
    }

}
