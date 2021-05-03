package me.dessie.twitchminecraft.events;

import me.dessie.twitchminecraft.events.twitchminecraft.TwitchExpireEvent;
import me.dessie.twitchminecraft.events.twitchminecraft.TwitchResubscribeEvent;
import me.dessie.twitchminecraft.events.twitchminecraft.TwitchSubscribeEvent;
import me.dessie.twitchminecraft.RewardHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SubscribeEvent implements Listener {

    @EventHandler
    public void subscribeEvent(TwitchSubscribeEvent event) {
        RewardHandler.give(event.getTwitchPlayer());
    }

    @EventHandler
    public void resubEvent(TwitchResubscribeEvent event) {
        RewardHandler.giveResub(event.getTwitchPlayer());
    }

    @EventHandler
    public void onExpire(TwitchExpireEvent event) {
        RewardHandler.remove(event.getTwitchPlayer());
    }
}
