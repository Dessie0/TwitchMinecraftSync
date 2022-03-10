package com.twitchmcsync.twitchminecraft.lang;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Language {
    private File langFile;
    private FileConfiguration langConfig;

    public Language() {
        this.langFile = new File(TwitchMinecraft.getInstance().getDataFolder() + "/lang.yml");
    }

    public File getLangFile() { return langFile; }
    public FileConfiguration getLangConfig() { return langConfig; }

    public void loadConfig() {
        this.langConfig = YamlConfiguration.loadConfiguration(this.getLangFile());
    }
}
