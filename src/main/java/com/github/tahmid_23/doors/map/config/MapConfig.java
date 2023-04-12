package com.github.tahmid_23.doors.map.config;

import java.util.List;

public record MapConfig(String name, List<RequiredRoom> requiredRooms, int roomCount, int paletteIndex) {
}
