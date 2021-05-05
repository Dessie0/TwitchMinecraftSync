package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class InfoCMD implements CommandExecutor {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("tinfo")) {
            if (sender.hasPermission("twitchmcsync.tinfo")) {
                if (args.length > 0) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                    TwitchPlayer tPlayer = getTwitchPlayer(player.getUniqueId().toString());

                    if(tPlayer == null) {
                        tPlayer = getTwitchPlayer(TwitchPlayer.getUUIDFromChannelName(args[0]));
                    }

                    if(tPlayer == null) {
                        sender.sendMessage(TwitchMinecraft.color("&cUnable to find a synced account with the name &e" + args[0]));
                        return true;
                    }

                    sender.sendMessage(TwitchMinecraft.color("&7&m----------&dTwitchSync&7&m----------"));
                    sender.sendMessage(TwitchMinecraft.color("&dPlayer Name: &a") + tPlayer.getName());
                    sender.sendMessage(TwitchMinecraft.color("&dTwitch Name: &a") + tPlayer.getChannelName());
                    if(tPlayer.getExpirationDate() != null) {
                        sender.sendMessage(TwitchMinecraft.color("&dSubscription Tier: &a") + tPlayer.getTier());
                        sender.sendMessage(TwitchMinecraft.color("&dStreak: &a") + tPlayer.getStreak());
                        sender.sendMessage(TwitchMinecraft.color("&dExpires On: &a") + TwitchMinecraft.formatExpiry(tPlayer.getExpirationDate()));
                    } else {
                        sender.sendMessage(TwitchMinecraft.color("&dLast Sub Date: &a" + TwitchMinecraft.formatExpiry(tPlayer.getLastSubDate())));
                    }

                    sender.sendMessage(TwitchMinecraft.color("&7&m----------&dTwitchSync&7&m----------"));

                    return true;
                }else sender.sendMessage(TwitchMinecraft.color("&cYou need to enter a player to get the data from!"));
            } else sender.sendMessage(TwitchMinecraft.color("&cYou do not have permission to do that!"));
        }
        return false;
    }

    private TwitchPlayer getTwitchPlayer(String uuid) {
        if(TwitchPlayer.isSubbed(uuid)) {
            return TwitchPlayer.create(uuid);
        } else if(TwitchPlayer.isSynced(uuid)) {
            return TwitchPlayer.createData(uuid);
        }
        return null;
    }
}