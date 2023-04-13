package com.github.tahmid_23.doors.game.monster;

import com.github.tahmid_23.doors.game.Tickable;
import com.github.tahmid_23.doors.game.object.closet.HidingSpot;
import com.github.tahmid_23.doors.game.object.closet.HidingSpotManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.TitlePart;

public class HideMonster implements Tickable {

    private final HidingSpotManager hidingSpotManager;

    public HideMonster(HidingSpotManager hidingSpotManager) {
        this.hidingSpotManager = hidingSpotManager;
    }

    @Override
    public void tick() {
        for (HidingSpot hidingSpot : hidingSpotManager.getHidingSpots()) {
            if (hidingSpot.getHideDuration() == 100L) {
                hidingSpot.getHider().ifPresent(hider -> {
                    hider.sendTitlePart(TitlePart.TITLE, Component.text("LEAVE", NamedTextColor.RED));
                });
            }
            if (hidingSpot.getHideDuration() == 120L) {
                hidingSpot.getHider().ifPresent(hider -> {
                    hider.sendTitlePart(TitlePart.TITLE, Component.text("GET OUT", NamedTextColor.RED));
                });
            }
            if (hidingSpot.getHideDuration() == 140L) {
                hidingSpot.getHider().ifPresent(hider -> {
                    Component message = Component.text()
                            .append(Component.text("XXX", null, TextDecoration.OBFUSCATED),
                                    Component.text("GET OUT NOW"),
                                    Component.text("XXX", null, TextDecoration.OBFUSCATED))
                            .color(NamedTextColor.RED)
                            .asComponent();
                    hider.sendTitlePart(TitlePart.TITLE, message);
                });
            }
            if (hidingSpot.getHideDuration() == 160L) {
                hidingSpot.getHider().ifPresent(hider -> {
                    hider.sendTitlePart(TitlePart.TITLE, Component.empty());
                });
                hidingSpot.unhide();
            }
        }
    }

}
