package com.twitchmcsync.twitchminecraft.authentication;

public class AccessTokenInvalidException extends Exception {

    public AccessTokenInvalidException(String message) {
        super(message);
    }
}
