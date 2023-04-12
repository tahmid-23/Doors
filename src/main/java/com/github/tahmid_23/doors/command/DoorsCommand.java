package com.github.tahmid_23.doors.command;

import com.github.tahmid_23.doors.game.DoorsGameManager;
import com.github.tahmid_23.doors.map.DoorsMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.Map;

public class DoorsCommand {

    private DoorsCommand() {

    }

    public static Command createCommand(DoorsGameManager gameManager, Map<String, DoorsMap> maps) {
        Command command = new Command("doors");
        Argument<String> mapNameArgument = new ArgumentString("mapName");
        mapNameArgument.setSuggestionCallback((sender, context, suggestion) -> {
            for (String mapName : maps.keySet()) {
                suggestion.addEntry(new SuggestionEntry(mapName));
            }
        });

        command.addConditionalSyntax((sender, commandString) -> sender instanceof Player, (sender, context) -> {
            DoorsMap map = maps.get(context.get(mapNameArgument));
            if (map == null) {
                sender.sendMessage(Component.text("Invalid map name", NamedTextColor.RED));
                return;
            }

            gameManager.createGame(map).thenAccept(game -> game.addPlayer((Player) sender));
        }, mapNameArgument);

        return command;
    }

}
