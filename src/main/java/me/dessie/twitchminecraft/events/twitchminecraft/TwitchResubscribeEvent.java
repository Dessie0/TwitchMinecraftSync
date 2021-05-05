package me.dessie.twitchminecraft.events.twitchminecraft;

import me.dessie.twitchminecraft.TwitchPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TwitchResubscribeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final TwitchPlayer twitchPlayer;
    private boolean cancelled;


    public TwitchResubscribeEvent(TwitchPlayer twitchPlayer) {
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
