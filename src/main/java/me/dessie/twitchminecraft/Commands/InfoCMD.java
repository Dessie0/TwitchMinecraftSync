package me.dessie.twitchminecraft.Commands;

import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InfoCMD implements CommandExecutor {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("tinfo")) {
            if (sender.hasPermission("twitchmcsync.tinfo")) {
                if (args.length > 0) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

                    if (!TwitchPlayer.playerExists(player.getUniqueId().toString())) {
                        //Attempt to get the information from a Twitch username.

                        Set<String> keys = plugin.twitchConfig.getConfigurationSection("").getKeys(false);
                        keys.remove("revoked");

                        List<String> uuids = keys.stream()
                                .filter(uuid -> plugin.twitchConfig.getString(uuid + ".channelName").equalsIgnoreCase(args[0]))
                                .collect(Collectors.toList());

                        if (!uuids.isEmpty()) {
                            player = Bukkit.getOfflinePlayer(UUID.fromString(uuids.get(0)));
                            if (!TwitchPlayer.playerExists(player.getUniqueId().toString())) {
                                sender.sendMessage(plugin.color("&d" + args[0] + "&c doesn't exist or hasn't synced their Twitch account!"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(plugin.color("&d" + args[0] + "&c doesn't exist or hasn't synced their Twitch account!"));
                            return true;
                        }
                    }

                    TwitchPlayer twitchPlayer = TwitchPlayer.create(player);

                    ZonedDateTime time = ZonedDateTime.parse(twitchPlayer.getExpires());
                    StringBuilder expiry = new StringBuilder();

                    String month = time.getMonth().toString().substring(0, 1) + time.getMonth().toString().substring(1).toLowerCase();

                    expiry.append(month)
                            .append(" ")
                            .append(time.getDayOfMonth())
                            .append(", ")
                            .append(time.getYear())
                            .append(" at ");

                    String hour;
                    if (time.getHour() < 10) {
                        hour = "0" + time.getHour();
                    } else {
                        hour = String.valueOf(time.getHour());
                    }

                    String minute;
                    if (time.getMinute() < 10) {
                        minute = "0" + time.getMinute();
                    } else {
                        minute = String.valueOf(time.getMinute());
                    }

                    expiry.append(hour).append(":").append(minute);

                    sender.sendMessage(plugin.color("&7&m----------&dTwitchSync&7&m----------"));
                    sender.sendMessage(plugin.color("&dPlayer Name: &a") + twitchPlayer.getName());
                    sender.sendMessage(plugin.color("&dTwitch Name: &a") + twitchPlayer.getChannelName());
                    sender.sendMessage(plugin.color("&dSubscription Tier: &a") + twitchPlayer.getTier());
                    sender.sendMessage(plugin.color("&dStreak: &a") + twitchPlayer.getStreak());
                    sender.sendMessage(plugin.color("&dExpires On: &a") + expiry);
                    sender.sendMessage(plugin.color("&7&m----------&dTwitchSync&7&m----------"));

                    return true;
                }else sender.sendMessage(plugin.color("&cYou need to enter a player to get the data from!"));
            } else sender.sendMessage(plugin.color("&cYou do not have permission to do that!"));
        }
        return false;
    }
}
