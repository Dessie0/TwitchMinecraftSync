package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import me.dessie.dessielib.annotations.command.CommandAliases;
import me.dessie.dessielib.annotations.command.CommandPermission;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@MinecraftCommand(name = "tinfo", description = "Retrieve information on a user's sync status.")
@CommandPermission(permission = "twitchmcsync.tinfo")
@CommandAliases(aliases = "twitchinfo")
public class TwitchInfoCommand extends XCommand {

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if(args.length == 0) {
            TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "need_player_argument");
            return;
        }

        if(!sender.hasPermission(this.getPermission()) && !sender.isOp()) {
            TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "no_permission");
            return;
        }

        CompletableFuture<TwitchPlayer> chain;

        //Attempt to load from Minecraft Username.
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
        if(offlinePlayer.hasPlayedBefore()) {
            chain = TwitchPlayer.load(offlinePlayer.getUniqueId()).thenCompose(player -> {
                if(player != null) {
                    return CompletableFuture.completedFuture(player);
                } else {
                    return TwitchPlayer.loadFromChannelName(args[0]);
                }
            });
        } else {
            chain = TwitchPlayer.loadFromChannelName(args[0]);
        }

        chain.thenAccept(player -> this.sendInfoMessage(sender, offlinePlayer.getName(), player));
    }

    private void sendInfoMessage(CommandSender sender, String playerName, TwitchPlayer player) {
        if(player == null) {
            TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "no_info_found_message");
            return;
        }

        TwitchMinecraft.getInstance().getLanguage().sendListMessage(sender, "info_message",
                Map.of("%player%", playerName,
                        "%twitch%", player.getChannelName(),
                        "%tier%", String.valueOf(player.getTier()),
                        "%twitch_id%", player.getChannelID(),
                        "%subbed%", String.valueOf(player.isSubbed()),
                        "%uuid%", player.getUuid().toString()));
    }

}
