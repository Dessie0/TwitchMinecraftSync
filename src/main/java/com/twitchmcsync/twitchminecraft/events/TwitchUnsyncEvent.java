package com.twitchmcsync.twitchminecraft.events;

import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

@Getter
public class TwitchUnsyncEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID uuid;
    private final TwitchPlayer twitchPlayer;
    private final ExpireMethod method;

    /**
     * This event will be fired when any of the following occur:
     *   - A user attempts to join the server, and the last time they joined, they were subscribed, but now they aren't.
     *   - A user's link is revoked using /revoke.
     *   - A user /unsyncs themselves.
     *
     * @param uuid
     * @param twitchPlayer
     */
    public TwitchUnsyncEvent(ExpireMethod method, UUID uuid, TwitchPlayer twitchPlayer) {
        this.method = method;
        this.uuid = uuid;
        this.twitchPlayer = twitchPlayer;
    }

    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }

    public static enum ExpireMethod {
        UNSYNCED, REVOKED, NOT_SUBBED
    }
}
