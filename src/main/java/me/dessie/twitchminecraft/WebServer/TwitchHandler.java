package me.dessie.twitchminecraft.WebServer;

import com.google.gson.JsonObject;
import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.time.ZonedDateTime;

public class TwitchHandler extends WebServer {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);
    private InetAddress playerIP;

    TwitchPlayer twitchPlayer;

    //Used to resync.
    public TwitchHandler(TwitchPlayer player, InetAddress playerIP) {
        this.twitchPlayer = player;
        this.playerIP = playerIP;
    }

    public InetAddress getPlayerIP() { return playerIP; }
    public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }

    public boolean checkIfSubbed(String accessToken, String userID) {
        String stringURL = "https://api.twitch.tv/kraken/users/" + userID + "/subscriptions/" + plugin.channelID;

        try {
            URL url = new URL(stringURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            con.setRequestProperty("Client-ID", plugin.getConfig().getString("clientID"));
            con.setRequestProperty("Authorization", "OAuth " + accessToken);

            con.setRequestMethod("GET");

            JsonObject json = plugin.getJsonObject(con.getInputStream());

            twitchPlayer.setTier(setTier(json));

            //They're still subbed, so we can add to their streak.
            int streak = 1;
            if(ZonedDateTime.parse(json.get("created_at").getAsString()).plusMonths(streak).isBefore(ZonedDateTime.now())) {
                streak++;
            }
            twitchPlayer.setStreak(streak);

            twitchPlayer.setExpires(ZonedDateTime.parse(json.get("created_at").getAsString()).plusMonths(twitchPlayer.getStreak()).toString());

            //We're done checking, so save all their new data.
            twitchPlayer.saveData(true);

            return true;

        } catch (IOException e) {
            if(e instanceof FileNotFoundException) {
                twitchPlayer.saveData(false);
                //User is not subscribed.
                return false;
            }
            e.printStackTrace();
        }

        twitchPlayer.saveData(false);
        return false;
    }

    public String getUserID(String accessToken) {
        try {
            URL url = new URL("https://api.twitch.tv/kraken/user");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            con.setRequestProperty("Client-ID", plugin.getConfig().getString("clientID"));
            con.setRequestProperty("Authorization", "OAuth " + accessToken);

            con.setRequestMethod("GET");

            JsonObject json = plugin.getJsonObject(con.getInputStream());

            twitchPlayer.setChannelID(json.get("_id").getAsString());
            twitchPlayer.setChannelName(json.get("name").getAsString());

            return twitchPlayer.getChannelID();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getAccessToken(String code) {
        String stringURL = "https://id.twitch.tv/oauth2/token" +
                "?client_id=" + plugin.getConfig().getString("clientID") +
                "&client_secret=" + plugin.getConfig().getString("clientSecret") +
                "&code=" + code + "&grant_type=authorization_code" +
                "&redirect_uri=" + plugin.getConfig().getString("redirectURI");

        try {
            URL url = new URL(stringURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            JsonObject json = plugin.getJsonObject(con.getInputStream());

            twitchPlayer.setRefreshToken(json.get("refresh_token").getAsString());

            return json.get("access_token").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getNewAccessToken(TwitchPlayer player) {
        String stringURL = "https://id.twitch.tv/oauth2/token" +
                "?grant_type=refresh_token" +
                "&refresh_token=" + player.getRefreshToken() +
                "&client_id=" + plugin.getConfig().getString("clientID") +
                "&client_secret=" + plugin.getConfig().getString("clientSecret") ;

        try {
            URL url = new URL(stringURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            JsonObject json = plugin.getJsonObject(con.getInputStream());

            player.setRefreshToken(json.get("refresh_token").getAsString());

            return json.get("access_token").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int setTier(JsonObject json) {
        switch (json.get("sub_plan").getAsInt()) {
            case 1000: return 1;
            case 2000: return 2;
            case 3000: return 3;
            default: return 0;
        }
    }

}
