package me.dessie.twitchminecraft;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
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

    public static void giveResub(TwitchPlayer player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            //Remove all possible roles from the player.
            //This will be re-applied after, but we want to make
            //sure that we're not duplicating roles if they
            //upgraded their tier.
            Arrays.asList(getResubRole(1), getResubRole(2), getResubRole(3))
                    .stream().filter(role -> role.length() > 0)
                    .forEach(role -> {
                        plugin.permission.playerRemoveGroup(null, player.getPlayer(), role);
                    });

            //Re-add the role based on their re-sub tier.
            String role = getResubRole(player.getTier());
            if(role.length() > 0) {
                plugin.permission.playerAddGroup(null, player.getPlayer(), role);
            }

            //Dispatch all the commands
            Map<String, CommandSender> commands = getResubCommands(player);
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
            Map<String, CommandSender> commands = getExpireCommands(player);
            for(String command : commands.keySet()) {
                Bukkit.dispatchCommand(commands.get(command), command);
            }
        });
    }

    //Get roles.
    public static String getGiveRole(int tier) { return plugin.getConfig().getString("rewards.tier" + tier + ".subscribe.role"); }
    public static String getResubRole(int tier) { return plugin.getConfig().getString("rewards.tier" + tier + ".resubscribe.role"); }
    public static String getRemoveRole(int tier) { return plugin.getConfig().getString("rewards.tier" + tier + ".expire.role"); }

    public static Map<String, CommandSender> getGiveCommands(TwitchPlayer player) { return getCommands(player, "subscribe"); }
    public static Map<String, CommandSender> getResubCommands(TwitchPlayer player) { return getCommands(player, "resubscribe"); }
    public static Map<String, CommandSender> getExpireCommands(TwitchPlayer player) { return getCommands(player, "expire"); }

    //Get all the commands in the tier.
    //Replace all placeholders where necessary
    //Determine who runs the command (-p)
    private static Map<String, CommandSender> getCommands(TwitchPlayer player, String type) {
        Map<String, CommandSender> commands = new HashMap<>();

        for(String command : plugin.getConfig().getStringList("rewards.tier" + player.getTier() + "." + type + ".commands")) {

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
