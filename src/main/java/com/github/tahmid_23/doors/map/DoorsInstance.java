package com.github.tahmid_23.doors.map;

import com.github.tahmid_23.doors.map.room.Room;
import net.minestom.server.instance.Instance;

import java.util.List;

public record DoorsInstance(Instance instance, List<Room> rooms) {

}
