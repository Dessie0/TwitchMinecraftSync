package com.twitchmcsync.twitchminecraft.events.twitchminecraft;

import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TwitchSubscribeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final TwitchPlayer twitchPlayer;
    private boolean cancelled;

    public TwitchSubscribeEvent(TwitchPlayer twitchPlayer) {
        this.twitchPlayer = twitchPlayer;
    }

    public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }

    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
