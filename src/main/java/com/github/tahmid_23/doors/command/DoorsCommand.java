package com.github.tahmid_23.doors.command;

import com.github.tahmid_23.doors.map.DoorsMap;
import com.github.tahmid_23.doors.map.generation.InstanceGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DoorsCommand {

    private DoorsCommand() {

    }

    public static Command createCommand(InstanceGenerator generator, Map<String, DoorsMap> maps) {
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

            generator.generate(map).thenAccept(instance -> {
                ((Entity) sender).setInstance(instance, Vec.ZERO);
            });
        }, mapNameArgument);

        return command;
    }

}
