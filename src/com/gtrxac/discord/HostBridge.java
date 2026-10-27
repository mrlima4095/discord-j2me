package com.gtrxac.discord;

/**
 * Optional hooks for running Discord inside another MIDlet.
 *
 * A standalone build leaves this unset and keeps the normal Discord UI. An
 * embedded build can register a bridge to surface notifications outside the
 * Discord screens, save text into the host's notepad and show the host's
 * current track on Discord screens.
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

    /** Returns the host's current track label, or null when nothing is playing. */
    String nowPlaying();
}
