package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import me.dessie.dessielib.annotations.command.CommandAliases;
import me.dessie.dessielib.annotations.command.CommandPermission;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.command.CommandSender;

@MinecraftCommand(name = "twitchreload", description = "Reloads TwitchMCSync")
@CommandPermission(permission = "twitchmcsync.reload")
@CommandAliases(aliases = "treload")
public class ReloadCommand extends XCommand {

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission(this.getPermission()) && !sender.isOp()) {
            TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "no_permission");
        }

        TwitchMinecraft.getInstance().reloadPlugin();
        TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "reload");
    }
}
