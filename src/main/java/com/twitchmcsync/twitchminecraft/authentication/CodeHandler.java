package com.twitchmcsync.twitchminecraft.authentication;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CodeHandler {

    @Getter
    private static String allowedCharacters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Getter
    public static final Map<UUID, String> codes = new HashMap<>();

    @Getter
    private static Random random = new Random();

    private static final int CODE_LENGTH = 6;

    private static String generateCode() {
        String code;
        do {
            StringBuilder generated = new StringBuilder(CODE_LENGTH);
            for(int i = 0; i < CODE_LENGTH; i++) {
                generated.append(allowedCharacters.charAt(getRandom().nextInt(allowedCharacters.length())));
            }
            code = generated.toString();
        } while (codes.containsValue(code));
        return code;
    }

    public static CompletableFuture<String> createCode(UUID uuid) {
        String code = getCodes().compute(uuid, (u, c) -> {
            if(c != null) return c;
            return generateCode();
        });

        //Send the code to the primary webserver.
        return uploadCode(uuid, code).thenApply(v -> {
            //Now we'll start a timer to expire the code in 5 mins. The server is going to do the same.
            Bukkit.getScheduler().runTaskLater(TwitchMinecraft.getInstance(), () -> {
                getCodes().remove(uuid);
            }, 20 * 60 * 5);

            return code;
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    public static CompletableFuture<Void> uploadCode(UUID uuid, String code) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            TwitchMinecraft plugin = TwitchMinecraft.getInstance();
            try {
                plugin.getSyncAPI().registerCode(code,
                        uuid.toString(),
                        plugin.getConfig().getString("redirect_url"),
                        plugin.getSecret(),
                        plugin.getConfig().getString("client_id"));

                //Update the webserver that the code was generated.
                getCodes().put(uuid, code);

                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
