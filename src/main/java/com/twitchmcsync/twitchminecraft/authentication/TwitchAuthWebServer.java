package com.twitchmcsync.twitchminecraft.authentication;

import com.github.twitch4j.helix.domain.Subscription;
import com.github.twitch4j.helix.domain.User;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.events.TwitchSyncEvent;
import com.twitchmcsync.twitchminecraft.twitchapi.twitch.OAuthToken;
import fi.iki.elonen.NanoHTTPD;
import lombok.Getter;
import me.dessie.dessielib.storageapi.util.JsonObjectBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Getter
public class TwitchAuthWebServer extends NanoHTTPD {

    private TwitchMinecraft plugin;
    private String secret;

    public TwitchAuthWebServer(TwitchMinecraft plugin, String sharedSecret, int port) {
        super("0.0.0.0", port);
        this.plugin = plugin;
        this.secret = sharedSecret;
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            if(session.getMethod() != Method.POST || !"/link".equalsIgnoreCase(session.getUri())) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
            }

            String auth = session.getHeaders().get("authorization");
            if(auth == null || !auth.equalsIgnoreCase(this.getSecret())) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden");
            }

            Map<String, String> body = new HashMap<>();
            try {
                session.parseBody(body);
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Body parse error");
            }

            Map<String, String> params = session.getParms();
            String uuidString = params.get("uuid"); //The uuid.
            String token = params.get("code"); //The code provided back from Twitch.

            if (uuidString == null || token == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing uuid or token");
            }

            UUID uuid = UUID.fromString(uuidString);

            //Code handled, so can be expired now.
            CodeHandler.getCodes().remove(uuid);

            //Obtain the player's username.
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

            OAuthToken accessToken = this.getPlugin().getTwitchWrapper().getToken(token).join();
            User user = this.getPlugin().getTwitchWrapper().getUserFromToken(accessToken).join();

            //Store the token & the players UUID somewhere.
            String playerName = player.getName();
            String channel = user.getDisplayName();
            String logoURL = user.getProfileImageUrl();

            //Attempt to load an existing TwitchPlayer from that UUID.
            //If another player is linked to this account already, this is invalid and we'll disallow the linking.
            TwitchPlayer existing = TwitchPlayer.loadFromChannelId(user.getId()).join();

            if(existing != null && existing.getUuid() != uuid) {
                //Respond to the server so it can display proper HTML to the user.
                String json = new JsonObjectBuilder().add("uuid", uuidString)
                        .add("playerName", Bukkit.getOfflinePlayer(existing.getUuid()).getName())
                        .add("channel", channel)
                        .add("logoURL", logoURL)
                        .add("tier", 0)
                        .add("response_type", "ACCOUNT_USED").build().toString();

                Response response = newFixedLengthResponse(Response.Status.OK, "application/json", json);
                response.addHeader("Content-Length", String.valueOf(json.getBytes(StandardCharsets.UTF_8).length));
                return response;
            }

            //Check if the player is subscribed.
            Subscription subscription = this.getPlugin().getTwitchWrapper().getSubscription(accessToken, user.getId())
                    .handle((result, ex) -> {
                        if(ex != null) {
                            if(ex instanceof NotSubscribedException) {
                                return null;
                            }
                            throw new CompletionException(ex);
                        }
                        return result;
                    }).join();

            int tier = subscription == null ? 0 : Integer.parseInt(String.valueOf(subscription.getTier().charAt(0)));

            //Save their access tokens and such for later.
            //When storing, we'll ALWAYS assume they're not subbed. The JoinAuthorization will handle setting this to true/false when it re-pings Twitch once they login again.
            TwitchPlayer twitchPlayer = new TwitchPlayer(uuid.toString(), user.getId(), channel, accessToken, tier, false);
            twitchPlayer.save();

            //At this point the user is synced, so if they're online we'll do the events.
            if(twitchPlayer.getPlayer() != null) {
                Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
                    Bukkit.getServer().getPluginManager().callEvent(new TwitchSyncEvent(twitchPlayer.getPlayer(), twitchPlayer, subscription, tier));
                });
            }

            //User is not subscribed, but we'll still keep the token for later if they decide to subscribe.
            if(subscription == null) {
                String json = new JsonObjectBuilder().add("uuid", uuidString)
                        .add("playerName", playerName)
                        .add("broadcaster", this.getPlugin().getTwitchWrapper().getBroadcaster())
                        .add("channel", channel)
                        .add("logoURL", logoURL)
                        .add("tier", "0")
                        .add("response_type", "NOT_SUBBED").build().toString();

                Response response = newFixedLengthResponse(Response.Status.OK, "application/json", json);
                response.addHeader("Content-Length", String.valueOf(json.getBytes(StandardCharsets.UTF_8).length));
                return response;
            }

            //Respond to the server so it can display proper HTML to the user.
            String json = new JsonObjectBuilder().add("uuid", uuidString)
                    .add("playerName", playerName)
                    .add("channel", channel)
                    .add("logoURL", logoURL)
                    .add("tier", tier)
                    .add("response_type", "CLAIMED").build().toString();

            Response response = newFixedLengthResponse(Response.Status.OK, "application/json", json);
            response.addHeader("Content-Length", String.valueOf(json.getBytes(StandardCharsets.UTF_8).length));
            return response;
        } catch (Exception e) {
            e.printStackTrace();

            //In the case of any error, we'll return the failed type with the message.
            String json = new JsonObjectBuilder()
                    .add("failed_message", e.getMessage())
                    .add("response_type", "FAILED").build().toString();
            Response response = newFixedLengthResponse(Response.Status.OK, "application/json", json);
            response.addHeader("Content-Length", String.valueOf(json.getBytes(StandardCharsets.UTF_8).length));
            return response;
        }
    }
}
