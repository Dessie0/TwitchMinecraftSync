package com.twitchmcsync.twitchminecraft.webserver;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchResubscribeEvent;
import com.twitchmcsync.twitchminecraft.events.twitchminecraft.TwitchSubscribeEvent;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class WebServer implements HttpHandler {

    private final TwitchMinecraft plugin;
    private HttpServer server;
    private ThreadPoolExecutor threadPoolExecutor;

    //Where key is their code, and the value is the current response. Updated in the handle() below.
    //Entries are removed when the client requests it & the response is either a fail or success (not waiting)
    private final Map<String, Response> responses = new HashMap<>();

    public WebServer(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }

    public void create(int port) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

            this.getServer().createContext("/", this);
            this.getServer().createContext("/twitchresponse", new TwitchResponseHandler(this.getPlugin()));
            this.getServer().setExecutor(threadPoolExecutor);
            this.getServer().start();

            Bukkit.getLogger().info("[TwitchMinecraftSync] Server started on port " + port);

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
        if(httpExchange.getRequestURI().toString().contains("?code") || httpExchange.getRequestURI().toString().equalsIgnoreCase("/")) {
            httpExchange.sendResponseHeaders(200, this.getPlugin().indexFile.length());
            OutputStream os = httpExchange.getResponseBody();
            Files.copy(this.getPlugin().indexFile.toPath(), os);
            os.close();

            handlePlayer(httpExchange);
        } else {
            //Serve other files as they are references in the index.html
            File file = new File(this.getPlugin().getDataFolder() + "/webserver" + httpExchange.getRequestURI().toString());

            httpExchange.sendResponseHeaders(200, file.length());
            OutputStream os = httpExchange.getResponseBody();
            Files.copy(file.toPath(), os);
            os.close();
        }
    }

    public void handlePlayer(HttpExchange httpExchange) {
        Map<String, String> params = getParams(httpExchange.getRequestURI().getRawQuery());

        String code = params.get("code");

        if(!params.containsKey("uuid")) {
            this.getResponses().put(code, new Response(Responses.FAILED, null));
        }

        String uuid = params.get("uuid");

        Optional<TwitchHandler> optionalHandler = TwitchHandler.getHandlers().stream()
                .filter(handle -> handle.getTwitchPlayer().getUuid().equalsIgnoreCase(uuid)).findAny();

        this.getResponses().put(code, new Response(Responses.WAITING, null));
        if(optionalHandler.isPresent()) {
            TwitchHandler handler = optionalHandler.get();

            Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
                //Get the access token
                String accessToken = handler.getAccessToken(code);

                //Set the LogoURL inside the TwitchPlayer.
                String logoURL = handler.getLogoURL(accessToken);
                handler.getTwitchPlayer().setLogoURL(logoURL);

                if (TwitchPlayer.isSubbed(handler.getTwitchPlayer().getUuid())) {
                    //Create the TwitchPlayer from their saved info.
                    TwitchPlayer saved = TwitchPlayer.create(handler.getTwitchPlayer().getUuid());

                    //Update the logoURL for the new TwitchPlayer.
                    saved.setLogoURL(logoURL);

                    this.getResponses().put(code, new Response(Responses.ALREADY_CLAIMED, saved));
                } else {
                    String userID = handler.getUserID(accessToken);
                    if (!TwitchPlayer.accountUsed(handler.getTwitchPlayer().getChannelID())) {

                        //Check if they've previously subbed.
                        boolean hasSubbed = TwitchPlayer.getSubbedList().contains(handler.getTwitchPlayer().getUuid());

                        if (handler.checkIfSubbed(accessToken, userID)) {
                            Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
                                //Call the respective event
                                //Calls Resubscribe if they have subbed in the past.
                                //Calls Subscribe if they haven't.
                                if(hasSubbed) {
                                    TwitchResubscribeEvent event = new TwitchResubscribeEvent(handler.getTwitchPlayer());
                                    Bukkit.getPluginManager().callEvent(event);
                                    if(!event.isCancelled()) {
                                        TwitchMinecraft.getInstance().getRewardHandler().giveResub(event.getTwitchPlayer());
                                        event.getTwitchPlayer().clearData();
                                        this.getResponses().put(code, new Response(Responses.RESUBBED, handler.getTwitchPlayer()));
                                    } else {
                                        this.getResponses().put(code, new Response(Responses.FAILED, handler.getTwitchPlayer()));
                                    }

                                } else {
                                    TwitchSubscribeEvent event = new TwitchSubscribeEvent(handler.getTwitchPlayer());
                                    Bukkit.getPluginManager().callEvent(event);
                                    if(!event.isCancelled()) {
                                        TwitchMinecraft.getInstance().getRewardHandler().give(event.getTwitchPlayer());
                                        event.getTwitchPlayer().clearData();
                                        this.getResponses().put(code, new Response(Responses.CLAIMED, handler.getTwitchPlayer()));
                                    } else {
                                        this.getResponses().put(code, new Response(Responses.FAILED, handler.getTwitchPlayer()));
                                    }
                                }
                            });
                        } else this.getResponses().put(code, new Response(Responses.NOT_SUBBED, handler.getTwitchPlayer()));
                    } else {
                        //Create the TwitchPlayer from the player who owns this account.
                        TwitchPlayer created = TwitchPlayer.create(TwitchPlayer.getUUIDFromChannelName(handler.getTwitchPlayer().getChannelName()));

                        //Set the logoURL for the new TwitchPlayer.
                        created.setLogoURL(logoURL);

                        this.getResponses().put(code, new Response(Responses.ACCOUNT_USED, created));
                    }
                }
            });

            TwitchHandler.getHandlers().remove(handler);
        } else {
            //No handler?
            this.getResponses().put(code, new Response(Responses.FAILED, null));
        }
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }

    public HttpServer getServer() {
        return server;
    }

    public Map<String, Response> getResponses() {
        return responses;
    }

    /**
     * Returns a parameter map from a request.
     * @param request The URL that was sent to the WebServer
     * @return A Map with the parameter key and values.
     */
    public static Map<String, String> getParams(String request) {
        if(request == null || !request.contains("&")) return new HashMap<>();

        String[] requests = request.split("&");

        return Arrays.stream(requests).collect(Collectors.toMap(
                (value) -> value.split("=")[0], (value) -> value.split("=")[1]));
    }

    class Response {
        private final Responses responses;
        private final TwitchPlayer twitchPlayer;
        Response(Responses responses, TwitchPlayer twitchPlayer) {
            this.responses = responses;
            this.twitchPlayer = twitchPlayer;
        }

        public TwitchPlayer getTwitchPlayer() { return twitchPlayer; }
        public Responses getResponses() { return responses; }
    }

    enum Responses {
        WAITING, ALREADY_CLAIMED, CLAIMED, RESUBBED, FAILED, ACCOUNT_USED, NOT_SUBBED
    }
}

//Is called whenever the <URI>/twitchresponse is pinged from the JavaScript.
class TwitchResponseHandler implements HttpHandler {

    private final TwitchMinecraft plugin;

    public TwitchResponseHandler(TwitchMinecraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().put("Content-Type", Arrays.asList("application/json"));

        JsonObject json = new JsonObject();
        String code = httpExchange.getRequestURI().getRawQuery().split("=")[1];

        //Failed, code was returned as null.
        if (code.equalsIgnoreCase("null")) {
            json.addProperty("response_type", WebServer.Responses.FAILED.toString());
        } else {
            WebServer.Response response = this.getPlugin().getWebServer().getResponses().get(code);

            json.addProperty("response_type", response.getResponses().toString());
            //Remove this from the map.
            if (response.getResponses() != WebServer.Responses.WAITING) {
                this.getPlugin().getWebServer().getResponses().remove(code);
                if (response.getResponses() != WebServer.Responses.FAILED) {
                    //Set all the TwitchPlayer information.
                    //This depends on which response we got.
                    json.addProperty("player_name", response.getTwitchPlayer().getName());
                    json.addProperty("uuid", response.getTwitchPlayer().getUuid());
                    json.addProperty("channel", response.getTwitchPlayer().getChannelName());
                    json.addProperty("logoURL", response.getTwitchPlayer().getLogoURL());

                    if (response.getResponses() != WebServer.Responses.NOT_SUBBED) {
                        json.addProperty("tier", response.getTwitchPlayer().getTier());
                    }
                }
            }
        }

        httpExchange.sendResponseHeaders(200, json.toString().getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(json.toString().getBytes());
        os.close();
    }

    public TwitchMinecraft getPlugin() {
        return plugin;
    }
}
