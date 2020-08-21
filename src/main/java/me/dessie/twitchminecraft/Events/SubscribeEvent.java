package me.dessie.twitchminecraft.Events;

import me.dessie.twitchminecraft.Events.twitchminecraft.TwitchSubscribeEvent;
import me.dessie.twitchminecraft.RewardHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SubscribeEvent implements Listener {

    @EventHandler
    public void subscribeEvent(TwitchSubscribeEvent event) {
        RewardHandler.give(event.getTwitchPlayer());
    }
}
