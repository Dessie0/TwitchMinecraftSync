package me.dessie.twitchminecraft.Events.twitchminecraft;

import me.dessie.twitchminecraft.TwitchPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TwitchResubscribeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    TwitchPlayer twitchPlayer;

    public TwitchResubscribeEvent(TwitchPlayer twitchPlayer) {
        this.twitchPlayer = twitchPlayer;
    }

    public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }

    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

}
