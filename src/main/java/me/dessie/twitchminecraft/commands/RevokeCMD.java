package me.dessie.twitchminecraft.commands;

import me.dessie.twitchminecraft.RewardHandler;
import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import me.dessie.twitchminecraft.events.twitchminecraft.TwitchRevokeEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
                    if(TwitchPlayer.isSubbed(player.getUniqueId().toString())) {
                        TwitchPlayer tPlayer = TwitchPlayer.create(player);

                        if(player.isOnline()) {
                            RewardHandler.remove(tPlayer);
                            sender.sendMessage(TwitchMinecraft.color("&aSuccessfully revoked " + player.getName() + "'s Twitch authorization. They will need to re-sync."));
                        } else {
                            //Save revoke information.
                            TwitchMinecraft.getTwitchConfig().set("revoked." + player.getUniqueId().toString() + ".tier", tPlayer.getTier());

                            TwitchMinecraft.saveFile(TwitchMinecraft.getTwitchData(), TwitchMinecraft.getTwitchConfig());
                            sender.sendMessage(TwitchMinecraft.color( "&c" + player.getName() + " is offline. Their authorization will be revoked when they login."));
                        }

                        revoke(player);
                    } else sender.sendMessage(TwitchMinecraft.color("&d" + args[0] + "&c doesn't exist or hasn't synced their Twitch account!"));
                } else sender.sendMessage(TwitchMinecraft.color("&cYou need to enter a player to revoke permissions!"));
            } else sender.sendMessage(TwitchMinecraft.color("&cYou do not have permission to do that!"));

            return true;
        }
        return false;
    }

    private static void revoke(OfflinePlayer player) {
        if(player.isOnline()) {
            player.getPlayer().sendMessage(TwitchMinecraft.color("&cYour Twitch authorization has been revoked!"));
            player.getPlayer().sendMessage(TwitchMinecraft.color("&cYou can re-sync by typing /sync"));
        }

        TwitchPlayer twitchPlayer = TwitchPlayer.create(player);

        //Call the revoke event.
        Bukkit.getPluginManager().callEvent(new TwitchRevokeEvent(twitchPlayer));

        //Set the last sub date, which was now.
        twitchPlayer.setLastSubDate(ZonedDateTime.now().toString());

        //Remove them from the YAML file.
        twitchPlayer.saveData(false);
    }
}
