package com.lenis0012.updater.api;

import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class UpdaterFactory {
    private static final String PACKAGE_BASE = "com.lenis0012.updater";
    private static final String PACKAGE_BUKKIT = ".bukkit.BukkitUpdater";
    private static final String PACKAGE_SPIGOT = ".spigot.SpigotUpdater";

    private final Plugin plugin;
    private String updaterInfo;
    private Class<? extends Updater> updaterClass;

    public UpdaterFactory(Plugin plugin) {
        this.plugin = plugin;
        List<String> platforms = Arrays.asList(PACKAGE_BUKKIT, PACKAGE_SPIGOT);
        for(String platform : platforms) {
            this.updaterClass = (Class<? extends Updater>) classExists(PACKAGE_BASE + platform);
            if(updaterClass != null) {
                break;
            }
        }

        if(updaterClass == null) {
            plugin.getLogger().log(Level.WARNING, "No compatible updater was founds for your platform!");
            plugin.getLogger().log(Level.INFO, "Plugin will not check for updates");
            return;
        }

        InputStream input = plugin.getResource("updater.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input));
            this.updaterInfo = reader.readLine();
        } catch(IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read updater info from updater.txt");
            plugin.getLogger().log(Level.INFO, "Plugin will not check for updates");
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch(IOException e1) {}
            }
        }
    }

    private Class<?> classExists(String path) {
        try {
            return Class.forName(path);
        } catch(Exception e) {
           return null;
        }
    }

    public Updater newUpdater(File pluginFile, boolean enabled) {
        if(updaterClass == null || updaterInfo == null) return null;
        try {
            return updaterClass.getConstructor(Plugin.class, File.class, String.class, boolean.class).newInstance(plugin, pluginFile, updaterInfo, enabled);
        } catch(Exception e) {
            plugin.getLogger().log(Level.WARNING, "Couldn't initiate updater", e);
            return null;
        }
    }
}
