package ee.taltech.examplegame.server.game.object;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import ee.taltech.examplegame.server.util.TargetUtils;
import lombok.Getter;
import lombok.Setter;
import message.dto.ActionState;
import message.dto.Direction;
import message.dto.MinionState;
import message.dto.Team;

import java.util.LinkedList;
import java.util.Queue;

import static constant.Constants.MINION_AGGRO_RANGE;
import static constant.Constants.MINION_ATTACK_COOLDOWN;
import static constant.Constants.MINION_ATTACK_RANGE;
import static constant.Constants.MINION_COLLISION_HEIGHT_IN_PIXELS;
import static constant.Constants.MINION_COLLISION_WIDTH_IN_PIXELS;
import static constant.Constants.MINION_DMG;
import static constant.Constants.MINION_HP;
import static constant.Constants.PLAYER_CATEGORY;
import static constant.Constants.PLAYER_MASK;
import static constant.Constants.PPM;

@Getter
@Setter
public class Minion implements AttackableTarget {
    // A noticeably closer enemy may steal aggro even if the current target is still valid.
    private static final float TARGET_SWITCH_MARGIN = 60f;
    // Being hit by a player temporarily overrides normal "nearest enemy" target selection.
    private static final long FORCED_AGGRO_DURATION_MS = 2_500L;
    // The nexus can visually look "reached" before the small server-side collision box says it is.
    private static final float NEXUS_ATTACK_LEEWAY = 8f;
    // Keep dead minions replicated briefly so the client can play the death animation.
    private static final long DEATH_ANIMATION_DURATION_MS = 1_000L;

    private final int id;
    private final GameInstance game;
    private final World world;
    private final Team team;
    private final float laneTargetX;
    private final float laneTargetY;
    private final long spawnTime = System.currentTimeMillis();

    private Body body;
    private float x;
    private float y;
    private int hp = MINION_HP;
    private final int maxHp = MINION_HP;
    private long lastAttackTime;
    private long lastRepathTime;
    private long forcedAggroUntil;
    private ActionState actionState = ActionState.IDLE;
    private final Queue<Vector2> pathQueue = new LinkedList<>();
    private Direction direction = Direction.DOWN;
    private AttackableTarget enemy;
    private AttackableTarget forcedAggroTarget;
    private final MinionMovementHandler movementHandler;
    private long deathTime = -1L;

    /**
     * Server-side NPC unit that pushes down the lane, reacquires nearby enemies,
     * and falls back to marching toward the opposing base when nothing is in aggro range.
     */
    public Minion(GameInstance game, World world, int id, Team team, float spawnX, float spawnY, float laneTargetX, float laneTargetY) {
        this.game = game;
        this.world = world;
        this.id = id;
        this.team = team;
        this.x = spawnX;
        this.y = spawnY;
        this.laneTargetX = laneTargetX;
        this.laneTargetY = laneTargetY;
        this.movementHandler = new MinionMovementHandler(this, game);
        defineMinionBody();
    }

    /**
     * Creates the Box2D body used for movement and collisions.
     */
    private void defineMinionBody() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(x / PPM, y / PPM);
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        body = world.createBody(bodyDef);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.friction = 0.0f;
        fixtureDef.density = 1.5f;
        fixtureDef.restitution = 0.0f;
        fixtureDef.filter.categoryBits = PLAYER_CATEGORY;
        fixtureDef.filter.maskBits = PLAYER_MASK;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(MINION_COLLISION_WIDTH_IN_PIXELS / (2f * PPM), MINION_COLLISION_HEIGHT_IN_PIXELS / (2f * PPM));
        fixtureDef.shape = shape;
        body.createFixture(fixtureDef);
        body.setUserData(this);
        shape.dispose();
    }

    /**
     * Runs one server tick of simple minion AI:
     * sync physics position, pick or keep a target, then either attack or move.
     */
    public void update() {
        if (isDestroyed()) {
            enterDeadState();
            return;
        }

        syncPosition();
        updateEnemySelection();

        if (enemy != null) {
            float attackRange = getEffectiveAttackRange(enemy);
            if (TargetUtils.isTargetInRange(x, y, enemy, attackRange)) {
                actionState = ActionState.AUTO_ATTACKING;
                body.setLinearVelocity(0f, 0f);
                updateDirection(enemy.getX(), enemy.getY());
                attemptAttack();
            } else {
                Vector2 movementTarget = movementHandler.resolveMovementTarget(enemy);
                movementHandler.moveToward(movementTarget.x, movementTarget.y);
            }
            return;
        }

        movementHandler.moveToward(laneTargetX, laneTargetY);
    }

    /**
     * Removes the physics body when this minion is cleaned up from the game.
     */
    public void dispose() {
        if (body != null) {
            world.destroyBody(body);
            body = null;
        }
    }

    public MinionState getState() {
        MinionState minionState = new MinionState();
        minionState.setId(id);
        minionState.setX(x);
        minionState.setY(y);
        minionState.setHp(hp);
        minionState.setMaxHp(maxHp);
        minionState.setTeam(team);
        minionState.setDirection(direction);
        minionState.setActionState(actionState);
        minionState.setDestroyed(isDestroyed());
        return minionState;
    }

    public boolean isReadyToDespawn() {
        return actionState == ActionState.DEAD
                && deathTime > 0L
                && System.currentTimeMillis() - deathTime >= DEATH_ANIMATION_DURATION_MS;
    }

    /**
     * Keeps the cached pixel coordinates in sync with the Box2D body.
     */
    private void syncPosition() {
        if (body == null) {
            return;
        }

        x = body.getPosition().x * PPM;
        y = body.getPosition().y * PPM;
    }

    private void enterDeadState() {
        if (actionState == ActionState.DEAD) {
            return;
        }

        syncPosition();
        actionState = ActionState.DEAD;
        enemy = null;
        forcedAggroTarget = null;
        pathQueue.clear();
        deathTime = System.currentTimeMillis();
        dispose();
    }

    private boolean isValidEnemy(AttackableTarget target) {
        return target != null
                && !target.isDestroyed()
                && target.getTeam() != null
                && target.getTeam() != team;
    }

    /**
     * Keeps the current target if it is still relevant; otherwise searches for a new nearby enemy.
     * Recently damaged minions temporarily lock onto the attacker before returning to normal aggro.
     */
    private void updateEnemySelection() {
        long now = System.currentTimeMillis();
        AttackableTarget bestCandidate = findNearestEnemy();

        if (forcedAggroTarget != null) {
            boolean forcedAggroExpired = now > forcedAggroUntil;
            if (!forcedAggroExpired && isValidEnemy(forcedAggroTarget)) {
                enemy = forcedAggroTarget;
                return;
            }

            forcedAggroTarget = null;
        }

        if (bestCandidate == null) {
            enemy = null;
            return;
        }

        if (enemy != null && isValidEnemy(enemy)) {
            float currentDistance = TargetUtils.distanceToAttackableTarget(x, y, enemy);
            float candidateDistance = TargetUtils.distanceToAttackableTarget(x, y, bestCandidate);

            boolean currentTooFar = currentDistance > MINION_AGGRO_RANGE * 1.4f;
            boolean candidateClearlyBetter = candidateDistance + TARGET_SWITCH_MARGIN < currentDistance;

            if (!currentTooFar && !candidateClearlyBetter) {
                return;
            }
        }

        enemy = bestCandidate;
    }

    /**
     * Chooses the nearest valid enemy in aggro range.
     * Mobile combat targets are preferred first; if none are nearby, the minion advances
     * to lane objectives by targeting the enemy turret before the nexus.
     */
    private AttackableTarget findNearestEnemy() {
        AttackableTarget combatTarget = findNearestCombatEnemy();
        if (combatTarget != null) {
            return combatTarget;
        }

        AttackableTarget structureTarget = findNearestStructureTarget();
        if (structureTarget != null) {
            return structureTarget;
        }

        return null;
    }

    /**
     * Enemy minions and players are treated as combat targets that can pull a minion off the lane.
     */
    private AttackableTarget findNearestCombatEnemy() {
        AttackableTarget closest = null;
        float closestDistanceSquared = MINION_AGGRO_RANGE * MINION_AGGRO_RANGE;

        for (Minion minion : game.getMinions()) {
            if (minion == this || !isValidEnemy(minion)) {
                continue;
            }

            float distanceSquared = squaredDistance(minion.getX(), minion.getY());
            if (distanceSquared < closestDistanceSquared) {
                closest = minion;
                closestDistanceSquared = distanceSquared;
            }
        }

        for (Player player : game.getPlayers()) {
            if (!isValidEnemy(player)) {
                continue;
            }

            float distanceSquared = squaredDistance(player.getX(), player.getY());
            if (distanceSquared < closestDistanceSquared) {
                closest = player;
                closestDistanceSquared = distanceSquared;
            }
        }

        return closest;
    }

    /**
     * Lane objectives: destroy the enemy turret first, then move on to the nexus.
     */
    private AttackableTarget findNearestStructureTarget() {
        AttackableTarget closest = null;
        float closestDistanceSquared = Float.MAX_VALUE;

        for (Turret turret : game.getTurrets()) {
            if (!isValidEnemy(turret)) {
                continue;
            }

            float distanceSquared = squaredDistance(turret.getX(), turret.getY());
            if (distanceSquared < closestDistanceSquared) {
                closest = turret;
                closestDistanceSquared = distanceSquared;
            }
        }

        if (closest != null) {
            return closest;
        }

        for (Nexus nexus : game.getNexuses()) {
            if (!isValidEnemy(nexus) || !TargetUtils.isNexusVulnerable(nexus, game)) {
                continue;
            }

            float distanceSquared = squaredDistance(nexus.getX(), nexus.getY());
            if (distanceSquared < closestDistanceSquared) {
                closest = nexus;
            }
        }

        return closest;
    }

    private float squaredDistance(float targetX, float targetY) {
        float dx = targetX - x;
        float dy = targetY - y;
        return dx * dx + dy * dy;
    }

    private float getEffectiveAttackRange(AttackableTarget target) {
        if (target instanceof Nexus) {
            return MINION_ATTACK_RANGE + NEXUS_ATTACK_LEEWAY;
        }
        return MINION_ATTACK_RANGE;
    }

    /**
     * Applies melee damage when the attack cooldown has elapsed.
     */
    private void attemptAttack() {
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < MINION_ATTACK_COOLDOWN) {
            return;
        }

        if (enemy != null && !enemy.isDestroyed()) {
            enemy.takeDamage(MINION_DMG);
            lastAttackTime = now;
        }
    }

    /**
     * Temporarily redirects aggro to the attacker so players can peel minions off another target.
     */
    public void onAttackedBy(AttackableTarget attacker) {
        if (!isValidEnemy(attacker)) {
            return;
        }

        forcedAggroTarget = attacker;
        forcedAggroUntil = System.currentTimeMillis() + FORCED_AGGRO_DURATION_MS;
        enemy = attacker;
    }

    /**
     * Picks a facing direction from the dominant axis toward the next target point.
     */
    private void updateDirection(float destinationX, float destinationY) {
        float dx = destinationX - x;
        float dy = destinationY - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        } else if (Math.abs(dy) > 0f) {
            direction = dy >= 0 ? Direction.UP : Direction.DOWN;
        }
    }

    @Override
    public void takeDamage(int damageAmount) {
        if (isDestroyed()) {
            return;
        }

        hp = Math.max(0, hp - damageAmount);
    }

    @Override
    public boolean isDestroyed() {
        return hp <= 0;
    }
}
