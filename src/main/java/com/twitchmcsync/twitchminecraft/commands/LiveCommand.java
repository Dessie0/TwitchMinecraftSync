package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.lang.Language;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.command.CommandSender;

import java.util.Map;

@MinecraftCommand(name = "live", description = "Displays all live users")
public class LiveCommand extends XCommand {

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if(TwitchMinecraft.getInstance().getLiveModule() == null) {
            TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "live.module_not_enabled");
            return;
        }
        Language language = TwitchMinecraft.getInstance().getLanguage();

        if(TwitchMinecraft.getInstance().getLiveModule().getLivePlayers().isEmpty()) {
            language.sendMessage(sender, "live.no_users_live");
            return;
        }

        language.sendMessage(sender, "live.command_header");
        for(TwitchPlayer live : TwitchMinecraft.getInstance().getLiveModule().getLivePlayers()) {
            language.sendMessage(sender, "live.user_display", Map.of("%player%", live.getPlayer().getName(), "%twitchname%", live.getChannelName()));
        }
        language.sendMessage(sender, "live.command_footer");
    }
}
