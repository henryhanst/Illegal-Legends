package ee.taltech.examplegame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stores desktop settings and applies them through LibGDX.
 * This keeps persistence and window-management logic out of the screen class.
 */
public final class SettingsManager {
    private static final String PREFERENCES_NAME = "illegal-legends-settings";
    private static final String WIDTH_KEY = "resolutionWidth";
    private static final String HEIGHT_KEY = "resolutionHeight";
    private static final String WINDOW_MODE_KEY = "windowMode";
    private static final String SOUND_VOLUME_KEY = "soundVolume";

    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 768;
    private static final int DEFAULT_SOUND_VOLUME = 30;

    private SettingsManager() {
    }

    /**
     * Supported window modes exposed in the settings menu.
     */
    public enum WindowMode {
        WINDOWED("Windowed"),
        FULLSCREEN("Fullscreen");

        private final String label;

        WindowMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static WindowMode fromLabel(String label) {
            for (WindowMode value : values()) {
                if (value.label.equals(label)) {
                    return value;
                }
            }
            return WINDOWED;
        }
    }

    /**
     * Immutable value object representing one complete set of saved settings.
     */
    public static class SettingsData {
        public final int width;
        public final int height;
        public final WindowMode windowMode;
        public final int soundVolume;

        public SettingsData(int width, int height, WindowMode windowMode, int soundVolume) {
            this.width = width;
            this.height = height;
            this.windowMode = windowMode;
            this.soundVolume = soundVolume;
        }

        public String getResolutionLabel() {
            return width + "x" + height;
        }
    }

    public static SettingsData load() {
        Preferences preferences = getPreferences();
        int width = preferences.getInteger(WIDTH_KEY, DEFAULT_WIDTH);
        int height = preferences.getInteger(HEIGHT_KEY, DEFAULT_HEIGHT);
        String windowModeValue = preferences.getString(WINDOW_MODE_KEY, WindowMode.WINDOWED.name());
        int soundVolume = preferences.getInteger(SOUND_VOLUME_KEY, DEFAULT_SOUND_VOLUME);

        WindowMode windowMode;
        try {
            windowMode = WindowMode.valueOf(windowModeValue);
        } catch (IllegalArgumentException ignored) {
            windowMode = WindowMode.WINDOWED;
        }

        return new SettingsData(width, height, windowMode, soundVolume);
    }

    /**
     * Saves the selected settings so they can be restored on the next launch.
     */
    public static void save(SettingsData settingsData) {
        Preferences preferences = getPreferences();
        preferences.putInteger(WIDTH_KEY, settingsData.width);
        preferences.putInteger(HEIGHT_KEY, settingsData.height);
        preferences.putString(WINDOW_MODE_KEY, settingsData.windowMode.name());
        preferences.putInteger(SOUND_VOLUME_KEY, settingsData.soundVolume);
        preferences.flush();
    }

    /**
     * Applies the selected resolution and window mode to the active game window.
     */
    public static void apply(SettingsData settingsData) {
        if (settingsData.windowMode == WindowMode.FULLSCREEN) {
            Graphics.DisplayMode fullscreenMode = findDisplayMode(settingsData.width, settingsData.height);
            Gdx.graphics.setFullscreenMode(fullscreenMode);
            return;
        }

        Gdx.graphics.setWindowedMode(settingsData.width, settingsData.height);
    }

    /**
     * Builds the resolution dropdown list from the display modes reported by the current monitor.
     */
    public static Array<String> getAvailableResolutionLabels() {
        List<String> labels = new ArrayList<>();
        for (Graphics.DisplayMode displayMode : Gdx.graphics.getDisplayModes()) {
            String label = displayMode.width + "x" + displayMode.height;
            if (!labels.contains(label)) {
                labels.add(label);
            }
        }

        labels.sort(Comparator
                .comparingInt(SettingsManager::extractWidth)
                .thenComparingInt(SettingsManager::extractHeight));

        if (!labels.contains(DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT)) {
            labels.add(DEFAULT_WIDTH + "x" + DEFAULT_HEIGHT);
            labels.sort(Comparator
                    .comparingInt(SettingsManager::extractWidth)
                    .thenComparingInt(SettingsManager::extractHeight));
        }

        return Array.with(labels.toArray(new String[0]));
    }

    /**
     * Converts string selections from the UI into a typed settings object.
     */
    public static SettingsData fromSelections(String resolutionLabel, String windowModeLabel, int soundVolume) {
        String[] resolutionParts = resolutionLabel.split("x");
        int width = Integer.parseInt(resolutionParts[0]);
        int height = Integer.parseInt(resolutionParts[1]);
        WindowMode windowMode = WindowMode.fromLabel(windowModeLabel);
        return new SettingsData(width, height, windowMode, soundVolume);
    }

    private static Preferences getPreferences() {
        return Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    /**
     * Finds a fullscreen display mode matching the requested resolution.
     * If several exist, the one with the highest refresh rate is preferred.
     */
    private static Graphics.DisplayMode findDisplayMode(int width, int height) {
        Graphics.DisplayMode fallback = Gdx.graphics.getDisplayMode();
        Graphics.DisplayMode bestMatch = fallback;

        for (Graphics.DisplayMode displayMode : Gdx.graphics.getDisplayModes()) {
            if (displayMode.width == width && displayMode.height == height) {
                if (bestMatch == fallback || displayMode.refreshRate > bestMatch.refreshRate) {
                    bestMatch = displayMode;
                }
            }
        }

        return bestMatch;
    }

    private static int extractWidth(String resolutionLabel) {
        return Integer.parseInt(resolutionLabel.split("x")[0]);
    }

    private static int extractHeight(String resolutionLabel) {
        return Integer.parseInt(resolutionLabel.split("x")[1]);
    }
}
