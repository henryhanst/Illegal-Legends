package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.component.LabelComponents;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.util.ScreenStyle;
import ee.taltech.examplegame.util.ViewportConfig;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;

public class ChooseLobbyScreen extends ScreenAdapter {
    private final Game game;
    private final Stage stage;
    private final int lobbyId1 = 1;
    private final int lobbyId2 = 2;
    private int pendingLobbyCode = 0;

    public ChooseLobbyScreen(Game game) {
        this.game = game;
        stage = ViewportConfig.createUiStage();
        stage.addActor(ScreenStyle.createScreenBackground());

        createUI();
    }

    @Override
    public void show() {
        // NB! important line - will make the stage listen for user input
        // For example when this is not set no hover or click events will be triggered
        Gdx.input.setInputProcessor(stage);
        ((ee.taltech.examplegame.Main) game).getAudioManager().playMenuMusic();
    }

    private void createUI() {
        Label titleLabel = LabelComponents.getLabel("SELECT LOBBY", 40);
        titleLabel.setColor(ScreenStyle.ACCENT_GOLD);

        var LobbyOneButton = getButton(20, "Lobby 1", () -> {
            Log.info("Clicked Lobby 1 button");
            tryJoinLobby(lobbyId1);
        });
        var LobbyTwoButton = getButton(20, "Lobby 2", () -> {
            Log.info("Clicked Lobby 2 button");
            tryJoinLobby(lobbyId2);
        });

        var exitButton = getButton(20, "Exit", () -> game.setScreen(new TitleScreen(game)));

        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.add(titleLabel).padBottom(60).row();
        table.add(LobbyOneButton).width(200).height(60).padBottom(20).row();
        table.add(LobbyTwoButton).width(200).height(60).padBottom(20).row();
        table.add(exitButton).width(200).height(60);
        stage.addActor(table);
    }

    private void tryJoinLobby(int lobbyId) {
        if (pendingLobbyCode != 0) return;
        pendingLobbyCode = lobbyId;
        // Get the saved name
        String name = ee.taltech.examplegame.Main.savedPlayerName;
        Log.info("Attempting to join lobby " + lobbyId + " as " + name);
        ServerConnection.getInstance().joinLobby(lobbyId, name, (message.LobbyUpdateMessage msg) -> {
            // It ONLY executes if ServerConnection calls callback.accept(msg).
            Gdx.app.postRunnable(() -> {
                // Only switch screens now because the server confirmed we are in the list.
                game.setScreen(new LobbyScreen(game, lobbyId, msg.getPlayers()));
                this.dispose();
            });
        });
        // Reset the code so the player can click the button again if the join failed.
        pendingLobbyCode = 0;
    }

    /**
     * Renders the ChooseLobbyScreen, clearing it and drawing the buttons.
     *
     * @param delta time since last frame.
     */
    @Override
    public void render(float delta) {
        super.render(delta);
        ScreenStyle.clearWindow();

        // draw the buttons
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        stage.dispose();
    }
}
