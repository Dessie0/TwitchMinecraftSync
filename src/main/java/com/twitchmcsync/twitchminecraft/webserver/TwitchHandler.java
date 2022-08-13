package com.twitchmcsync.twitchminecraft.webserver;

import com.google.gson.JsonObject;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TwitchHandler {

    private static final List<TwitchHandler> handlers = new ArrayList<>();

    private final TwitchMinecraft plugin;
    private final TwitchPlayer twitchPlayer;

    //Used to resync.
    public TwitchHandler(TwitchPlayer player) {
        this.plugin = player.getPlugin();
        this.twitchPlayer = player;
        handlers.add(this);
    }

    public TwitchPlayer getTwitchPlayer() {
        return twitchPlayer;
    }
    public static List<TwitchHandler> getHandlers() {
        return handlers;
    }
    public TwitchMinecraft getPlugin() {
        return plugin;
    }

    public boolean checkIfSubbed(String accessToken, String userID) {
        String stringURL = "https://api.twitch.tv/helix/subscriptions/user?user_id=" + userID + "&broadcaster_id=" + this.getPlugin().getChannelID();
        try {
            URL url = new URL(stringURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            con.setRequestProperty("Client-ID", this.getPlugin().getConfig().getString("clientID"));
            con.setRequestProperty("Authorization", "Bearer " + accessToken);

            con.setRequestMethod("GET");

            JsonObject json = this.getPlugin().getJsonObject(con.getInputStream()).get("data").getAsJsonArray().get(0).getAsJsonObject();

            twitchPlayer.setTier(setTier(json));

            //They're still subbed, so we can add to their streak.
//            int streak = 1;
//            while(ZonedDateTime.parse(json.get("created_at").getAsString()).plusMonths(streak).isBefore(ZonedDateTime.now())) {
//                streak++;
//            }
//            twitchPlayer.setStreak(streak);

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
            URL url = new URL("https://api.twitch.tv/helix/users");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            con.setRequestProperty("Client-ID", this.getPlugin().getConfig().getString("clientID"));
            con.setRequestProperty("Authorization", "Bearer " + accessToken);

            con.setRequestMethod("GET");

            JsonObject json = this.getPlugin().getJsonObject(con.getInputStream()).get("data").getAsJsonArray().get(0).getAsJsonObject();

            twitchPlayer.setChannelID(json.get("id").getAsString());
            twitchPlayer.setChannelName(json.get("login").getAsString());

            return twitchPlayer.getChannelID();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getLogoURL(String accessToken) {
        try {
            URL url = new URL("https://api.twitch.tv/helix/users");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
            con.setRequestProperty("Client-ID", this.getPlugin().getConfig().getString("clientID"));
            con.setRequestProperty("Authorization", "Bearer " + accessToken);

            con.setRequestMethod("GET");

            return this.getPlugin().getJsonObject(con.getInputStream()).get("data").getAsJsonArray().get(0).getAsJsonObject().get("profile_image_url").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getAccessToken(String code) {
        String stringURL = "https://id.twitch.tv/oauth2/token" +
                "?client_id=" + this.getPlugin().getConfig().getString("clientID") +
                "&client_secret=" + this.getPlugin().getConfig().getString("clientSecret") +
                "&code=" + code + "&grant_type=authorization_code" +
                "&redirect_uri=https://twitchmcsync.com";

        try {
            URL url = new URL(stringURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            JsonObject json = this.getPlugin().getJsonObject(con.getInputStream());

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
                "&client_id=" + this.getPlugin().getConfig().getString("clientID") +
                "&client_secret=" + this.getPlugin().getConfig().getString("clientSecret") ;

        try {
            URL url = new URL(stringURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");

            JsonObject json = this.getPlugin().getJsonObject(con.getInputStream());

            player.setRefreshToken(json.get("refresh_token").getAsString());

            return json.get("access_token").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int setTier(JsonObject json) {
        switch (json.get("tier").getAsInt()) {
            case 1000: return 1;
            case 2000: return 2;
            case 3000: return 3;
            default: return 0;
        }
    }

}
