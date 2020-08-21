package me.dessie.twitchminecraft;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class RewardHandler {

    private static TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    public static void give(TwitchPlayer player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String role = getGiveRole(player.getTier());
            if(role.length() > 0) {
                plugin.permission.playerAddGroup(null, player.getPlayer(), role);
            }

            //Dispatch all the commands
            Map<String, CommandSender> commands = getGiveCommands(player);
            for(String command : commands.keySet()) {
                Bukkit.dispatchCommand(commands.get(command), command);
            }
        });
    }

    public static void remove(TwitchPlayer player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String role = getRemoveRole(player.getTier());
            if(role.length() > 0) {
                plugin.permission.playerRemoveGroup(null, player.getPlayer(), role);
            }

            //Dispatch all the commands
            Map<String, CommandSender> commands = getRevokeCommands(player);
            for(String command : commands.keySet()) {
                Bukkit.dispatchCommand(commands.get(command), command);
            }
        });
    }

    //Get roles.
    private static String getGiveRole(int tier) { return plugin.getConfig().getString("rewards.tier" + tier + ".subscribe.role"); }
    private static String getRemoveRole(int tier) { return plugin.getConfig().getString("rewards.tier" + tier + ".expire.role"); }

    //Get all the commands in the tier.
    //Replace all placeholders where necessary
    //Determine who runs the command (-p)
    private static Map<String, CommandSender> getGiveCommands(TwitchPlayer player) {
        Map<String, CommandSender> commands = new HashMap<>();

        for(String command : plugin.getConfig().getStringList("rewards.tier" + player.getTier() + ".subscribe.commands")) {

            command = command.replaceAll("%player%", player.getName());
            command = command.replaceAll("%twitchname%", player.getChannelName());

            if(command.substring(command.length() - 2).equalsIgnoreCase("-p")) {
                commands.put(command.substring(0, command.length() - 2), player.getPlayer().getPlayer());
                continue;
            }
            commands.put(command, plugin.getServer().getConsoleSender());
        }

        return commands;
    }

    private static Map<String, CommandSender> getRevokeCommands(TwitchPlayer player) {
        Map<String, CommandSender> commands = new HashMap<>();

        for(String command : plugin.getConfig().getStringList("rewards.tier" + player.getTier() + ".expire.commands")) {

            command = command.replaceAll("%player%", player.getName());
            command = command.replaceAll("%twitchname%", player.getChannelName());

            if(command.substring(command.length() - 2).equalsIgnoreCase("-p")) {
                commands.put(command.substring(0, command.length() - 2), player.getPlayer().getPlayer());
                continue;
            }
            commands.put(command, plugin.getServer().getConsoleSender());
        }

        return commands;
    }
}
