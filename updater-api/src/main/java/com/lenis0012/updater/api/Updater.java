package com.lenis0012.updater.api;

import org.bukkit.inventory.ItemStack;

public interface Updater {

    /**
     * Check whether or not a new update is available.
     *
     * @return True if outdated, false otherwise
     */
    boolean hasUpdate();

    /**
     * Get information about the latest version.
     *
     * @return Information
     */
    Version getNewVersion();

    /**
     * Get a book with all changelog details.
     *
     * @return Changelog
     */
    ItemStack getChangelog();

    /**
     * Download the latest version.
     *
     * @return Null if successful, error message otherwise
     */
    String downloadVersion();

    /**
     * Whether or not this is the bukkit updater.
     *
     * @return True if bukkit updater, false otherwise
     */
    boolean isBukkitUpdater();

    /**
     * Get the release channel this updater runs on.
     *
     * @return
     */
    ReleaseType getChannel();

    /**
     * Set the release channel this updater runs on.
     *
     * @param channel
     */
    void setChannel(ReleaseType channel);
}
