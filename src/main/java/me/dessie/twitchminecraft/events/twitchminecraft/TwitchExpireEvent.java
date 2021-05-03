package me.dessie.twitchminecraft.events.twitchminecraft;

import me.dessie.twitchminecraft.TwitchPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TwitchExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    TwitchPlayer twitchPlayer;

    public TwitchExpireEvent(TwitchPlayer twitchPlayer) {
        this.twitchPlayer = twitchPlayer;
    }

    public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }

    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

}
