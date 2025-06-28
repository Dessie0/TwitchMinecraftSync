package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.lang.Language;
import me.dessie.dessielib.annotations.command.CommandAliases;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import me.dessie.dessielib.core.utils.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@MinecraftCommand(name = "tmessage", description = "Sends a message directly to a player with component support.")
@CommandAliases(aliases = "twitchmessage")
public class TwitchMessageCommand extends XCommand {

    @Override
    protected void execute(ConsoleCommandSender console, String[] args) {
        Language language = TwitchMinecraft.getInstance().getLanguage();
        if(args.length == 0) {
            language.sendMessage(console, "need_player_argument");
            return;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if(player == null) {
            console.sendMessage(Colors.color("&cNo player found."));
            return;
        }

        String messageRaw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Component component = MiniMessage.miniMessage().deserialize(messageRaw);

        player.sendMessage(component);
    }
}
