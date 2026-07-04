package ee.taltech.examplegame.server.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.object.Bullet;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.Minion;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import ee.taltech.examplegame.server.game.object.turret.TurretTargeting;
import ee.taltech.examplegame.server.listener.PlayerCollisionListener;
import ee.taltech.examplegame.server.listener.ServerListener;
import ee.taltech.examplegame.server.lobby.Lobby;
import ee.taltech.examplegame.server.lobby.LobbyManager;
import message.dto.ChampionType;
import message.dto.Team;
import lombok.Getter;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static constant.Constants.GAME_TICK_RATE;
import static constant.Constants.GRAVITY;
import static constant.Constants.MINIONS_PER_WAVE;
import static constant.Constants.MINION_SPAWN_INTERVAL_MS;
import static constant.Constants.MINION_XP_SHARE_RANGE;
import static constant.Constants.XP_PER_MINION;

/**
 * Represents the game logic and server-side management of the game instance.
 * Handles player connections, game state updates, bullet collisions, and communication with clients.
 * <p>
 * This class extends {@link Thread} because the game loop needs to run continuously
 * in the background, independent of other server operations. By running in a separate thread,
 * it ensures that the game state updates at a fixed tick rate without blocking other processes in the main server.
 */
public class GameInstance extends Thread {

    private static final float[][] BLUE_SPAWN_POINTS = {
            {3000f, 725f},
            {3050f, 700f},
            {3100f, 725f},
            {3150f, 700f},
            {2950f, 700f},
            {3000f, 750f}
    };
    private static final float[][] RED_SPAWN_POINTS = {
            {1130f, -295f},
            {1065f, -350f},
            {1195f, -350f},
            {1075f, -240f},
            {1185f, -240f},
            {1130f, -190f}
    };
    private static final float[][] NEXUS_POINTS = {
            {2950f, 635f}, // Blue team nexus
            {1215f, -235f} // Red team nexus
    };
    private static final float[][] TURRET_POINTS = { // X is Up and Y is right
            {2610f, 545f}, // Blue team Turret
            {1600f, 35f} // Red team Turret
    };
    private static final float[][] BLUE_MINION_SPAWN_POINTS = {
            {2700f, 625f},
            {2600f, 600f},
            {2800f, 450f}
    };
    private static final float[][] RED_MINION_SPAWN_POINTS = {
            {1380f, -110f},
            {1345f, -140f},
            {1415f, -140f}
    };

    private final ServerListener server;
    private final LobbyManager lobbyManager;
    private final int lobbyId;
    private final BulletCollisionHandler collisionHandler = new BulletCollisionHandler();
    private final GameStateHandler gameStateHandler = new GameStateHandler();
    @Getter
    private final Box2dWorldGenerator box2dWorldGenerator;
    @Getter
    private final AStarPathfinder aStarPathfinder;

    private final Set<Connection> connections =
            Collections.synchronizedSet(new HashSet<>());  // Avoid a connection (player) joining the game twice
    @Getter
    private final List<Player> players = new ArrayList<>();
    @Getter
    private final List<Minion> minions = new ArrayList<>();
    @Getter
    private final List<Nexus> nexuses = new ArrayList<>();
    @Getter
    private final List<Turret> turrets = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private final World world;
    private final Map<Integer, Player> activePlayers = new ConcurrentHashMap<>();
    private final int requiredPlayerCount;
    private int nextMinionId = 1;
    private long lastMinionSpawnTime = System.currentTimeMillis() - MINION_SPAWN_INTERVAL_MS;
    private int blueSpawnIndex;
    private int redSpawnIndex;

    /**
     * Initializes the game instance.
     *
     * @param server Reference to ServerListener to call dispose() when the game is finished or all players leave.
     * @param firstConnection Connection of the first player.
     */
    public GameInstance(ServerListener server, Connection firstConnection, String firstName, Team firstTeam, ChampionType firstChampion, LobbyManager lobbyManager, int lobbyId, int lobbySize) {
        this.server = server;
        this.lobbyManager = lobbyManager;
        this.lobbyId = lobbyId;
        this.requiredPlayerCount = lobbySize;
        this.world = new World(new Vector2(0, GRAVITY), true);  // has gravity -9.8f. If you want no gravity just set to 0 and change movement // doSleep so the characters are only rendered if they move and not every instance
        this.world.setContactListener(new PlayerCollisionListener());

        this.box2dWorldGenerator = new Box2dWorldGenerator();
        box2dWorldGenerator.initializeWorld(world); // set up the tiled collision map
        this.aStarPathfinder = new AStarPathfinder(box2dWorldGenerator.getPathfindingGrid());
        initializeNexuses();
        initializeTurrets();

        Player newPlayer = new Player(firstConnection, this, world);
        newPlayer.setName(firstName);
        newPlayer.setTeam(firstTeam);
        newPlayer.assignChampion(firstChampion);
        players.add(newPlayer);
        assignSpawnPoint(newPlayer);
        activePlayers.put(firstConnection.getID(), newPlayer);
        connections.add(firstConnection);
    }

    public void addBullet(Bullet bullet) {
        this.bullets.add(bullet);
    }

    public Player findPlayerById(int playerId) {
        return activePlayers.get(playerId);
    }

    public void notifyChampionDamagedUnderTurret(Player victim, Player attacker) {
        if (victim == null || attacker == null || victim.getTeam() == attacker.getTeam()) {
            return;
        }

        for (Turret turret : turrets) {
            if (turret.isDestroyed() || turret.getTeam() != victim.getTeam()) {
                continue;
            }

            if (!TurretTargeting.isTargetInRange(turret, victim)
                    || !TurretTargeting.isTargetInRange(turret, attacker)) {
                continue;
            }

            if (turret.getCurrentTarget() instanceof Player currentPlayerTarget
                    && !currentPlayerTarget.isDestroyed()
                    && TurretTargeting.isTargetInRange(turret, currentPlayerTarget)) {
                continue;
            }

            turret.forcePlayerAggro(attacker);
        }
    }

    /**
     * Check if the game has the required number of players to start.
     */
    public boolean hasEnoughPlayers() {
        return connections.size() >= requiredPlayerCount;
    }

    /**
     * Adds a new connection and player to the game.
     * If the required number of players is reached, the game is ready to start.
     *
     * @param connection Connection to the client side of the player.
     */
    public void addConnection(Connection connection, String name, Team team, ChampionType championType) {
        if (hasEnoughPlayers()) {
            Log.info("Cannot add connection: Required number of players already connected.");
            return;
        }

        // Add new player and connection
        Player newPlayer = new Player(connection, this, world);
        newPlayer.setName(name);
        newPlayer.setTeam(team);
        newPlayer.assignChampion(championType);
        players.add(newPlayer);
        assignSpawnPoint(newPlayer);
        activePlayers.put(connection.getID(), newPlayer);
        connections.add(connection);

        // Check if the game is ready to start
        if (hasEnoughPlayers()) {
            gameStateHandler.setAllPlayersHaveJoined(true);
        }
    }

    public void removeConnection(Connection connection) {
        this.connections.remove(connection);
        Player player = activePlayers.remove(connection.getID());
        if (player != null) {
            players.remove(player);
        }

        var gameStateMessage = gameStateHandler.getGameStateMessage(players, minions, bullets, nexuses, turrets);
        connections.forEach(c -> c.sendTCP(gameStateMessage));

        Log.info("Player left the game: " + connection.getID());
    }

    private void assignSpawnPoint(Player player) {
        float[] spawn = switch (player.getTeam()) {
            case TEAM_BLUE -> BLUE_SPAWN_POINTS[blueSpawnIndex++ % BLUE_SPAWN_POINTS.length];
            case TEAM_RED -> RED_SPAWN_POINTS[redSpawnIndex++ % RED_SPAWN_POINTS.length];
            case NONE -> BLUE_SPAWN_POINTS[blueSpawnIndex++ % BLUE_SPAWN_POINTS.length];
        };
        Vector2 resolvedSpawn = resolveSpawnPoint(spawn[0], spawn[1]);
        player.setBaseSpawnPosition(resolvedSpawn.x, resolvedSpawn.y);
    }

    private Vector2 resolveSpawnPoint(float spawnX, float spawnY) {
        int[] grid = box2dWorldGenerator.mapCoordinatesToGrid(spawnX, spawnY);
        int[] walkable = aStarPathfinder.findNearestWalkable(grid[0], grid[1]);

        if (walkable[0] == -1) {
            return new Vector2(spawnX, spawnY);
        }

        return box2dWorldGenerator.gridToMapCoordinates(walkable[0], walkable[1]);
    }

    private void initializeNexuses() {
        nexuses.add(new Nexus(world, Team.TEAM_BLUE, NEXUS_POINTS[0][0], NEXUS_POINTS[0][1]));
        nexuses.add(new Nexus(world, Team.TEAM_RED, NEXUS_POINTS[1][0], NEXUS_POINTS[1][1]));
    }

    private void initializeTurrets() {
        turrets.add(new Turret(world, Team.TEAM_BLUE, TURRET_POINTS[0][0], TURRET_POINTS[0][1]));
        turrets.add(new Turret(world, Team.TEAM_RED, TURRET_POINTS[1][0], TURRET_POINTS[1][1]));
    }

    /**
     * Stops and disposes the current game instance, so a new one can be created with the same or new players.
     */
    private void disposeGame() {
        players.forEach(Player::dispose);  // remove movement and shooting listeners
        minions.forEach(Minion::dispose);
        connections.clear();
        // Notify the lobby to dispose this game instance
        Lobby lobby = lobbyManager.getLobby(lobbyId);
        if (lobby != null) {
            lobby.disposeGame();
            Log.info("Game disposed for lobby: " + lobbyId);
        } else {
            Log.warn("Could not find lobby " + lobbyId + " to dispose game");
        }
    }

    /**
     * Game loop. Updates the game state, checks for collisions, and sends updates to clients.
     * The game loop runs until the game is stopped or no players remain.
     */
    @Override
    public void run() {
        boolean isGameRunning = true;

        while (isGameRunning) {
            gameStateHandler.incrementGameTimeIfPlayersPresent();
            spawnMinionsIfNeeded();

            // Move player before sending game state.
            players.forEach(Player::update);
            minions.forEach(Minion::update);
            removeDestroyedMinions();
            turrets.forEach(turret -> TurretTargeting.updateTurretFire(turret, this));

            // Step Box2D simulation so bodies move.
            // Timestep based on tick rate; velocity/position iterations are standard defaults.
            world.step(1f / GAME_TICK_RATE, 6, 2);


            // update bullets, check for collisions and remove out of bounds bullets
            bullets.forEach(Bullet::update);
            bullets = collisionHandler.handleCollisions(bullets, players, nexuses, turrets, minions);

            // construct gameStateMessage
            var gameStateMessage = gameStateHandler.getGameStateMessage(players, minions, bullets, nexuses, turrets);
            // send the state of current game to all connected clients
            connections.forEach(connection -> connection.sendUDP(gameStateMessage));


            // If any nexus is destroyed, end the game
            if (nexuses.stream().anyMatch(Nexus::isDestroyed)) {
                // Use TCP to ensure that the last gameStateMessage reaches all clients
                connections.forEach(connection -> connection.sendTCP(gameStateMessage));
                disposeGame();
                isGameRunning = false;
            }
            // If no players are connected, stop the game loop
            if (connections.isEmpty()) {
                Log.info("No players connected, stopping game loop.");
                disposeGame();
                isGameRunning = false;
            }

            try {
                // We don't want to update the game state every millisecond, that would be
                // too much for the server to handle. So a tick rate is used to limit the
                // amount of updates per second.
                Thread.sleep(Duration.ofMillis(1000 / GAME_TICK_RATE));
            } catch (InterruptedException e) {
                Log.error("Game loop sleep interrupted", e);
            }
        }
    }

    private void spawnMinionsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastMinionSpawnTime < MINION_SPAWN_INTERVAL_MS) {
            return;
        }

        spawnWave(Team.TEAM_BLUE, BLUE_MINION_SPAWN_POINTS, NEXUS_POINTS[1]);
        spawnWave(Team.TEAM_RED, RED_MINION_SPAWN_POINTS, NEXUS_POINTS[0]);
        lastMinionSpawnTime = now;
    }

    private void spawnWave(Team team, float[][] spawnPoints, float[] laneTarget) {
        for (int i = 0; i < Math.min(MINIONS_PER_WAVE, spawnPoints.length); i++) {
            float[] spawn = spawnPoints[i];
            minions.add(new Minion(this, world, nextMinionId++, team, spawn[0], spawn[1], laneTarget[0], laneTarget[1]));
        }
    }

    private void removeDestroyedMinions() {
        Iterator<Minion> iterator = minions.iterator();
        while (iterator.hasNext()) {
            Minion minion = iterator.next();
            if (!minion.isReadyToDespawn()) {
                continue;
            }
            XpForNearbyPlayers(minion);
            minion.dispose();
            iterator.remove();
        }
    }

    private void XpForNearbyPlayers(Minion minion) {
        for  (Player player : players) {
            if (player.isDestroyed() || player.getTeam() == null || player.getTeam() == minion.getTeam()) {
                continue;
            }
            float dx = player.getX() - minion.getX();
            float dy = player.getY() - minion.getY();
            float distanceSquared = dx * dx + dy * dy;

            if (distanceSquared <= MINION_XP_SHARE_RANGE * MINION_XP_SHARE_RANGE) {
                player.addXp(XP_PER_MINION);
            }
        }
    }
}
