package com.github.tahmid_23.doors.game.object.closet.flashable;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;

public class BlockFlashable implements Flashable {

    private final Block.Setter instance;

    private final Point location;

    private final Block litBlock;

    private final Block unlitBlock;

    private final long duration;

    private long currentTick = 0;

    private long flashTick = -1;

    private boolean lit = true;

    public BlockFlashable(Block.Setter instance, Point location, Block litBlock, Block unlitBlock, long duration) {
        this.instance = instance;
        this.location = location;
        this.litBlock = litBlock;
        this.unlitBlock = unlitBlock;
        this.duration = duration;
    }

    @Override
    public void flash() {
        flashTick = currentTick;
    }

    public void tick() {
        ++currentTick;

        if (flashTick != -1 && currentTick - flashTick < duration) {
            if ((currentTick - flashTick) % 2 == 0) {
                lit = !lit;
            }

            if (lit) {
                instance.setBlock(location, litBlock);
            } else {
                instance.setBlock(location, unlitBlock);
            }
        } else if (currentTick - flashTick == duration) {
            lit = true;
            instance.setBlock(location, litBlock);
        }
    }

}
