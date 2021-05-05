package me.dessie.twitchminecraft.commands;

import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.webserver.WebServer;
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
                    sender.sendMessage(TwitchMinecraft.color("&aStopping WebServer on port &d" + plugin.webServer.server.getAddress().getPort()));
                    //Stop the webserver.
                    plugin.webServer.remove();

                    sender.sendMessage(TwitchMinecraft.color("&aStarting new WebServer on port &d" + plugin.getConfig().getInt("port")));
                    plugin.webServer = new WebServer();
                    plugin.webServer.create(plugin.getConfig().getInt("port"));

                    sender.sendMessage(TwitchMinecraft.color("&aSuccessfully restarted the WebServer."));
                });
            } else sender.sendMessage(TwitchMinecraft.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }
}