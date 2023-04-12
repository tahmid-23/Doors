package com.github.tahmid_23.doors.game.map;

import com.github.tahmid_23.doors.game.map.room.Room;
import net.minestom.server.instance.Instance;

import java.util.List;

public record DoorsInstance(Instance instance, List<Room> rooms) {

}
