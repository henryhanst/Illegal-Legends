package ee.taltech.examplegame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.screen.TitleScreen;
import ee.taltech.examplegame.util.AudioManager;
import ee.taltech.examplegame.util.SettingsManager;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class Main extends Game {
    private static Main instance;
    public static String savedPlayerName = "";
    public static int myID = -1;
    private AssetManager assetManager;
    private AudioManager audioManager;
    @Override
    public void create() {
        instance = this;
        // establish connection to the server
        ServerConnection.init(this);
        ServerConnection.getInstance().connect();
        SettingsManager.SettingsData settings = SettingsManager.load();
        SettingsManager.apply(settings);
        assetManager = new AssetManager();
        audioManager = new AudioManager(assetManager);
        audioManager.loadAssets();
        audioManager.setMasterVolume(settings.soundVolume / 100f);

        // display the title screen to the user
        setScreen(new TitleScreen(this));
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void setScreen(Screen screen) {
        super.setScreen(screen);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (audioManager != null) {
            audioManager.dispose();
        }
        if (assetManager != null) {
            assetManager.dispose();
        }
    }
}
