package com.github.tahmid_23.doors.game;

import com.github.tahmid_23.doors.map.DoorsMap;
import com.github.tahmid_23.doors.map.generation.RoomGenerator;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.ConnectionManager;

import java.util.concurrent.CompletableFuture;

public class DoorsGameCreator {

    private final ConnectionManager connectionManager;

    private final RoomGenerator generator;

    public DoorsGameCreator(ConnectionManager connectionManager, RoomGenerator generator) {
        this.connectionManager = connectionManager;
        this.generator = generator;
    }

    public CompletableFuture<DoorsGame> createGame(DoorsMap map, Instance instance, EventNode<Event> gameNode) {
        return generator.generate(map, instance).thenApply(doorsInstance -> {
            DoorsGame game = new DoorsGame(connectionManager, doorsInstance);

            gameNode.addListener(PlayerDisconnectEvent.class, event -> {
                if (game.getPlayers().contains(event.getPlayer().getUuid())) {
                    game.removePlayer(event.getPlayer().getUuid());
                }
            });
            gameNode.addListener(PlayerSpawnEvent.class, event -> {
                if (event.getSpawnInstance() != instance && game.getPlayers().contains(event.getPlayer().getUuid())) {
                    game.removePlayer(event.getPlayer().getUuid());
                }
            });

            return game;
        });
    }

}
