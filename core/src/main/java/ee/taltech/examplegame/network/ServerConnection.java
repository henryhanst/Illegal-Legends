package ee.taltech.examplegame.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.esotericsoftware.kryonet.Client;
import ee.taltech.examplegame.network.listener.LobbyListener;
import ee.taltech.examplegame.screen.ChampionSelectScreen;
import lombok.Getter;
import lombok.Setter;
import message.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static constant.Constants.*;
import static network.KryoHelper.registerClasses;

/**
 * Handles the connection to the server.
 * This class is a singleton, meaning that only one instance of this class can exist at a time.
 * More about singletons:
 * <a href="https://javadoc.pages.taltech.ee/design_patterns/creational_patterns.html#singel-singleton">...</a>
 */
@Getter
@Setter
public class ServerConnection {
    private static ServerConnection instance;
    private final Client client;
    private final LobbyListener lobbyListener;
    private final Map<Integer, Consumer<LobbyUpdateMessage>> pendingJoinCallbacks = new ConcurrentHashMap<>();
    private Consumer<LobbyUpdateMessage> lobbyUpdateListener;
    private Consumer<StartGameMessage> startGameListener;
    private Consumer<LobbyExitMessage> lobbyExitListener;
    private final Game game;

    private ServerConnection(Game game) {
        client = new Client();
        this.game = game;

        // register classes that are sent over the network
        registerClasses(client.getKryo());

        lobbyListener = new LobbyListener(this);
        client.addListener(lobbyListener);

        client.start();
    }
    // This method is called out once during the launch of the game
    public static void init(Game game) {
        if (instance == null) {
            instance = new ServerConnection(game);
        }
    }
    public static ServerConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ServerConnection not initialized");
        }
        return instance;
    }

    public void connect() {
        try {
            client.connect(5000, SERVER_IP, PORT_TCP, PORT_UDP);
        } catch (Exception e) {
            Gdx.app.error("ServerConnection", "Failed to connect to server", e);
        }
    }

    /**
     * Send join request and register a one-time callback.
     * Callback will be invoked when a LobbyUpdateMessage for this lobbyId arrives.
     */
    public void joinLobby(int lobbyId, String playerName, Consumer<LobbyUpdateMessage> callback) {
        pendingJoinCallbacks.put(lobbyId, callback);
        message.GameJoinMessage join = new message.GameJoinMessage(playerName, lobbyId);
        try {
            client.sendTCP(join);
        } catch (Exception e) {
            pendingJoinCallbacks.remove(lobbyId);
            Gdx.app.error("ServerConnection", "Failed to send join request", e);
        }
    }

    /**
     * Called by LobbyListener when LobbyUpdateMessage arrives.
     * First invokes pending join callback (if any), then active listener.
     */
    public void handleLobbyUpdate(LobbyUpdateMessage msg) {
        // If server sends empty list then the lobby is full
        if (msg.getPlayers() == null || msg.getPlayers().isEmpty()) {
            // Remove callback to release memory
            pendingJoinCallbacks.remove(msg.getLobbyId());
            Gdx.app.log("ServerConnection", "Lobby " + msg.getLobbyId() + " is full. Ignoring callback.");
            // Return so the callback.accept doesnt start
            return;
        }
        // If we reach here, the lobby has space.
        // We retrieve the one-time callback registered by the button click.
        Consumer<LobbyUpdateMessage> callback = pendingJoinCallbacks.remove(msg.getLobbyId());
        if (callback != null) {
            try {
                // This triggers the setScreen function inside ChooseLobbyScreen.
                callback.accept(msg);
            } catch (Exception e) {
                Gdx.app.error("ServerConnection", "Error in join callback", e);
            }
        } else if (lobbyUpdateListener != null) {
            try {
                // This part handles regular updates for players already inside the lobby.
                lobbyUpdateListener.accept(msg);
            } catch (Exception e) {
                Gdx.app.error("ServerConnection", "Error in lobby update listener", e);
            }
        }
    }

    /**
     * Called by LobbyListener when StartGameMessage arrives.
     */
    public void handleStartGame(StartGameMessage msg) {
        if (startGameListener != null) {
            try {
                startGameListener.accept(msg);
            } catch (Exception e) {
                Gdx.app.error("ServerConnection", "Error in start game listener", e);
            }
        }
    }

    /**
     * Called by LobbyListener when LobbyExitMessage arrives.
     */
    public void handleLobbyExit(LobbyExitMessage msg) {
        if (lobbyExitListener != null) {
            try {
                lobbyExitListener.accept(msg);
            } catch (Exception e) {
                Gdx.app.error("ServerConnection", "Error in lobby exit listener", e);
            }
        }
    }

    public void handleChampionSelectUpdate(ChampionSelectUpdate msg) {
        Gdx.app.postRunnable(() -> {
            if (!(game.getScreen() instanceof ChampionSelectScreen)) {
                game.setScreen(new ChampionSelectScreen(game, msg.getLobbyId(), msg.getPlayers()));
            }

            if (game.getScreen() instanceof ChampionSelectScreen screen) {
                screen.updatePlayers(msg.getPlayers());
            }
        });
    }

}
