package com.twitchmcsync.twitchminecraft.twitchapi;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.TwitchAuth;
import com.github.twitch4j.common.exception.NotFoundException;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.SubscriptionList;
import com.github.twitch4j.helix.domain.User;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.AccessTokenInvalidException;
import com.twitchmcsync.twitchminecraft.authentication.NotSubscribedException;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.twitchapi.twitch.OAuthToken;
import com.twitchmcsync.twitchminecraft.twitchapi.twitch.TwitchService;
import lombok.Getter;
import org.bukkit.Bukkit;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Getter
public class TwitchWrapper {

    private TwitchMinecraft plugin;
    private String broadcaster;
    private String broadcasterId;
    private TwitchClient client;

    private TwitchService twitchService;

    private CredentialManager credentialManager;

    private String clientId;
    private String clientSecret;

    public TwitchWrapper(TwitchMinecraft plugin, String broadcaster, String clientId, String clientSecret) {
        this.broadcaster = broadcaster;
        this.plugin = plugin;
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        this.twitchService = new Retrofit.Builder()
                .baseUrl("https://id.twitch.tv/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(TwitchService.class);

        if(this.getClientId() == null || this.getClientSecret() == null || this.getBroadcaster() == null) {
            this.getPlugin().getLogger().severe("Unable to setup TwitchMinecraftSync, please verify Client ID, Client Secret, and Broadcaster are correct.");
            return;
        }

        if(this.getClientId().contains("<") || this.getClientSecret().contains("<")) {
            this.getPlugin().getLogger().log(Level.INFO, "Welcome to TwitchMinecraftSync! Please follow the directions found at https://github.com/Dessie0/TwitchMinecraftSync#readme");
            return;
        }

        this.credentialManager = CredentialManagerBuilder.builder().build();
        TwitchAuth.registerIdentityProvider(credentialManager, clientId, clientSecret, "https://twitchmcsync.com", false);

        this.client = TwitchClientBuilder.builder()
                .withCredentialManager(this.getCredentialManager())
                .withClientSecret(this.getClientSecret())
                .withClientId(this.getClientId())
                .withEnableHelix(true)
                .withDefaultEventHandler(SimpleEventHandler.class)
                .build();

        this.retrieveAndSetBroadcasterId();
    }

    public CompletableFuture<Boolean> isUserLive(String channelId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            StreamList list = this.getClient().getHelix().getStreams(null, null, null, null, null, null, List.of(channelId), null).execute();

            future.complete(list != null && !list.getStreams().isEmpty());
        });

        return future;
    }

    public CompletableFuture<Boolean> isUserLive(TwitchPlayer player) {
        return this.isUserLive(player.getChannelID());
    }

    private CompletableFuture<String> retrieveAndSetBroadcasterId() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            this.getUser(this.getBroadcaster()).thenAccept(user -> {
                this.broadcasterId = user.getId();
                this.getPlugin().getLogger().info("Twitch syncing enabled for " + this.getBroadcaster() + " (" + this.getBroadcasterId() + ")");
            });
        });

        return future;
    }

    public CompletableFuture<User> getUser(String twitchUsername) {
        CompletableFuture<User> future = new CompletableFuture<>();

        try {
            List<User> users = this.getClient().getHelix().getUsers(null, null, Collections.singletonList(twitchUsername.toLowerCase()))
                    .queue().get().getUsers();
            if(!users.isEmpty()) {
                future.complete(users.getFirst());
            } else {
                this.getPlugin().getLogger().severe("Unable to obtain user id for " + twitchUsername + " please make sure it's been spelled correctly.");
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return future;
    }

    public CompletableFuture<User> getUserFromToken(OAuthToken token) {
        CompletableFuture<User> future = new CompletableFuture<>();

        try {
            List<User> users = this.getClient().getHelix().getUsers(token.getAccessToken(), null, null).queue().get().getUsers();
            if(!users.isEmpty()) {
                future.complete(users.getFirst());
            } else {
                this.getPlugin().getLogger().severe("Unable to obtain user id from token.");
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return future;
    }

    public CompletableFuture<OAuthToken> refreshToken(TwitchPlayer player) {
        return this.getTwitchService().refreshAccessToken(this.getClientId(), this.getClientSecret(), player.getToken().getRefreshToken(), "refresh_token")
                .whenComplete((response, throwable) -> {
                    if(throwable == null) return;
                    throwable.printStackTrace();
                });
    }

    public CompletableFuture<OAuthToken> getToken(String code) {
        return this.getTwitchService().getAccessToken(this.getClientId(), this.getClientSecret(), code, "authorization_code", "https://twitchmcsync.com/oauth")
                .whenComplete((response, throwable) -> {
                    if(throwable == null) return;
                    throwable.printStackTrace();
                });
    }

    public CompletableFuture<Subscription> getSubscription(OAuthToken token, String channelId) {
        CompletableFuture<Subscription> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            try {
                SubscriptionList subscriptions = this.getClient().getHelix()
                        .checkUserSubscription(token.getAccessToken(), this.getBroadcasterId(), channelId).queue().get();
                future.complete(subscriptions.getSubscriptions().isEmpty() ? null : subscriptions.getSubscriptions().getFirst());
            } catch (ExecutionException e) {
                //TODO This method of error handling is actually ridiculous and Twitch4J is stupid for implementing it this way.
                //Hopefully temporary until I can find a better way to handle this.
                if(e.getCause() instanceof HystrixRuntimeException hystrixEx) {
                    if(hystrixEx.getCause() instanceof NotFoundException ex) {
                        future.completeExceptionally(new NotSubscribedException());
                    }

                    if(hystrixEx.getCause() instanceof RuntimeException runtime && runtime.getMessage().contains("authentication token may be invalid")) {
                        future.completeExceptionally(new AccessTokenInvalidException("Access token invalid"));
                    }
                }
            } catch (InterruptedException e) {
                future.complete(null);
                throw new RuntimeException(e);
            }
        });

        return future;
    }

    public CompletableFuture<Subscription> getSubscriptionWithRefreshToken(TwitchPlayer twitchPlayer) {
        return this.getSubscription(twitchPlayer.getToken(), twitchPlayer.getChannelID()).handle((result, throwable) -> {
            if(throwable == null) return CompletableFuture.completedFuture(result);

            if(throwable instanceof AccessTokenInvalidException) {
                return this.refreshToken(twitchPlayer).thenCompose(newToken -> {
                    twitchPlayer.setToken(newToken);
                    twitchPlayer.save();
                    return this.getSubscription(twitchPlayer.getToken(), twitchPlayer.getChannelID());
                });
            }

            // Re-throw other exceptions
            CompletableFuture<Subscription> failed = new CompletableFuture<>();
            failed.completeExceptionally(throwable);
            return failed;
        }).thenCompose(f -> f);
    }
}
