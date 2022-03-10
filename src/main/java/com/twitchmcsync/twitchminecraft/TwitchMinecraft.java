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

public class TwitchMinecraft extends JavaPlugin {

    private static TwitchMinecraft instance;

    private Permission permission;
    private WebServer webServer;
    private String channelID;

    public File htmlFolder = new File(getDataFolder(), "webserver");
    public File indexFile = new File(getDataFolder(), "webserver" + File.separator + "index.html");

    private Language language;

    private static File twitchData;
    private static FileConfiguration twitchConfig;

    private static boolean floodGateEnabled;
    private static boolean vaultEnabled;

    private String appAccess;


    @Override
    public void onEnable() {
        instance = this;

        //Hook into Floodgate and Vault.
        floodGateEnabled = this.getServer().getPluginManager().isPluginEnabled("floodgate");
        vaultEnabled = setupPermissions();

        //Create the WebServer for hosting the local website.
        this.webServer = new WebServer();

        loadFiles();
        saveDefaultConfig();
        createFiles();

        this.webServer.create(getConfig().getInt("port"));

        getCommand("sync").setExecutor(new SyncCMD());
        getCommand("revoke").setExecutor(new RevokeCMD());
        getCommand("tinfo").setExecutor(new InfoCMD());
        getCommand("twitchreload").setExecutor(new ReloadCMD());
        getCommand("twitchserverreload").setExecutor(new ReloadServerCMD());

        //Get the channel ID of the channel we're check for subs.
        retrieveChannelID();

        getServer().getPluginManager().registerEvents(new JoinListener(), this);
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

            try {
                URL url = new URL("https://id.twitch.tv/oauth2/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");

                JsonObject json = getJsonObject(con.getInputStream());
                this.appAccess = json.get("access_token").getAsString();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                URL url = new URL("https://api.twitch.tv/helix/users?login=" + getConfig().getString("channelName"));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
                con.setRequestProperty("Client-ID", getConfig().getString("clientID"));
                con.setRequestProperty("Authorization", "Bearer " + this.appAccess);

                con.setRequestMethod("GET");

                try {
                    JsonObject json = getJsonObject(con.getInputStream());
                    this.channelID = json.get("data").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
                } catch (IndexOutOfBoundsException | IOException e) {
                    Bukkit.getLogger().severe("[TwitchMinecraftSync] Invalid Twitch channel name or Client ID.");
                }

                con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public JsonObject getJsonObject(InputStream stream) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            JsonObject object = new JsonParser().parse(in).getAsJsonObject();
            in.close();

            return object;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if(rsp == null) {
            return false;
        }

        permission = rsp.getProvider();
        return true;
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static File getTwitchData() {return twitchData;}
    public static FileConfiguration getTwitchConfig() {
        return twitchConfig;
    }
    public static TwitchMinecraft getInstance() {
        return instance;
    }
    public static boolean isFloodGateEnabled() {return floodGateEnabled;}
    public static boolean isVaultEnabled() {return vaultEnabled;}
}
