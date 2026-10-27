package com.gtrxac.discord;

/**
 * Optional hooks for running Discord inside another MIDlet.
 *
 * A standalone build leaves this unset and keeps the normal Discord UI. An
 * embedded build can register a bridge to surface notifications outside the
 * Discord screens, save text into the host's notepad and open the host's
 * player from a Discord command.
 */
public interface HostBridge {
    /**
     * Shows a message notification outside the Discord UI.
     *
     * @return true if the host displayed the notification, false to let
     *         Discord show its own alert
     */
    boolean showNotification(String location, String author, String content,
        String guildID, String channelID);

    /** Saves text as a host note, returning true on success. */
    boolean saveNote(String text);

    /** Returns the localized label for the host's now-playing command. */
    String nowPlayingLabel();

    /** Opens the host's player screen. */
    void openNowPlaying();
}
