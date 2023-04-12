package com.github.tahmid_23.doors.game.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;

public class DoorsPlayerSneakEvent implements Event {

    private final Player player;

    public DoorsPlayerSneakEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
