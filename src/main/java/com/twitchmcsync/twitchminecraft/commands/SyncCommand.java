package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.CodeHandler;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import me.dessie.dessielib.annotations.command.CommandAliases;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.entity.Player;

import java.util.Map;

@MinecraftCommand(name = "sync", description = "Create an auth code for syncing to Twitch.")
@CommandAliases(aliases = "twitchsync")
public class SyncCommand extends XCommand {

    @Override
    protected void execute(Player player, String[] args) {
        TwitchPlayer.load(player.getUniqueId()).thenAccept(twitchPlayer -> {
            if(twitchPlayer == null) {
                CodeHandler.createCode(player.getUniqueId()).thenAccept(code -> {
                    if(code == null) {
                        TwitchMinecraft.getInstance().getLanguage().sendMessage(player, "code_generation_failed");
                    } else {
                        TwitchMinecraft.getInstance().getLanguage().sendMessage(player, "sync", Map.of("%code%", code));
                    }


                });
            } else {
                TwitchMinecraft.getInstance().getLanguage().sendMessage(player, "already_synced");
            }
        });
    }
}
