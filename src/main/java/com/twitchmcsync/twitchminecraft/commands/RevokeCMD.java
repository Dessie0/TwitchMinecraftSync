package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchRevokeEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.time.ZonedDateTime;
import java.util.Collections;

public class RevokeCMD implements CommandExecutor {

    private final TwitchMinecraft plugin;
    
    public RevokeCMD(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("revoke")) {
            if(!sender.hasPermission("twitchmcsync.revoke")) {
                this.getPlugin().getLanguage().sendMessage(sender, "no_permission");
                return true;
            }

            if(args.length <= 0) {
                this.getPlugin().getLanguage().sendMessage(sender, "enter_player_argument");
                return true;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            if (TwitchPlayer.isSubbed(player.getUniqueId().toString())) {
                TwitchPlayer tPlayer = TwitchPlayer.create(player);

                if (player.isOnline()) {
                    this.getPlugin().getRewardHandler().remove(tPlayer);
                    this.getPlugin().getLanguage().sendMessage(sender, "revoke_success", Collections.singletonMap("player", player.getName()));
                } else {
                    //Save revoke information.
                    this.getPlugin().getTwitchConfig().set("revoked." + player.getUniqueId() + ".tier", tPlayer.getTier());

                    TwitchMinecraft.saveFile(this.getPlugin().getTwitchData(), this.getPlugin().getTwitchConfig());
                    this.getPlugin().getLanguage().sendMessage(sender, "revoke_offline", Collections.singletonMap("player", player.getName()));
                }

                this.revoke(player);
            } else this.getPlugin().getLanguage().sendMessage(sender, "no_synced_account", Collections.singletonMap("player", args[0]));

            return true;
        }
        return false;
    }

    private void revoke(OfflinePlayer player) {
        if(player.isOnline()) {
            this.getPlugin().getLanguage().sendMessage(player.getPlayer(), "revoked");
        }

        TwitchPlayer twitchPlayer = TwitchPlayer.create(player);

        //Call the revoke event.
        Bukkit.getPluginManager().callEvent(new TwitchRevokeEvent(twitchPlayer));

        //Set the last sub date, which was now.
        twitchPlayer.setLastSubDate(ZonedDateTime.now().toString());

        //Remove them from the YAML file.
        twitchPlayer.saveData(false);
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}
