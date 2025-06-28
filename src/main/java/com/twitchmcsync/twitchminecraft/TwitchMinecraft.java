package com.twitchmcsync.twitchminecraft;

import com.twitchmcsync.twitchminecraft.authentication.JoinAuthorization;
import com.twitchmcsync.twitchminecraft.authentication.TwitchAuthWebServer;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.commands.*;
import com.twitchmcsync.twitchminecraft.events.TwitchEventListener;
import com.twitchmcsync.twitchminecraft.lang.Language;
import com.twitchmcsync.twitchminecraft.live.LiveCommands;
import com.twitchmcsync.twitchminecraft.live.LiveModule;
import com.twitchmcsync.twitchminecraft.reward.SyncReward;
import com.twitchmcsync.twitchminecraft.storage.DatabaseManager;
import com.twitchmcsync.twitchminecraft.submode.SubMode;
import com.twitchmcsync.twitchminecraft.submode.SubmodeWindow;
import com.twitchmcsync.twitchminecraft.twitchapi.TwitchSyncAPI;
import com.twitchmcsync.twitchminecraft.twitchapi.TwitchWrapper;
import com.twitchmcsync.twitchminecraft.twitchapi.twitch.OAuthToken;
import lombok.Getter;
import me.dessie.dessielib.commandapi.CommandAPI;
import me.dessie.dessielib.storageapi.SpigotStorageAPI;
import me.dessie.dessielib.storageapi.format.flatfile.JSONContainer;
import me.dessie.dessielib.storageapi.format.flatfile.YAMLContainer;
import me.dessie.dessielib.storageapi.settings.StorageSettings;
import net.luckperms.api.LuckPerms;
import okhttp3.OkHttpClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Getter
public class TwitchMinecraft extends JavaPlugin {

    @Getter
    private static TwitchMinecraft instance;

    private TwitchAuthWebServer webServer;
    private TwitchSyncAPI syncAPI;
    private String secret;
    private OkHttpClient httpClient;
    private TwitchWrapper twitchWrapper;

    private YAMLContainer configContainer;
    private YAMLContainer rewardContainer;
    private YAMLContainer liveModuleContainer;
    private JSONContainer flatfileContainer;

    private LiveModule liveModule;
    private DatabaseManager databaseManager;
    private Language language;
    private SubMode subMode;

    private SpigotStorageAPI storageAPI;
    private CommandAPI commandAPI;
    private LuckPerms luckPerms;

    private String broadcasterUsername;

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();

        this.commandAPI = CommandAPI.register(this, false);
        this.getCommandAPI().registerCommand(new TwitchInfoCommand());
        this.getCommandAPI().registerCommand(new ReloadCommand());
        this.getCommandAPI().registerCommand(new SubModeCommand());
        this.getCommandAPI().registerCommand(new SyncCommand());
        this.getCommandAPI().registerCommand(new UnsyncCommand());
        this.getCommandAPI().registerCommand(new RevokeCommand());
        this.getCommandAPI().registerCommand(new LiveCommand());
        this.getCommandAPI().registerCommand(new TwitchMessageCommand());

        //Load LuckPerms, if it exists.
        if(Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPerms = provider.getProvider();
            }
        }

        //Register the class for storing in flatfile.
        this.storageAPI = SpigotStorageAPI.register(this, false);
        this.getStorageAPI().registerAnnotatedDecomposer(TwitchPlayer.class);
        this.getStorageAPI().registerAnnotatedDecomposer(OAuthToken.class);
        this.getStorageAPI().registerAnnotatedDecomposer(SyncReward.class);
        this.getStorageAPI().registerAnnotatedDecomposer(SubMode.class);
        this.getStorageAPI().registerAnnotatedDecomposer(SubmodeWindow.class);
        this.getStorageAPI().registerAnnotatedDecomposer(LiveCommands.class);

        //Load all the values.
        this.reloadPlugin();

        //Default HTTP Client for OkHttp
        this.httpClient = new OkHttpClient();

        //Production version
        this.syncAPI = new Retrofit.Builder().baseUrl("https://twitchmcsync.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(this.getHttpClient())
                .build().create(TwitchSyncAPI.class);

        this.getServer().getPluginManager().registerEvents(new JoinAuthorization(), this);
        this.getServer().getPluginManager().registerEvents(new TwitchEventListener(), this);
    }

    public void reloadPlugin() {
        this.reloadConfig();
        this.configContainer = new YAMLContainer(this.getStorageAPI(), new File(this.getDataFolder(), "config.yml"));

        //Generate the server secret for this session.
        this.secret = UUID.randomUUID().toString().replace("-", "");

        //Create the WebServer for hosting the local website.
        try {
            if(this.getWebServer() != null) {
                this.getWebServer().stop();
            }

            this.webServer = new TwitchAuthWebServer(this, this.getSecret(), this.getConfig().getInt("port"));
            this.webServer.start();
            this.getLogger().info("Started on port " + this.getConfig().getInt("port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Setup values so we don't have to constantly read from the config.
        this.broadcasterUsername = this.getConfig().getString("broadcaster_username");
        this.subMode = this.getConfigContainer().retrieve(SubMode.class, "submode");

        //Close the old one.
        if(this.getTwitchWrapper() != null) {
            this.getTwitchWrapper().getClient().close();
        }

        //Setup Storage options for holding Twitch Data.
        String storageFormat = this.getConfig().getString("storage_format");
        storageFormat = storageFormat == null ? "flatfile" : storageFormat;

        if(storageFormat.equalsIgnoreCase("flatfile")) {
            File file = new File(this.getDataFolder(), "twitchdata.json");

            if(!file.exists()) {
                saveResource("twitchdata.json", false);
            }

            this.flatfileContainer = new JSONContainer(this.getStorageAPI(), file);
            this.getFlatfileContainer().getSettings().setUsesCache(false);

        } else if(storageFormat.equalsIgnoreCase("db") || storageFormat.equalsIgnoreCase("database")) {
            String host = this.getConfig().getString("database.host");
            int port = this.getConfig().getInt("database.port");
            String database = this.getConfig().getString("database.database");
            String username = this.getConfig().getString("database.username");
            String password = this.getConfig().getString("database.password");

            this.databaseManager = new DatabaseManager(host, port, database, username, password);
        }

        //Create the API wrapper for Twitch.
        this.twitchWrapper = new TwitchWrapper(this, this.getBroadcasterUsername(),
                this.getConfig().getString("client_id"), this.getConfig().getString("client_secret"));

        this.saveDefaultLoadFile("lang.yml");
        this.language = new Language(this);

        //Load sync rewards.
        this.rewardContainer = this.saveDefaultLoadFile("sync_rewards.yml");
        SyncReward.reloadRewards(this);

        //Load live module rewards.
        this.liveModuleContainer = this.saveDefaultLoadFile("live.yml");
        if(this.getLiveModuleContainer().retrieve("enabled")) {
            if(this.liveModule == null) {
                this.liveModule = new LiveModule(this);
                this.getServer().getPluginManager().registerEvents(this.getLiveModule(), this);
            } else {
                this.getLiveModule().reload();
            }
        }
    }

    private YAMLContainer saveDefaultLoadFile(String name) {
        File file = new File(this.getDataFolder(), name);
        if(!file.exists()) {
            this.saveResource(name, false);
        }
        return new YAMLContainer(this.getStorageAPI(), new File(this.getDataFolder(), name), new StorageSettings().setUsesCache(false));
    }

    @Override
    public void onDisable() {
        if(this.getWebServer() != null) {
            this.webServer.stop();
        }
    }
}
