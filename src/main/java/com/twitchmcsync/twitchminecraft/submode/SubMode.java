package com.twitchmcsync.twitchminecraft.submode;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import lombok.Getter;
import lombok.Setter;
import me.dessie.dessielib.annotations.storageapi.RecomposeConstructor;
import me.dessie.dessielib.annotations.storageapi.Stored;
import me.dessie.dessielib.storageapi.format.flatfile.YAMLContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SubMode {

    @Stored(storeAs = "enabled")
    @Setter
    private boolean enabled;

    @Stored(storeAs = "kick_non_subs_when_enabled")
    @Setter
    private boolean kickUsersWhenEnabled;

    @Stored(storeAs = "auto_submode")
    private final boolean autoSubmode;

    @Stored(storeAs = "auto_submode_timezone")
    private final String autoSubmodeTimezone;

    private final List<SubmodeWindow> submodeWindows;
    private boolean subWindowActive;

    @RecomposeConstructor
    public SubMode(boolean enabled, boolean kickUsersWhenEnabled, boolean autoSubmode, String autoSubmodeTimezone) {
        this.enabled = enabled;
        this.kickUsersWhenEnabled = kickUsersWhenEnabled;
        this.autoSubmode = autoSubmode;
        this.autoSubmodeTimezone = autoSubmodeTimezone;
        this.submodeWindows = new ArrayList<>();

        //Load the SubmodeWindows manually
        YAMLContainer container = TwitchMinecraft.getInstance().getConfigContainer();
        container.getKeys("submode.submode_windows").forEach(key -> {
            this.getSubmodeWindows().add(container.retrieve(SubmodeWindow.class, "submode.submode_windows." + key));
        });

        if(this.isAutoSubmode()) {
            Bukkit.getScheduler().runTaskTimer(TwitchMinecraft.getInstance(), this::evaluateSubWindows, 0, 20 * 60);
        }
    }

    public boolean canKickUser(Player player) {
        return this.isEnabled() && this.isKickUsersWhenEnabled() && !player.hasPermission("twitchmcsync.submode.bypass");
    }

    public void attemptKickAllPlayers() {
        if(!this.isEnabled() || !this.isKickUsersWhenEnabled()) return;

        Bukkit.getOnlinePlayers().forEach(player -> {
            if(!this.canKickUser(player)) return;

            TwitchPlayer.load(player.getUniqueId()).thenAccept(twitchPlayer -> {
                //Kick them if not synced or not subbed.
                if(twitchPlayer != null && twitchPlayer.isSubbed()) return;
                Bukkit.getScheduler().runTask(TwitchMinecraft.getInstance(), () -> {
                    player.kick(TwitchMinecraft.getInstance().getLanguage().getComponent("submode.kicked"));
                });
            });
        });

    }

    public void evaluateSubWindows() {
        //Don't evaluate if manual override is active.
        if(this.isForcedEnabled()) return;

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(this.getAutoSubmodeTimezone()));

        boolean current = this.isEnabled();

        this.subWindowActive = this.getSubmodeWindows().stream().anyMatch(window -> window != null && window.isInWindow(now));

        //Submode was auto-enabled, so kick everyone online.
        if(!current && this.subWindowActive) {
            this.attemptKickAllPlayers();
        }
    }

    public boolean isForcedEnabled() {
        return this.enabled;
    }

    public boolean isEnabled() {
        return this.enabled || this.subWindowActive;
    }

    public void save() {
        TwitchMinecraft.getInstance().getConfig().set("submode.enabled", this.isForcedEnabled());
        TwitchMinecraft.getInstance().saveConfig();
    }
}
