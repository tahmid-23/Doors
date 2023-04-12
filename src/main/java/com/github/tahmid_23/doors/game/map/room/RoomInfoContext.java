package com.github.tahmid_23.doors.game.map.room;

import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.key.Key;

import java.util.List;
import java.util.Map;

public record RoomInfoContext(List<RoomInfo> randomRooms, Map<Key, RoomInfo> nameToRoom, Map<RoomType, IntList> indices) {

}
