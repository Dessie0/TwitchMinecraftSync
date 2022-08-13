package com.twitchmcsync.twitchminecraft.events;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchExpireEvent;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchResubscribeEvent;
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

    private final TwitchMinecraft plugin;
    
    public JoinListener(TwitchMinecraft plugin) {
        this.plugin = plugin;
    } 
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TwitchPlayer twitchPlayer;

        if(this.getPlugin().getTwitchConfig().getConfigurationSection("revoked." + player.getUniqueId()) != null) {
            ConfigurationSection section = this.getPlugin().getTwitchConfig().getConfigurationSection("revoked." + player.getUniqueId());

            TwitchPlayer temp = new TwitchPlayer(this.getPlugin(), player);
            temp.setTier(section.getInt("tier"));
            temp.setChannelName(this.getPlugin().getTwitchConfig().getConfigurationSection("data." + player.getUniqueId()).getString("channelName"));

            this.getPlugin().getRewardHandler().remove(temp);

            this.getPlugin().getTwitchConfig().set("revoked." + player.getUniqueId(), null);
            TwitchMinecraft.saveFile(this.getPlugin().getTwitchData(), this.getPlugin().getTwitchConfig());

            player.sendMessage(TwitchMinecraft.color("&cYour Twitch authorization has been revoked!"));
            player.sendMessage(TwitchMinecraft.color("&cYou can re-sync by typing /sync"));
            return;
        }

        if(TwitchPlayer.isSubbed(player.getUniqueId().toString())) {
            twitchPlayer = TwitchPlayer.create(player);
        } else {
            twitchPlayer = new TwitchPlayer(this.getPlugin(), player);

            //Check if the player has any of the groups that only subs have.
            //If they do, re-run the revoke commands.
            for(int tier = 1; tier < 4; tier++) {
                String role = this.getPlugin().getRewardHandler().getGiveRole(tier);
                if(role.length() > 0 && this.getPlugin().isVaultEnabled()) {
                    if(this.getPlugin().getPermission().playerInGroup(player, role)) {
                        twitchPlayer.setTier(tier);
                        this.getPlugin().getRewardHandler().remove(twitchPlayer);
                    }
                }
            }

            if(this.getPlugin().getChannelID() != null) {
                player.sendMessage(TwitchMinecraft.color("&cLooks like you're not synced to Twitch! Use /sync to gain access to the server!"));
            }
            return;
        }

        TwitchHandler handler = new TwitchHandler(twitchPlayer);

        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            Player onlinePlayer = handler.getTwitchPlayer().getPlayer().getPlayer();
            if(handler.checkIfSubbed(handler.getNewAccessToken(twitchPlayer), twitchPlayer.getChannelID())) {

                //Just let them know they've been renewed!
                Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
                    TwitchResubscribeEvent resubEvent = new TwitchResubscribeEvent(handler.getTwitchPlayer());
                    Bukkit.getPluginManager().callEvent(resubEvent);
                    if(!resubEvent.isCancelled()) {
                        this.getPlugin().getRewardHandler().giveResub(resubEvent.getTwitchPlayer());
                        twitchPlayer.clearData();
                    } else {
                        onlinePlayer.sendMessage(TwitchMinecraft.color("&d[TwitchMinecraftSync] &cSomething went wrong when attempting to resync your subscription!"));
                        this.getPlugin().getRewardHandler().remove(handler.getTwitchPlayer());
                    }
                });

            } else {
                //Revoke stuff, not subbed anymore.
                Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
                    TwitchExpireEvent expireEvent = new TwitchExpireEvent(handler.getTwitchPlayer());
                    Bukkit.getPluginManager().callEvent(expireEvent);
                    if(!expireEvent.isCancelled()) {
                        handler.getTwitchPlayer().setLastSubDate(ZonedDateTime.now().toString());
                        handler.getTwitchPlayer().saveData(false);

                        this.getPlugin().getRewardHandler().remove(expireEvent.getTwitchPlayer());
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

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}
