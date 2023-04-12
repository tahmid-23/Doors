package com.github.tahmid_23.doors.map.config;

import com.github.steanky.ethylene.core.collection.ConfigContainer;

import java.util.List;

public record MapConfig(String name, List<RequiredRoom> requiredRooms, ConfigContainer transforms, int roomCount, int paletteIndex) {
}
