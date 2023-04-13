package com.github.tahmid_23.doors.game;

import com.github.tahmid_23.doors.game.event.DoorsPlayerInteractEntityEvent;
import com.github.tahmid_23.doors.game.event.DoorsPlayerLeaveEvent;
import com.github.tahmid_23.doors.game.event.DoorsPlayerSneakEvent;
import com.github.tahmid_23.doors.game.generation.RoomGenerator;
import com.github.tahmid_23.doors.game.map.DoorsMap;
import com.github.tahmid_23.doors.game.monster.HideMonster;
import com.github.tahmid_23.doors.game.object.closet.HidingSpotManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.ConnectionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DoorsGameCreator {

    private final ConnectionManager connectionManager;

    private final RoomGenerator generator;

    public DoorsGameCreator(ConnectionManager connectionManager, RoomGenerator generator) {
        this.connectionManager = connectionManager;
        this.generator = generator;
    }

    public CompletableFuture<DoorsGame> createGame(DoorsMap map, Instance instance, EventNode<Event> gameNode) {
        Set<UUID> players = new HashSet<>();
        HidingSpotManager hidingSpotManager = new HidingSpotManager(connectionManager, players, 4.0);
        HideMonster hideMonster = new HideMonster(hidingSpotManager);
        return generator.generate(map, hidingSpotManager, instance, gameNode).thenApply(doorsInstance -> {
            DoorsGame game = new DoorsGame(connectionManager, players, doorsInstance, List.of(hidingSpotManager, hideMonster));

            gameNode.addListener(PlayerDisconnectEvent.class, event -> {
                if (game.getPlayers().contains(event.getPlayer().getUuid())) {
                    gameNode.call(new DoorsPlayerLeaveEvent(event.getPlayer()));
                    game.removePlayer(event.getPlayer().getUuid());
                }
            });
            gameNode.addListener(PlayerSpawnEvent.class, event -> {
                if (event.getSpawnInstance() != instance && game.getPlayers().contains(event.getPlayer().getUuid())) {
                    gameNode.call(new DoorsPlayerLeaveEvent(event.getPlayer()));
                    game.removePlayer(event.getPlayer().getUuid());
                }
            });
            gameNode.addListener(PlayerEntityInteractEvent.class, event -> {
                if (game.getPlayers().contains(event.getPlayer().getUuid()) && event.getHand() == Player.Hand.MAIN) {
                    gameNode.call(new DoorsPlayerInteractEntityEvent(event.getPlayer(), event.getTarget()));
                }
            });
            gameNode.addListener(PlayerStartSneakingEvent.class, event -> {
                if (game.getPlayers().contains(event.getPlayer().getUuid())) {
                    gameNode.call(new DoorsPlayerSneakEvent(event.getPlayer()));
                }
            });

            return game;
        });
    }

}
