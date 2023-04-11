package com.github.tahmid_23.doors.map.room;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import java.util.Map;

public record RoomList(List<RoomInfo> rooms, Map<RoomType, IntList> indices) {

}
