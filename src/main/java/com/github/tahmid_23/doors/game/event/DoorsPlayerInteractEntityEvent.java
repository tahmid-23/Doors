package com.github.tahmid_23.doors.game.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;

public class DoorsPlayerInteractEntityEvent implements Event {

    private final Player player;

    private final Entity target;

    public DoorsPlayerInteractEntityEvent(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public Player getPlayer() {
        return player;
    }

    public Entity getTarget() {
        return target;
    }
}
