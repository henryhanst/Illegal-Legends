package ee.taltech.examplegame.server.lobby;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.listener.ServerListener;
import message.GameJoinMessage;
import message.LobbyExitMessage;
import message.StartGameMessage;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * LobbyManager handles all lobbies on the server.
 * It manages lobby creation, destruction, and player routing.
 */
@Data
public class LobbyManager {
    private final Map<Integer, Lobby> lobbies = new HashMap<>();
    private final ServerListener serverListener;

    public LobbyManager(ServerListener serverListener) {
        this.serverListener = serverListener;
    }

    /**
     * Handles a player's request to join a lobby
     */
    public void handleJoinRequest(Connection connection, GameJoinMessage joinMessage) {
        int lobbyId = joinMessage.getLobbyId();
        String name = joinMessage.getPlayerName();
        if (lobbyId <= 0) {
            Log.warn("Invalid lobby ID: " + lobbyId);
            return;
        }

        // Remove player from any existing lobby first
        removePlayerFromAllLobbies(connection);

        // Get or create lobby
        Lobby lobby = lobbies.computeIfAbsent(lobbyId, id -> new Lobby(id, serverListener, this));

        // Try to add player
        boolean success = lobby.addPlayer(connection, name);

        if (!success) {
            Log.info("Failed to add player to lobby: " + lobbyId);
        }
    }

    /**
     * Handles a player's request to start a game
     */
    public void handleStartGameRequest(Connection connection, StartGameMessage startMessage) {
        int lobbyId = startMessage.getLobbyId();
        Lobby lobby = lobbies.get(lobbyId);

        if (lobby != null && lobby.hasPlayer(connection)) {
            if (lobby.getGameInstance() == null) {
                if (lobby.canStartChampionSelect()) {
                    lobby.notifyChampionSelect();
                } else {
                    Log.info("Cannot start game for lobby " + lobbyId + " because not all players have joined a team or champion select already started.");
                }
            } else {
                Log.info("Game already running in lobby: " + lobbyId + ". Ignoring start request.");
            }
        } else {
            Log.warn("Player attempted to start game in lobby they're not in: " + lobbyId);
        }
    }

    /**
     * Handles a player's request to exit a lobby
     */
    public void handleExitRequest(Connection connection, LobbyExitMessage exitMessage) {
        int lobbyId = exitMessage.getLobbyId();
        Lobby lobby = lobbies.get(lobbyId);

        if (lobby != null) {
            // Remove player from game if it exists
            if (lobby.getGameInstance() != null) {
                lobby.getGameInstance().removeConnection(connection);
            }

            // Remove player from lobby
            lobby.removePlayer(connection);
            // Send exit confirmation to the leaving player
            LobbyExitMessage confirmationMessage = new LobbyExitMessage();
            confirmationMessage.setLobbyId(lobbyId);
            connection.sendTCP(confirmationMessage);

            // Clean up empty lobby
            if (lobby.isEmpty()) {
                lobbies.remove(lobbyId);
                Log.info("Lobby " + lobbyId + " removed (empty)");
            }
        }
    }

    /**
     * Removes a player from all lobbies (used for cleanup when player disconnects)
     */
    public void removePlayerFromAllLobbies(Connection connection) {
        for (Map.Entry<Integer, Lobby> entry : lobbies.entrySet()) {
            int lobbyId = entry.getKey();
            Lobby lobby = entry.getValue();

            if (lobby.removePlayer(connection)) {
                // Player was in this lobby
                Log.info("Removed disconnected player from lobby: " + lobbyId);

                // Remove from game instance if it exists
                if (lobby.getGameInstance() != null) {
                    lobby.getGameInstance().removeConnection(connection);
                }

                // Clean up empty lobby
                if (lobby.isEmpty()) {
                    lobbies.remove(lobbyId);
                    Log.info("Lobby " + lobbyId + " removed (empty)");
                }
                break; // Player can only be in one lobby
            }
        }
    }

    /**
     * Searches all active lobbies to find the one containing the specified connection.
     * * @param connection The player's connection to search for
     * @return The Lobby containing the player, or null if not found
     */
    public Lobby getLobbyByConnection(Connection connection) {
        for (Lobby lobby : lobbies.values()) {
            if (lobby.hasPlayer(connection)) {
                return lobby;
            }
        }
        return null;
    }

    /**
     * Gets a lobby by ID
     */
    public Lobby getLobby(int lobbyId) {
        return lobbies.get(lobbyId);
    }

    public void removeLobby(int lobbyId) {
        lobbies.remove(lobbyId);
        Log.info("Lobby " + lobbyId + " has been closed (all players disconnected).");
    }
}
