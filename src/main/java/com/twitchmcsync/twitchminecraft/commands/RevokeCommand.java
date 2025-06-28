package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.TwitchUnsyncEvent;
import com.twitchmcsync.twitchminecraft.submode.SubMode;
import me.dessie.dessielib.annotations.command.CommandAliases;
import me.dessie.dessielib.annotations.command.CommandPermission;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@MinecraftCommand(name = "revoke", description = "Revokes a user's Twitch sync status")
@CommandPermission(permission = "twitchmcsync.revoke")
@CommandAliases(aliases = "twitchrevoke")
public class RevokeCommand extends XCommand {

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

        chain.thenAccept(player -> {
            if(player == null) {
                TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "no_info_found_message");
                return;
            }

            Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
                Bukkit.getServer().getPluginManager().callEvent(new TwitchUnsyncEvent(TwitchUnsyncEvent.ExpireMethod.REVOKED, player.getUuid(), player));
                SubMode subMode = TwitchMinecraft.getInstance().getSubMode();
                if(player.getPlayer() != null && subMode.canKickUser(player.getPlayer())) {
                    player.getPlayer().kick(TwitchMinecraft.getInstance().getLanguage().getComponent("submode.kicked"));
                } else if(player.getPlayer() != null) {
                    TwitchMinecraft.getInstance().getLanguage().sendMessage(player.getPlayer(), "revoke.revoked");
                }

                player.delete();
                TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "revoke.success", Map.of("%player%", args[0]));
            });
        });

    }
}
