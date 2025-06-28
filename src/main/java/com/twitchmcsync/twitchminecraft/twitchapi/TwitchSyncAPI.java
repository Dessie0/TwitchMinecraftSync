package com.twitchmcsync.twitchminecraft.twitchapi;

import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.concurrent.CompletableFuture;

public interface TwitchSyncAPI {

    @POST("/register-code")
    CompletableFuture<String> registerCode(@Query("code") String code, @Query("uuid") String uuid, @Query("serverUrl") String serverUrl, @Query("sharedSecret") String secret, @Query("clientId") String clientId);

}
