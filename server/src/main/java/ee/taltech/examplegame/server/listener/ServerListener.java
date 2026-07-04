package ee.taltech.examplegame.server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.lobby.ChampionSelectHandler;
import ee.taltech.examplegame.server.lobby.Lobby;
import ee.taltech.examplegame.server.lobby.LobbyManager;
import message.*;

/**
 * This class listens for all connections and messages that are
 * sent to the server by the clients.
 * <p>
 * It contains 3 methods that can be overridden to add custom logic
 */
public class ServerListener extends Listener {
    private GameInstance game;
    private final LobbyManager lobbyManager;
    private final ChampionSelectHandler championSelectHandler;

    public ServerListener() {
        this.lobbyManager = new LobbyManager(this);
        this.championSelectHandler = new ChampionSelectHandler();
    }

    /**
     * When a client connects to the server, this method is called.
     * Include any logic that should be executed when a client connects to the server.
     * ex - add the client to the game, etc.
     *
     * @param connection The connection object that is created when a client connects to the server.
     */
    @Override
    public void connected(Connection connection) {
        Log.info("Client connected: " + connection.getRemoteAddressTCP().getAddress().getHostAddress());

        super.connected(connection);
    }

    /**
     * When a client disconnects from the server, this method is called.
     * Include any logic that should be executed when a client disconnects from the server.
     * ex - clean up resources, remove the client from the game, etc.
     *
     * @param connection The connection object of the client that disconnected.
     */
    @Override
    public void disconnected(Connection connection) {
        Log.info("Client disconnected: " + connection.getID());
        Lobby lobby = lobbyManager.getLobbyByConnection(connection);
        if (lobby != null) {
            // If there is an active game in that lobby, remove the player from it
            if (lobby.getGameInstance() != null) {
                lobby.getGameInstance().removeConnection(connection);
            }
            // Remove them from the lobby list (so they don't show up in the UI)
            lobby.removePlayer(connection);
            // Clean up if the lobby is now empty
            if (lobby.isEmpty()) {
                lobbyManager.removeLobby(lobby.getLobbyId());
            }
        }
        super.disconnected(connection);
    }

    /**
     * When a message is received from a client, this method is called.
     * ex - client sends a JoinGame message, the server will add the client to the game.
     *
     * @param connection The connection object of the client that sent the message.
     * @param object     The object that is sent by the client.
     */
    @Override
    public void received(Connection connection, Object object) {
        Log.debug("Received message from client: " + object.toString());

        // Handle messages that DON'T require being in a lobby first (Joining)
        if (object instanceof GameJoinMessage joinMessage) {
            lobbyManager.handleJoinRequest(connection, joinMessage);
            return;
        }
        // Handle EXISTING PLAYERS (Player is already in a lobby)
        // For everything else, try to find the player's lobby
        Lobby lobby = lobbyManager.getLobbyByConnection(connection);
        if (lobby == null) return; // Safety check

        // Handle messages for players ALREADY in a lobby
        switch (object) {
            case TeamRequestMessage teamMsg -> {
                if (lobby != null) {
                    lobby.changePlayerTeam(connection, teamMsg.getRequestedTeam());
                }
            }
            case PlayerClassMessage playerClassMessage -> {
                if (lobby != null) {
                    championSelectHandler.handleChampionSelection(lobby, connection, playerClassMessage);
                }
            }
            case StartGameMessage startMsg -> {
                if (lobby != null) {
                    lobbyManager.handleStartGameRequest(connection, startMsg);
                }
            }
            case LobbyExitMessage exitMsg -> {
                lobbyManager.handleExitRequest(connection, exitMsg);
            }
            default -> {
            }
        }

        super.received(connection, object);
    }

    /**
     * Disposes of the current game instance by setting the game reference to null.
     */
    public void disposeGame() {
        this.game = null;
    }
}
