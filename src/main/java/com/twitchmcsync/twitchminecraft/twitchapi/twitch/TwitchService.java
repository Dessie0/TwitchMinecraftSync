package com.twitchmcsync.twitchminecraft.twitchapi.twitch;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.util.concurrent.CompletableFuture;

public interface TwitchService {

    @FormUrlEncoded
    @POST("oauth2/token")
    CompletableFuture<OAuthToken> getAccessToken(
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("code") String code,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri);

    @FormUrlEncoded
    @POST("oauth2/token")
    CompletableFuture<OAuthToken> refreshAccessToken(
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret,
            @Field("refresh_token") String refreshToken,
            @Field("grant_type") String grantType
    );

}
