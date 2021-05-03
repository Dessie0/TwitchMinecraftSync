package me.dessie.twitchminecraft;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TwitchPlayer {

    private static TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);
    private static final List<TwitchPlayer> players = new ArrayList<>();
    public static List<String> subbedList = plugin.twitchConfig.getStringList("hasSubbed");

    private OfflinePlayer player;
    private String channelID;
    private String channelName;
    private String expirationDate;
    private String refreshToken;
    private int tier;
    private int streak;

    private String logoURL;

    //Create the instance.
    public TwitchPlayer(Player player) {
        this.player = player;
        players.add(this);
    }

    public TwitchPlayer(OfflinePlayer player, String channelID, String channelName, String expires, String refreshToken, int tier, int streak) {
        this.player = player;
        this.channelID = channelID;
        this.channelName = channelName;
        this.expirationDate = expires;
        this.refreshToken = refreshToken;
        this.tier = tier;
        this.streak = streak;

        players.add(this);
    }

    public int getTier() { return tier; }
    public int getStreak() { return streak; }
    public OfflinePlayer getPlayer() { return player; }
    public String getChannelID() { return channelID; }
    public String getChannelName() { return channelName; }
    public String getExpirationDate() { return expirationDate; }
    public String getUuid() { return player.getUniqueId().toString(); }
    public String getName() { return player.getName(); }
    public String getRefreshToken() { return refreshToken; }
    public String getLogoURL() { return logoURL; }

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setTier(int tier) { this.tier = tier; }
    public void setExpirationDate(String expires) { this.expirationDate = expires; }
    public void setChannelID(String channelID) { this.channelID = channelID; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setStreak(int streak) { this.streak = streak; }
    public void setLogoURL(String logoURL) { this.logoURL = logoURL; }

    public static List<TwitchPlayer> getPlayers() {
        return players;
    }

    public static TwitchPlayer getFromUUID(String uuid) {
        return players.stream().filter(handler -> handler.getUuid().equals(uuid)).findAny().orElse(null);
    }

    public void saveData(boolean isSubbed) {
        if(isSubbed) {
            plugin.twitchConfig.set(this.getUuid() + ".name", this.getName());
            plugin.twitchConfig.set(this.getUuid() + ".channelID", this.channelID);
            plugin.twitchConfig.set(this.getUuid() + ".channelName", this.channelName);
            plugin.twitchConfig.set(this.getUuid() + ".expires", this.expirationDate);
            plugin.twitchConfig.set(this.getUuid() + ".tier", this.tier);
            plugin.twitchConfig.set(this.getUuid() + ".refreshToken", this.refreshToken);
            plugin.twitchConfig.set(this.getUuid() + ".streak", this.streak);

            //Add them to the hasSubbed list if they're not already in it.
            if(!subbedList.contains(this.getUuid())) {
                subbedList.add(this.getUuid());
                plugin.twitchConfig.set("hasSubbed", subbedList);
            }
        } else {
            //Remove them from the config.
            plugin.twitchConfig.set(this.getUuid(), null);
        }

        plugin.saveFile(plugin.twitchData, plugin.twitchConfig);
    }

    public static boolean playerExists(String uuid) {
        return plugin.twitchConfig.getConfigurationSection(uuid) != null;
    }

    public static boolean accountUsed(String channelID) {
        Set<String> keys = plugin.twitchConfig.getConfigurationSection("").getKeys(false);
        keys.removeIf(key -> plugin.twitchConfig.getConfigurationSection(key) == null);

        return !keys.stream().map(key -> plugin.twitchConfig.getConfigurationSection(key).getString("channelID"))
                .filter(id -> id.equalsIgnoreCase(channelID)).collect(Collectors.toList()).isEmpty();
    }

    public static String getUUIDFromChannelName(String channelName) {
        Set<String> keys = plugin.twitchConfig.getConfigurationSection("").getKeys(false);
        keys.removeIf(key -> plugin.twitchConfig.getConfigurationSection(key) == null);

        List<String> uuid = keys.stream()
                .filter(key -> plugin.twitchConfig.getConfigurationSection(key).getString("channelName").equalsIgnoreCase(channelName))
                .collect(Collectors.toList());

        return uuid.size() > 0 ? uuid.get(0) : null;
    }

    public static TwitchPlayer create(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return new TwitchPlayer(player,
                plugin.twitchConfig.getString(uuid + ".channelID"),
                plugin.twitchConfig.getString(uuid + ".channelName"),
                plugin.twitchConfig.getString(uuid + ".expires"),
                plugin.twitchConfig.getString(uuid + ".refreshToken"),
                plugin.twitchConfig.getInt(uuid + ".tier"),
                plugin.twitchConfig.getInt(uuid + ".streak"));
    }

    public static TwitchPlayer create(String uuid) {
        return new TwitchPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)),
                plugin.twitchConfig.getString(uuid + ".channelID"),
                plugin.twitchConfig.getString(uuid + ".channelName"),
                plugin.twitchConfig.getString(uuid + ".expires"),
                plugin.twitchConfig.getString(uuid + ".refreshToken"),
                plugin.twitchConfig.getInt(uuid + ".tier"),
                plugin.twitchConfig.getInt(uuid + ".streak"));
    }

}
