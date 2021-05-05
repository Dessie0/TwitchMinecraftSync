package com.twitchmcsync.twitchminecraft;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TwitchPlayer {

    private static TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);
    private static final List<String> subbedList = TwitchMinecraft.getTwitchConfig().getStringList("hasSubbed");

    private OfflinePlayer player;
    private String channelID;
    private String channelName;
    private String expirationDate;
    private String lastSubDate;
    private String refreshToken;
    private int tier;
    private int streak;

    private String logoURL;

    /**
     * Creates an empty TwitchPlayer object 
     * @param player The player to reference
     */
    //Create the instance.
    public TwitchPlayer(OfflinePlayer player) {
        this.player = player;
    }

    /**
     * Creates a fulfilled TwitchPlayer object.
     * {@link #create(String)} and {@link #create(OfflinePlayer)} for easier usage.
     * 
     * @param player The player object this TwitchPlayer is for.
     * @param channelID The Twitch channel ID
     * @param channelName The Twitch username
     * @param expires The expiration string
     * @param refreshToken The refresh token for this user
     * @param tier The tier of this subscription
     * @param streak The streak of this subscription
     */
    public TwitchPlayer(OfflinePlayer player, String channelID, String channelName, String expires, String refreshToken, int tier, int streak) {
        this.player = player;
        this.channelID = channelID;
        this.channelName = channelName;
        this.expirationDate = expires;
        this.refreshToken = refreshToken;
        this.tier = tier;
        this.streak = streak;
    }

    /**
     * @return The current Twitch Subscription tier this user has.
     */
    public int getTier() { return tier; }

    /**
     * @return Their current subscription streak
     */
    public int getStreak() { return streak; }

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
     * @return When the subscription expires.
     */
    public String getExpirationDate() { return expirationDate; }

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

    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setTier(int tier) { this.tier = tier; }
    public void setExpirationDate(String expires) { this.expirationDate = expires; }
    public void setChannelID(String channelID) { this.channelID = channelID; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setStreak(int streak) { this.streak = streak; }
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
            TwitchMinecraft.getTwitchConfig().set(path + ".expires", this.getExpirationDate());
            TwitchMinecraft.getTwitchConfig().set(path + ".tier", this.getTier());
            TwitchMinecraft.getTwitchConfig().set(path + ".refreshToken", this.getRefreshToken());
            TwitchMinecraft.getTwitchConfig().set(path + ".streak", this.getStreak());

            //Add them to the hasSubbed list if they're not already in it.
            if(!subbedList.contains(this.getUuid())) {
                subbedList.add(this.getUuid());
                TwitchMinecraft.getTwitchConfig().set("hasSubbed", subbedList);
            }
        } else {
            path = "data." + this.getUuid();
            TwitchMinecraft.getTwitchConfig().set(path + ".lastSubDate", this.getLastSubDate());
            TwitchMinecraft.getTwitchConfig().set(this.getUuid(), null);
        }

        TwitchMinecraft.getTwitchConfig().set(path + ".name", this.getName());
        TwitchMinecraft.getTwitchConfig().set(path + ".channelID", this.getChannelID());
        TwitchMinecraft.getTwitchConfig().set(path + ".channelName", this.getChannelName());

        TwitchMinecraft.saveFile(TwitchMinecraft.getTwitchData(), TwitchMinecraft.getTwitchConfig());
    }

    /**
     * @param uuid The UUID of the player.
     * @return If a player is currently synced & subbed.
     */
    public static boolean isSubbed(String uuid) {
        return TwitchMinecraft.getTwitchConfig().getConfigurationSection(uuid) != null;
    }

    /**
     * @param channelID
     * @return If the Twitch account by channel ID is currently in use.
     */
    public static boolean accountUsed(String channelID) {
        Set<String> keys = TwitchMinecraft.getTwitchConfig().getConfigurationSection("").getKeys(false);
        keys.removeIf(key -> TwitchMinecraft.getTwitchConfig().getConfigurationSection(key) == null || TwitchMinecraft.getTwitchConfig().getConfigurationSection(key).getString("channelID") == null);

        return !keys.stream().map(key -> TwitchMinecraft.getTwitchConfig().getConfigurationSection(key).getString("channelID"))
                .filter(id -> id.equalsIgnoreCase(channelID)).collect(Collectors.toList()).isEmpty();
    }

    /**
     * @param uuid The player to check if they're synced
     * @return If the player has synced their Twitch and Minecraft account at all.
     */
    public static boolean isSynced(String uuid) {
        return isSubbed(uuid) || TwitchMinecraft.getTwitchConfig().getConfigurationSection("data." + uuid) != null;
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
            keys = TwitchMinecraft.getTwitchConfig().getConfigurationSection("").getKeys(false);
        } else {
            keys = TwitchMinecraft.getTwitchConfig().getConfigurationSection("data").getKeys(false);
        }

        keys.removeIf(key -> TwitchMinecraft.getTwitchConfig().getConfigurationSection(key) == null);

        List<String> uuid = keys.stream()
                .filter(key -> TwitchMinecraft.getTwitchConfig().getConfigurationSection(key).getString("channelName").equalsIgnoreCase(channelName))
                .collect(Collectors.toList());

        return uuid.size() > 0 ? uuid.get(0) : getUUIDFromChannelName(channelName, false);
    }

    /**
     * Clears all data from the data section of twitchdata.yml
     * @param uuid The uuid to clear
     */
    public static void clearData(String uuid) {
        TwitchMinecraft.getTwitchConfig().set("data." + uuid, null);
        TwitchMinecraft.saveFile(TwitchMinecraft.getTwitchData(), TwitchMinecraft.getTwitchConfig());
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
        if(TwitchMinecraft.getTwitchConfig().getConfigurationSection(uuid) == null) return null;

        return new TwitchPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)),
                TwitchMinecraft.getTwitchConfig().getString(uuid + ".channelID"),
                TwitchMinecraft.getTwitchConfig().getString(uuid + ".channelName"),
                TwitchMinecraft.getTwitchConfig().getString(uuid + ".expires"),
                TwitchMinecraft.getTwitchConfig().getString(uuid + ".refreshToken"),
                TwitchMinecraft.getTwitchConfig().getInt(uuid + ".tier"),
                TwitchMinecraft.getTwitchConfig().getInt(uuid + ".streak"));
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
        ConfigurationSection section = TwitchMinecraft.getTwitchConfig().getConfigurationSection("data." + uuid);
        if(section == null) {
            return create(uuid);
        }

        TwitchPlayer player = new TwitchPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
        player.setChannelName(section.getString("channelName"));
        player.setChannelID(section.getString("channelID"));
        player.setLastSubDate(section.getString("lastSubDate"));

        return player;
    }

    /**
     * @return All UUIDs that have been previously synced and subscribed.
     */
    public static List<String> getSubbedList() {
        return subbedList;
    }

}
