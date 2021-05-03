package me.dessie.twitchminecraft.commands;

import me.dessie.twitchminecraft.TwitchMinecraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCMD implements CommandExecutor {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("twitchreload")) {
            if (sender.hasPermission("twitchmcsync.twitchreload")) {

                //Load the custom files in.
                plugin.createFiles();

                //Reload the config.yml.
                plugin.reloadConfig();

                //Reload the channel ID.
                plugin.getChannelID();

                sender.sendMessage(plugin.color("&aSuccessfully reloaded configuration files."));

            } else sender.sendMessage(plugin.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }
}