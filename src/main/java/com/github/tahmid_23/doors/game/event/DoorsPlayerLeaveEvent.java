package com.github.tahmid_23.doors.game.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;

public record DoorsPlayerLeaveEvent(Player player) implements Event {

}
