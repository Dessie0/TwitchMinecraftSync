package com.twitchmcsync.twitchminecraft.commands;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.TwitchUnsyncEvent;
import com.twitchmcsync.twitchminecraft.submode.SubMode;
import me.dessie.dessielib.annotations.command.MinecraftCommand;
import me.dessie.dessielib.commandapi.XCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@MinecraftCommand(name = "unsync", description = "Un-links your own Twitch account.")
public class UnsyncCommand extends XCommand {

    @Override
    protected void execute(Player player, String[] args) {

        TwitchPlayer.load(player.getUniqueId()).thenAccept(twitchPlayer -> {
            if(twitchPlayer == null) {
                TwitchMinecraft.getInstance().getLanguage().sendMessage(player, "not_synced");
                return;
            }

            //Send out the ExpireEvent
            Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
                Bukkit.getServer().getPluginManager().callEvent(new TwitchUnsyncEvent(TwitchUnsyncEvent.ExpireMethod.UNSYNCED, player.getUniqueId(), twitchPlayer));

                //Kick them if Submode is enabled.
                SubMode subMode = TwitchMinecraft.getInstance().getSubMode();
                if(subMode.canKickUser(player)) {
                    player.kick(TwitchMinecraft.getInstance().getLanguage().getComponent("submode.kicked"));
                } else {
                    TwitchMinecraft.getInstance().getLanguage().sendMessage(player, "unsync_success");
                }

                //Delete from the storage.
                twitchPlayer.delete();
            });
        });
    }
}
