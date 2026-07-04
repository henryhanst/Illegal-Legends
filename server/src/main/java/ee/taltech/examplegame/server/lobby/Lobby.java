package ee.taltech.examplegame.server.lobby;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.listener.ServerListener;
import message.*;
import lombok.Data;
import message.dto.ChampionType;
import message.dto.Team;

import java.util.*;

import static constant.Constants.LOBBY_LIMIT;

@Data

/**
 * Represents a lobby where players can gather before starting a game.
 *
 * A Lobby manages a group of connected players and coordinates game initialization.
 * Lobbies handle player operations, broadcast updates to all players in the lobby, and
 * manage the transition from lobby state to active game state.
 *
 * Key responsibilities:
 * - Adding and removing players from the lobby
 * - Notifying all players when the lobby composition changes
 * - Starting a game instance when requested
 * - Cleaning up resources when the game ends
 *
 */
public class Lobby {
    private final ServerListener serverListener;
    private final LobbyManager lobbyManager;
    private String playerName;
    private final int lobbyId;
    private final Map<Connection, PlayerLobbyInfo> playerMap = new HashMap<>();
    private GameInstance gameInstance;
    private boolean championSelectStarted = false;

    public Lobby(int lobbyId, ServerListener serverListener, LobbyManager lobbyManager) {
        this.lobbyId = lobbyId;
        this.serverListener = serverListener;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Attempts to add a player to this lobby
     *
     * @param connection The player's connection
     * @param playerName The player's chosen name
     * @return true if player was added, false if lobby is full
     */
    public boolean addPlayer(Connection connection, String playerName) {
        if (championSelectStarted || playerMap.size() >= LOBBY_LIMIT) {
            LobbyUpdateMessage rejectionMessage = new LobbyUpdateMessage();
            rejectionMessage.setLobbyId(lobbyId);
            rejectionMessage.setPlayers(new ArrayList<>());
            connection.sendTCP(rejectionMessage);
            Log.info("Lobby " + lobbyId + " is unavailable. Rejected player.");
            return false;
        }
        PlayerLobbyInfo info = new PlayerLobbyInfo();
        info.setConnectionId(connection.getID());
        info.setPlayerName(playerName);
        info.setTeam(Team.NONE);
        info.setChampionType(ChampionType.NONE);
        info.setLockedIn(false);
        // Add player and notify all players
        playerMap.put(connection, info);
        broadcastLobbyUpdate();
        Log.info("Player " + playerName + " joined lobby " + lobbyId + "(Team: NONE)");
        return true;
    }

    /**
     * Removes a player from this lobby
     *
     * @param connection The player's connection
     * @return true if player was removed, false if player wasn't in this lobby
     */
    public boolean removePlayer(Connection connection) {
        if (playerMap.remove(connection) != null) {
            broadcastLobbyUpdate();
            return true;
        }
        return false;
    }

    /**
     * Checks if a specific player is in this lobby
     *
     * @param connection The player's connection
     * @return true if player is in this lobby
     */
    public boolean hasPlayer(Connection connection) {
        return playerMap.containsKey(connection);
    }

    /**
     * Starts the game for all players in this lobby
     */
    public void startGame() {
        if (playerMap.isEmpty()) {
            Log.warn("Cannot start game - no players in lobby: " + lobbyId);
            return;
        }
        // First player is the host
        Connection host = playerMap.keySet().iterator().next();
        PlayerLobbyInfo hostInfo = playerMap.get(host);

//        if (!playerMap.containsKey(startingPlayer)) {
//            Log.warn("Player not in lobby attempted to start game: " + lobbyId);
//            return;
//        }
//        String starterName = playerMap.get(startingPlayer).getPlayerName();
//        Team starterTeam = playerMap.get(startingPlayer).getTeam();
        // Create new game instance for this lobby
        gameInstance = new GameInstance(serverListener, host, hostInfo.getPlayerName(),
                hostInfo.getTeam(), hostInfo.getChampionType(), lobbyManager, lobbyId, playerMap.size());
        playerMap.forEach((connection, info) -> {
            if (connection != host) {
                gameInstance.addConnection(connection, info.getPlayerName(), info.getTeam(), info.getChampionType());
            }
        });
        gameInstance.start();

        // Send start message
        StartGameMessage startMessage = new StartGameMessage();
        startMessage.setLobbyId(lobbyId);
        playerMap.keySet().forEach(connection -> connection.sendTCP(startMessage));
        System.out.println("LibGDX version: " + com.badlogic.gdx.Version.VERSION);
        Log.info("Game started for lobby: " + lobbyId + " with " + playerMap.size() + " players");
    }

    /**
     * Sends lobby update to all players in the lobby
     */
    private void broadcastLobbyUpdate() {
        LobbyUpdateMessage updateMessage = new LobbyUpdateMessage();
        updateMessage.setLobbyId(lobbyId);
        updateMessage.setPlayers(new ArrayList<>(playerMap.values()));

        playerMap.keySet().forEach(conn -> conn.sendTCP(updateMessage));
    }

    /**
     * Checks if this lobby is empty
     *
     * @return true if no players are in the lobby
     */
    public boolean isEmpty() {
        return playerMap.isEmpty();
    }

    /**
     * Disposes of the game instance
     */
    public void disposeGame() {
        // Clean up game resources if needed
        gameInstance = null;
        Log.info("Disposed game instance for lobby: " + lobbyId);
    }

    public void changePlayerTeam (Connection connection, Team requestedTeam) {
        PlayerLobbyInfo player = playerMap.get(connection);
        if (player == null) return;
        if (championSelectStarted) {
            Log.info("Cannot change team in lobby " + lobbyId + " after champion select has started.");
            return;
        }

        // Count players on the requested team
        long teamSize = playerMap.values().stream()
                .filter(p -> p.getTeam() == requestedTeam)
                .count();

        // 3v3 limit check
        if (requestedTeam == Team.NONE || teamSize < 3) {
            player.setTeam(requestedTeam);
            player.setChampionType(ChampionType.NONE);
            player.setLockedIn(false);
            broadcastLobbyUpdate();
        } else {
            Log.info("Team " + requestedTeam + " is full!");
        }
    }

    public void broadcastChampionSelectUpdate() {
        ChampionSelectUpdate updateMessage = new ChampionSelectUpdate();
        updateMessage.setLobbyId(lobbyId);
        updateMessage.setPlayers(new ArrayList<>(playerMap.values()));
        playerMap.keySet().forEach(conn -> conn.sendTCP(updateMessage));
    }
    public PlayerLobbyInfo getPlayerInfo(Connection connection) {
        return playerMap.get(connection);
    }

    public void notifyChampionSelect() {
        championSelectStarted = true;
        broadcastChampionSelectUpdate();
    }

    public boolean canStartChampionSelect() {
        return !championSelectStarted
                && !playerMap.isEmpty()
                && playerMap.values().stream().allMatch(player -> player.getTeam() != Team.NONE);
    }
}
