package me.dessie.twitchminecraft.commands;

import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import me.dessie.twitchminecraft.webserver.TwitchHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SyncCMD implements CommandExecutor {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("sync")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;

                ComponentBuilder builder = new ComponentBuilder();

                //If the user should be forced to verify their Twitch account.
                boolean force = args.length > 0 && args[0].equalsIgnoreCase("true");

                String url = "https://twitchmcsync.com?client_id=" + plugin.getConfig().getString("clientID")
                        + "&redirect_uri=" + plugin.getConfig().getString("redirectURI")
                        + "&response=send_to_twitch" + (force ? "&force_verify=true" : "")
                        + "&uuid=" + player.getUniqueId().toString()
                        + "&channel=" + plugin.getConfig().getString("channelName");

                builder.append("Click ").color(ChatColor.GREEN)
                        .append("here").color(ChatColor.LIGHT_PURPLE).event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .append(" to sync your Twitch account to this server!").color(ChatColor.GREEN);

                player.spigot().sendMessage(builder.create());

                //Create a TwitchHandler for this sync.
                new TwitchHandler(new TwitchPlayer(player));

                return true;
            }
        }

        return false;
    }
}
