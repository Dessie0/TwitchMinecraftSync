package com.twitchmcsync.twitchminecraft.live;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.TwitchSyncEvent;
import com.twitchmcsync.twitchminecraft.events.TwitchUnsyncEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LiveModule implements Listener {

    private TwitchClient client;
    private TwitchMinecraft plugin;
    private LiveCommands commands;

    private List<TwitchPlayer> livePlayers = new ArrayList<>();

    public LiveModule(TwitchMinecraft plugin) {
        this.plugin = plugin;
        this.reload();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.getClient().getEventManager().onEvent(ChannelGoLiveEvent.class, event -> {
                //Run the go live commands.
                TwitchPlayer.loadFromChannelId(event.getChannel().getId()).thenAccept(twitchPlayer -> {
                    this.getCommands().executeLiveCommands(twitchPlayer);
                    this.livePlayers.add(twitchPlayer);
                });
            });

            this.getClient().getEventManager().onEvent(ChannelGoOfflineEvent.class, event -> {
                //Run the stop live commands.
                TwitchPlayer.loadFromChannelId(event.getChannel().getId()).thenAccept(twitchPlayer -> {
                    this.getCommands().executeOfflineCommands(twitchPlayer.getPlayer().getName(), twitchPlayer);
                    this.livePlayers.removeIf(livePlayer -> livePlayer.getUuid().equals(twitchPlayer.getUuid()));
                });
            });
        });

        this.getPlugin().getLogger().info("Live module loaded.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            TwitchPlayer.load(event.getPlayer().getUniqueId()).thenAccept(twitchPlayer -> {
                if(twitchPlayer == null) return;
                this.getClient().getClientHelper().enableStreamEventListener(twitchPlayer.getChannelID(), twitchPlayer.getChannelName());

                //Check if they're live, if they are, run the start live commands.
                this.getPlugin().getTwitchWrapper().isUserLive(twitchPlayer).thenAccept(isLive -> {
                    if(isLive) {
                        this.getCommands().executeLiveCommands(twitchPlayer);
                        this.livePlayers.add(twitchPlayer);
                    }
                });

            });
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            TwitchPlayer.load(event.getPlayer().getUniqueId()).thenAccept(twitchPlayer -> {
                if(twitchPlayer == null) return;

                //Disable listener.
                this.getClient().getClientHelper().disableStreamEventListenerForId(twitchPlayer.getChannelID());

                //Check if they're live, if they are, run the stop live commands.
                this.getPlugin().getTwitchWrapper().isUserLive(twitchPlayer).thenAccept(isLive -> {
                    if(isLive) {
                        this.getCommands().executeOfflineCommands(event.getPlayer().getName(), twitchPlayer);
                        this.livePlayers.removeIf(livePlayer -> livePlayer.getUuid().equals(twitchPlayer.getUuid()));
                    }
                });
            });
        });
    }

    @EventHandler
    public void onExpire(TwitchUnsyncEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            this.getClient().getClientHelper().disableStreamEventListenerForId(event.getTwitchPlayer().getChannelID());

            String playerName = event.getTwitchPlayer().getPlayer().getName();

            this.getPlugin().getTwitchWrapper().isUserLive(event.getTwitchPlayer()).thenAccept(isLive -> {
                if(isLive) {
                    //Run the stop live commands.
                    this.getCommands().executeOfflineCommands(playerName, event.getTwitchPlayer());
                    this.livePlayers.removeIf(livePlayer -> livePlayer.getUuid().equals(event.getTwitchPlayer().getUuid()));
                }
            });

        });
    }

    @EventHandler
    public void onSubscribe(TwitchSyncEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            if(event.getTwitchPlayer().getPlayer() == null) return;

            this.getClient().getClientHelper().enableStreamEventListener(event.getTwitchPlayer().getChannelID());
            this.getPlugin().getTwitchWrapper().isUserLive(event.getTwitchPlayer()).thenAccept(isLive -> {
                if(isLive) {
                    //Run the stop live commands.
                    this.getCommands().executeLiveCommands(event.getTwitchPlayer());
                    this.livePlayers.add(event.getTwitchPlayer());
                }
            });

        });
    }

    public void reload() {
        this.commands = TwitchMinecraft.getInstance().getLiveModuleContainer().retrieve(LiveCommands.class,"commands");
        this.client = TwitchMinecraft.getInstance().getTwitchWrapper().getClient();

        //Re-add all the online users to the live sync listener.
        Bukkit.getOnlinePlayers().forEach(player -> {
            TwitchPlayer.load(player.getUniqueId()).thenAccept(twitchPlayer -> {
                if(twitchPlayer == null) return;
                this.getClient().getClientHelper().enableStreamEventListener(twitchPlayer.getChannelID(), twitchPlayer.getChannelName());
            });
        });

    }
}
