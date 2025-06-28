## TwitchMCSync
A Spigot plugin developed for linking Minecraft accounts and Twitch.

## :pencil2: Setup
1. Go to https://dev.twitch.tv/console/apps and click "Register Your Application"
2. Give your application a name. Be aware this will be shown to users.
3. Add an OAuth Redirect URL of `https://twitchmcsync.com/oauth` and set the Category as `Game Integration` <b>This step is extremely important, failing to do this will result in server errors.</b>
4. Click `Manage` on your newly created Application and copy your Client ID, and generate a new Client Secret. Save both, you will need these later.
5. Place `TwitchMCSync.jar` in your `/plugins` folder and restart the server.
6. Navigate to `/plugins/TwitchMCSync/config.yml` and paste in the Client ID and Client Secret.
7. Type your Twitch username in `channelName`
8. Set your Redirect URI. This, generally, is your server IP and an open port on your server.
<br>&nbsp;&nbsp;&nbsp;- The Redirect URI should be http://(server ip):(port), for example, http://172.182.52.222:8177
<br>&nbsp;&nbsp;&nbsp;- You can open a port on most popular server hosts. 
<br>&nbsp;&nbsp;&nbsp;- The port is NOT your server port.
9. Restart your server and you should be good to go!

## :books: Requirements
- Java 21
- PaperSpigot 1.21.5

## :newspaper: API

### :package: Installation / Download

#### Gradle
```groovy
maven {
  url "https://maven.pkg.github.com/dessie0/twitchminecraftsync"
}

dependencies {
  compile 'com.twitchmcsync:twitchminecraftsync:2.0.0'
}
```

#### Maven
```xml
<dependencies>
  <dependency>
    <groupId>com.twitchmcsync</groupId>
    <artifactId>twitchminecraftsync</artifactId>
    <version>2.0.0</version>
  </dependency>
</dependencies>
```

#### Manual
[Download JAR](https://github.com/Dessie0/TwitchMCSync/releases)

### API Events

TwitchMCSync adds two events you can listen to.

[`TwitchSyncEvent`](https://github.com/Dessie0/TwitchMCSync/blob/master/src/main/java/me/dessie/twitchminecraft/events/twitchminecraft/TwitchSubscribeEvent.java), 
[`TwitchUnsyncEvent`](https://github.com/Dessie0/TwitchMCSync/blob/master/src/main/java/me/dessie/twitchminecraft/events/twitchminecraft/TwitchExpireEvent.java), and

<details>
<summary>Example class for using events</summary>

```java
public class Example implements Listener {

    @EventHandler
    public void onSubscribe(TwitchSyncEvent event) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + event.getTwitchPlayer().getName() + 
                    " (" + event.getTwitchPlayer().getChannelName() + ") just subscribed at tier " 
                    + event.getTwitchPlayer().getTier() + "!");
        });
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

## :box: Modules
TwitchMCSync comes with a couple different features that you can use.

- Sub-only Mode - Prevents players from joining if they're not subscribed. Unsynced users will be granted a code to sync.
- Sub-only Windows - Enables Sub-only Mode automatically in the given time windows. 
- Live Module - Run commands as your synced users stream on Twitch.

## :clipboard: Configuration
TwitchMCSync is highly configurable, and you can run commands when a synced player subscribes or unsubscribes from your channel.

`%player%` will be replaced with their Minecraft username.<br>
`%twitchname%` will be replaced with their Twitch username.

The default configurations can be seen below.

<details>
    <summary>config.yml</summary>
    
```yaml
#You can find full setup instructions at https://github.com/Dessie0/TwitchMinecraftSync#readme

#The Client Secret given to you by Twitch at https://dev.twitch.tv/console/apps
client_id: "<Your client ID>"

#The Client Secret given to you by Twitch at https://dev.twitch.tv/console/apps
#YOU SHOULD NEVER SHARE YOUR CLIENT SECRET WITH ANYONE.
client_secret: "<Your client secret>"

#The broadcaster you want to sync with.
broadcaster_username: "<Twitch username>"

#This will never match your server's Minecraft IP:Port, usually it will be the same IP on the port listed below.
#Optionally, you can set this up to use a domain.
#Examples:
#  - http://localhost:8080
#  - http://172.182.52.222:8177
#  - http://sync.my-cool-server.com
redirect_url: "<Server URL>"

#The port that the webserver will be started on. If you're using a host, you may need to add this as an additional port
#and set this value to whatever they assign to you.
port: 8080

#Use either "flatfile" or "db". If you choose db, the database settings must be configured.
storage_format: "flatfile"

database:
  host: "localhost"
  port: 3306
  database: "database"
  username: "syncdatabase"
  password: "averysecurepassword"

#Enables sub-only mode.
#When enabled, only people subscribed to the Twitch broadcaster above will be allowed to join.
#When disabled, all players will be able to join.
submode:
  #Current value, also controlled via /submode
  enabled: false

  #When re-enabled, should we kick users that are not synced/subscribed?
  kick_non_subs_when_enabled: true

  #Automatically enable sub-mode during certain time periods.
  #If Submode is enabled via the /submode command, this will have no effect.
  #This only works if the above "enabled" value is false.
  auto_submode: true

  #Which Timezone to use when calculating if a submode window is active
  auto_submode_timezone: "America/New_York"

  #Submode windows, you can define as many of these as you wish and the name does not matter.
  #In the example provided, Sub-mode would be automatically turned on at Midnight on Saturday,
  #and would turn off at Midnight on Monday.
  submode_windows:
    sub_weekend:
      days:
        - SATURDAY
        - SUNDAY
      start: "00:00"
      end: "23:59"
```
</details>

<details>
    <summary>sync_rewards.yml</summary>

```yaml
#These commands are executed when a player is synced with a specific tier level.
#The subscribe commands will only be ran the first time they sync, or if they were unsubscribed at any point and then re-subscribed.
#They will not run if their subscription never expired and they continued subscribing through multiple months.

#Placeholders ---
# %player% - Minecraft Username
# %twitchname% - Twitch Username
# %tier% - Subscription Tier: Always 1, 2, or 3.

#Additional Notes
# You can use -p at the end of a command for it to be ran by the Player.
# By Default, all commands are executed by Console.

all:
  subscribe:
    - tmessage %player% <green>Thank you <aqua>%player% <green>for syncing your Twitch account at tier <aqua>%tier%! <gold>(%twitchname%)
  expire:
    - tmessage %player% <green>%player% <gold>(%twitchname%) <red>has not renewed their subscription :(

tier_1:
  subscribe:
    - give %player% diamond 1
    - spawn -p
  expire:
    - spawn -p

tier_2:
  subscribe:
    - give %player% diamond 5
    - spawn -p
  expire:
    - spawn -p

tier_3:
  subscribe:
    - give %player% diamond 15
    - spawn -p
  expire:
    - spawn -p
```


</details>


<details>
    <summary>live.yml</summary>

```yaml
#This module is used for running commands if a synced player goes live while on the server.
enabled: false

commands:
#Commands to run when the user goes live.
#These will also be fired if they join the server while live.
live:
- "tmessage %player% <green>%player% <gold>has gone live at <aqua><click:open_url:'https://twitch.tv/%twitchname%/'>https://twitch.tv/%twitchname%/</click>."
- "lp user %player% parent set live"

#Commands to run when the user goes offline.
#These will also be fired if the user unsyncs, subscription expires/revoked, or they leave the server.
offline:
- "tmessage %player% <green>%player% <gold>has stopped streaming :("
- "lp user %player% parent remove live"
```

</details>

<details>
    <summary>lang.yml</summary>

```yaml
sync: "<light_purple>[TwitchMCSync] <green>You can sync by <aqua><u><click:open_url:'https://twitchmcsync.com/'>clicking here</click></u> <green>using the code <gold>%code%"
already_synced: "<light_purple>[TwitchMCSync] <red>You are already synced. Please /unsync before attempting to sync again."
not_synced: "<light_purple>[TwitchMCSync] <red>You are not currently synced to Twitch."
unsync_success: "<green>Successfully unsynced you with Twitch."
code_generation_failed: "<red>Unable to generate code for syncing. Please try again. If this issue persists, please contact an administrator."
not_subscribed: "<red>This server is in submode and you are not subscribed. <newline>Please subscribe at <light_purple>https://twitch.tv/%broadcaster%/ <red>to join."
twitch_response_error: "<red>Unable to contact Twitch, please try again. If this error persists, please contact an administrator."

revoke:
  success: "<green>Successfully revoked %player%'s Twitch authorization. They will need to re-sync."
  revoked: "<light_purple>[TwitchMCSync] <red>Your Twitch authorization has been revoked!\n<light_purple>[TwitchMCSync] <red>You can re-sync by typing /sync"

submode:
  enabled: "<green>Successfully enabled submode."
  disabled: "<red>Successfully disabled submode."
  kicked: "<red>Submode has been enabled, and you are not currently subscribed. Please subscribe and resync if you would like to join this server while its in sub mode."
  join_not_synced: "<red>This server is in Sub Mode and you are not linked to Twitch.\nVisit twitchmcsync.com and enter code: %code%"

live:
  module_not_enabled: "<red>This module is not enabled."
  no_users_live: "<red>No online players are live right now."
  command_header: "<gray><strikethrough>----------</strikethrough><red><b>LIVE</b><gray><strikethrough>----------</strikethrough>"
  user_display: "<green>%player% is <red><b>LIVE.</b> <green>Watch <aqua><click:open_url:'https://twitch.tv/%twitchname%/'>here</click>"
  command_footer: "<gray><strikethrough>----------</strikethrough><red><b>LIVE</b><gray><strikethrough>----------</strikethrough>"

need_player_argument: "<light_purple>[TwitchMCSync] <red>You must provide a Minecraft or Twitch username."
no_permission: "<light_purple>[TwitchMCSync] <red>You do not have permission to do that!"
no_info_found_message: "<red>Unable to find a linked account with that name."
info_message:
  - "<gray><strikethrough>----------</strikethrough><light_purple>TwitchMCSync<gray><strikethrough>----------</strikethrough>"
  - "<light_purple>Minecraft Username: <green>%player% <gold>(%uuid%)"
  - "<light_purple>Twitch Name: <green>%twitch% <gold>(%twitch_id%)"
  - "<light_purple>Subscription Tier: <green>%tier%"
  - "<light_purple>Is Subbed: <green>%subbed%"
  - "<gray><strikethrough>----------</strikethrough><light_purple>TwitchMCSync<gray><strikethrough>----------</strikethrough>"

reload: "<green>Successfully reloaded configuration files and webserver."
```
</details>

## :wrench: Commands
- `/sync` - Generates and sends a sync code to the player. (Granted by default)
- `/unsync` - Allows a player to unsync themselves from Twitch. (Granted by default)
- `/live` - Displays all users that are actively streaming on Twitch. Only works if the live module is enabled. (Granted by default)
- `/submode` - Enables or disables Sub-only Mode. (twitchmcsync.submode)
- `/revoke <user>` - Revokes a user's synced status, and expire commands will be ran. (twitchmcsync.revoke)
- `/tinfo <user>` - Displays information regarding a user's current sync status. (twitchmcsync.tinfo)
- `/twitchreload` - Reloads the configuration. (twitchmcsync.reload)
- `/tmessage <player> <message>` - Sends a message directly to a player with Adventure component support. Only can be ran by Console.

## :bug: Known Bugs
- Resubscribe Commands are fired everytime a Player joins the server. Unfortunately, this is a result of a Twitch's Helix API limitation where the endpoint does not provide an expiration date or streak to applications. 
This means that tracking when a user subscribed (and therefore resubscribed) is impossible. If you wish to help have this feature added back to the API, please upvote [here](https://twitch.uservoice.com/forums/310213-developers/suggestions/44874949-re-add-created-at-streak-and-or-expires-to-check)
