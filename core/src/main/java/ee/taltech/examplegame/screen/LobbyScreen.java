package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.component.LabelComponents;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.util.ScreenStyle;
import ee.taltech.examplegame.util.ViewportConfig;
import message.*;
import message.dto.Team;


import java.util.List;

import static constant.Constants.LOBBY_LIMIT;
import static ee.taltech.examplegame.component.ButtonComponents.getButton;
import static ee.taltech.examplegame.component.LabelComponents.createLabel;

/**
 * LobbyScreen represents the lobby where players wait for others to join before starting the game.
 * It displays the current player count and provides options to leave or start the game.
 */
public class LobbyScreen extends ScreenAdapter {
    private final Stage stage;
    private final Game game;
    private final int lobbyId;
    private List<PlayerLobbyInfo> playerList;
    private Label playerCountLabel;
    private int currentPlayerCount;
    private Table teamTable;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField nameField;

    public LobbyScreen(Game game, int lobbyId, List<PlayerLobbyInfo> initialPlayers) {
        this.game = game;
        this.lobbyId = lobbyId;
        this.playerList = initialPlayers;
        stage = ViewportConfig.createUiStage();
        stage.addActor(ScreenStyle.createScreenBackground());

        // Set up input processor
        Gdx.input.setInputProcessor(stage);

        // Create UI
        createUI();
    }

    private void createUI() {
        // Create labels
        Label lobbyCodeLabel = createLabel("Lobby: " + lobbyId, ScreenStyle.ACCENT_GOLD, 40);
        nameField = ee.taltech.examplegame.component.TextFieldComponents.getTextField(Main.savedPlayerName, 20);
        nameField.setMessageText("Enter Name...");
        // Add a listener so every time the name changes, it updates the server
        nameField.setTextFieldListener((textField, c) -> {
            String newName = textField.getText().trim();
            if (!newName.isEmpty()) {
                Main.savedPlayerName = newName; // Update Main
                // Send a message to the server so everyone else sees your new name
                ServerConnection.getInstance().getClient().sendTCP(new GameJoinMessage(newName, lobbyId));
            }
        });
        // Create buttons
        // Start game button
        var startGameButton = getButton(20, "Start Game", () -> {
            // Send start game message to server
            StartGameMessage startMessage = new StartGameMessage();
            startMessage.setLobbyId(lobbyId);
            ServerConnection.getInstance().getClient().sendTCP(startMessage);
        });
        // Leave lobby button
        var leaveLobbyButton = getButton(20, "Leave Lobby", () -> {
            // Send leave message to server
            LobbyExitMessage exitMessage = new LobbyExitMessage();
            exitMessage.setLobbyId(lobbyId);
            ServerConnection.getInstance().getClient().sendTCP(exitMessage);
        });

        // positioning the buttons. you can think of the following as a table (or flexbox) in HTML
        teamTable = new Table();
        var joinA = getButton(15, "Join Team Blue", () -> sendTeamRequest(Team.TEAM_BLUE));
        var joinB = getButton(15, "Join Team Red", () -> sendTeamRequest(Team.TEAM_RED));

        var table = new Table();
        table.setFillParent(true);

        // THE HUD (Lobby, Username)
        // Left spacer to balance the name field
        table.add().width(150);

        // Lobby Label in the exact middle
        table.add(lobbyCodeLabel).expandX().top().padTop(20).center();

        // Name Field pinned to the right
        table.add(nameField).width(150).height(40).top().right().pad(20);
        table.row();

        // This invisible space pushes the content block to the center
        table.add().colspan(3).expandY();
        table.row();

        // Centered content block (Player count, join buttons, start game, leave lobby)
        Table contentTable = new Table();

        playerCountLabel = createLabel("Players: 0/" + LOBBY_LIMIT, ScreenStyle.TEXT_LIGHT, 25);
        contentTable.add(playerCountLabel).padBottom(30).row();
        contentTable.add(teamTable).padBottom(20).row();

        // Join buttons side-by-side
        Table buttonTable = new Table();
        buttonTable.add(joinA).padRight(70);
        buttonTable.add(joinB).padLeft(70);
        contentTable.add(buttonTable).row();

        contentTable.add(startGameButton).padTop(80).padBottom(15).row();
        contentTable.add(leaveLobbyButton).row();

        // Add the finished block to the main table
        table.add(contentTable).colspan(3).center();
        table.row();

        table.add().colspan(3).expandY();

        stage.addActor(table);

        refreshTeamDisplay();

    }
    private void sendTeamRequest(Team team) {
        TeamRequestMessage msg = new TeamRequestMessage();
        msg.setRequestedTeam(team);
        ServerConnection.getInstance().getClient().sendTCP(msg);
    }

    /**
     * This method is displays team sizes without the UI(in the createUI).
     * It is the helper method.
     */
    private void refreshTeamDisplay() {
        if (teamTable == null || playerList == null) return;
        teamTable.clear();
        Table teamBlueColumn = new Table();
        Table teamRedColumn = new Table();

        // Calculate current sizes
        int teamBlueSize = (int) playerList.stream().filter(p -> p.getTeam() == Team.TEAM_BLUE).count();
        int teamRedSize = (int) playerList.stream().filter(p -> p.getTeam() == Team.TEAM_RED).count();
        // Display counts in the header: "TEAM A (1/3)"
        teamBlueColumn.add(createLabel("TEAM Blue (" + teamBlueSize + "/3)", ScreenStyle.TEAM_BLUE, 18)).padBottom(10).row();
        teamRedColumn.add(createLabel("TEAM Red (" + teamRedSize + "/3)", ScreenStyle.TEAM_RED, 18)).padBottom(10).row();
        for (PlayerLobbyInfo p : playerList) {
            // Use the name from the player info, or a placeholder if null
            String displayName = (p.getPlayerName() != null && !p.getPlayerName().isEmpty())
                    ? p.getPlayerName()
                    : "Joining...";
            if (p.getTeam() == Team.TEAM_BLUE) {
                teamBlueColumn.add(LabelComponents.getLabel(displayName, 16)).padBottom(5).row();
            } else if (p.getTeam() == Team.TEAM_RED) {
                teamRedColumn.add(LabelComponents.getLabel(displayName, 16)).padBottom(5).row();
            } else {
                Log.info(displayName + " has not chosen a team yet.");
            }
        }

        teamTable.add(teamBlueColumn).uniform().top().padRight(240);
        teamTable.add(teamRedColumn).uniform().top();
    }

    /**
     * Setting up listeners for lobby updates, game start, and lobby exit events.
     * They register/unregister the UI callbacks when the screen becomes active/inactive.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        ((Main) game).getAudioManager().playMenuMusic();
        ServerConnection connection = ServerConnection.getInstance();

        connection.setLobbyUpdateListener(msg -> {
            if (msg.getLobbyId() == lobbyId) {
                Gdx.app.postRunnable(() -> {
                    updatePlayerCount(msg.getPlayers());
                });
            }
        });

        connection.setStartGameListener(msg -> {
            if (msg.getLobbyId() == lobbyId) {
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new GameScreen(game, lobbyId, playerList));
                    this.dispose();
                });
            }
        });

        connection.setLobbyExitListener(msg -> {
            if (msg.getLobbyId() == lobbyId) {
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new ChooseLobbyScreen(game));
                    this.dispose();
                });
            }
        });
    }

    @Override
    public void hide() {
        ServerConnection.getInstance().setLobbyUpdateListener(null);
        ServerConnection.getInstance().setStartGameListener(null);
        ServerConnection.getInstance().setLobbyExitListener(null);
    }

    /**
     * Updating the player count in the lobby.
     *
     * @param players how many players
     */
    public void updatePlayerCount(List<PlayerLobbyInfo> players) {
        this.playerList = players;

        if (playerCountLabel != null) {

            long selectedPlayers = playerList.stream()
                    .filter(p -> p.getTeam() == Team.TEAM_BLUE || p.getTeam() == Team.TEAM_RED)
                    .count();

            playerCountLabel.setText("Players: " + selectedPlayers + "/" + LOBBY_LIMIT);
        }

        refreshTeamDisplay();
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        ScreenStyle.clearWindow();

        // Update and draw stage
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
