package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.webserver.WebServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadServerCMD implements CommandExecutor {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("twitchserverreload")) {
            if (sender.hasPermission("twitchmcsync.twitchserverreload")) {

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    sender.sendMessage(TwitchMinecraft.color("&aStopping WebServer on port &d" + plugin.getWebServer().server.getAddress().getPort()));
                    sender.sendMessage(TwitchMinecraft.color("&aStarting new WebServer on port &d" + plugin.getConfig().getInt("port")));
                    plugin.restartWebServer();
                    sender.sendMessage(TwitchMinecraft.color("&aSuccessfully restarted the WebServer."));
                });
            } else sender.sendMessage(TwitchMinecraft.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }
}