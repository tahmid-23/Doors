package com.github.tahmid_23.doors.game.object.closet;

import com.github.tahmid_23.doors.game.Tickable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;

import java.util.*;

public class HidingSpotManager implements Tickable {

    private final Collection<HidingSpot> hidingSpots = new ArrayList<>();

    private final ConnectionManager connectionManager;

    private final Set<UUID> players;

    private final double rangeSquared;

    public HidingSpotManager(ConnectionManager connectionManager, Set<UUID> players, double rangeSquared) {
        this.connectionManager = connectionManager;
        this.players = players;
        this.rangeSquared = rangeSquared;
    }

    public void addHidingSpot(HidingSpot hidingSpot) {
        hidingSpots.add(hidingSpot);
    }

    public Collection<HidingSpot> getHidingSpots() {
        return hidingSpots;
    }

    @Override
    public void tick() {
        for (HidingSpot hidingSpot : hidingSpots) {
            hidingSpot.tick();
        }

        for (UUID playerUUID : players) {
            Player player = connectionManager.getPlayer(playerUUID);
            if (player == null || isPlayerHiding(player)) {
                continue;
            }

            for (HidingSpot hidingSpot : hidingSpots) {
                if (!hidingSpot.canHide()) {
                    continue;
                }

                if (hidingSpot.getSource().distance(player.getPosition()) < rangeSquared) {
                    ComponentLike message = Component.text()
                            .append(Component.text("Use "),
                                    Component.keybind("key.use"),
                                    Component.text(" to hide!"))
                            .color(NamedTextColor.YELLOW);
                    player.sendActionBar(message);
                    break;
                }
            }
        }
    }

    private boolean isPlayerHiding(Player player) {
        for (HidingSpot hidingSpot : hidingSpots) {
            Optional<Player> hiderOptional = hidingSpot.getHider();
            if (hiderOptional.isEmpty()) {
                continue;
            }

            if (hiderOptional.get() == player) {
                return true;
            }
        }

        return false;
    }

}
