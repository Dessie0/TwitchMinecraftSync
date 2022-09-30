package com.twitchmcsync.twitchminecraft;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TwitchPlayer {
    private final TwitchMinecraft plugin;
    private final OfflinePlayer player;
    private String channelID;
    private String channelName;
    private String lastSubDate;
    private String refreshToken;
    private int tier;

    private String logoURL;

    /**
     * Creates an empty TwitchPlayer object 
     * @param player The player to reference
     */
    //Create the instance.
    public TwitchPlayer(TwitchMinecraft plugin, OfflinePlayer player) {
        this(plugin, player, null, null, null, 0);
    }

    /**
     * Creates a fulfilled TwitchPlayer object.
     * {@link #create(String)} and {@link #create(OfflinePlayer)} for easier usage.
     *
     * @param plugin The Plugin instance
     * @param player The player object this TwitchPlayer is for.
     * @param channelID The Twitch channel ID
     * @param channelName The Twitch username
     * @param refreshToken The refresh token for this user
     * @param tier The tier of this subscription
     */
    public TwitchPlayer(TwitchMinecraft plugin, OfflinePlayer player, String channelID, String channelName, String refreshToken, int tier) {
        this.plugin = plugin;
        this.player = player;
        this.channelID = channelID;
        this.channelName = channelName;
        this.refreshToken = refreshToken;
        this.tier = tier;
    }

    /**
     * @return The current Twitch Subscription tier this user has.
     */
    public int getTier() { return tier; }

    /**
     * @return The player related to this TwitchPlayer
     */
    public OfflinePlayer getPlayer() { return player; }

    /**
     * @return The Twitch ID of this object
     */
    public String getChannelID() { return channelID; }

    /**
     * @return The Twitch username of this object
     */
    public String getChannelName() { return channelName; }

    /**
     * @return The UUID of the player
     */
    public String getUuid() { return player.getUniqueId().toString(); }

    /**
     * @return The Minecraft username of the player
     */
    public String getName() { return player.getName(); }

    /**
     * @return The refresh token returned by the OAuth handshake.
     */
    public String getRefreshToken() { return refreshToken; }

    /**
     * @return The URL to obtain the Twitch user's profile picture
     */
    public String getLogoURL() { return logoURL; }

    /**
     * @return The last known expiration date of this user, if they're not currently subscribed.
     */
    public String getLastSubDate() { return lastSubDate; }

    /**
     * @return The Plugin instance
     */
    public TwitchMinecraft getPlugin() {
        return plugin;
    }

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setTier(int tier) { this.tier = tier; }
    public void setChannelID(String channelID) { this.channelID = channelID; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setLogoURL(String logoURL) { this.logoURL = logoURL; }
    public void setLastSubDate(String lastSubDate) { this.lastSubDate = lastSubDate; }

    /**
     * Saves this TwitchPlayer to the twitchdata.json file
     * This method should be called when you're finished modifying the object.
     *
     * @param isSubbed Tells if the method if it should save specific fields.
     *                 If they're not subbed, expire date, tier, streak, and refresh token are not saved.
     *
     */
    public void saveData(boolean isSubbed) {
        String path = this.getUuid();
        if(isSubbed) {
            this.getPlugin().getTwitchConfig().set(path + ".tier", this.getTier());
            this.getPlugin().getTwitchConfig().set(path + ".refreshToken", this.getRefreshToken());

            //Add them to the hasSubbed list if they're not already in it.
            if(!getSubbedList().contains(this.getUuid())) {
                List<String> subbedList = getSubbedList();
                subbedList.add(this.getUuid());
                this.getPlugin().getTwitchConfig().set("hasSubbed", subbedList);
            }
        } else {
            path = "data." + this.getUuid();
            this.getPlugin().getTwitchConfig().set(path + ".lastSubDate", this.getLastSubDate());
            this.getPlugin().getTwitchConfig().set(this.getUuid(), null);
        }

        this.getPlugin().getTwitchConfig().set(path + ".name", this.getName());
        this.getPlugin().getTwitchConfig().set(path + ".channelID", this.getChannelID());
        this.getPlugin().getTwitchConfig().set(path + ".channelName", this.getChannelName());

        TwitchMinecraft.saveFile(this.getPlugin().getTwitchData(), this.getPlugin().getTwitchConfig());
    }

    /**
     * @param uuid The UUID of the player.
     * @return If a player is currently synced & subbed.
     */
    public static boolean isSubbed(String uuid) {
        return TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(uuid) != null;
    }

    /**
     * @param channelID The Twitch channel ID
     * @return If the Twitch account by channel ID is currently in use.
     */
    public static boolean accountUsed(String channelID) {
        Set<String> keys = TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection("").getKeys(false);
        keys.removeIf(key -> TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(key) == null || TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(key).getString("channelID") == null);

        return keys.stream().map(key -> TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(key).getString("channelID"))
                .filter(Objects::nonNull).anyMatch(id -> id.equalsIgnoreCase(channelID));
    }

    /**
     * @param uuid The player to check if they're synced
     * @return If the player has synced their Twitch and Minecraft account at all.
     */
    public static boolean isSynced(String uuid) {
        return isSubbed(uuid) || TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection("data." + uuid) != null;
    }

    /**
     * @param channelName The Twitch channel name.
     * @return The synced UUID with this Twitch name.
     */
    public static String getUUIDFromChannelName(String channelName) {
        return getUUIDFromChannelName(channelName, true);
    }

    private static String getUUIDFromChannelName(String channelName, boolean subbed) {
        Set<String> keys;
        if(subbed) {
            keys = TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection("").getKeys(false);
        } else {
            keys = TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection("data").getKeys(false);
        }

        keys.removeIf(key -> TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(key) == null || TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(key).getString("channelName") == null);

        List<String> uuid = keys.stream().filter(key -> TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection(key).getString("channelName").equalsIgnoreCase(channelName)).collect(Collectors.toList());

        return uuid.size() > 0 ? uuid.get(0) : subbed ? getUUIDFromChannelName(channelName, false) : null;
    }

    /**
     * Clears all data from the data section of twitchdata.yml
     */
    public void clearData() {
        this.getPlugin().getTwitchConfig().set("data." + this.getUuid(), null);
        TwitchMinecraft.saveFile(this.getPlugin().getTwitchData(), this.getPlugin().getTwitchConfig());
    }

    /**
     * Creates a TwitchPlayer from the data file.
     * @param player The Player to create
     * @return The TwitchPlayer for the player.
     */
    public static TwitchPlayer create(OfflinePlayer player) {
        return create(player.getUniqueId().toString());
    }

    /**
     * Creates a TwitchPlayer from the data file.
     * @param uuid The UUID to create.
     * @return The TwitchPlayer for the player with the specified UUID.
     */
    public static TwitchPlayer create(String uuid) {
        FileConfiguration config = TwitchMinecraft.getInstance().getTwitchConfig();
        if(config.getConfigurationSection(uuid) == null) return null;

        return new TwitchPlayer(TwitchMinecraft.getInstance(), Bukkit.getOfflinePlayer(UUID.fromString(uuid)),
                config.getString(uuid + ".channelID"),
                config.getString(uuid + ".channelName"),
                config.getString(uuid + ".refreshToken"),
                config.getInt(uuid + ".tier"));
    }

    /**
     * Creates a TwitchPlayer object from the data stored in the data ConfigurationSection
     * of the twitchdata.yml file.
     *
     * This method will provide their Twitch name, Twitch ID, and last known expiration date if available.
     *
     * @param uuid The UUID of the player.
     * @return A TwitchPlayer object with non-subscriber specific information.
     */
    public static TwitchPlayer createData(String uuid) {
        ConfigurationSection section = TwitchMinecraft.getInstance().getTwitchConfig().getConfigurationSection("data." + uuid);
        if(section == null) {
            return create(uuid);
        }

        TwitchPlayer player = new TwitchPlayer(TwitchMinecraft.getInstance(), Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
        player.setChannelName(section.getString("channelName"));
        player.setChannelID(section.getString("channelID"));
        player.setLastSubDate(section.getString("lastSubDate"));

        return player;
    }

    /**
     * @return All UUIDs that have been previously synced and subscribed.
     */
    public static List<String> getSubbedList() {
        return TwitchMinecraft.getInstance().getTwitchConfig().getStringList("hasSubbed");
    }
}
