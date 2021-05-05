package com.twitchmcsync.twitchminecraft.events.twitchminecraft;

import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TwitchRevokeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final TwitchPlayer twitchPlayer;
    private boolean cancelled;

    public TwitchRevokeEvent(TwitchPlayer twitchPlayer) {
        this.twitchPlayer = twitchPlayer;
    }

    public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }

    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
