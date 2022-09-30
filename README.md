## TwitchMinecraftSync
A Spigot plugin developed for linking Minecraft accounts and Twitch.

## :pencil2: Setup
1. Go to https://dev.twitch.tv/console/apps and click "Register Your Application"
2. Give your application a name. Be aware this will be shown to users.
3. Add an OAuth Redirect URL of `https://twitchmcsync.com` and set the Category as `Game Integration`
4. Click `Manage` on your newly created Application and copy your Client ID, and generate a new Client Secret. Save both, you will need these later.
5. Place `TwitchMinecraftSync.jar` in your `/plugins` folder and restart the server.
6. Navigate to `/plugins/TwitchMinecraftSync/config.yml` and paste in the Client ID and Client Secret.
7. Type your Twitch username in `channelName`
8. Set your Redirect URI. This is your server IP and an open port on your server.
<br>&nbsp;&nbsp;&nbsp;- The Redirect URI should be http://(server ip):(port), for example, http://172.182.52.222:8177
<br>&nbsp;&nbsp;&nbsp;- You can open a port on most popular server hosts. 
<br>&nbsp;&nbsp;&nbsp;- The port is NOT your server port.
9. Restart your server and you should be good to go!

## :books: Requirements
- Java 17
- [`Vault`](https://www.spigotmc.org/resources/vault.34315/)

## :newspaper: API

### :package: Installation / Download

#### Gradle
```groovy
maven {
  url "https://maven.pkg.github.com/dessie0/twitchminecraftsync"
}

dependencies {
  compile 'com.twitchmcsync:twitchminecraftsync:1.2.5'
}
```

#### Maven
```xml
<dependencies>
  <dependency>
    <groupId>com.twitchmcsync</groupId>
    <artifactId>twitchminecraftsync</artifactId>
    <version>1.2.5</version>
  </dependency>
</dependencies>
```

#### Manual
[Download JAR](https://github.com/Dessie0/TwitchMinecraftSync/releases)

### API Events

TwitchMinecraftSync adds four events to listen to.

[`TwitchSubscribeEvent`](https://github.com/Dessie0/TwitchMinecraftSync/blob/master/src/main/java/me/dessie/twitchminecraft/events/twitchminecraft/TwitchSubscribeEvent.java), 
[`TwitchResubscribeEvent`](https://github.com/Dessie0/TwitchMinecraftSync/blob/master/src/main/java/me/dessie/twitchminecraft/events/twitchminecraft/TwitchResubscribeEvent.java),
[`TwitchExpireEvent`](https://github.com/Dessie0/TwitchMinecraftSync/blob/master/src/main/java/me/dessie/twitchminecraft/events/twitchminecraft/TwitchExpireEvent.java), and
[`TwitchRevokeEvent`](https://github.com/Dessie0/TwitchMinecraftSync/blob/master/src/main/java/me/dessie/twitchminecraft/events/twitchminecraft/TwitchRevokeEvent.java)

<details>
<summary>Example class for using events</summary>

```java
public class Example implements Listener {

    @EventHandler
    public void onSubscribe(TwitchSubscribeEvent event) {
        if(event.getTwitchPlayer().getStreak() > 6) {
            if(event.getTwitchPlayer().getPlayer().isOnline()) {
                event.getTwitchPlayer().getPlayer().getPlayer().sendMessage(ChatColor.GREEN + "Thank you for supporting us for " + event.getTwitchPlayer().getStreak() + " months! You're awesome!");
            }
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + event.getTwitchPlayer().getName() + 
                    " (" + event.getTwitchPlayer().getChannelName() + ") just subscribed at tier " 
                    + event.getTwitchPlayer().getTier() + "!");
        });
    }

    @EventHandler
    public void onResubscribe(TwitchResubscribeEvent event) {
        if(event.getTwitchPlayer().getChannelName().equalsIgnoreCase("abadperson")) {
            if(event.getTwitchPlayer().getPlayer().isOnline()) {
                event.getTwitchPlayer().getPlayer().getPlayer().sendMessage(ChatColor.RED + "You're not allowed to resubscribe, maybe it's your username?");
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExpire(TwitchExpireEvent event) {
        if(event.getTwitchPlayer().getPlayer().isOnline() && event.getTwitchPlayer().getPlayer().getPlayer().isOp()) {
            event.getTwitchPlayer().getPlayer().getPlayer().sendMessage(ChatColor.RED + "Your sub expired, but luckily for you, you're exempt!");
            event.setCancelled(true);
        }
    }
}
```
</details>


## :clipboard: Configuration
TwitchMinecraftSync is highly configurable, and you can run any commands when a player subscribes, their subscription expires, or when they re-subscribe.

`%player%` will be replaced with their Minecraft username.<br>
`%twitchname%` will be replaced with their Twitch username.

You can also utilize permission roles that are granted or taken away as needed. Vault is required for this reason. 
<br><br> Additionally, you can perform different commands and roles for each tier.

The default configurations can be seen below.

<details>
    <summary>config.yml</summary>
    
```yaml
clientID: "<Your client ID>"
clientSecret: "<Your client secret>"
channelName: "<Channel to check for subscriptions>"
redirectURI: "http://localhost:8080"
port: 8080

rewards:
  tier1:
    subscribe:
      #The role to give them when they subscribe
      #Set to '' to ignore role giving.
      role: "Subscriber"

      #The commands to execute when they subscribe
      #%player% will get their Minecraft username
      #%twitchname% will get their Twitch username.

      #Use -p at the end if you want the command to be executed by the player.
      #By default all commands are ran by console.
      commands:
        - say Thank you %player% for syncing your Twitch account! (%twitchname%)
        - give %player% diamond 1
        - spawn -p

    resubscribe:
      role: "Subscriber"
      commands:
        - say Thank you %player% for re-subscribing! (%twitchname%)

    expire:
      #The role to remove from them when it expires.
      role: "Subscriber"
      commands:
        - say %player% (%twitchname%) has not renewed their subscription :(
        - spawn -p

  tier2:
    subscribe:
      role: "Subscriber2"
      commands:
        - say Thank you %player% for syncing your Twitch account at tier 2! (%twitchname%)
        - give %player% diamond 5
        - spawn -p

    resubscribe:
      role: "Subscriber2"
      commands:
        - say Thank you %player% for re-subscribing at tier 2! (%twitchname%)

    expire:
      role: "Subscriber2"
      commands:
        - say %player% (%twitchname%) has not renewed their subscription :(
        - spawn -p

  tier3:
    subscribe:
      role: "Subscriber3"
      commands:
        - say Thank you %player% for syncing your Twitch account at tier 3! (%twitchname%)
        - give %player% diamond 15
        - spawn -p

    resubscribe:
      role: "Subscriber3"
      commands:
        - say Thank you %player% for re-subscribing at tier 3! (%twitchname%)

    expire:
      role: "Subscriber3"
      commands:
        - say %player% (%twitchname%) has not renewed their subscription :(
        - spawn -p
```
</details>

<details>
    <summary>lang.yml</summary>

```yaml
join_message: "&d[TwitchMinecraftSync] &cYou're not synced to Twitch! Type /sync to synchronize Twitch and Minecraft!"
no_permission: "&d[TwitchMinecraftSync] &cYou do not have permission to do that!"
revoked: "&d[TwitchMinecraftSync] &cYour Twitch authorization has been revoked!\n&d[TwitchMinecraftSync] &cYou can re-sync by typing /sync"
resync_error: "&d[TwitchMinecraftSync] &cSomething went wrong when attempting to resync your subscription!"
sub_expired: "&d[TwitchMinecraftSync] &cWe could not confirm that you have resynced your Twitch account!\n&d[TwitchMinecraftSync] &cIf you would like to re-sync, please re-sub and type /sync!"
unable_to_sync: "&d[TwitchMinecraftSync] &cUnable to sync, plugin is not setup properly!"
floodgate_type: "&d[TwitchMinecraftSync] &aPlease type this link into a browser to sync your Twitch! &d%url%"

enter_player_argument: "&cYou need to enter a player!"
no_synced_account: "&cUnable to find a synced account with the name &e%player%"

revoke_success: "&aSuccessfully revoked %player%'s Twitch authorization. They will need to re-sync."
revoke_offline: "%player% is offline. Their authorization will be revoked when they login."

reload_files: "&aSuccessfully reloaded configuration files."
stopping_webserver: "&aStopping WebServer on port &d%port%"
starting_webserver: "&aStarting new WebServer on port &d%port%"
webserver_restart_success: "&aSuccessfully restarted the WebServer."

info_header: "&7&m----------&dTwitchSync&7&m----------"
info_footer: "&7&m----------&dTwitchSync&7&m----------"
info_player_name: "&dPlayer Name: &a%player%"
info_twitch_name: "&dTwitch Name: &a%twitch%"
info_subscription_tier: "&dSubscription Tier: &a%tier%"
```
</details>

## :wrench: Commands
- `/sync` - Sends the Twitch OAuth link to the player. (Granted by default)
- `/revoke <user>` - Revokes a user's synced status, and expire commands will be ran. (twitchmcsync.revoke)
- `/tinfo <user>` - Displays information regarding a user's current sync status. (twitchmcsync.tinfo)
- `/twitchreload` - Reloads the configuration. (twitchmcsync.twitchreload)
- `/twitchserverreload` - Reloads the WebServer. (twitchmcsync.twitchserverreload)

## :bug: Known Bugs
- Resubscribe Commands are fired everytime a Player joins the server. Unfortunately, this is a result of a Twitch's Helix API limitation where the endpoint does not provide an expiration date or streak to applications. 
This means that tracking when a user subscribed (and therefore resubscribed) is impossible. If you wish to help have this feature added back to the API, please upvote [here](https://twitch.uservoice.com/forums/310213-developers/suggestions/44874949-re-add-created-at-streak-and-or-expires-to-check)

## :eyeglasses: Advanced
#### Webserver Configuration
By default, TwitchMinecraftSync comes with a generic look for it's locally hosted web server. (Thanks [luaq](https://github.com/luaqs))

Almost any part of this can be modified by simply changing modifying the resources with in `/webserver`

It is not recommended to change these files unless you know what you're doing, and do NOT touch the `getTwitchResponse()` function. All other functions and files can be safely edited. 
