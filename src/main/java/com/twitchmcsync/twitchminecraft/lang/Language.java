package com.twitchmcsync.twitchminecraft.lang;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import lombok.Getter;
import me.dessie.dessielib.storageapi.format.flatfile.YAMLContainer;
import me.dessie.dessielib.storageapi.settings.StorageSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class Language {

    private final YAMLContainer container;

    @Getter
    private final MiniMessage deserializer;

    public Language(TwitchMinecraft plugin) {
        this.container = new YAMLContainer(plugin.getStorageAPI(), new File(plugin.getDataFolder(), "lang.yml"), new StorageSettings().setUsesCache(false));
        this.deserializer = MiniMessage.miniMessage();
    }

    public Component getComponent(String path) {
        return this.getDeserializer().deserialize(this.getContainer().retrieve(String.class, path));
        //"Not linked to Twitch.\nVisit twitchmcsync.com and enter code: " + code
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        String message = this.getContainer().retrieve(String.class, path);

        for(Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return this.getDeserializer().deserialize(message);
    }

    public void sendListMessage(CommandSender player, String path) {
        this.sendMessage(player, path, Collections.emptyMap());
    }

    public void sendListMessage(CommandSender player, String path, Map<String, String> placeholders) {
        this.getContainer().retrieveList(String.class, path).forEach(message -> this.message(player, message, placeholders));
    }

    public void sendMessage(CommandSender player, String path) {
        this.sendMessage(player, path, Collections.emptyMap());
    }

    public void sendComponentMessage(CommandSender player, String path, Map<String, Component> placeholders) {
        this.messageComponent(player, this.getContainer().retrieve(path), placeholders);
    }

    public void sendMessage(CommandSender player, String path, Map<String, String> placeholders) {
        this.message(player, this.getContainer().retrieve(path), placeholders);
    }

    private void message(CommandSender player, String message, Map<String, String> placeholders) {
        for(String placeholder : placeholders.keySet()) {
            message = message.replace(placeholder, placeholders.get(placeholder));
        }
        if(message.equalsIgnoreCase("")) return;

        Component textMessage = this.getDeserializer().deserialize(message);
        player.sendMessage(textMessage);
    }

    private void messageComponent(CommandSender player, String message, Map<String, Component> placeholders) {
        Component component = this.getDeserializer().deserialize(message); // Base message with placeholder strings

        for (Map.Entry<String, Component> entry : placeholders.entrySet()) {
            component = component.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(entry.getKey())
                    .replacement(entry.getValue())
                    .build());
        }
        player.sendMessage(component);
    }


    public YAMLContainer getContainer() {
        return container;
    }
}
