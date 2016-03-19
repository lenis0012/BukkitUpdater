package com.lenis0012.updater.spigot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lenis0012.updater.api.BaseUpdater;
import com.lenis0012.updater.api.ReleaseType;
import com.lenis0012.updater.api.Version;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;

public class SpigotUpdater extends BaseUpdater {
    private final String versionURL;

    public SpigotUpdater(Plugin plugin, File pluginFile, String versionURL, boolean enabled) {
        super(plugin, pluginFile);
        this.enabled = enabled;
        this.versionURL = versionURL;
        loadGravityFile(false);
    }

    @Override
    protected void read() {
        JsonElement json = readJsonFromURL(versionURL, false);
        if(json == null) {
            // Not present (or other error)
            return;
        }

        JsonObject version = json.getAsJsonObject();
        if(version.has("release")) {
            // Uses new format
            version = readChannel(version);
        }

        String name = version.get("name").getAsString();
        ReleaseType type = ReleaseType.valueOf(version.get("type").getAsString().toUpperCase());
        String serverVersion = version.get("gameVersion").getAsString();
        String downloadURL = version.get("downloadURL").getAsString();
        this.newVersion = new Version(name, type, serverVersion, downloadURL);
        this.isOutdated = !compareVersions(currentVersion, name);
    }

    @Override
    public boolean isBukkitUpdater() {
        return false;
    }

    @Override
    public String downloadVersion() {
        String result = super.downloadVersion();
        if(result == null) {
            readChangelog(new File(Bukkit.getUpdateFolderFile(), pluginFile.getName()));
        }
        return result;
    }

    /**
     * Read latest version in channel.
     *
     * @param versions versions data
     * @return Latest version in channel
     */
    private JsonObject readChannel(JsonObject versions) {
        JsonObject latest = null;
        String newest = null;
        for(int i = ReleaseType.RELEASE.ordinal(); i >= channel.ordinal(); i--) {
            String base = ReleaseType.values()[i].toString().toLowerCase();
            if(!versions.has(base)) continue; // Version type not present.
            JsonObject version = versions.get(base).getAsJsonObject();
            String name = version.get("name").getAsString();
            if(latest == null || compareVersions(newest, name)) {
                latest = version;
                newest = name;
            }
        }

        return latest;
    }


    /**
     * Read changelog from file inside of jar called changelog.json.
     *
     * @param file Jar File to read from
     */
    private void readChangelog(File file) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setAuthor("lenis0012");
        meta.setTitle(currentVersion + " Changelog");
        JsonObject json = readJsonFromJar(file, "changelog.json");
        if(json == null || !json.get("version").getAsString().equalsIgnoreCase(newVersion.getName())) {
            // Changelog outdated, don't show
            return;
        }

        JsonArray pages = json.get("data").getAsJsonArray();
        for(int i = 0; i < pages.size(); i++) {
            JsonArray lines = pages.get(i).getAsJsonArray();
            StringBuilder page = new StringBuilder();
            for(int j = 0; j < lines.size(); j++) {
                page.append(lines.get(j).getAsString()).append('\n');
            }
            page.setLength(page.length() - 1);
            meta.addPage(page.toString());
        }
        book.setItemMeta(meta);
        this.changelog = book;
    }
}
