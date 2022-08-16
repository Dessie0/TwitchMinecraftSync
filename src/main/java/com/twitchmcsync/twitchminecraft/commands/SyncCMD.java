package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.webserver.TwitchHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncCMD implements TabExecutor {

    private final TwitchMinecraft plugin;

    public SyncCMD(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("sync")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;

                if(this.getPlugin().getChannelID() == null) {
                    player.sendMessage(TwitchMinecraft.color("&cUnable to sync, plugin is not setup properly!"));
                    return true;
                }

                ComponentBuilder builder = new ComponentBuilder("");

                //If the user should be forced to verify their Twitch account.
                boolean force = args.length > 0 && args[0].equalsIgnoreCase("force");

                String url = "https://twitchmcsync.com?client_id=" + this.getPlugin().getConfig().getString("clientID")
                        + "&redirect_uri=" + this.getPlugin().getConfig().getString("redirectURI")
                        + "&response=send_to_twitch" + (force ? "&force_verify=true" : "")
                        + "&uuid=" + player.getUniqueId();

                //Send Bedrock players the link itself. They need to manually type it in.
                if(this.getPlugin().isFloodGateEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    player.sendMessage(TwitchMinecraft.color("&aPlease type this link into a browser to sync your Twitch! &d") + url);
                } else {
                    builder.append("Click ").color(ChatColor.GREEN)
                            .append("here").color(ChatColor.LIGHT_PURPLE).event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                            .append(" to sync your Twitch account to this server!").color(ChatColor.GREEN);
                    player.spigot().sendMessage(builder.create());
                }

                //Create a TwitchHandler for this sync.
                new TwitchHandler(new TwitchPlayer(this.getPlugin(), player));

                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("sync")) {
            if(sender instanceof Player && args.length == 1) {
                return Collections.singletonList("force");
            }
        }

        return new ArrayList<>();
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}
