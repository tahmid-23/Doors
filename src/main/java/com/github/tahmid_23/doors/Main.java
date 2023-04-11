package com.github.tahmid_23.doors;

import com.github.steanky.ethylene.codec.toml.TomlCodec;
import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.MappingProcessorSource;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.tahmid_23.doors.command.DoorsCommand;
import com.github.tahmid_23.doors.config.ServerConfig;
import com.github.tahmid_23.doors.map.DoorsMap;
import com.github.tahmid_23.doors.map.MapConfig;
import com.github.tahmid_23.doors.map.MapLoadException;
import com.github.tahmid_23.doors.map.MapLoader;
import com.github.tahmid_23.doors.map.generation.InstanceGenerator;
import com.github.tahmid_23.doors.structure.StructureReader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        Logger logger = LoggerFactory.getLogger("Doors");
        ConfigCodec codec = new TomlCodec();
        MappingProcessorSource processorSource = MappingProcessorSource.builder()
                .withStandardSignatures()
                .withStandardTypeImplementations()
                .build();

        StructureReader structureReader = new StructureReader();
        ConfigProcessor<MapConfig> mapConfigProcessor = processorSource.processorFor(Token.ofClass(MapConfig.class));
        MapLoader mapLoader = new MapLoader(structureReader, codec, mapConfigProcessor);

        Map<String, DoorsMap> maps = new HashMap<>();
        try (Stream<Path> mapDirectory = Files.list(Path.of("./maps/"))) {
            for (Path path : (Iterable<? extends Path>) mapDirectory::iterator) {
                DoorsMap map;
                try {
                    map = mapLoader.loadMap(path);
                } catch (MapLoadException e) {
                    logger.warn("Failed to load map from " + path, e);
                    continue;
                }

                maps.put(map.mapConfig().name(), map);
            }
        } catch (IOException e) {
            return;
        }

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(DoorsCommand.createCommand(new InstanceGenerator(instanceManager, new Random()), maps));

        Instance instance = instanceManager.createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setTimeRate(0);

        EventNode<Event> globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(PlayerLoginEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(0, 40, 0));
        });
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            event.getPlayer().setGameMode(GameMode.CREATIVE);
            event.getPlayer().setFlying(true);
        });

        ConfigProcessor<ServerConfig> serverConfigProcessor = processorSource.processorFor(Token.ofClass(ServerConfig.class));
        ServerConfig serverConfig;
        try {
            serverConfig = Configuration.read(Path.of("./server.toml"), codec, serverConfigProcessor);
        } catch (IOException e) {
            logger.warn("Failed to load server config", e);
            return;
        }

        MojangAuth.init();
        server.start("0.0.0.0", serverConfig.port());
    }

}
