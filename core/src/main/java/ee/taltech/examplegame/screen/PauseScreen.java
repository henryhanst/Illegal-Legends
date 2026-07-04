package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.component.ButtonComponents;
import ee.taltech.examplegame.component.LabelComponents;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.util.ScreenStyle;
import ee.taltech.examplegame.util.ViewportConfig;
import message.LobbyExitMessage;

/**
 * PauseScreen represents the pause menu shown when the player presses ESC during gameplay.
 * It allows the player to continue playing, access settings, or exit the game.
 * The pause menu can be dismissed by pressing ESC again or clicking the "Continue" button.
 */
public class PauseScreen extends ScreenAdapter {
    private final Stage stage;
    private final Game game;
    private final GameScreen gameScreen;
    private final int lobbyId;

    /**
     * Constructs the pause screen.
     *
     * @param game the LibGDX Game instance used for screen transitions
     * @param gameScreen the GameScreen instance to return to when continuing
     * @param lobbyId the ID of the current lobby/game
     */
    public PauseScreen(Game game, GameScreen gameScreen, int lobbyId) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.lobbyId = lobbyId;
        this.stage = ViewportConfig.createUiStage();
        setupUI();
    }

    /**
     * Sets up the UI elements for the pause menu.
     * Creates the title, buttons (Continue, Settings, Exit Game), and arranges them vertically in the center.
     */
    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ScreenStyle.createColoredDrawable(ScreenStyle.SCREEN_BACKGROUND_COLOR));

        // Main title
        Label titleLabel = LabelComponents.getLabel("IN GAME MENU", 56);
        titleLabel.setAlignment(Align.center);
        titleLabel.setColor(ScreenStyle.ACCENT_GOLD);

        // --- CONTINUE BUTTON ---
        TextButton continueButton = ButtonComponents.getButton(24, "RESUME", () -> {
            game.setScreen(gameScreen);
        });

        // --- SETTINGS BUTTON ---
        TextButton settingsButton = ButtonComponents.getButton(24, "SETTINGS", () -> {
            game.setScreen(new SettingsScreen(game, this));
        });

        // --- EXIT GAME BUTTON ---
        TextButton exitGameButton = ButtonComponents.getButton(24, "LEAVE MATCH", () -> {
            // Send lobby exit message to server
            if (lobbyId > 0) {
                LobbyExitMessage exitMessage = new LobbyExitMessage();
                exitMessage.setLobbyId(lobbyId);
                ServerConnection.getInstance().getClient().sendTCP(exitMessage);
            }
            game.setScreen(new TitleScreen(game));
        });

        // Center content table with buttons
        Table centerTable = new Table();
        centerTable.defaults().width(280).height(70).padBottom(30);

        centerTable.add(titleLabel).padBottom(60).row();
        centerTable.add(continueButton).row();
        centerTable.add(settingsButton).row();
        centerTable.add(exitGameButton).row();

        // Add center table to root with expansion
        root.add(centerTable).center();

        stage.addActor(root);
    }

    /**
     * Handles keyboard input for the pause menu.
     * Pressing ESC will resume the game by returning to the GameScreen.
     */
    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(gameScreen);
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        ((Main) game).getAudioManager().playMenuMusic();
    }

    @Override
    public void render(float delta) {
        ScreenStyle.clearWindow();
        handleInput();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
