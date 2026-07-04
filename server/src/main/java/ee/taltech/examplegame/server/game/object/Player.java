package ee.taltech.examplegame.server.game.object;

// Box2D and tiled imports
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryonet.Connection;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.ability.InstantAbility;
import ee.taltech.examplegame.server.game.object.ability.TimedEffect;
import ee.taltech.examplegame.server.game.object.champion.*;
import ee.taltech.examplegame.server.listener.PlayerAbilityListener;
import ee.taltech.examplegame.server.listener.PlayerMovementListener;
import lombok.Getter;
import lombok.Setter;
import message.PlayerMovementMessage;
import message.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static constant.Constants.*;
import static ee.taltech.examplegame.server.util.TargetUtils.findTarget;
import static ee.taltech.examplegame.server.util.TargetUtils.nearTarget;
import static message.dto.ActionState.*;

/**
 * Server-side representation of a player in the game. This class listens for player movements or shooting actions
 * and changes the player's server-side state accordingly. Lives management.
 */
@Getter
@Setter
public class Player implements AttackableTarget {

    private static final long RESPAWN_DELAY_MS = 5_000L;

    // Keep track of listener objects for each player connection, so they can be disposed when the game ends
    private final PlayerMovementListener movementListener = new PlayerMovementListener(this);
    private final PlayerAbilityListener abilityListener = new PlayerAbilityListener(this);

    // Idendity & networking
    private final int id;
    private String name;
    private int level = 1;
    private int currentXp;
    private int xpForNextLevel = calculateXpForNextLevel(1);
    private Team team;
    private final Connection connection;
    private int killCount;
    private int deathCount;

    private final GameInstance game;
    private ChampionType temporarySelection = ChampionType.NONE;
    private boolean lockedIn = false;
    private Champion champion = new Ranged();
    private int lives = champion.getBaseHp();

    // Box2D world generation. All collisions happen here.
    private final World world;
    private Body body;

    // Player spawnpoint cordinates (before PPM conversion)
    private float x = 1750f;
    private float y = 150f;
    private float baseSpawnX = x;
    private float baseSpawnY = y;
    private long deathTime = -1L;


    // STATE MACHINE
    private ActionState actionState =  ActionState.IDLE;

    // Movement

    private Queue<Vector2> pathQueue = new ConcurrentLinkedQueue<>();
    private Direction direction = Direction.DOWN;
    private float speedMultiplier = 1f;
    private final PlayerMovementHandler movementHandler;

    // Combat
    private AttackableTarget enemy;
    private float damageMultiplier = 1f;

    // Casting ability
    private AbilitySlot pendingAbilitySlot;
    private int pendingMouseX;
    private int pendingMouseY;
    private String castAbility;
    // When the current cast or auto-attack wind-up started; -1 means nothing is in progress; used to enforce casting for time before casting ability.
    private long actionStartTime = -1;
    private boolean autoAttackActive;
    private int autoAttackSequence;
    private int tankHealSequence;
    private long lastPassiveRegenTime = System.currentTimeMillis();

    private List<TimedEffect> activeEffects = new ArrayList<>();

    /**
     * Initializes a new server-side representation of a Player with a game reference and connection to client-side.
     *
     * @param connection Connection to client-side.
     * @param game Game instance that this player is a part of.
     */
    public Player(Connection connection, GameInstance game, World world) {
        this.connection = connection;
        this.id = connection.getID();
        this.game = game;
        this.name = "Player";
        this.world = world;
        this.movementHandler = new PlayerMovementHandler(this, game);
        this.lives = getMaxHp();
        this.lastPassiveRegenTime = System.currentTimeMillis();

        this.connection.addListener(movementListener);
        this.connection.addListener(abilityListener);
        definePlayer();
    }

    private void definePlayer() {
        BodyDef bdef = new BodyDef();
        bdef.position.set(x / PPM, y / PPM); // Starts at a visibile position and not in a collision box
        bdef.type = BodyDef.BodyType.DynamicBody; // Dynamic body not static
        bdef.fixedRotation = true; // don't tip / rotate when colliding

        body = world.createBody(bdef);

        FixtureDef fdef = new FixtureDef();
        // Keep player friction low to avoid grabbing onto vertical walls.
        fdef.friction = 0.0f;
        fdef.density = 2.0f;
        fdef.restitution = 0.0f;
        fdef.filter.categoryBits = PLAYER_CATEGORY;
        fdef.filter.maskBits = PLAYER_MASK;


        PolygonShape shape = new PolygonShape(); // Also circleShapes, PolygonShapes etc.
        // Sets the hitbox size.
        shape.setAsBox(PLAYER_COLLISION_WIDTH_IN_PIXELS / (2f * PPM), PLAYER_COLLISION_HEIGHT_IN_PIXELS / (2f * PPM));

        fdef.shape = shape;
        body.createFixture(fdef);
        body.setUserData(this);
        shape.dispose();
    }

    public void assignChampion(ChampionType type) {
        ChampionType resolvedType = (type == null || type == ChampionType.NONE) ? ChampionType.RANGED : type;
        this.champion = ChampionFactory.create(resolvedType);
        this.temporarySelection = resolvedType;
        this.lives = champion.getMaxHpAtLevel(level);
        this.lastPassiveRegenTime = System.currentTimeMillis();
    }

    public void update() {
        syncPosition();
        tickEffects();
        applyPassiveHealthRegeneration();


        if (lives <= 0) {
            actionState = DEAD;
            if (deathTime == -1L) {
                deathTime = System.currentTimeMillis();
            }
        }

        switch (actionState) {
            case IDLE:
                body.setLinearVelocity(0, 0);
                break;
            case MOVING:
                movementHandler.updateMovement();
                break;
            case CASTING:
                updateCasting();
                break;
            case AUTO_ATTACKING:
                updateCombat();
                break;
            case STUNNED:
                body.setLinearVelocity(0, 0);
                break;
            case DEAD:
                body.setLinearVelocity(0, 0);
                attemptRespawn();
                break;
        }
    }


    private void syncPosition() {
        x = body.getPosition().x * PPM;
        y = body.getPosition().y * PPM;
        if (actionState == IDLE) {
            body.setLinearVelocity(0, 0);
        }
    }


    /**
     * Gets called when player state is ATTACKING
     */
    private void updateCombat() {
        if (enemy == null || enemy.isDestroyed()) {
            autoAttackActive = false;
            actionState = IDLE;
            return;
        }

        if (nearTarget(this, enemy)) {
            autoAttackActive = true;
            body.setLinearVelocity(0, 0);
            pathQueue.clear();
            attemptAutoAttack();
        } else {
            autoAttackActive = false;
            actionStartTime = -1;
            pathQueue.clear();
            pathQueue.addAll(movementHandler.buildCombatPath(enemy));
            movementHandler.updateMovement();
        }

    }


    private void attemptAutoAttack() {
        long now = System.currentTimeMillis();

        if (actionStartTime == -1) {
            actionStartTime = now; // Begin casting auto-attack
        }

        if (now - actionStartTime > ACTION_DELAY) {
            champion.autoAttack(this, enemy, game); // The casting time is over, we can now attack
            autoAttackSequence++;
            actionStartTime = -1;
        }
    }

    public void applyEffect(TimedEffect effect) {
        effect.startTimer();
        effect.onApply(this);
        activeEffects.add(effect);
    }

    private void tickEffects() {
        activeEffects.removeIf(effect -> {
            if (effect.isExpired()) {
                effect.onExpire(this);
                return true;
            }
            return false;
        });
    }


    /**
     * Gets called when player is CASTING, waits for delay and then fires the queued ability
     */
    private void updateCasting() {
        body.setLinearVelocity(0, 0);

        // actionStartTime started in useAbility(), if the casting time is up ability gets used.
        if (System.currentTimeMillis() - actionStartTime < ACTION_DELAY) {
            return;
        }

        if (pendingAbilitySlot == AbilitySlot.Q) {
            champion.useQ(id, x, y, pendingMouseX, pendingMouseY, game);
        } else if (pendingAbilitySlot == AbilitySlot.W) {
            if (champion instanceof Tank tank) {
                if (tank.tryUseHeal(id, game)) {
                    tankHealSequence++;
                }
            } else {
                champion.useW(id, x, y, pendingMouseX, pendingMouseY, game);
            }
        }

        actionStartTime = -1;
        actionState = IDLE;
    }

    // =========================================================================
    // Public API (called by listeners / game logic)
    // =========================================================================


    /**
     * Moves the player toward destinaation, or switches to ATTACKING
     * state if the destination contains an enemy.
     *
     * @param destination target position sent by the client
     */
    public void move(PlayerMovementMessage destination) {
        if (destination == null || actionState == DEAD || actionState == STUNNED) return;

        autoAttackActive = false;
        enemy = findTarget(destination, this, game);

        if (enemy != null) {
            actionState = AUTO_ATTACKING;
            return;
        }

        pathQueue.clear();
        pathQueue.addAll(movementHandler.buildPath(destination));

        actionState = pathQueue.isEmpty() ? IDLE : MOVING;
        actionStartTime = -1;
    }


    /**
     * Queues an ability cast; the actual firing is deferred by the cast delay
     * inside {@link #updateCasting()}.
     *
     * @param slot   which ability slot was activated (Q or W)
     * @param mouseX client mouse X at time of cast
     * @param mouseY client mouse Y at time of cast
     */
    public void useAbility(AbilitySlot slot, int mouseX, int mouseY) {
        if (champion == null || actionState == DEAD || actionState == CASTING) return;

        // Prevent casting when ability is on cooldown
        if (slot == AbilitySlot.Q && !champion.isQReady()) return;
        if (slot == AbilitySlot.W && !champion.isWReady()) return;

        if (champion.getQAbility() instanceof InstantAbility && slot == AbilitySlot.Q) {
            champion.useQ(id, x, y, mouseX, mouseY, game);
            return;
        }

        if (champion.getWAbility() instanceof InstantAbility && slot == AbilitySlot.W) {
            champion.useW(id, x, y, mouseX, mouseY, game);
            return;
        }

        pendingAbilitySlot = slot;
        pendingMouseX = mouseX;
        pendingMouseY = mouseY;

        actionStartTime = System.currentTimeMillis();

        actionState = CASTING;
    }


    public void takeDamage(int damageAmount) {
        takeDamage(damageAmount, null);
    }

    public void takeDamage(int damageAmount, Player attacker) {
        if (isDestroyed()) {
            return;
        }

        boolean wasAlive = lives > 0;
        lives = Math.max(0, lives - damageAmount);

        if (attacker != null && attacker.getId() != id) {
            game.notifyChampionDamagedUnderTurret(this, attacker);
        }

        if (wasAlive && lives == 0) {
            deathCount++;
            if (attacker != null && attacker.getId() != id) {
                attacker.killCount++;
            }
        }
    }

    public void addXp(int amount) {
        if (amount <= 0 || level >= MAX_PLAYER_LEVEL) return;
        currentXp += amount;
        while (level < MAX_PLAYER_LEVEL && currentXp >= xpForNextLevel) {
            currentXp -= xpForNextLevel;
            int oldMaxHp = getMaxHp();
            level++;
            int newMaxHp = getMaxHp();
            lives += (newMaxHp - oldMaxHp);
            xpForNextLevel = calculateXpForNextLevel(level);
        }
        if (level >= MAX_PLAYER_LEVEL) {
            level = MAX_PLAYER_LEVEL;
            currentXp = xpForNextLevel;
        }
    }

    private void applyPassiveHealthRegeneration() {
        int maxHp = getMaxHp();
        long now = System.currentTimeMillis();

        if (lives <= 0 || lives >= maxHp) {
            lastPassiveRegenTime = now;
            return;
        }

        while (now - lastPassiveRegenTime >= 1000) {
            int healAmount = champion.getHpRegenPerSecond();
            lives = Math.min(maxHp, lives + healAmount);
            lastPassiveRegenTime += 1000;

            if (lives >= maxHp) {
                break;
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return lives <= 0;
    }

    public void setBaseSpawnPosition(float spawnX, float spawnY) {
        baseSpawnX = spawnX;
        baseSpawnY = spawnY;
        teleportTo(spawnX, spawnY);
    }

    public void teleportTo(float spawnX, float spawnY) {
        this.x = spawnX;
        this.y = spawnY;

        if (body != null) {
            body.setTransform(spawnX / PPM, spawnY / PPM, 0f);
            body.setLinearVelocity(0f, 0f);
        }
    }


    /**
     * Returns the current state of the player, consisting of their position and remaining lives.
     */
    public PlayerState getState() {
        PlayerState playerState = new PlayerState();
        playerState.setId(connection.getID());


        playerState.setX(body.getPosition().x * PPM); // Gets players position
        playerState.setY(body.getPosition().y * PPM);

        playerState.setHp(lives);
        playerState.setMaxHp(champion.getMaxHpAtLevel(level));
        playerState.setLives(lives);
        playerState.setKillCount(killCount);
        playerState.setDeathCount(deathCount);
        playerState.setLevel(level);
        playerState.setCurrentXp(currentXp);
        playerState.setXpForNextLevel(xpForNextLevel);
        playerState.setName(name);
        playerState.setTeam(team);
        playerState.setChampionType(champion.getType());
        playerState.setActionState(actionState);
        playerState.setDirection(direction);
        playerState.setAutoAttackActive(autoAttackActive);
        playerState.setAutoAttackSequence(autoAttackSequence);
        playerState.setTankHealSequence(tankHealSequence);
        playerState.setQCooldownRemainingMs(champion.getQCooldownRemainingMs());
        playerState.setQCooldownTotalMs(champion.getQCooldownTotalMs());
        playerState.setWCooldownRemainingMs(champion.getWCooldownRemainingMs());
        playerState.setWCooldownTotalMs(champion.getWCooldownTotalMs());
        playerState.setCastAbility(castAbility);
        castAbility = null;

        return playerState;
    }

    private void attemptRespawn() {
        if (deathTime == -1L || System.currentTimeMillis() - deathTime < RESPAWN_DELAY_MS) {
            return;
        }

        lives = champion.getMaxHpAtLevel(level);
        deathTime = -1L;
        lastPassiveRegenTime = System.currentTimeMillis();
        enemy = null;
        autoAttackActive = false;
        actionStartTime = -1L;
        pathQueue.clear();
        actionState = IDLE;
        teleportTo(baseSpawnX, baseSpawnY);
    }


    /**
     * Removes the movement and ability listeners from the player's connection.
     * This should be called when the player disconnects or the game ends.
     * Disposing of the listeners prevents potential thread exceptions when reusing
     * same connections for future game instances.
     */
    public void dispose() {
        connection.removeListener(movementListener);
        connection.removeListener(abilityListener);
    }

    private int calculateXpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_PLAYER_LEVEL) {
            return BASE_XP_TO_LEVEL + (MAX_PLAYER_LEVEL - 1) * XP_TO_LEVEL_GROWTH;
        }
        return BASE_XP_TO_LEVEL + (currentLevel - 1) * XP_TO_LEVEL_GROWTH;
    }

    public int getMaxHp() {
        return champion.getMaxHpAtLevel(level);
    }
    public int getAutoAttackDamage() {
        return champion.getAutoAttackDamageAtLevel(level);
    }

}
