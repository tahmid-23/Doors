package com.github.tahmid_23.doors.game;

import com.github.tahmid_23.doors.map.DoorsInstance;
import com.github.tahmid_23.doors.map.room.Room;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;

import java.util.*;

public class DoorsGame {

    private final ConnectionManager connectionManager;

    private final DoorsInstance doorsInstance;

    private final Set<UUID> players = new HashSet<>();

    private long currentTick = 0;

    private long winTick = -1;

    private boolean started = false;

    public DoorsGame(ConnectionManager connectionManager, DoorsInstance doorsInstance) {
        this.connectionManager = connectionManager;
        this.doorsInstance = doorsInstance;
    }

    public void addPlayer(Player player) {
        players.add(player.getUuid());
        player.setInstance(doorsInstance.instance(), Vec.ZERO);
        started = true;
    }

    public void removePlayer(UUID playerUUID) {
        players.remove(playerUUID);
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public void tick() {
        ++currentTick;
        if (winTick != -1) {
            return;
        }

        Room lastRoom = doorsInstance.rooms().get(doorsInstance.rooms().size() - 1);
        List<Player> winners = new ArrayList<>(players.size());
        boolean win = true;
        for (UUID playerUUID : players) {
            Player player = connectionManager.getPlayer(playerUUID);
            if (player == null) {
                continue;
            }

            if (!lastRoom.bounds().contains(player.getPosition())) {
                win = false;
                break;
            }

            winners.add(player);
        }

        if (win) {
            for (Player player : winners) {
                player.sendMessage(Component.text("You win!", NamedTextColor.GREEN));
            }

            winTick = currentTick;
        }
    }

    public boolean isComplete() {
        return (started && players.isEmpty()) || (winTick != -1 && currentTick - winTick > 100);
    }

    public DoorsInstance getDoorsInstance() {
        return doorsInstance;
    }
}
