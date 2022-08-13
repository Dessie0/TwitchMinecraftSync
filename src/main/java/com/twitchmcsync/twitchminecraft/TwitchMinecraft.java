package com.twitchmcsync.twitchminecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twitchmcsync.twitchminecraft.commands.*;
import com.twitchmcsync.twitchminecraft.events.JoinListener;
import com.twitchmcsync.twitchminecraft.lang.Language;
import com.twitchmcsync.twitchminecraft.webserver.WebServer;
import net.lingala.zip4j.ZipFile;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class TwitchMinecraft extends JavaPlugin {

    private static TwitchMinecraft instance;

    private Permission permission;
    private WebServer webServer;
    private RewardHandler rewardHandler;
    private String channelID;

    public File htmlFolder = new File(getDataFolder(), "webserver");
    public File indexFile = new File(getDataFolder(), "webserver" + File.separator + "index.html");

    private Language language;

    private File twitchData;
    private FileConfiguration twitchConfig;

    private boolean floodGateEnabled;
    private boolean vaultEnabled;

    private String appAccess;

    @Override
    public void onEnable() {
        instance = this;

        //Hook into Floodgate and Vault.
        this.floodGateEnabled = this.getServer().getPluginManager().isPluginEnabled("floodgate");
        this.vaultEnabled = setupPermissions();

        if(!isVaultEnabled()) {
            this.getLogger().log(Level.WARNING, "Unable to hook into Vault, groups will not be added or removed to users.");
        }

        //Create the WebServer for hosting the local website.
        this.webServer = new WebServer(this);

        //Setup the RewardHandler for synchronization.
        this.rewardHandler = new RewardHandler(this);

        this.loadFiles();
        this.saveDefaultConfig();
        this.createFiles();

        this.webServer.create(getConfig().getInt("port"));

        this.getCommand("sync").setExecutor(new SyncCMD(this));
        this.getCommand("revoke").setExecutor(new RevokeCMD(this));
        this.getCommand("tinfo").setExecutor(new InfoCMD());
        this.getCommand("twitchreload").setExecutor(new ReloadCMD(this));
        this.getCommand("twitchserverreload").setExecutor(new ReloadServerCMD(this));

        //Get the channel ID of the channel we're check for subs.
        this.retrieveChannelID();

        this.getServer().getPluginManager().registerEvents(new JoinListener(this), this);
    }

    @Override
    public void onDisable() {
        this.webServer.remove();
    }

    /**
     * Retrieves the Channel ID of the Twitch User from Twitch.
     */
    public void retrieveChannelID() {
        Bukkit.getLogger().info("[TwitchMinecraftSync] Getting Channel ID for " + getConfig().getString("channelName"));

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {

            String clientId = this.getConfig().getString("clientID");
            String clientSecret = this.getConfig().getString("clientSecret");

            if(clientId == null || clientSecret == null) {
                this.channelID = null;
                this.getLogger().log(Level.SEVERE, "Unable to find client ID or client secret. Syncing has been disabled.");
                return;
            }

            if(clientId.contains("<") || clientSecret.contains("<")) {
                this.channelID = null;
                this.getLogger().log(Level.INFO, "You need to setup the configuration with your Client ID and Client Secret. More setup information can be found at https://github.com/Dessie0/TwitchMinecraftSync#readme");
                return;
            }

            try {
                URL url = new URL("https://id.twitch.tv/oauth2/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");

                JsonObject json = getJsonObject(con.getInputStream());
                this.appAccess = json.get("access_token").getAsString();
            } catch (IOException e) {
                this.channelID = null;
                this.getLogger().log(Level.SEVERE, "Invalid Client ID or Client Secret, please make sure the configuration matches with your Twitch Application IDs.");
                return;
            }

            try {
                String channelName = getConfig().getString("channelName");

                if(channelName == null) {
                    this.channelID = null;
                    this.getLogger().log(Level.SEVERE, "Unable to find twitch channel name");
                    return;
                }

                if(channelName.contains("<")) {
                    this.channelID = null;
                    this.getLogger().log(Level.INFO, "You need to setup the configuration with your channel name. More setup information can be found at https://github.com/Dessie0/TwitchMinecraftSync#readme");
                    return;
                }

                URL url = new URL("https://api.twitch.tv/helix/users?login=" + channelName);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
                con.setRequestProperty("Client-ID", getConfig().getString("clientID"));
                con.setRequestProperty("Authorization", "Bearer " + this.appAccess);

                con.setRequestMethod("GET");

                try {
                    JsonObject json = getJsonObject(con.getInputStream());
                    this.channelID = json.get("data").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
                } catch (IndexOutOfBoundsException | IOException e) {
                    this.getLogger().log(Level.SEVERE, "Unable to find a Twitch channel by the name " + channelName + ".");
                }

                this.getLogger().log(Level.INFO, "Successfully started TwitchMinecraftSync for user " + channelName);

                con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public JsonObject getJsonObject(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        JsonObject object = JsonParser.parseReader(in).getAsJsonObject();
        in.close();

        return object;
    }

    /**
     * Restarts the WebServer
     */
    public void restartWebServer() {
        this.getWebServer().remove();
        this.getWebServer().create(this.getConfig().getInt("port"));
    }

    public Language getLanguage() {
        return language;
    }
    public Permission getPermission() {
        return permission;
    }
    public WebServer getWebServer() {
        return webServer;
    }
    public String getChannelID() {
        return channelID;
    }

    public String getAppAccess() {
        return appAccess;
    }

    public void createFiles() {
        //Create the files if they do not exist.
        if(!twitchData.exists()) {
            saveResource(twitchData.getName(), false);
        }

        if(!this.getLanguage().getLangFile().exists()) {
            saveResource(this.getLanguage().getLangFile().getName(), false);
        }

        if(!htmlFolder.exists()) {
            htmlFolder.mkdirs();
            try {
                //Copy the zip file.
                FileUtils.copyInputStreamToFile(this.getResource("serverdisplay.zip"), new File(getDataFolder() + "/webserver/serverdisplay.zip"));

                //Unzip
                new ZipFile(getDataFolder() + "/webserver/serverdisplay.zip").extractAll(getDataFolder() + "/webserver");

                //Delete zip file
                new File(getDataFolder() + "/webserver/serverdisplay.zip").delete();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Load the configurations.
        this.loadFiles();
    }

    public void loadFiles() {
        this.language = new Language();

        twitchData = new File(getInstance().getDataFolder() + "/twitchdata.yml");
        twitchConfig = YamlConfiguration.loadConfiguration(twitchData);

        //Load the Language Configuration
        this.getLanguage().loadConfig();
    }

    public static void saveFile(File file, FileConfiguration fconf) {
        try{
            fconf.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean setupPermissions() {
        try {
            //Only used to throw ClassNotFoundException.
            Class<?> clazz = Class.forName("net.milkbowl.vault.permission.Permission");
            RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp == null) {
                return false;
            }

            permission = rsp.getProvider();
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public RewardHandler getRewardHandler() {
        return rewardHandler;
    }

    public File getTwitchData() {return twitchData;}
    public FileConfiguration getTwitchConfig() {
        return twitchConfig;
    }

    public boolean isFloodGateEnabled() {
        return floodGateEnabled;
    }
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public static TwitchMinecraft getInstance() {
        return instance;
    }
}
