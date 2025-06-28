package com.twitchmcsync.twitchminecraft.events;

import com.github.twitch4j.helix.domain.Subscription;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

@Getter
public class TwitchSyncEvent extends Event  {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final TwitchPlayer twitchPlayer;
    private final Subscription subscription;
    private final int tier;

    /**
     * This event will be fired when any of the following occur:
     *   - A user joins the server for the first time after syncing.
     *   - A user is online and syncs.
     *   - A user joins and their subscription tier has been changed.
     *
     * @param player The Player that joined the server.
     * @param twitchPlayer The TwitchPlayer as they join the server. isSubbed will be false.
     * @param subscription The subscription instance, if any.
     * @param tier The new tier, will be 0 if they are not subscribed.
     */
    public TwitchSyncEvent(Player player, TwitchPlayer twitchPlayer, @Nullable Subscription subscription, int tier) {
        this.player = player;
        this.twitchPlayer = twitchPlayer;
        this.subscription = subscription;
        this.tier = tier;
    }

    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
