package ee.taltech.examplegame.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.util.DamageVignette;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.util.Sprites;
import lombok.Getter;
import message.PlayerLobbyInfo;
import message.dto.BulletState;
import message.GameStateMessage;
import message.dto.MinionState;
import message.dto.NexusState;
import message.dto.PlayerState;
import message.dto.TurretState;
import message.dto.Team;

import java.util.ArrayList;
import java.util.List;

import static constant.Constants.BULLET_HEIGHT_IN_PIXELS;
import static constant.Constants.BULLET_WIDTH_IN_PIXELS;
import static constant.Constants.PPM;

/**
 * Initialize a new Arena, which is responsible for updating and rendering the following: players, bullets, map.
 * Updating - modifying the inner state of objects (e.g. Player) based on game state messages received from the server.
 * Rendering - making the (updated) objects visible on the screen.
 */
public class Arena {

    private final List<Player> players = new ArrayList<>();
    private final List<Minion> minions = new ArrayList<>();
    private final List<Nexus> nexuses = new ArrayList<>();
    private final List<Turret> turrets = new ArrayList<>();
    private List<BulletState> bullets = new ArrayList<>();
    private boolean turretDestroyedThisFrame;
    private boolean turretDestroyedSoundPlayed;
    /**
     * -- GETTER --
     *  Gets the team of the local player.
     *
     * @return the Team of the local player, or null if not yet determined
     */
    @Getter
    private Team localPlayerTeam;
    public final DamageVignette damageVignette = new DamageVignette();
    private Player localPlayer;

    // Arrow variables
    private float arrowX;
    private float arrowY;
    private boolean showArrow;
    private long arrowSpawnTime;
    private static final long ARROW_DURATION = 500;

    public Arena(List<PlayerLobbyInfo> lobbyPlayers) {}
    /**
     * Update players and bullets, so they are later rendered in the correct position.
     *
     * @param gameStateMessage latest game state received from the server
     */
    public void update(GameStateMessage gameStateMessage) {
        turretDestroyedThisFrame = false;
        // Find all the players in this server
        var currentPlayerIds = gameStateMessage.getPlayerStates()
                .stream()
                .map(PlayerState::getId)
                .toList();

        // Remove players that are no longer in the game state
        players.removeIf(player -> !currentPlayerIds.contains(player.getId()));

        Team localPlayerTeam = gameStateMessage.getPlayerStates().stream()
                .filter(playerState -> playerState.getId() == ee.taltech.examplegame.Main.myID)
                .findFirst()
                .map(PlayerState::getTeam)
                .orElse(null);

        // Update existing players and add new ones
        gameStateMessage.getPlayerStates().forEach(playerState -> {
            var player = players
                    .stream()
                    .filter(p -> p.getId() == playerState.getId())
                    .findFirst()
                    .orElseGet(() -> {
                        System.out.println(playerState.getName());
                        var newPlayer = new Player(playerState.getId());
                        newPlayer.setChampionType(playerState.getChampionType());
                        if (playerState.getId() == ee.taltech.examplegame.Main.myID) {
                            newPlayer.setName(ee.taltech.examplegame.Main.savedPlayerName);
                        } else {
                            newPlayer.setName(playerState.getName());
                        }
                        players.add(newPlayer);
                        return newPlayer;
                    });

            // Server sends pixels; client world is pixels/PPM when rendering with 1/PPM.
            player.setX(playerState.getX() / PPM);
            player.setY(playerState.getY() / PPM);
            int playerOldHP = player.getHp();
            player.setHp(playerState.getHp());
            int damageTaken = playerOldHP - playerState.getHp();
            if ((playerOldHP - playerState.getHp()) > 0) {
                player.spawnDamageIndicator(damageTaken);
                // Trigger vignette ONLY if the local player takes damage (from turrets, players)
                if (player.getId() == ee.taltech.examplegame.Main.myID && damageTaken > 20) {
                    damageVignette.trigger();
                }
            }
            player.setLives(playerState.getLives());
            player.setMaxHp(playerState.getMaxHp());
            player.setLevel(playerState.getLevel());
            player.setName(playerState.getName());
            player.setChampionType(playerState.getChampionType());
            player.setActionState(playerState.getActionState());
            player.setDirection(playerState.getDirection());
            player.setAutoAttackActive(playerState.isAutoAttackActive());
            player.setAutoAttackSequence(playerState.getAutoAttackSequence());
            player.setTankHealSequence(playerState.getTankHealSequence());
            if (player.getId() == ee.taltech.examplegame.Main.myID) {
                this.localPlayer = player;
            }
            playSoundIfWithinRange(player, playerState.getCastAbility());

            player.setTeam(playerState.getTeam());
            player.setLocalPlayerTeam(localPlayerTeam);
        });
        this.bullets = gameStateMessage.getBulletStates();
    }

    public void updateMinion(GameStateMessage gameStateMessage) {
        var currentMinionIds = gameStateMessage.getMinionStates()
                .stream()
                .map(MinionState::getId)
                .toList();
        minions.removeIf(minion -> !currentMinionIds.contains(minion.getId()));

        Team localPlayerTeam = gameStateMessage.getPlayerStates().stream()
                .filter(playerState -> playerState.getId() == ee.taltech.examplegame.Main.myID)
                .findFirst()
                .map(PlayerState::getTeam)
                .orElse(null);

        gameStateMessage.getMinionStates().forEach(minionState -> {
            var minion = minions
                    .stream()
                    .filter(existingMinion -> existingMinion.getId() == minionState.getId())
                    .findFirst()
                    .orElseGet(() -> {
                        var newMinion = new Minion(minionState.getId());
                        minions.add(newMinion);
                        return newMinion;
                    });

            minion.setX(minionState.getX() / PPM);
            minion.setY(minionState.getY() / PPM);
            minion.setHp(minionState.getHp());
            minion.setMaxHp(minionState.getMaxHp());
            minion.setTeam(minionState.getTeam());
            minion.setLocalPlayerTeam(localPlayerTeam);
            minion.setDirection(minionState.getDirection());
            minion.setActionState(minionState.getActionState());
        });

    }

public void updateTurret(GameStateMessage gameStateMessage) {
    this.localPlayerTeam = resolveLocalPlayerTeam(gameStateMessage);
    var currentTurretTeams = gameStateMessage.getTurretStates()
                .stream()
                .map(TurretState::getTeam)
                .toList();
        turrets.removeIf(turret -> !currentTurretTeams.contains(turret.getTeam()));

        gameStateMessage.getTurretStates().forEach(turretState -> {
            var turret = turrets
                    .stream()
                    .filter(existingTurret -> existingTurret.getTeam() == turretState.getTeam())
                    .findFirst()
                    .orElseGet(() -> {
                        var newTurret = new Turret();
                        newTurret.setTeam(turretState.getTeam());
                        turrets.add(newTurret);
                        return newTurret;
                    });

            turret.setX(turretState.getX() / PPM);
            turret.setY(turretState.getY() / PPM);
            turret.setHp(turretState.getHp());
            turret.setMaxHp(turretState.getMaxHp());
            turret.setLocalPlayerTeam(localPlayerTeam);
            turret.setDestroyed(turretState.isDestroyed());
        });
    }

public void updateNexus(GameStateMessage gameStateMessage) {
    this.localPlayerTeam = resolveLocalPlayerTeam(gameStateMessage);
    var currentNexusTeams = gameStateMessage.getNexusStates()
                .stream()
                .map(NexusState::getTeam)
                .toList();
        nexuses.removeIf(nexus -> !currentNexusTeams.contains(nexus.getTeam()));

        gameStateMessage.getNexusStates().forEach(nexusState -> {
            var nexus = nexuses
                    .stream()
                    .filter(existingNexus -> existingNexus.getTeam() == nexusState.getTeam())
                    .findFirst()
                    .orElseGet(() -> {
                        var newNexus = new Nexus();
                        newNexus.setTeam(nexusState.getTeam());
                        nexuses.add(newNexus);
                        return newNexus;
                    });

            nexus.setX(nexusState.getX() / PPM);
            nexus.setY(nexusState.getY() / PPM);
            nexus.setHp(nexusState.getHp());
            nexus.setMaxHp(nexusState.getMaxHp());
            nexus.setLocalPlayerTeam(localPlayerTeam);
            nexus.setDestroyed(nexusState.isDestroyed());
            boolean turretDestroyed = gameStateMessage.getTurretStates()
                    .stream()
                    .filter(turretState -> turretState.getTeam() == nexusState.getTeam())
                    .findFirst()
                    .map(message.dto.TurretState::isDestroyed)
                    .orElse(true);
            nexus.setTurretDestroyed(turretDestroyed);
        });

        var currentTurretTeams = gameStateMessage.getTurretStates()
                .stream()
                .map(TurretState::getTeam)
                .toList();
        turrets.removeIf(turret -> !currentTurretTeams.contains(turret.getTeam()));

        gameStateMessage.getTurretStates().forEach(turretState -> {
            if (turretState.isDestroyed() && !turretDestroyedSoundPlayed) {
                turretDestroyedThisFrame = true;
                turretDestroyedSoundPlayed = true;
            }

            var turret = turrets
                    .stream()
                    .filter(existingTurret -> existingTurret.getTeam() == turretState.getTeam())
                    .findFirst()
                    .orElseGet(() -> {
                        var newTurret = new Turret();
                        newTurret.setTeam(turretState.getTeam());
                        turrets.add(newTurret);
                        return newTurret;
                    });

            turret.setX(turretState.getX() / PPM);
            turret.setY(turretState.getY() / PPM);
            turret.setHp(turretState.getHp());
            turret.setMaxHp(turretState.getMaxHp());
            turret.setLocalPlayerTeam(localPlayerTeam);
            turret.setDestroyed(turretState.isDestroyed());
        });

        this.bullets = gameStateMessage.getBulletStates();
    }

    private Team resolveLocalPlayerTeam(GameStateMessage gameStateMessage) {
        return gameStateMessage.getPlayerStates().stream()
                .filter(playerState -> playerState.getId() == ee.taltech.examplegame.Main.myID)
                .findFirst()
                .map(PlayerState::getTeam)
                .orElse(null);
    }

    public boolean hasTurretDestroyedThisFrame() {
        return turretDestroyedThisFrame;
    }

    private void playSoundIfWithinRange(Player player, String abilityUsed) {
        // If no ability was cast or local player isn't set yet, do nothing
        if (abilityUsed == null || localPlayer == null) {
            return;
        }

        Log.info("used ability: " + abilityUsed);

        // If the player casting is the local player, skip it
        if (player.getId() == localPlayer.getId()) {
            Main.getInstance().getAudioManager().playAbilitySound(abilityUsed);
            return;
        }

        // Calculate distance from local player
        float dx = localPlayer.getX() - player.getX();
        float dy = localPlayer.getY() - player.getY();

        // Convert LibGDX world units back to pixels
        float distanceInPixels = (float) Math.sqrt(dx * dx + dy * dy) * PPM;

        if (distanceInPixels < 400f) {
            Main.getInstance().getAudioManager().playAbilitySound(abilityUsed);
        }
    }

    public void setMovementArrow(float x, float y) {
        this.arrowX = x;
        this.arrowY = y;
        showArrow = true;
        arrowSpawnTime = System.currentTimeMillis();
    }

    /**
     * Render map, players and bullets. This makes them visible on the screen.
     *
     * @param spriteBatch used for rendering (and scaling/resizing) all visual elements
     */
    public void render(SpriteBatch spriteBatch, Camera camera) {
        renderMovementArrow(spriteBatch);
        renderNexuses(spriteBatch, camera);
        renderTurrets(spriteBatch, camera);
        renderMinions(spriteBatch, camera);
        renderPlayers(spriteBatch, camera);
        renderBullets(spriteBatch);
        renderDamageVignette(spriteBatch, camera);
    }

    private void renderNexuses(SpriteBatch spriteBatch, Camera camera) {
        nexuses.forEach(nexus -> nexus.render(spriteBatch, camera));
    }

    private void renderPlayers(SpriteBatch spriteBatch, Camera camera) {
        players.forEach(player -> player.render(spriteBatch, camera));
    }

    private void renderMinions(SpriteBatch spriteBatch, Camera camera) {
        minions.forEach(minion -> minion.render(spriteBatch, camera));
    }

    private void renderTurrets(SpriteBatch spriteBatch, Camera camera) {
        turrets.forEach(turret -> turret.render(spriteBatch, camera));
    }

    private void renderBullets(SpriteBatch spriteBatch) {
        bullets.forEach(bullet -> spriteBatch.draw(
                Sprites.getBulletTexture(bullet.getChampionType()),
                bullet.getX() / PPM,
                bullet.getY() / PPM,
                BULLET_WIDTH_IN_PIXELS / PPM,
                BULLET_HEIGHT_IN_PIXELS / PPM
        ));
    }



    private void renderMovementArrow(SpriteBatch spriteBatch) {
        if (!showArrow) {
            return;
        }

        // scaling for the current arrow sprite
        float scale = 0.5f;

        float width = (Sprites.movementArrow.getWidth()) / PPM * scale;
        float height = (Sprites.movementArrow.getHeight()) / PPM * scale;

        long now =  System.currentTimeMillis();
        if (now - arrowSpawnTime < ARROW_DURATION) {
            spriteBatch.draw(
                    Sprites.movementArrow,
                    arrowX - (width / 2),
                    arrowY -  (height / 2),
                    width,
                    height);

        }
    }

    private void renderDamageVignette(SpriteBatch spriteBatch, Camera camera) {
        damageVignette.render(spriteBatch, camera);
    }

}
