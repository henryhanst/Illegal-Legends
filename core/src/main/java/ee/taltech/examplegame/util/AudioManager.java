package ee.taltech.examplegame.util;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static final String MENU_MUSIC = "sounds/Music/MenuMusic.mp3";
    private static final String CHAMP_SELECT_MUSIC = "sounds/Music/ChampSelect.mp3";
    private static final String DEFAULT_SOUND_EFFECT = "sounds/UserInterface/click.ogg";
    private static final String TURRET_DESTROYED_SOUND = "sounds/Announcer/war_target_destroyed.ogg";
    private static final String GAME_OVER_SOUND = "sounds/Announcer/game_over_female.ogg";

    // Abilities
    private static final String HEAL_SOUND = "sounds/Abilities/heal.mp3";
    private static final String FLASH_SOUND = "sounds/Abilities/flash.mp3";
    private static final String SKILLSHOT_SOUND = "sounds/Abilities/skillshot.mp3";
    private static final String STUN_SOUND = "sounds/Abilities/stun.mp3";
    private static final String RAGE_SOUND = "sounds/Abilities/rage.mp3";
    private static final String EMPOWERED_SOUND = "sounds/Abilities/empowered.mp3";

    private static final String[] MUSIC_FILES = {
        MENU_MUSIC,
        CHAMP_SELECT_MUSIC,
    };

    private static final String[] SOUND_FILES = {
        FLASH_SOUND,
        HEAL_SOUND,
        SKILLSHOT_SOUND,
        STUN_SOUND,
            RAGE_SOUND,
            EMPOWERED_SOUND,
        GAME_OVER_SOUND,
        TURRET_DESTROYED_SOUND,
        "sounds/Attack/hammer.mp3",
        "sounds/Attack/ranged.mp3",
        "sounds/Attack/slash.mp3",
        DEFAULT_SOUND_EFFECT
    };

    private final AssetManager assetManager;
    private final Map<String, Music> musicTracks = new HashMap<>();
    private final Map<String, Sound> soundEffects = new HashMap<>();

    private Music backgroundMusic;
    private float musicVolume = 1.0f;
    @Setter
    private float soundVolume = 1.0f;

    public AudioManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /** Loads audio assets and caches the default tracks. */
    public void loadAssets() {
        loadAll(MUSIC_FILES, Music.class);
        loadAll(SOUND_FILES, Sound.class);
        assetManager.finishLoading();

        cacheAssets(MUSIC_FILES, Music.class, musicTracks);
        cacheAssets(SOUND_FILES, Sound.class, soundEffects);

        backgroundMusic = musicTracks.get(MENU_MUSIC);
    }

    public void playBackgroundMusic() {
        if (backgroundMusic == null || backgroundMusic.isPlaying()) {
            return;
        }
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(musicVolume);
        backgroundMusic.play();
    }

    public void playMenuMusic() {
        playMusic(MENU_MUSIC);
    }

    public void playChampionSelectMusic() {
        playMusic(CHAMP_SELECT_MUSIC);
    }

    public void stopBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.pause();
        }
    }

    /** Plays the default UI click sound. */
    public void playSoundEffect() {
        playSound(DEFAULT_SOUND_EFFECT);
    }

    /**
     * Plays a specific sound effect based on the ability name sent by the server.
     */
    public void playAbilitySound(String abilityName) {
        if (abilityName == null) return;

        String soundFile;
        switch (abilityName) {
            case "SkillshotAbility":
                soundFile = SKILLSHOT_SOUND;
                break;
            case "HealAbility":
                soundFile = HEAL_SOUND;
                break;
            case "FlashAbility":
                soundFile = FLASH_SOUND;
                break;
            case "RageAbility":
                soundFile = RAGE_SOUND;
                break;
            case "StunAbility":
                soundFile = STUN_SOUND;
                break;
            case "EmpoweredAbility":
                soundFile = EMPOWERED_SOUND;
                break;
            default:
                soundFile = DEFAULT_SOUND_EFFECT;
                break;
        }

        playSound(soundFile);
    }

    public void playTurretDestroyedSound() {
        playSound(TURRET_DESTROYED_SOUND);
    }

    public void playGameOverSound() {
        playSound(GAME_OVER_SOUND);
    }

    public void setMusicVolume(float volume) {
        musicVolume = volume;
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
    }

    /** Applies one volume value to both music and sound effects. */
    public void setMasterVolume(float volume) {
        setMusicVolume(volume);
        setSoundVolume(volume);
    }

    /** Unloads every audio asset managed by this class. */
    public void dispose() {
        unloadAll(MUSIC_FILES);
        unloadAll(SOUND_FILES);
        musicTracks.clear();
        soundEffects.clear();
    }

    private <T> void loadAll(String[] files, Class<T> type) {
        for (String file : files) {
            assetManager.load(file, type);
        }
    }

    private <T> void cacheAssets(String[] files, Class<T> type, Map<String, T> target) {
        for (String file : files) {
            target.put(file, assetManager.get(file, type));
        }
    }

    private void unloadAll(String[] files) {
        for (String file : files) {
            if (assetManager.isLoaded(file)) {
                assetManager.unload(file);
            }
        }
    }

    private void playMusic(String musicFile) {
        Music nextTrack = musicTracks.get(musicFile);
        if (nextTrack == null) {
            return;
        }
        if (backgroundMusic != null && backgroundMusic != nextTrack) {
            backgroundMusic.stop();
        }
        backgroundMusic = nextTrack;
        playBackgroundMusic();
    }

    private void playSound(String soundFile) {
        Sound sound = soundEffects.get(soundFile);
        if (sound != null) {
            sound.play(soundVolume);
        }
    }
}
