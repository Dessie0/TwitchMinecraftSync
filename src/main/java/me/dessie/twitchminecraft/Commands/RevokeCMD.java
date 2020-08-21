package me.dessie.twitchminecraft.Commands;

import me.dessie.twitchminecraft.RewardHandler;
import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class RevokeCMD implements CommandExecutor {

    private static TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("revoke")) {
            if(sender.hasPermission("twitchmcsync.revoke")) {
                if(args.length > 0) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                    if(TwitchPlayer.playerExists(player.getUniqueId().toString())) {
                        if(player.isOnline()) {
                            revoke(player.getPlayer(), false);
                            sender.sendMessage(plugin.color("&aSuccessfully revoked " + player.getName() + "'s Twitch authorization. They will need to re-sync."));
                        } else {
                            plugin.twitchConfig.set("revoked", Arrays.asList(plugin.twitchConfig.getStringList("revoked"), player.getUniqueId().toString()));
                            plugin.saveFile(plugin.twitchData, plugin.twitchConfig);
                            sender.sendMessage(plugin.color( "&c" + player.getName() + " is offline. Their authorization will be revoked when they login."));
                        }
                    } else sender.sendMessage(plugin.color("&d" + args[0] + "&c doesn't exist or hasn't synced their Twitch account!"));
                } else sender.sendMessage(plugin.color("&cYou need to enter a player to revoke permissions!"));
            } else sender.sendMessage(plugin.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }

    public static void revoke(Player player, boolean removeFromConfig) {
        player.getPlayer().sendMessage(plugin.color("&cYour Twitch authorization has been revoked!"));
        player.getPlayer().sendMessage(plugin.color("&cYou can re-sync by typing /sync"));

        TwitchPlayer twitchPlayer = TwitchPlayer.create(player.getPlayer());

        //Execute remove rewards for this player.
        RewardHandler.remove(twitchPlayer);

        //Remove them from the YAML file.
        twitchPlayer.saveData(false);

        if(removeFromConfig) {
            List<String> revoke = plugin.twitchConfig.getStringList("revoked");
            revoke.remove(player.getUniqueId().toString());
            plugin.twitchConfig.set("revoked", revoke);
            plugin.saveFile(plugin.twitchData, plugin.twitchConfig);
        }
    }
}
