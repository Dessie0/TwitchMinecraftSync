package me.dessie.twitchminecraft.Events;

import me.dessie.twitchminecraft.Commands.RevokeCMD;
import me.dessie.twitchminecraft.Events.twitchminecraft.TwitchExpireEvent;
import me.dessie.twitchminecraft.Events.twitchminecraft.TwitchResubscribeEvent;
import me.dessie.twitchminecraft.Events.twitchminecraft.TwitchSubscribeEvent;
import me.dessie.twitchminecraft.RewardHandler;
import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import me.dessie.twitchminecraft.WebServer.TwitchHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.ZonedDateTime;

public class JoinListener implements Listener {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TwitchPlayer twitchPlayer;

        if(plugin.twitchConfig.getStringList("revoked").contains(player.getUniqueId().toString())) {
            RevokeCMD.revoke(player, true);
            return;
        }

        if(TwitchPlayer.playerExists(player.getUniqueId().toString())) {
            twitchPlayer = TwitchPlayer.create(player);
        } else {
            player.sendMessage(plugin.color("&cLooks like you're not synced to Twitch! Use /sync to gain access to the server!"));
            return;
        }

        TwitchHandler handler = new TwitchHandler(twitchPlayer, player.getAddress().getAddress());

        //We need to re-check if they re-subscribed.
        if(ZonedDateTime.now().isAfter(ZonedDateTime.parse(twitchPlayer.getExpires()))) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if(handler.checkIfSubbed(handler.getNewAccessToken(twitchPlayer), twitchPlayer.getChannelID())) {
                    //Just let them know they've been renewed!
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &aWe've updated your expiry date for this server."));
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &aYour sub will expire on &e" + TwitchMinecraft.formatExpiry(handler.getTwitchPlayer().getExpires())));
                    Bukkit.getPluginManager().callEvent(new TwitchResubscribeEvent(handler.getTwitchPlayer()));
                } else {
                    //Revoke stuff, not subbed anymore.
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage("&d[TwitchMinecraftSync] &cWe could not confirm that you have resynced your account!");
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage("&d[TwitchMinecraftSync] &cWe've removed your perms and sent you to the &0Black Box &c.");
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage("&d[TwitchMinecraftSync] &cIf you want perms to the server back, please re-sub and type /sync!");
                    Bukkit.getPluginManager().callEvent(new TwitchExpireEvent(handler.getTwitchPlayer()));
                }
            });
        }
    }
}
