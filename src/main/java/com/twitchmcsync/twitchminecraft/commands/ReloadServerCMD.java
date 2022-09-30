package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Collections;

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
                    this.getPlugin().getLanguage().sendMessage(sender, "stopping_webserver", Collections.singletonMap("port", String.valueOf(this.getPlugin().getWebServer().getServer().getAddress().getPort())));
                    this.getPlugin().getLanguage().sendMessage(sender, "starting_webserver", Collections.singletonMap("port", String.valueOf(this.getPlugin().getWebServer().getServer().getAddress().getPort())));
                    this.getPlugin().restartWebServer();

                    this.getPlugin().getLanguage().sendMessage(sender, "webserver_restart_success");
                });
            } else this.getPlugin().getLanguage().sendMessage(sender, "no_permission");

            return true;
        }
        return false;
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}