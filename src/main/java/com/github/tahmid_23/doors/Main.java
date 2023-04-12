package com.github.tahmid_23.doors;

import com.github.steanky.element.core.context.ContextManager;
import com.github.steanky.element.core.key.BasicKeyParser;
import com.github.steanky.element.core.key.KeyParser;
import com.github.steanky.ethylene.codec.toml.TomlCodec;
import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.MappingProcessorSource;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.tahmid_23.doors.command.DoorsCommand;
import com.github.tahmid_23.doors.config.ServerConfig;
import com.github.tahmid_23.doors.game.DoorsGameCreator;
import com.github.tahmid_23.doors.game.DoorsGameManager;
import com.github.tahmid_23.doors.game.generation.RoomGenerator;
import com.github.tahmid_23.doors.game.generation.transform.ClosetTransform;
import com.github.tahmid_23.doors.game.generation.transform.DoorTransform;
import com.github.tahmid_23.doors.game.generation.transform.RemoveTransform;
import com.github.tahmid_23.doors.game.generation.transform.SignTransform;
import com.github.tahmid_23.doors.game.map.DoorsMap;
import com.github.tahmid_23.doors.game.map.MapLoadException;
import com.github.tahmid_23.doors.game.map.MapLoader;
import com.github.tahmid_23.doors.game.map.config.MapConfig;
import com.github.tahmid_23.doors.game.map.config.RoomConfig;
import com.github.tahmid_23.doors.structure.StructureReader;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
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
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        Logger logger = LoggerFactory.getLogger("Doors");
        ConfigCodec codec = new TomlCodec();
        KeyParser keyParser = new BasicKeyParser("doors");
        Signature<Point> pointSignature = Signature.builder(Token.ofClass(Point.class), (ignored, pointArgs) -> new Vec(pointArgs.get(0), pointArgs.get(1), pointArgs.get(2)), point -> List.of(point.x(), point.y(), point.z()), Map.entry("x", Token.DOUBLE), Map.entry("y", Token.DOUBLE), Map.entry("z", Token.DOUBLE))
                .matchingNames().matchingTypeHints().build();
        Signature<Pos> posSignature = Signature.builder(Token.ofClass(Pos.class), (ignored, posArgs) -> new Pos(posArgs.get(0), posArgs.get(1), posArgs.get(2), posArgs.get(3), posArgs.get(4)), pos -> List.of(pos.x(), pos.y(), pos.z(), pos.yaw(), pos.pitch()), Map.entry("x", Token.DOUBLE), Map.entry("y", Token.DOUBLE), Map.entry("z", Token.DOUBLE), Map.entry("yaw", Token.FLOAT), Map.entry("pitch", Token.FLOAT))
                .matchingNames().matchingTypeHints().build();
        ScalarSignature<Key> keySignature = ScalarSignature.of(Token.ofClass(Key.class), element -> keyParser.parseKey(element.asString()),
                key -> key == null ? ConfigPrimitive.NULL : ConfigPrimitive.of(key.asString()));
        ScalarSignature<Block> blockSignature = ScalarSignature.of(Token.ofClass(Block.class), element -> Block.fromNamespaceId(element.asString()),
                block -> block == null ? ConfigPrimitive.NULL : ConfigPrimitive.of(block.namespace().asString()));
        MappingProcessorSource processorSource = MappingProcessorSource.builder()
                .withCustomSignatures(posSignature, pointSignature)
                .withScalarSignatures(keySignature, blockSignature)
                .withStandardSignatures()
                .withStandardTypeImplementations()
                .ignoringLengths()
                .build();

        ContextManager contextManager = ContextManager.builder("doors").withMappingProcessorSourceSupplier(() -> processorSource).build();
        contextManager.registerElementClass(ClosetTransform.class);
        contextManager.registerElementClass(DoorTransform.class);
        contextManager.registerElementClass(RemoveTransform.class);
        contextManager.registerElementClass(SignTransform.class);

        StructureReader structureReader = new StructureReader();
        ConfigProcessor<MapConfig> mapConfigProcessor = processorSource.processorFor(Token.ofClass(MapConfig.class));
        ConfigProcessor<RoomConfig> roomConfigProcessor = processorSource.processorFor(Token.ofClass(RoomConfig.class));
        MapLoader mapLoader = new MapLoader(structureReader, codec, mapConfigProcessor, roomConfigProcessor);

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
        logger.info("Loaded {} maps", maps.size());

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        EventNode<Event> globalNode = MinecraftServer.getGlobalEventHandler();

        Instance instance = instanceManager.createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));
        instance.setTimeRate(0);

        RoomGenerator generator = new RoomGenerator(contextManager, keyParser, new Random());
        DoorsGameCreator gameCreator = new DoorsGameCreator(MinecraftServer.getConnectionManager(), generator);
        DoorsGameManager gameManager = new DoorsGameManager(instanceManager, instance, gameCreator, globalNode);
        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(DoorsCommand.createCommand(gameManager, maps));

        MinecraftServer.getSchedulerManager().scheduleTask(gameManager::tick, TaskSchedule.immediate(), TaskSchedule.tick(1));

        globalNode.addListener(PlayerLoginEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(0, 40, 0));
        });
        globalNode.addListener(PlayerSpawnEvent.class, event -> {
            event.getPlayer().setGameMode(GameMode.CREATIVE);
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
