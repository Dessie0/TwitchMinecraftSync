package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.submode.SubMode;
import me.dessie.dessielib.annotations.command.CommandPermission;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.command.CommandSender;

@MinecraftCommand(name = "submode", description = "Enable sub-mode only.")
@CommandPermission(permission = "twitchmcsync.submode")
public class SubModeCommand extends XCommand {

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission(this.getPermission()) && !sender.isOp()) {
            TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, "no_permission");
            return;
        }

        SubMode subMode = TwitchMinecraft.getInstance().getSubMode();
        subMode.setEnabled(!subMode.isForcedEnabled());
        subMode.save();

        //If it was turned off, re-eval the sub windows.
        if(!subMode.isForcedEnabled()) {
            subMode.evaluateSubWindows();
        }

        TwitchMinecraft.getInstance().getLanguage().sendMessage(sender, subMode.isForcedEnabled() ? "submode.enabled" : "submode.disabled");
        subMode.attemptKickAllPlayers();
    }
}
