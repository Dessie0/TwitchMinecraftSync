package com.twitchmcsync.twitchminecraft.twitchapi.twitch;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import me.dessie.dessielib.annotations.storageapi.RecomposeConstructor;
import me.dessie.dessielib.annotations.storageapi.Stored;

@Getter
public class OAuthToken {

    @SerializedName("access_token")
    @Stored
    public String accessToken;

    @SerializedName("refresh_token")
    @Stored
    public String refreshToken;

    @RecomposeConstructor
    public OAuthToken(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
