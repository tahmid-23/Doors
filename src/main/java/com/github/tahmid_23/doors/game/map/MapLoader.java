package com.github.tahmid_23.doors.game.map;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.tahmid_23.doors.game.map.config.MapConfig;
import com.github.tahmid_23.doors.game.map.config.PoolType;
import com.github.tahmid_23.doors.game.map.config.RoomConfig;
import com.github.tahmid_23.doors.game.map.room.Exit;
import com.github.tahmid_23.doors.game.map.room.RoomInfo;
import com.github.tahmid_23.doors.game.map.room.RoomInfoContext;
import com.github.tahmid_23.doors.game.map.room.RoomType;
import com.github.tahmid_23.doors.structure.Structure;
import com.github.tahmid_23.doors.structure.StructureFormatException;
import com.github.tahmid_23.doors.structure.StructureReader;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.jglrxavpok.hephaistos.nbt.NBTReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class MapLoader {

    private final StructureReader structureReader;

    private final ConfigCodec codec;

    private final ConfigProcessor<MapConfig> mapConfigProcessor;

    private final ConfigProcessor<RoomConfig> roomConfigProcessor;

    public MapLoader(StructureReader structureReader, ConfigCodec codec, ConfigProcessor<MapConfig> mapConfigProcessor, ConfigProcessor<RoomConfig> roomConfigProcessor) {
        this.structureReader = structureReader;
        this.codec = codec;
        this.mapConfigProcessor = mapConfigProcessor;
        this.roomConfigProcessor = roomConfigProcessor;
    }

    public DoorsMap loadMap(Path mapPath) throws MapLoadException {
        MapConfig mapConfig = loadMapConfig(mapPath);

        List<RoomInfo> randomRooms = new ArrayList<>();
        Map<Key, RoomInfo> nameToRoom = new HashMap<>();
        Map<RoomType, IntList> indices = new EnumMap<>(RoomType.class);
        try {
            try (Stream<Path> files = Files.list(mapPath.resolve("rooms"))) {
                int randomRoomCount = 0;
                for (Path roomPath : (Iterable<? extends Path>) files::iterator) {
                    RoomConfig roomConfig;
                    try {
                        roomConfig = Configuration.read(roomPath, codec, roomConfigProcessor);
                    } catch (IOException e) {
                        throw new MapLoadException("Failed to load room config in " + roomPath, e);
                    }

                    Path structurePath = mapPath.resolve("structures/" + roomConfig.roomKey().namespace() + "/" + roomConfig.roomKey().value() + ".nbt");
                    Structure structure = loadStructure(structurePath);
                    RoomInfo roomInfo = createRoomInfo(structure, mapConfig.paletteIndex());
                    nameToRoom.put(roomConfig.roomKey(), roomInfo);

                    if (roomConfig.poolType() == PoolType.RANDOM) {
                        randomRooms.add(roomInfo);
                        indices.computeIfAbsent(roomInfo.roomType(), type -> new IntArrayList()).add(randomRoomCount);
                        ++randomRoomCount;
                    }
                }
            }
        } catch (IOException e) {
            throw new MapLoadException("Failed to list files in " + mapPath, e);
        }

        Map<Key, Structure> extraStructures = new HashMap<>(mapConfig.extraStructures().size());
        for (Key structureKey : mapConfig.extraStructures()) {
            Path structurePath = mapPath.resolve("structures/" + structureKey.namespace() + "/" + structureKey.value() + ".nbt");
            Structure structure = loadStructure(structurePath);
            extraStructures.put(structureKey, structure);
        }

        RoomInfoContext roomInfoContext = new RoomInfoContext(randomRooms, nameToRoom, indices);
        return new DoorsMap(mapConfig, roomInfoContext, extraStructures);
    }

    private MapConfig loadMapConfig(Path mapPath) throws MapLoadException {
        String extension = codec.getPreferredExtension();
        String fileName = extension.isEmpty() ? "config" : "config." + extension;

        try {
            return Configuration.read(mapPath.resolve(fileName), codec, mapConfigProcessor);
        } catch (IOException e) {
            throw new MapLoadException("Failed to load map config", e);
        }
    }

    private Structure loadStructure(Path structurePath) throws MapLoadException {
        try (NBTReader nbtReader = new NBTReader(structurePath)) {
            NBTCompound structureNBT = (NBTCompound) nbtReader.read();
            return structureReader.readStructure(structureNBT);
        } catch (IOException | NBTException | StructureFormatException e) {
            throw new MapLoadException("Failed to read structure from " + structurePath, e);
        }
    }

    private RoomInfo createRoomInfo(Structure structure, int paletteIndex) {
        Point[] blockPositions = structure.blockPositions();
        int[] blockStates = structure.blockStates();
        Block[] palette = structure.palettes()[paletteIndex];

        Point entrance = null;
        List<Exit> exits = new ArrayList<>();
        for (int j = 0; j < blockStates.length; ++j) {
            Block block = palette[blockStates[j]];
            if (block.compare(Block.REDSTONE_BLOCK)) {
                entrance = blockPositions[j];
            } else if (block.compare(Block.DISPENSER)) {
                String facing = block.getProperty("facing");
                BlockFace exitFace = switch (facing) {
                    case "north" -> BlockFace.NORTH;
                    case "south" -> BlockFace.SOUTH;
                    case "west" -> BlockFace.WEST;
                    case "east" -> BlockFace.EAST;
                    default -> null;
                };

                exits.add(new Exit(blockPositions[j], exitFace));
            }
        }

        RoomType roomType;
        if (exits.size() == 1) {
            if (exits.get(0).face() == BlockFace.SOUTH) {
                roomType = RoomType.HALLWAY;
            } else {
                roomType = RoomType.CORNER;
            }
        } else {
            roomType = RoomType.INTERSECTION;
        }

        return new RoomInfo(roomType, structure, entrance, exits);
    }

}
