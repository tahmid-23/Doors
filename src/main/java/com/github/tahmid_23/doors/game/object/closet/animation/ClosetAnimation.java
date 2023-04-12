package com.github.tahmid_23.doors.game.object.closet.animation;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.FallingBlockMeta;
import net.minestom.server.sound.SoundEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ClosetAnimation {

    private final Collection<Entity> doorEntities;

    private final Pos start;

    private final Pos end;

    private final float reverseYaw;

    private final float reversePitch;

    private Player hider = null;

    private Entity camera = null;

    private long currentTick = 0L;

    private boolean hiding = false;

    private boolean inProgress = false;

    public ClosetAnimation(Collection<Entity> doorEntities, Point start, Point end) {
        this.doorEntities = doorEntities;
        this.start = Pos.fromPoint(start).withLookAt(end);
        this.end = Pos.fromPoint(end);
        Pos reverse = this.end.withLookAt(this.start);
        this.reverseYaw = reverse.yaw();
        this.reversePitch = reverse.pitch();
    }

    public void hide(Player hider) {
        this.hider = hider;
        hider.setInvisible(true);
        hider.teleport(start).thenRun(() -> hider.setView(start.pitch(), start.yaw()));

        camera = new Entity(EntityType.ZOMBIE);
        camera.setNoGravity(true);
        camera.setInstance(hider.getInstance(), start).thenRun(() -> hider.spectate(camera));

        currentTick = 0L;
        hiding = true;
        inProgress = true;
        changeDoors(true, true);
    }

    public void unhide() {
        currentTick = 0L;
        hiding = false;
        inProgress = true;
        changeDoors(true, true);
    }

    public void reset() {
        resetState();
        changeDoors(false, false);
    }

    private void resetState() {
        if (hider != null) {
            hider.setInvisible(false);
            hider.stopSpectating();
            hider = null;
        }
        if (camera != null) {
            camera.remove();
            camera = null;
        }
        currentTick = 0L;
        hiding = false;
        inProgress = false;
    }

    public void tick() {
        if (!isInProgress()) {
            return;
        }

        ++currentTick;
        if (hiding) {
            if (currentTick <= 10) {
                float pitch = start.pitch() + (end.pitch() - start.pitch()) * currentTick / 10.0F;
                Pos pos = new Pos(start.x() + (end.x() - start.x()) * currentTick / 10.0, start.y() + (end.y() - start.y()) * currentTick / 10.0, start.z() + (end.z() - start.z()) * currentTick / 10.0, start.yaw(), pitch);
                camera.setView(start.yaw(), pitch);
                camera.teleport(pos);
            } else if (currentTick <= 20) {
                float yaw = start.yaw() + (end.yaw() - start.yaw()) * (currentTick - 10) / 10.0F;
                Pos pos = end.withYaw(yaw);
                camera.setView(yaw, end.pitch());
                camera.teleport(pos);
            }
            if (currentTick == 20L) {
                inProgress = false;
                currentTick = 0L;
                changeDoors(false, true);
            }
        } else {
            if (currentTick <= 10) {
                float pitch = reversePitch + (0.0F - reversePitch) * currentTick / 10.0F;
                Pos pos = new Pos(end.x() + (start.x() - end.x()) * currentTick / 10.0, end.y() + (start.y() - end.y()) * currentTick / 10.0, end.z() + (start.z() - end.z()) * currentTick / 10.0, reverseYaw, pitch);
                camera.setView(reverseYaw, pitch);
                camera.teleport(pos);
            }
            if (currentTick == 10L) {
                hider.teleport(start.withView(reverseYaw, 0.0F));
                hider.setInvisible(false);
                hider.stopSpectating();
                hider = null;

                camera.remove();
                camera = null;
            }
            if (currentTick == 20L) {
                resetState();
                changeDoors(false, true);
            }
        }
    }

    public boolean isInProgress() {
        return inProgress;
    }

    private void changeDoors(boolean open, boolean playSound) {
        Collection<Entity> newEntities = new ArrayList<>();
        for (Entity oldEntity : doorEntities) {
            FallingBlockMeta oldMeta = (FallingBlockMeta) oldEntity.getEntityMeta();

            Entity newEntity = new Entity(EntityType.FALLING_BLOCK);
            newEntity.setNoGravity(true);
            FallingBlockMeta newMeta = (FallingBlockMeta) newEntity.getEntityMeta();
            newMeta.setBlock(oldMeta.getBlock().withProperty("open", open ? "true" : "false"));
            newMeta.setSpawnPosition(oldMeta.getSpawnPosition());
            CompletableFuture<?> future = newEntity.setInstance(oldEntity.getInstance(), oldEntity.getPosition());
            if (playSound) {
                future.thenRun(() -> {
                    newEntity.getInstance().playSound(Sound.sound(
                            open ? SoundEvent.BLOCK_WOODEN_DOOR_OPEN : SoundEvent.BLOCK_WOODEN_DOOR_CLOSE,
                            Sound.Source.BLOCK,
                            1.0F,
                            1.0F
                    ), newEntity);
                });
            }

            oldEntity.remove();
            newEntities.add(newEntity);
        }

        doorEntities.clear();
        doorEntities.addAll(newEntities);
    }

}
