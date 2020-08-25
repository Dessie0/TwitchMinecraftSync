package me.dessie.twitchminecraft.WebServer;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.dessie.twitchminecraft.Events.twitchminecraft.TwitchSubscribeEvent;
import me.dessie.twitchminecraft.TwitchMinecraft;
import me.dessie.twitchminecraft.TwitchPlayer;
import net.lingala.zip4j.ZipFile;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class WebServer implements HttpHandler {

    static HttpServer server;
    ThreadPoolExecutor threadPoolExecutor;
    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    //Where key is their code, and the value is the current response. Updated in the handle() below.
    //Entries are removed when the client requests it & the response is either a fail or success (not waiting)
    Map<String, Response> responses = new HashMap<>();

    public void create(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", this);
            server.createContext("/twitchresponse", new TwitchResponseHandler());
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

            server.setExecutor(threadPoolExecutor);
            server.start();

            System.out.println("Server started on port " + port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove() {
        server.stop(1);
        threadPoolExecutor.shutdownNow();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        //Serve the index.html if necessary
        if(httpExchange.getRequestURI().toString().contains("&scope")) {
            httpExchange.sendResponseHeaders(200, plugin.indexFile.length());
            OutputStream os = httpExchange.getResponseBody();
            Files.copy(plugin.indexFile.toPath(), os);
            os.close();
        } else {
            //Serve other files as they are references in the index.html
            File file = new File(plugin.getDataFolder() + "/webserver" + httpExchange.getRequestURI().toString());

            httpExchange.sendResponseHeaders(200, file.length());
            OutputStream os = httpExchange.getResponseBody();
            Files.copy(file.toPath(), os);
            os.close();
        }

        handlePlayer(httpExchange);
    }

    public void handlePlayer(HttpExchange httpExchange) {
        Optional<TwitchHandler> optionalHandler = plugin.handlers.values().stream()
                .filter(handle -> handle.getPlayerIP().toString().equalsIgnoreCase(httpExchange.getRemoteAddress().getAddress().toString())
                        || handle.getPlayerIP().toString().equalsIgnoreCase("/127.0.0.1")).findAny();

        String code = httpExchange.getRequestURI().getRawQuery().split("=")[1].split("&")[0];

        responses.put(code, new Response(Responses.WAITING, null));
        if(optionalHandler.isPresent()) {
            TwitchHandler handler = optionalHandler.get();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                //Get the access token
                String accessToken = handler.getAccessToken(code);

                //Set the LogoURL inside the TwitchPlayer.
                String logoURL = handler.getLogoURL(accessToken);
                handler.getTwitchPlayer().setLogoURL(logoURL);

                if (TwitchPlayer.playerExists(handler.getTwitchPlayer().getUuid())) {
                    //Create the TwitchPlayer from their saved info.
                    TwitchPlayer saved = TwitchPlayer.create(handler.getTwitchPlayer().getUuid());

                    //Update the logoURL for the new TwitchPlayer.
                    saved.setLogoURL(logoURL);

                    responses.put(code, new Response(Responses.ALREADY_CLAIMED, saved));
                } else {
                    String userID = handler.getUserID(accessToken);
                    if (!TwitchPlayer.accountUsed(handler.getTwitchPlayer().getChannelID())) {
                        if (handler.checkIfSubbed(accessToken, userID)) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                //Call the TwitchSubscribeEvent
                                Bukkit.getPluginManager().callEvent(new TwitchSubscribeEvent(handler.getTwitchPlayer()));
                                responses.put(code, new Response(Responses.CLAIMED, handler.getTwitchPlayer()));
                            });
                        } else responses.put(code, new Response(Responses.NOT_SUBBED, handler.getTwitchPlayer()));
                    } else {
                        //Create the TwitchPlayer from the player who owns this account.
                        TwitchPlayer created = TwitchPlayer.create(TwitchPlayer.getUUIDFromChannelName(handler.getTwitchPlayer().getChannelName()));

                        //Set the logoURL for the new TwitchPlayer.
                        created.setLogoURL(logoURL);

                        responses.put(code, new Response(Responses.ACCOUNT_USED, created));
                    }
                }
            });

            plugin.handlers.remove(handler.getTwitchPlayer().getUuid());
        } else {
            //No handler?
            responses.put(code, new Response(Responses.FAILED, null));
        }
    }

    class Response {

        Responses responses;
        TwitchPlayer twitchPlayer;
        Response(Responses responses, TwitchPlayer twitchPlayer) {
            this.responses = responses;
            this.twitchPlayer = twitchPlayer;
        }

        public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }
        public Responses getResponses() { return responses; }
    }

    enum Responses {
        WAITING, ALREADY_CLAIMED, CLAIMED, FAILED, ACCOUNT_USED, NOT_SUBBED
    }
}

//Is called whenever the <URI>/twitchresponse is pinged from the JavaScript.
class TwitchResponseHandler implements HttpHandler {

    private TwitchMinecraft plugin = TwitchMinecraft.getPlugin(TwitchMinecraft.class);

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().put("Content-Type", Arrays.asList("application/json"));

        String code = httpExchange.getRequestURI().getRawQuery().split("=")[1];
        WebServer.Response response = plugin.webServer.responses.get(code);

        JsonObject json = new JsonObject();
        json.addProperty("response_type", response.getResponses().toString());

        //Remove this from the map.
        if(response.getResponses() != WebServer.Responses.WAITING) {
            plugin.webServer.responses.remove(code);
            if(response.getResponses() != WebServer.Responses.FAILED) {
                //Set all the TwitchPlayer information.
                //This depends on which response we got.
                json.addProperty("player_name", response.getTwitchPlayer().getName());
                json.addProperty("uuid", response.getTwitchPlayer().getUuid());
                json.addProperty("channel", response.getTwitchPlayer().getChannelName());
                json.addProperty("logoURL", response.getTwitchPlayer().getLogoURL());

                if(response.getResponses() != WebServer.Responses.NOT_SUBBED) {
                    json.addProperty("tier", response.getTwitchPlayer().getTier());
                    json.addProperty("expires", TwitchMinecraft.formatExpiry(response.getTwitchPlayer().getExpires()));
                    json.addProperty("streak", response.getTwitchPlayer().getStreak());
                }
            }
        }

        httpExchange.sendResponseHeaders(200, json.toString().getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(json.toString().getBytes());
        os.close();
    }
}
