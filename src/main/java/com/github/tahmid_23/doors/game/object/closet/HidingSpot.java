package com.github.tahmid_23.doors.game.object.closet;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;

import java.util.Optional;

public interface HidingSpot {

    void hidePlayer(Player player);

    Optional<Player> getHider();

    void unhide();

    boolean canHide();

    long getHideDuration();

    void tick();

    Point getSource();

}
