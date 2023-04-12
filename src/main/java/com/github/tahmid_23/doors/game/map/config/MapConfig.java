package com.github.tahmid_23.doors.game.map.config;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import net.kyori.adventure.key.Key;

import java.util.List;

public record MapConfig(String name, List<RequiredRoom> requiredRooms, List<Key> extraStructures, ConfigContainer transforms, int roomCount, int paletteIndex) {
}
