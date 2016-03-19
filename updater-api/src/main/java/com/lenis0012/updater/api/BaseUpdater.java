package com.lenis0012.updater.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.logging.Level;

public abstract class BaseUpdater implements Updater {
    private static final long UPDATE_CACHE = TimeUnit.HOURS.toMillis(3); // Cache for 3 hours

    protected final Plugin plugin;
    protected final JsonParser jsonParser;
    protected final File pluginFile;
    protected String currentVersion;
    protected boolean enabled;
    protected String apiKey;

    protected Version newVersion = null;
    protected boolean isOutdated = false;
    protected long nextUpdateCheck = 0L;
    protected ItemStack changelog;
    protected ReleaseType channel;

    public BaseUpdater(Plugin plugin, File pluginFile) {
        this.plugin = plugin;
        this.jsonParser = new JsonParser();
        this.pluginFile = pluginFile;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    @Override
    public ReleaseType getChannel() {
        return channel;
    }

    @Override
    public void setChannel(ReleaseType channel) {
        this.channel = channel;
    }

    public boolean hasUpdate() {
        if(!enabled) return false;
        if(nextUpdateCheck < System.currentTimeMillis()) {
            this.nextUpdateCheck = System.currentTimeMillis() + UPDATE_CACHE;
            read();
        }

        return !isOutdated;
    }

    public Version getNewVersion() {
        return newVersion;
    }

    public ItemStack getChangelog() {
        return changelog;
    }

    public String downloadVersion() {
        if(newVersion == null) return "Updater is disabled, enable in config.";

        Bukkit.getUpdateFolderFile().mkdir();
        File destination = new File(Bukkit.getUpdateFolderFile(), pluginFile.getName());
        try {
            download(newVersion.getDownloadURL(), destination);
            this.currentVersion = newVersion.getName();
            return null;
        } catch(IOException e) {
            return e.getMessage();
        }
    }

    protected abstract void read();

    /**
     * Load settings from Gravity's Updater config.
     *
     * @param allSettings Whether you want to load all settings, or just the api key
     */
    protected void loadGravityFile(boolean allSettings) {
        File dir = new File(plugin.getDataFolder().getParentFile(), "Updater");
        File file = new File(dir, "config.yml");
        if(!file.exists()) {
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.load(file);
            String key = config.getString("api-key");
            if(key != null && !key.equals("PUT_API_KEY_HERE")) {
                this.apiKey = key;
            }

            if(allSettings) {
                this.enabled = !config.getBoolean("disable", false);
            }
        } catch(Exception e) {
            log(Level.WARNING, "Error while reading gravity updater config", e);
        }
    }

    /**
     * Read JSON object from a file inside of a jar archive.
     *
     * @param archive Jar archive
     * @param fileName Name of file inside
     * @return Json object
     */
    protected JsonObject readJsonFromJar(File archive, String fileName) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(archive);
            InputStream input = jarFile.getInputStream(jarFile.getEntry(fileName));
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder builder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }

            return jsonParser.parse(builder.toString()).getAsJsonObject();
        } catch(IOException e) {
            log(Level.WARNING, "Failed to read json from jar file", e);
            return null;
        } finally {
            if(jarFile != null) {
                try {
                    jarFile.close();
                } catch(IOException e) {}
            }
        }
    }

    /**
     * Read JSON from a url.
     *
     * @param downloadURL Url to parse
     * @param withApiKey Whether or not we will pass in our API key
     * @return Json object
     */
    protected JsonElement readJsonFromURL(String downloadURL, boolean withApiKey) {
        BufferedReader reader = null;
        try {
            URL url = new URL(downloadURL);
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            connection.addRequestProperty("User-Agent", getClass().getSimpleName() + "/v1 (by lenis0012)");
            if(apiKey != null && withApiKey) {
                connection.addRequestProperty("X-API-Key", apiKey);
            }

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }

            return jsonParser.parse(builder.toString());
        } catch(IOException e) {
            log(Level.WARNING, "Failed to read json from url " + downloadURL);
            return null;
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch(IOException e) {}
            }
        }
    }

    /**
     * Download file from a URL into destination.
     *
     * @param downloadURL URL to download from
     * @param destination to put file at
     * @throws IOException Error
     */
    protected void download(String downloadURL, File destination) throws IOException {
        log(Level.INFO, "Downloading file " + destination.getName() + "...");
        URL url = new URL(downloadURL);
        InputStream input = null;
        FileOutputStream output = null;
        try {
            input = url.openStream();
            output = new FileOutputStream(destination);
            byte[] buffer = new byte[1024];
            int length;
            while((length = input.read(buffer, 0, buffer.length)) != -1) {
                output.write(buffer, 0, length);
            }
            log(Level.INFO, "Download complete!");
        } finally {
            if(input != null) {
                try {
                    input.close();
                } catch(IOException e) {}
            }
            if(output != null) {
                try {
                    output.close();
                }catch(IOException e) {}
            }
        }
    }

    protected void log(Level level, String message) {
        plugin.getLogger().log(level, message);
    }

    protected void log(Level level, String message, Throwable error) {
        plugin.getLogger().log(level, message, error);
    }

    /**
     * Check if new version is greater than old version.
     * 1.1 > 1.0
     * 1.2.5 < 2.0
     *
     * @param oldVersion Old version
     * @param newVersion New version
     * @return newVersion > oldVersion
     */
    protected boolean compareVersions(String oldVersion, String newVersion) {
        int oldId = matchLength(oldVersion, newVersion);
        int newId = matchLength(newVersion, oldVersion);
        return newId > oldId;
    }

    private int matchLength(String a, String b) {
        a = a.replaceAll("[^0-9]", "");
        b = b.replaceAll("[^0-9]", "");
        while(a.length() < b.length()) {
            a += "0";
        }
        return Integer.parseInt(a);
    }
}
