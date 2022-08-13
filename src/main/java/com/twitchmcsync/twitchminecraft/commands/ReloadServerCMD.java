package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadServerCMD implements CommandExecutor {

    private final TwitchMinecraft plugin;

    public ReloadServerCMD(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("twitchserverreload")) {
            if (sender.hasPermission("twitchmcsync.twitchserverreload")) {

                Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
                    sender.sendMessage(TwitchMinecraft.color("&aStopping WebServer on port &d" + this.getPlugin().getWebServer().getServer().getAddress().getPort()));
                    sender.sendMessage(TwitchMinecraft.color("&aStarting new WebServer on port &d" + this.getPlugin().getConfig().getInt("port")));
                    this.getPlugin().restartWebServer();
                    sender.sendMessage(TwitchMinecraft.color("&aSuccessfully restarted the WebServer."));
                });
            } else sender.sendMessage(TwitchMinecraft.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}