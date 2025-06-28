package com.twitchmcsync.twitchminecraft.live;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import lombok.Getter;
import me.dessie.dessielib.annotations.storageapi.RecomposeConstructor;
import me.dessie.dessielib.annotations.storageapi.StoredList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
public class LiveCommands {

    @StoredList(type = String.class, storeAs = "live")
    private final List<String> liveCommands;

    @StoredList(type = String.class, storeAs = "offline")
    private final List<String> offlineCommands;

    @RecomposeConstructor
    public LiveCommands(List<String> liveCommands, List<String> offlineCommands) {
        this.liveCommands = liveCommands;
        this.offlineCommands = offlineCommands;
    }

    public void executeLiveCommands(TwitchPlayer player) {
        if(player == null) return;

        //Trigger on another thread so we don't get async issues.
        Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
            Player onlinePlayer = player.getPlayer();
            for(String command : this.getLiveCommands()) {
                if(onlinePlayer != null) {
                    command = command.replace("%player%", onlinePlayer.getName());
                }

                command = command.replace("%twitchname%", player.getChannelName())
                        .replace("%tier%", String.valueOf(player.getTier()));

                if(command.endsWith("-p") && onlinePlayer != null) {
                    Bukkit.getServer().dispatchCommand(onlinePlayer, command);
                } else {
                    Bukkit.getServer().dispatchCommand(TwitchMinecraft.getInstance().getServer().getConsoleSender(), command);
                }
            }
        });
    }

    public void executeOfflineCommands(String playerName, TwitchPlayer twitchPlayer) {
        if(twitchPlayer == null) return;

        Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
            for (String command : this.getOfflineCommands()) {
                command = command.replace("%player%", playerName);
                command = command.replace("%twitchname%", twitchPlayer.getChannelName())
                        .replace("%tier%", String.valueOf(twitchPlayer.getTier()));

                if (command.endsWith("-p") && twitchPlayer.getPlayer() != null) {
                    Bukkit.getServer().dispatchCommand(twitchPlayer.getPlayer(), command);
                } else {
                    Bukkit.getServer().dispatchCommand(TwitchMinecraft.getInstance().getServer().getConsoleSender(), command);
                }
            }
        });
    }
}
