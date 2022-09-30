package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collections;

public class InfoCMD implements CommandExecutor {

    private final TwitchMinecraft plugin;

    public InfoCMD(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tinfo")) {
            if (sender.hasPermission("twitchmcsync.tinfo")) {
                if (args.length > 0) {

                    //Attempt to get the TwitchPlayer from a Minecraft username.
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                    TwitchPlayer tPlayer = getTwitchPlayer(player.getUniqueId().toString());

                    //If it's null, attempt to get it from a Twitch username.
                    if(tPlayer == null) {

                        //Attempt to get it from a subbed user.
                        tPlayer = getTwitchPlayer(TwitchPlayer.getUUIDFromChannelName(args[0]));
                        if(tPlayer == null) {
                            this.getPlugin().getLanguage().sendMessage(sender,"no_synced_account", Collections.singletonMap("player", args[0]));
                            return true;
                        }
                    }

                    this.getPlugin().getLanguage().sendMessage(sender, "info_header");
                    this.getPlugin().getLanguage().sendMessage(sender, "info_player_name", Collections.singletonMap("player", tPlayer.getName()));
                    this.getPlugin().getLanguage().sendMessage(sender, "info_twitch_name", Collections.singletonMap("twitch", tPlayer.getChannelName()));
                    if(tPlayer.getTier() != 0) {
                        this.getPlugin().getLanguage().sendMessage(sender, "info_subscription_tier", Collections.singletonMap("tier", String.valueOf(tPlayer.getTier())));
                    }

                    this.getPlugin().getLanguage().sendMessage(sender, "info_footer");

                    return true;
                }else this.getPlugin().getLanguage().sendMessage(sender, "enter_player_argument");
            } else this.getPlugin().getLanguage().sendMessage(sender, "no_permission");
        }
        return false;
    }

    private TwitchPlayer getTwitchPlayer(String uuid) {
        if(uuid == null) return null;

        if(TwitchPlayer.isSubbed(uuid)) {
            return TwitchPlayer.create(uuid);
        } else if(TwitchPlayer.isSynced(uuid)) {
            return TwitchPlayer.createData(uuid);
        }
        return null;
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}