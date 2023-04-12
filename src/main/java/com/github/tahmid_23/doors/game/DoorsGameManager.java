package com.github.tahmid_23.doors.game;

import com.github.tahmid_23.doors.game.map.DoorsMap;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DoorsGameManager {

    private final Collection<GameContext> contexts = new LinkedList<>();

    private final InstanceManager instanceManager;

    private final Instance fallbackInstance;

    private final DoorsGameCreator gameCreator;

    private final EventNode<Event> parentNode;

    public DoorsGameManager(InstanceManager instanceManager, Instance fallbackInstance, DoorsGameCreator gameCreator, EventNode<Event> parentNode) {
        this.instanceManager = instanceManager;
        this.fallbackInstance = fallbackInstance;
        this.gameCreator = gameCreator;
        this.parentNode = parentNode;
    }

    public CompletableFuture<DoorsGame> createGame(DoorsMap map) {
        Instance instance = instanceManager.createInstanceContainer();
        instance.setTimeRate(0);
        EventNode<Event> gameNode = EventNode.all(UUID.randomUUID().toString());
        parentNode.addChild(gameNode);
        CompletableFuture<DoorsGame> gameFuture = gameCreator.createGame(map, instance, gameNode);
        gameFuture.thenAccept(game -> {
            contexts.add(new GameContext(game, gameNode));
        });

        return gameFuture;
    }

    public void tick() {
        for (Iterator<GameContext> iterator = contexts.iterator(); iterator.hasNext(); ) {
            GameContext context = iterator.next();

            if (context.game().isComplete()) {
                parentNode.removeChild(context.gameNode());
                CompletableFuture<?>[] futures = new CompletableFuture<?>[context.game().getDoorsInstance().instance().getPlayers().size()];
                int i = -1;
                for (Player player : context.game().getDoorsInstance().instance().getPlayers()) {
                    futures[++i] = player.setInstance(fallbackInstance, new Vec(0, 40, 0));
                }
                CompletableFuture.allOf(futures).thenRun(() -> {
                    instanceManager.unregisterInstance(context.game().getDoorsInstance().instance());
                });

                iterator.remove();
                continue;
            }

            context.game().tick();
        }
    }

    private record GameContext(DoorsGame game, EventNode<Event> gameNode) {

    }

}
