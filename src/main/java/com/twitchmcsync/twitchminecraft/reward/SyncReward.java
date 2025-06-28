package com.twitchmcsync.twitchminecraft.reward;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import lombok.Getter;
import me.dessie.dessielib.annotations.storageapi.RecomposeConstructor;
import me.dessie.dessielib.annotations.storageapi.StoredList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SyncReward {

    @Getter
    private static Map<String, SyncReward> rewardMap = new HashMap<>();

    @StoredList(type = String.class, storeAs = "subscribe")
    private final List<String> subscribeCommands;

    @StoredList(type = String.class, storeAs = "expire")
    private final List<String> expireCommands;

    @RecomposeConstructor
    public SyncReward(List<String> subscribeCommands, List<String> expireCommands) {
        this.subscribeCommands = subscribeCommands;
        this.expireCommands = expireCommands;
    }

    public void executeSubscribeCommands(TwitchPlayer player) {
        if(player == null) return;

        //Trigger on another thread so we don't get async issues.
        Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
            Player onlinePlayer = player.getPlayer();
            for(String command : this.getSubscribeCommands()) {
                if(onlinePlayer != null) {
                    command = command.replace("%player%", onlinePlayer.getName());
                }

                command = command.replace("%twitchname%", player.getChannelName())
                        .replace("%tier%", String.valueOf(player.getTier()))
                        .replace("%broadcaster%", TwitchMinecraft.getInstance().getBroadcasterUsername());

                if(command.endsWith("-p") && onlinePlayer != null) {
                    Bukkit.getServer().dispatchCommand(onlinePlayer, command);
                } else {
                    Bukkit.getServer().dispatchCommand(TwitchMinecraft.getInstance().getServer().getConsoleSender(), command);
                }
            }
        });
    }

    public void executeExpireCommands(String playerName, TwitchPlayer twitchPlayer) {
        if(twitchPlayer == null) return;
        Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
            for (String command : this.getExpireCommands()) {
                command = command.replace("%player%", playerName);
                command = command.replace("%twitchname%", twitchPlayer.getChannelName())
                        .replace("%tier%", String.valueOf(twitchPlayer.getTier()))
                        .replace("%broadcaster%", TwitchMinecraft.getInstance().getBroadcasterUsername());

                if (command.endsWith("-p") && twitchPlayer.getPlayer() != null) {
                    Bukkit.getServer().dispatchCommand(twitchPlayer.getPlayer(), command);
                } else {
                    Bukkit.getServer().dispatchCommand(TwitchMinecraft.getInstance().getServer().getConsoleSender(), command);
                }
            }
        });
    }

    public static void reloadRewards(TwitchMinecraft plugin) {
        getRewardMap().clear();
        plugin.getRewardContainer().getKeys("").forEach(key -> {
            getRewardMap().put(key, plugin.getRewardContainer().retrieve(SyncReward.class, key));
        });
    }
}
