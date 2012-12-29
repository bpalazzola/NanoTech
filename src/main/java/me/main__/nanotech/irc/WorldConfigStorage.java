package me.main__.nanotech.irc;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldConfigStorage {
    private static final class CacheEntry {
        private final File file;
        private final FileConfiguration config;

        private CacheEntry(final File file, final FileConfiguration config) {
            this.file = file;
            this.config = config;
        }
    }

    private final File folder;
    private final Map<String, CacheEntry> configCache;

    public WorldConfigStorage(final File folder) {
        folder.mkdirs();
        this.folder = folder;
        this.configCache = new HashMap<String, CacheEntry>();
    }

    public ConfigurationSection getConfig(String world) {
        if (configCache.containsKey(world))
            return configCache.get(world).config;

        try {
            File file = new File(folder, world + ".yml");
            file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configCache.put(world, new CacheEntry(file, config));
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveAll() {
        for (CacheEntry e : configCache.values())
            try {
                e.config.save(e.file);
            } catch (IOException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }

    public void unload(String world) {
        CacheEntry entry = configCache.remove(world);
        try {
            entry.config.save(entry.file);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
