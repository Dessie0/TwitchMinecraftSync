package com.twitchmcsync.twitchminecraft.authentication;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.storage.DatabaseManager;
import com.twitchmcsync.twitchminecraft.twitchapi.twitch.OAuthToken;
import lombok.Getter;
import lombok.Setter;
import me.dessie.dessielib.annotations.storageapi.RecomposeConstructor;
import me.dessie.dessielib.annotations.storageapi.Stored;
import me.dessie.dessielib.storageapi.format.flatfile.JSONContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class TwitchPlayer {
    private final TwitchMinecraft plugin;

    @Stored
    private String storedUuid;

    @Stored
    private final String channelID;

    @Stored
    private final String channelName;

    @Stored
    @Setter
    private OAuthToken token;

    @Stored
    private final int tier;

    @Stored
    @Setter
    private boolean subbed;

    private final UUID uuid;

    @RecomposeConstructor
    public TwitchPlayer(String uuid, String channelID, String channelName, OAuthToken token, int tier, boolean subbed) {
        this(TwitchMinecraft.getInstance(), UUID.fromString(uuid), channelID, channelName, token, tier, subbed);
    }

    public TwitchPlayer(TwitchMinecraft plugin, UUID uuid, String channelID, String channelName, OAuthToken token, int tier, boolean subbed) {
        this.plugin = plugin;
        this.storedUuid = uuid.toString();
        this.uuid = uuid;
        this.channelID = channelID;
        this.channelName = channelName;
        this.token = token;
        this.tier = tier;
        this.subbed = subbed;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(this.getUuid());
    }

    public static CompletableFuture<TwitchPlayer> loadFromChannelName(String channelName) {
        CompletableFuture<TwitchPlayer> future = new CompletableFuture<>();

        JSONContainer container = TwitchMinecraft.getInstance().getFlatfileContainer();
        DatabaseManager manager = TwitchMinecraft.getInstance().getDatabaseManager();

        //If the container is not null, we're using flatfile and not a DB.
        if(container != null) {
            for(String uuid : container.getKeys("data")) {
                if (container.retrieve(String.class, "data." + uuid + ".channelName").equalsIgnoreCase(channelName)) {
                    return container.retrieveAsync(TwitchPlayer.class, "data." + uuid);
                }
            }

            future.complete(null);
            return future;
        } else if(manager != null) {
            return manager.loadFromChannelName(channelName);
        }

        TwitchMinecraft.getInstance().getLogger().severe("Unable to load Twitch data due to invalid storage settings. Please check your config.");
        return future;
    }

    public static CompletableFuture<TwitchPlayer> loadFromChannelId(String channelID) {
        CompletableFuture<TwitchPlayer> future = new CompletableFuture<>();

        JSONContainer container = TwitchMinecraft.getInstance().getFlatfileContainer();
        DatabaseManager manager = TwitchMinecraft.getInstance().getDatabaseManager();

        //If the container is not null, we're using flatfile and not a DB.
        if(container != null) {
            for(String uuid : container.getKeys("data")) {
                if (container.retrieve(String.class, "data." + uuid + ".channelID").equalsIgnoreCase(channelID)) {
                    return container.retrieveAsync(TwitchPlayer.class, "data." + uuid);
                }
            }

            future.complete(null);
            return future;
        } else if(manager != null) {
            return manager.loadFromChannelId(channelID);
        }

        TwitchMinecraft.getInstance().getLogger().severe("Unable to load Twitch data due to invalid storage settings. Please check your config.");
        return future;
    }

    //TODO Add Cache for this.
    public static CompletableFuture<TwitchPlayer> load(UUID uuid) {
        CompletableFuture<TwitchPlayer> future = new CompletableFuture<>();

        JSONContainer container = TwitchMinecraft.getInstance().getFlatfileContainer();
        DatabaseManager manager = TwitchMinecraft.getInstance().getDatabaseManager();

        //If the container is not null, we're using flatfile and not a DB.
        if(container != null) {
            if(!container.getKeys("data").contains(uuid.toString())) {
                future.complete(null);
                return future;
            }
            return container.retrieveAsync(TwitchPlayer.class, "data." + uuid).thenApply(player -> player);
        } else if(manager != null) {
            return manager.loadPlayer(uuid);
        }

        TwitchMinecraft.getInstance().getLogger().severe("Unable to load Twitch data due to invalid storage settings. Please check your config.");
        return future;
    }

    public void save() {
        JSONContainer container = TwitchMinecraft.getInstance().getFlatfileContainer();
        DatabaseManager manager = TwitchMinecraft.getInstance().getDatabaseManager();

        if(container != null) {
            container.store("data." + this.getUuid(), this);
            return;
        } else if(manager != null) {
            manager.savePlayer(this);
            return;
        }

        TwitchMinecraft.getInstance().getLogger().severe("Unable to save Twitch data due to invalid storage settings. Please check your config.");
    }

    public void delete() {
        JSONContainer container = TwitchMinecraft.getInstance().getFlatfileContainer();
        DatabaseManager manager = TwitchMinecraft.getInstance().getDatabaseManager();

        if(container != null) {
            container.store("data." + this.getUuid(), null);
            return;
        } else if(manager != null) {
            manager.deletePlayer(this);
            return;
        }

        TwitchMinecraft.getInstance().getLogger().severe("Unable to save Twitch data due to invalid storage settings. Please check your config.");
    }
}
