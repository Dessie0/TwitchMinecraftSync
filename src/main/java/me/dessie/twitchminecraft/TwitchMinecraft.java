package me.dessie.twitchminecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.dessie.twitchminecraft.commands.*;
import me.dessie.twitchminecraft.events.JoinListener;
import me.dessie.twitchminecraft.webserver.WebServer;
import net.lingala.zip4j.ZipFile;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TwitchMinecraft extends JavaPlugin {

    private static TwitchMinecraft instance;

    public Permission permission;
    public WebServer webServer;
    public String channelID;

    public File htmlFolder = new File(getDataFolder(), "webserver");
    public File indexFile = new File(getDataFolder(), "webserver" + File.separator + "index.html");

    private static File twitchData;
    private static FileConfiguration twitchConfig;

    @Override
    public void onEnable() {
        instance = this;

        webServer = new WebServer();
        setupPermissions();
        saveDefaultConfig();
        createFiles();

        webServer.create(getConfig().getInt("port"));

        getCommand("sync").setExecutor(new SyncCMD());
        getCommand("revoke").setExecutor(new RevokeCMD());
        getCommand("tinfo").setExecutor(new InfoCMD());
        getCommand("twitchreload").setExecutor(new ReloadCMD());
        getCommand("twitchserverreload").setExecutor(new ReloadServerCMD());

        //Get the channel ID of the channel we're check for subs.
        getChannelID();

        getServer().getPluginManager().registerEvents(new JoinListener(), this);
    }

    @Override
    public void onDisable() {
        webServer.remove();
    }

    public void getChannelID() {
        Bukkit.getLogger().info("[TwitchMinecraftSync] Getting Channel ID for " + getConfig().getString("channelName"));

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL("https://api.twitch.tv/kraken/users?login=" + getConfig().getString("channelName"));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
                con.setRequestProperty("Client-ID", getConfig().getString("clientID"));
                con.setRequestMethod("GET");

                try {
                    JsonObject json = getJsonObject(con.getInputStream());
                    channelID = json.get("users").getAsJsonArray().get(0).getAsJsonObject().get("_id").getAsString();
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
        StringBuilder content = new StringBuilder();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JsonParser().parse(content.toString()).getAsJsonObject();
    }

    public static Map<String, String> getParams(String request) {
        if(request == null || !request.contains("&")) return new HashMap<>();

        String[] requests = request.split("&");

        return Arrays.stream(requests).collect(Collectors.toMap(
                (value) -> value.split("=")[0], (value) -> value.split("=")[1]));
    }

    public static String formatExpiry(String expiry) {
        ZonedDateTime time = ZonedDateTime.parse(expiry);
        StringBuilder formatted = new StringBuilder();

        String month = time.getMonth().toString().substring(0, 1) + time.getMonth().toString().substring(1).toLowerCase();

        formatted.append(month)
                .append(" ")
                .append(time.getDayOfMonth())
                .append(", ")
                .append(time.getYear())
                .append(" at ");

        String hour;
        if (time.getHour() < 10) {
            hour = "0" + time.getHour();
        } else {
            hour = String.valueOf(time.getHour());
        }

        String minute;
        if (time.getMinute() < 10) {
            minute = "0" + time.getMinute();
        } else {
            minute = String.valueOf(time.getMinute());
        }

        formatted.append(hour).append(":").append(minute);

        return formatted.toString();
    }

    public void createFiles() {
        loadFiles();
        if(!twitchData.exists()) {
            saveResource(twitchData.getName(), false);
        }

        try {
            twitchConfig.load(twitchData);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
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
    }

    public static void loadFiles() {
        twitchData = new File(getInstance().getDataFolder() + "/twitchdata.yml");
        twitchConfig = YamlConfiguration.loadConfiguration(twitchData);
    }

    public static void saveFile(File file, FileConfiguration fconf) {
        try{
            fconf.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static File getTwitchData() {
        return twitchData;
    }

    public static FileConfiguration getTwitchConfig() {
        return twitchConfig;
    }

    public static TwitchMinecraft getInstance() {
        return instance;
    }
}
