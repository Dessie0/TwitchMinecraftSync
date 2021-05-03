package me.dessie.twitchminecraft.events;

import me.dessie.twitchminecraft.commands.RevokeCMD;
import me.dessie.twitchminecraft.events.twitchminecraft.TwitchExpireEvent;
import me.dessie.twitchminecraft.events.twitchminecraft.TwitchResubscribeEvent;
import me.dessie.twitchminecraft.RewardHandler;
import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import me.dessie.twitchminecraft.webserver.TwitchHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;
import java.util.Optional;

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
            twitchPlayer = new TwitchPlayer(player);

            //Check if the player has any of the groups that only subs have.
            //If they do, re-run the revoke commands.
            for(int tier = 1; tier < 4; tier++) {
                if(plugin.permission.playerInGroup(player, RewardHandler.getGiveRole(tier))) {
                    twitchPlayer.setTier(tier);
                    RewardHandler.remove(new TwitchPlayer(player));
                }
            }

            player.sendMessage(plugin.color("&cLooks like you're not synced to Twitch! Use /sync to gain access to the server!"));
            return;
        }

        TwitchHandler handler = new TwitchHandler(twitchPlayer);

        //We need to re-check if they re-subscribed.
        if(ZonedDateTime.now().isAfter(ZonedDateTime.parse(twitchPlayer.getExpirationDate()))) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if(handler.checkIfSubbed(handler.getNewAccessToken(twitchPlayer), twitchPlayer.getChannelID())) {
                    //Just let them know they've been renewed!
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &aWe've updated your expiry date for this server."));
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &aYour sub will expire on &e" + TwitchMinecraft.formatExpiry(handler.getTwitchPlayer().getExpirationDate())));
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new TwitchResubscribeEvent(handler.getTwitchPlayer())));
                } else {
                    //Revoke stuff, not subbed anymore.
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &cWe could not confirm that you have resynced your account!"));
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &cWe've removed your perms and sent you to the &0Black Box &c."));
                    handler.getTwitchPlayer().getPlayer().getPlayer().sendMessage(plugin.color("&d[TwitchMinecraftSync] &cIf you want perms to the server back, please re-sub and type /sync!"));
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new TwitchExpireEvent(handler.getTwitchPlayer())));
                }
            });
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        TwitchPlayer player = TwitchPlayer.getFromUUID(event.getPlayer().getUniqueId().toString());
        TwitchPlayer.getPlayers().remove(player);

        Optional<TwitchHandler> handler = TwitchHandler.getHandlers().stream().filter(handler1 -> handler1.getTwitchPlayer() == player).findAny();
        handler.ifPresent(twitchHandler -> TwitchHandler.getHandlers().remove(twitchHandler));
    }
}