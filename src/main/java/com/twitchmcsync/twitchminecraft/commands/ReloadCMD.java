package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCMD implements CommandExecutor {

    private final TwitchMinecraft plugin;

    public ReloadCMD(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("twitchreload")) {
            if (sender.hasPermission("twitchmcsync.twitchreload")) {

                //Reload the custom files.
                this.getPlugin().createFiles();

                //Reload the config.yml.
                this.getPlugin().reloadConfig();

                //Reload the channel ID.
                this.getPlugin().retrieveChannelID();

                sender.sendMessage(TwitchMinecraft.color("&aSuccessfully reloaded configuration files."));
            } else sender.sendMessage(TwitchMinecraft.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}