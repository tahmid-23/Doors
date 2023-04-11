package com.github.tahmid_23.doors.structure;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

public record Structure(int width, int height, int length, Point[] blockPositions, int[] blockStates,
                        NBTCompound[] blockEntityNBT,
                        Block[][] palettes) {
}
