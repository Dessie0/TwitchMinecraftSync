package com.twitchmcsync.twitchminecraft.lang;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import me.dessie.dessielib.core.utils.Colors;
import me.dessie.dessielib.storageapi.format.flatfile.YAMLContainer;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class Language {

    private final YAMLContainer container;

    public Language(TwitchMinecraft plugin) {
        plugin.saveResource("lang.yml", false);
        this.container = new YAMLContainer(plugin.getStorageAPI(), new File(plugin.getDataFolder(), "lang.yml"));
    }

    public void sendMessage(CommandSender player, String path) {
        this.sendMessage(player, path, Collections.emptyMap());
    }

    public void sendMessage(CommandSender player, String path, Map<String, String> placeholders) {
        String message = Colors.color(this.getContainer().retrieve(path));

        for(String placeholder : placeholders.keySet()) {
            message = message.replace("%" + placeholder + "%", placeholders.get(placeholder));
        }

        if(message.equalsIgnoreCase("")) return;

        player.sendMessage(message);
    }

    public YAMLContainer getContainer() {
        return container;
    }
}
