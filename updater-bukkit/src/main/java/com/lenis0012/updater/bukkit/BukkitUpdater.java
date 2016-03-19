package com.lenis0012.updater.bukkit;

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
import java.io.IOException;
import java.util.logging.Level;

public class BukkitUpdater extends BaseUpdater {
    private static final String BASE_URL = "https://api.curseforge.com";
    private static final String API_FILES = "/servermods/files?projectIds=";
    private static final String API_SEARCH = "/servermods/projects?search=";
    private int projectId = -1;

    public BukkitUpdater(Plugin plugin, File pluginFile, final String projectId, boolean enabled) {
        super(plugin, pluginFile);
        this.enabled = enabled;
        if(projectId.startsWith("slug:")) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    readSlug(projectId.substring("slug:".length()));
                }
            });
        } else {
            this.projectId = Integer.parseInt(projectId);
        }

        loadGravityFile(false);
    }

    @Override
    protected void read() {
        if(projectId < 0) {
            return; // Still reading slug...
        }

        JsonElement json = readJsonFromURL(BASE_URL + API_FILES + projectId, true);
        if(json == null) {
            return;
        }

        // Read latest file in channel
        JsonArray files = json.getAsJsonArray();
        JsonObject latest = null;
        ReleaseType type = null;
        for(int i = files.size() - 1; i >= 0; i--) {
            JsonObject file = files.get(i).getAsJsonObject();
            type = ReleaseType.valueOf(file.get("releaseType").getAsString().toUpperCase());
            if(type.ordinal() >= channel.ordinal()) {
                latest = file;
                break;
            }
        }
        if(latest == null) {
            // No version was found in channel
            return;
        }

        // Parse
        String name = latest.get("name").getAsString();
        String serverVersion = latest.get("gameVersion").getAsString();
        String downloadURL = latest.get("downloadUrl").getAsString();
        this.newVersion = new Version(name, type, serverVersion, downloadURL);
        this.isOutdated = !compareVersions(currentVersion, name);
    }

    @Override
    public String downloadVersion() {
        String result = super.downloadVersion();
        if(result == null) {
            readChangelog(new File(Bukkit.getUpdateFolderFile(), pluginFile.getName()));
        }
        return result;
    }

    @Override
    public boolean isBukkitUpdater() {
        return true;
    }

    private void readSlug(String slug) {
        JsonArray projects = readJsonFromURL(BASE_URL + API_SEARCH + slug, true).getAsJsonArray();
        if(projects.size() < 1) {
            return;
        }

        JsonObject project = projects.get(0).getAsJsonObject();
        this.projectId = project.get("id").getAsInt();
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
