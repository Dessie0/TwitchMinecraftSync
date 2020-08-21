package me.dessie.twitchminecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.dessie.twitchminecraft.Commands.InfoCMD;
import me.dessie.twitchminecraft.Commands.RevokeCMD;
import me.dessie.twitchminecraft.Commands.SyncCMD;
import me.dessie.twitchminecraft.Events.JoinListener;
import me.dessie.twitchminecraft.Events.SubscribeEvent;
import me.dessie.twitchminecraft.WebServer.TwitchHandler;
import me.dessie.twitchminecraft.WebServer.WebServer;
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
import java.util.HashMap;
import java.util.Map;

public class TwitchMinecraft extends JavaPlugin {

    public Permission permission;
    public WebServer webServer;
    public String channelID;

    public Map<String, TwitchHandler> handlers = new HashMap<>();

    public File twitchData = new File(getDataFolder() + "/twitchdata.yml");
    public File htmlFolder = new File(getDataFolder(), "webserver");
    public File indexFile = new File(getDataFolder(), "webserver" + File.separator + "index.html");
    public File jsFile = new File(getDataFolder(), "webserver" + File.separator + "twitchminecraft.js");

    public FileConfiguration twitchConfig = YamlConfiguration.loadConfiguration(twitchData);

    @Override
    public void onEnable() {
        webServer = new WebServer();
        setupPermissions();
        saveDefaultConfig();
        createFiles();

        webServer.create(getConfig().getInt("port"));

        getCommand("sync").setExecutor(new SyncCMD());
        getCommand("revoke").setExecutor(new RevokeCMD());
        getCommand("tinfo").setExecutor(new InfoCMD());
        getChannelID();

        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new SubscribeEvent(), this);
    }

    @Override
    public void onDisable() {
        webServer.remove();
    }

    public void getChannelID() {
        System.out.println("Getting Channel ID for " + getConfig().getString("channelName"));

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL("https://api.twitch.tv/kraken/users?login=" + getConfig().getString("channelName"));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
                con.setRequestProperty("Client-ID", getConfig().getString("clientID"));
                con.setRequestMethod("GET");

                JsonObject json = getJsonObject(con.getInputStream());
                channelID = json.get("users").getAsJsonArray().get(0).getAsJsonObject().get("_id").getAsString();

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

    private void createFiles() {
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
                FileUtils.copyInputStreamToFile(this.getResource("serverdisplay/index.html"), new File(getDataFolder() + "/webserver/index.html"));
                FileUtils.copyInputStreamToFile(this.getResource("serverdisplay/twitchminecraft.js"), new File(getDataFolder() + "/webserver/twitchminecraft.js"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveFile(File file, FileConfiguration fconf) {
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

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}
