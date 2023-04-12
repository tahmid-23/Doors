package com.github.tahmid_23.doors.game.map;

import com.github.tahmid_23.doors.game.map.config.MapConfig;
import com.github.tahmid_23.doors.game.map.room.RoomInfoContext;
import com.github.tahmid_23.doors.structure.Structure;
import net.kyori.adventure.key.Key;

import java.util.Map;

public record DoorsMap(MapConfig mapConfig, RoomInfoContext roomInfoContext, Map<Key, Structure> extraStructures) {

}
