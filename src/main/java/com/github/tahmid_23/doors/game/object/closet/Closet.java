package com.github.tahmid_23.doors.game.object.closet;

import com.github.tahmid_23.doors.game.event.DoorsPlayerInteractEntityEvent;
import com.github.tahmid_23.doors.game.event.DoorsPlayerLeaveEvent;
import com.github.tahmid_23.doors.game.event.DoorsPlayerSneakEvent;
import com.github.tahmid_23.doors.game.object.closet.animation.ClosetAnimation;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

import java.util.Collection;
import java.util.Optional;

public class Closet implements HidingSpot {

    private final Collection<Entity> doorEntities;

    private final ClosetAnimation animation;

    private final Point source;

    private Player hider = null;

    private long hideDuration = 0L;

    public Closet(Collection<Entity> doorEntities, ClosetAnimation animation, Point source) {
        this.doorEntities = doorEntities;
        this.animation = animation;
        this.source = source;
    }

    @Override
    public void hidePlayer(Player player) {
        hider = player;
        animation.reset();
        animation.hide(player);
    }

    @Override
    public Optional<Player> getHider() {
        return Optional.ofNullable(hider);
    }

    @Override
    public void unhide() {
        hider = null;
        if (animation.isInProgress()) {
            animation.reset();
        } else {
            animation.unhide();
        }
        hideDuration = 0L;
    }

    @Override
    public boolean canHide() {
        return hider == null && !animation.isInProgress();
    }

    @Override
    public long getHideDuration() {
        return hideDuration;
    }

    @Override
    public void tick() {
        if (hider != null) {
            ++hideDuration;
        }
        animation.tick();
    }

    @Override
    public Point getSource() {
        return source;
    }

    public void onPlayerLeave(DoorsPlayerLeaveEvent event) {
        if (event.player() == hider) {
            hider = null;
            animation.reset();
            hideDuration = 0L;
        }
    }

    public void onInteract(DoorsPlayerInteractEntityEvent event) {
        for (Entity door : doorEntities) {
            if (door.getUuid().equals(event.getTarget().getUuid())) {
                if (canHide()) {
                    hidePlayer(event.getPlayer());
                }

                break;
            }
        }
    }

    public void onPlayerSneak(DoorsPlayerSneakEvent event) {
        if (event.getPlayer() == hider && !animation.isInProgress()) {
            unhide();
        }
    }

}
