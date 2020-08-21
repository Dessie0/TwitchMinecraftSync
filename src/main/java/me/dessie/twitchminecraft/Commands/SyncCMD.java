package me.dessie.twitchminecraft.Commands;

import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import me.dessie.twitchminecraft.WebServer.TwitchHandler;
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

                plugin.handlers.put(player.getUniqueId().toString(), new TwitchHandler(new TwitchPlayer(player), player.getAddress().getAddress()));

                ComponentBuilder builder = new ComponentBuilder();

                String url = "https://id.twitch.tv/oauth2/authorize?response_type=code&client_id="
                        + plugin.getConfig().getString("clientID")
                        + "&scope=user_subscriptions+user_read"
                        + "&redirect_uri="
                        + plugin.getConfig().getString("redirectURI");

                builder.append("Click ").color(ChatColor.GREEN)
                        .append("here").color(ChatColor.LIGHT_PURPLE).event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .append(" to sync your Twitch account to this server!").color(ChatColor.GREEN);

                player.spigot().sendMessage(builder.create());

                return true;
            }
        }

        return false;
    }
}
