package com.twitchmcsync.twitchminecraft.authentication;

import com.github.twitch4j.helix.domain.Subscription;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.events.TwitchSyncEvent;
import com.twitchmcsync.twitchminecraft.events.TwitchUnsyncEvent;
import lombok.SneakyThrows;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class JoinAuthorization implements Listener {

    private Map<UUID, TwitchSyncEvent> joinSubscribers = new HashMap<>();

    @EventHandler
    @SneakyThrows
    public void onAttemptJoin(AsyncPlayerPreLoginEvent event) {
        //If SubMode is disabled, don't bother with any of this.
        if(!TwitchMinecraft.getInstance().getSubMode().isEnabled()) return;

        //Check if they have permission
        LuckPerms perms = TwitchMinecraft.getInstance().getLuckPerms();
        if(perms != null && perms.getUserManager().loadUser(event.getUniqueId()).join().getCachedData().getPermissionData().checkPermission("twitchmcsync.submode.bypass").asBoolean()) {
            return;
        }

        //Attempt to load in any data that may already exist.
        TwitchPlayer player = TwitchPlayer.load(event.getUniqueId()).join();
        if(player == null) {
            String code;
            try {
                code = CodeHandler.createCode(event.getUniqueId())
                        .orTimeout(5, TimeUnit.SECONDS)
                        .join();
            } catch (Exception e) {
                code = null;
            }

            if(code == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, TwitchMinecraft.getInstance().getLanguage().getComponent("code_generation_failed"));
            } else {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, TwitchMinecraft.getInstance().getLanguage().getComponent("submode.join_not_synced", Map.of("%code%", code)));
            }

            return;
        }

        //If they are linked, we need to check if they're subbed.
        try {
            Subscription subscription = TwitchMinecraft.getInstance().getTwitchWrapper().getSubscriptionWithRefreshToken(player).join();
            int tier = subscription == null ? 0 : Integer.parseInt(String.valueOf(subscription.getTier().charAt(0)));

            //If their loaded player object wasn't subbed, but they are not, trigger the subscription rewards for their tier.
            //If they're subbed, but the last known tier has changed, we'll trigger a subscribe event for the new tier.
            if((!player.isSubbed() && subscription != null) || (player.isSubbed() && tier != player.getTier())) {
                //Create a temporary event, this cannot be called directly but we can provide values here.
                joinSubscribers.put(event.getUniqueId(), new TwitchSyncEvent(null, player, subscription, tier));
            }

            //Check if they were subbed, but now they aren't.
            if(player.isSubbed() && subscription == null) {
                Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
                    Bukkit.getServer().getPluginManager().callEvent(new TwitchUnsyncEvent(TwitchUnsyncEvent.ExpireMethod.NOT_SUBBED, event.getUniqueId(), player));
                });
            }

            //They are not subscribed, do not allow them to join.
            if(subscription == null) {
                String broadcaster = TwitchMinecraft.getInstance().getBroadcasterUsername();
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, TwitchMinecraft.getInstance().getLanguage().getComponent("not_subscribed", Map.of("%broadcaster%", broadcaster)));
            }
        } catch (Exception e) {
            if(e instanceof CompletionException ex && ex.getCause() instanceof NotSubscribedException) {
                //Fire the unsync event for not being subscribed
                if(player.isSubbed()) {
                    Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
                        Bukkit.getServer().getPluginManager().callEvent(new TwitchUnsyncEvent(TwitchUnsyncEvent.ExpireMethod.NOT_SUBBED, event.getUniqueId(), player));
                    });
                }

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, TwitchMinecraft.getInstance().getLanguage().getComponent("not_subscribed", Map.of("%broadcaster%", TwitchMinecraft.getInstance().getBroadcasterUsername())));
                return;
            }

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, TwitchMinecraft.getInstance().getLanguage().getComponent("twitch_response_error"));
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(joinSubscribers.containsKey(event.getPlayer().getUniqueId())) {
            TwitchSyncEvent fakeEvent = joinSubscribers.get(event.getPlayer().getUniqueId());
            Bukkit.getServer().getPluginManager().callEvent(new TwitchSyncEvent(event.getPlayer(), fakeEvent.getTwitchPlayer(), fakeEvent.getSubscription(), fakeEvent.getTier()));
            joinSubscribers.remove(event.getPlayer().getUniqueId());
        }
    }
}
