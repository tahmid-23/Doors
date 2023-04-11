package com.github.tahmid_23.doors.map.room;

import com.github.tahmid_23.doors.structure.Structure;
import net.minestom.server.coordinate.Point;

import java.util.List;

public record RoomInfo(RoomType roomType, Structure structure, Point entrance, List<Exit> exits) {

}
