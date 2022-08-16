package com.twitchmcsync.twitchminecraft;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

public class RewardHandler {

    private final TwitchMinecraft plugin;
    
    public RewardHandler(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }
    
    public void give(TwitchPlayer player) {
        Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
            String role = getGiveRole(player.getTier());
            addGroup(player.getPlayer(), role);

            //Dispatch all the commands
            Map<String, CommandSender> commands = getGiveCommands(player);
            for(String command : commands.keySet()) {
                Bukkit.dispatchCommand(commands.get(command), command);
            }
        });
    }

    public void giveResub(TwitchPlayer player) {
        Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
            //Remove all possible roles from the player.
            //This will be re-applied after, but we want to make
            //sure that we're not duplicating roles if they
            //upgraded their tier.
            if(this.getPlugin().isVaultEnabled()) {
                Stream.of(getResubRole(1), getResubRole(2), getResubRole(3))
                        .filter(role -> role.length() > 0)
                        .forEach(role -> {
                            removeGroup(player.getPlayer(), role);
                        });

                //Re-add the role based on their re-sub tier.
                String role = getResubRole(player.getTier());
                addGroup(player.getPlayer(), role);
            }

            //Dispatch all the commands
            Map<String, CommandSender> commands = getResubCommands(player);
            for(String command : commands.keySet()) {
                Bukkit.dispatchCommand(commands.get(command), command);
            }
        });
    }

    public void remove(TwitchPlayer player) {
        Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
            String role = getRemoveRole(player.getTier());
            removeGroup(player.getPlayer(), role);

            //Dispatch all the commands
            Map<String, CommandSender> commands = getExpireCommands(player);
            for(String command : commands.keySet()) {
                Bukkit.dispatchCommand(commands.get(command), command);
            }
        });
    }

    //Get roles.
    public String getGiveRole(int tier) { return this.getPlugin().getConfig().getString("rewards.tier" + tier + ".subscribe.role"); }
    public String getResubRole(int tier) { return this.getPlugin().getConfig().getString("rewards.tier" + tier + ".resubscribe.role"); }
    public String getRemoveRole(int tier) { return this.getPlugin().getConfig().getString("rewards.tier" + tier + ".expire.role"); }

    public Map<String, CommandSender> getGiveCommands(TwitchPlayer player) { return getCommands(player, "subscribe"); }
    public Map<String, CommandSender> getResubCommands(TwitchPlayer player) { return getCommands(player, "resubscribe"); }
    public Map<String, CommandSender> getExpireCommands(TwitchPlayer player) { return getCommands(player, "expire"); }

    //Get all the commands in the tier.
    //Replace all placeholders where necessary
    //Determine who runs the command (-p)
    private Map<String, CommandSender> getCommands(TwitchPlayer player, String type) {
        Map<String, CommandSender> commands = new HashMap<>();

        for(String command : this.getPlugin().getConfig().getStringList("rewards.tier" + player.getTier() + "." + type + ".commands")) {
            command = command.replaceAll("%player%", player.getName());
            command = command.replaceAll("%twitchname%", player.getChannelName());

            if(command.substring(command.length() - 2).equalsIgnoreCase("-p")) {
                commands.put(command.substring(0, command.length() - 2), player.getPlayer().getPlayer());
                continue;
            }
            commands.put(command, this.getPlugin().getServer().getConsoleSender());
        }

        return commands;
    }

    private void removeGroup(OfflinePlayer player, String role) {
        if(role.length() > 0 && this.getPlugin().isVaultEnabled()) {
            try {
                this.getPlugin().getPermission().playerRemoveGroup(null, player.getPlayer(), role);
            } catch (UnsupportedOperationException e) {
                this.getPlugin().getLogger().log(Level.SEVERE, "Unable to remove player from group \"" + role + "\" because it does not exist!");
            }
        }
    }

    private void addGroup(OfflinePlayer player, String role) {
        if(role.length() > 0 && this.getPlugin().isVaultEnabled()) {
            try {
                this.getPlugin().getPermission().playerAddGroup(null, player.getPlayer(), role);
            } catch (UnsupportedOperationException e) {
                this.getPlugin().getLogger().log(Level.SEVERE, "Unable to add player to group \"" + role + "\" because it does not exist!");
            }
        }
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}
