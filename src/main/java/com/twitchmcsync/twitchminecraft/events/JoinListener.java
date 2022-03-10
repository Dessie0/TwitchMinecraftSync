package com.twitchmcsync.twitchminecraft.events;

import com.twitchmcsync.twitchminecraft.RewardHandler;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchExpireEvent;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchResubscribeEvent;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.lang.DateFormatter;
import com.twitchmcsync.twitchminecraft.webserver.TwitchHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
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

        if(TwitchMinecraft.getTwitchConfig().getConfigurationSection("revoked." + player.getUniqueId()) != null) {
            ConfigurationSection section = TwitchMinecraft.getTwitchConfig().getConfigurationSection("revoked." + player.getUniqueId());

            TwitchPlayer temp = new TwitchPlayer(player);
            temp.setTier(section.getInt("tier"));
            temp.setChannelName(TwitchMinecraft.getTwitchConfig().getConfigurationSection("data." + player.getUniqueId()).getString("channelName"));

            RewardHandler.remove(temp);

            TwitchMinecraft.getTwitchConfig().set("revoked." + player.getUniqueId(), null);
            TwitchMinecraft.saveFile(TwitchMinecraft.getTwitchData(), TwitchMinecraft.getTwitchConfig());

            player.sendMessage(TwitchMinecraft.color("&cYour Twitch authorization has been revoked!"));
            player.sendMessage(TwitchMinecraft.color("&cYou can re-sync by typing /sync"));
            return;
        }

        if(TwitchPlayer.isSubbed(player.getUniqueId().toString())) {
            twitchPlayer = TwitchPlayer.create(player);
        } else {
            twitchPlayer = new TwitchPlayer(player);

            //Check if the player has any of the groups that only subs have.
            //If they do, re-run the revoke commands.
            for(int tier = 1; tier < 4; tier++) {
                String role = RewardHandler.getGiveRole(tier);
                if(role.length() > 0 && TwitchMinecraft.isVaultEnabled()) {
                    if(plugin.getPermission().playerInGroup(player, role)) {
                        twitchPlayer.setTier(tier);
                        RewardHandler.remove(twitchPlayer);
                    }
                }
            }

            player.sendMessage(TwitchMinecraft.color("&cLooks like you're not synced to Twitch! Use /sync to gain access to the server!"));
            return;
        }

        TwitchHandler handler = new TwitchHandler(twitchPlayer);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player onlinePlayer = handler.getTwitchPlayer().getPlayer().getPlayer();
            if(handler.checkIfSubbed(handler.getNewAccessToken(twitchPlayer), twitchPlayer.getChannelID())) {
                //Just let them know they've been renewed!
                Bukkit.getScheduler().runTask(plugin, () -> {
                    TwitchResubscribeEvent resubEvent = new TwitchResubscribeEvent(handler.getTwitchPlayer());
                    Bukkit.getPluginManager().callEvent(resubEvent);
                    if(!resubEvent.isCancelled()) {
                        RewardHandler.giveResub(resubEvent.getTwitchPlayer());
                        TwitchPlayer.clearData(resubEvent.getTwitchPlayer().getUuid());
                    } else {
                        onlinePlayer.sendMessage(TwitchMinecraft.color("&d[TwitchMinecraftSync] &cSomething went wrong when attempting to resync your subscription!"));
                        RewardHandler.remove(handler.getTwitchPlayer());
                    }
                });

            } else {
                //Revoke stuff, not subbed anymore.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    TwitchExpireEvent expireEvent = new TwitchExpireEvent(handler.getTwitchPlayer());
                    Bukkit.getPluginManager().callEvent(expireEvent);
                    if(!expireEvent.isCancelled()) {
                        handler.getTwitchPlayer().setLastSubDate(ZonedDateTime.now().toString());
                        handler.getTwitchPlayer().saveData(false);

                        RewardHandler.remove(expireEvent.getTwitchPlayer());
                        onlinePlayer.sendMessage(TwitchMinecraft.color("&d[TwitchMinecraftSync] &cWe could not confirm that you have resynced your Twitch account!"));
                        onlinePlayer.sendMessage(TwitchMinecraft.color("&d[TwitchMinecraftSync] &cIf you would like to re-sync, please re-sub and type /sync!"));
                    }
                });
            }
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Optional<TwitchHandler> handler = TwitchHandler.getHandlers().stream()
                .filter(handler1 -> handler1.getTwitchPlayer().getUuid().equals(event.getPlayer().getUniqueId().toString()))
                .findAny();

        handler.ifPresent(twitchHandler -> TwitchHandler.getHandlers().remove(twitchHandler));
    }
}
