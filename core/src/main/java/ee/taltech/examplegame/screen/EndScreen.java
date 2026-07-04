package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
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
import message.dto.Team;

/**
 * EndScreen represents the game over screen shown when the game concludes.
 * It displays the game result (WIN/LOSS), allows the player to return to the menu,
 * and provides other post-game options.
 */
public class EndScreen extends ScreenAdapter {
    private final Stage stage;
    private final Game game;
    private final int lobbyId;
    private final boolean isWin;
    private final Team playerTeam;

    /**
     * Constructs the end screen.
     *
     * @param game the LibGDX Game instance used for screen transitions
     * @param lobbyId the ID of the lobby/game that just ended
     * @param isWin whether the player's team won
     * @param playerTeam the team the player belongs to
     */
    public EndScreen(Game game, int lobbyId, boolean isWin, Team playerTeam) {
        this.game = game;
        this.lobbyId = lobbyId;
        this.isWin = isWin;
        this.playerTeam = playerTeam;
        this.stage = ViewportConfig.createUiStage();
        setupUI();
    }

    /**
     * Sets up the UI elements for the end screen.
     * Displays game result, team info, and action buttons.
     */
    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ScreenStyle.createColoredDrawable(ScreenStyle.SCREEN_BACKGROUND_COLOR));

        // Result title - Victory or Defeat
        String resultText = isWin ? "VICTORY" : "DEFEAT";
        com.badlogic.gdx.graphics.Color resultColor = isWin ? ScreenStyle.ACCENT_GOLD : new com.badlogic.gdx.graphics.Color(200 / 255f, 50 / 255f, 50 / 255f, 1f);
        Label resultLabel = LabelComponents.getLabel(resultText, 72);
        resultLabel.setAlignment(Align.center);
        resultLabel.setColor(resultColor);

        // Team info
        String teamText = playerTeam == Team.TEAM_BLUE ? "TEAM BLUE" : "TEAM RED";
        Label teamLabel = LabelComponents.createLabel(teamText, playerTeam == Team.TEAM_BLUE ? ScreenStyle.TEAM_BLUE : ScreenStyle.TEAM_RED, 32);
        teamLabel.setAlignment(Align.center);

        // Result message
        String messageText = isWin ? "You have led your team to victory!" : "Your team has been defeated...";
        Label messageLabel = LabelComponents.getLabel(messageText, 20);
        messageLabel.setAlignment(Align.center);
        messageLabel.setColor(ScreenStyle.TEXT_LIGHT);

        // --- PLAY AGAIN BUTTON ---
        TextButton playAgainButton = ButtonComponents.getButton(24, "PLAY AGAIN", () -> {
            if (lobbyId > 0) {
                LobbyExitMessage exitMessage = new LobbyExitMessage();
                exitMessage.setLobbyId(lobbyId);
                ServerConnection.getInstance().getClient().sendTCP(exitMessage);
            }
            game.setScreen(new ChooseLobbyScreen(game));
        });

        // --- MAIN MENU BUTTON ---
        TextButton mainMenuButton = ButtonComponents.getButton(24, "MAIN MENU", () -> {
            if (lobbyId > 0) {
                LobbyExitMessage exitMessage = new LobbyExitMessage();
                exitMessage.setLobbyId(lobbyId);
                ServerConnection.getInstance().getClient().sendTCP(exitMessage);
            }
            game.setScreen(new TitleScreen(game));
        });

        // Center content table
        Table centerTable = new Table();
        centerTable.defaults().padBottom(40);

        centerTable.add(resultLabel).center().row();
        centerTable.padTop(20).add(teamLabel).center().row();
        centerTable.padTop(20).add(messageLabel).center().row();

        // Button section
        Table buttonTable = new Table();
        buttonTable.defaults().width(280).height(70).padBottom(20);
        buttonTable.add(playAgainButton).row();
        buttonTable.add(mainMenuButton).row();

        centerTable.add(buttonTable).padTop(60).row();

        // Add to root
        root.add(centerTable).center();

        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        ((Main) game).getAudioManager().playMenuMusic();
    }

    @Override
    public void render(float delta) {
        ScreenStyle.clearWindow();
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

