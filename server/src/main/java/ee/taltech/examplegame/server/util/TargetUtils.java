package ee.taltech.examplegame.server.util;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Minion;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.FriendlyFire;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import message.PlayerMovementMessage;

import static constant.Constants.*;

public class TargetUtils {
    // Reuse the same hit-validation rule for auto-target selection.
    private static final FriendlyFire FRIENDLY_FIRE = new FriendlyFire() {};

    private TargetUtils(){}
    /**
     * Finds the closest valid target to the cursor position from the movement message,
     * within {MELEE_RANGE} pixels. Enemy players, minions, turrets, and a vulnerable enemy
     * nexus are considered valid targets. Returns {null} if nothing is close enough.
     *
     * @param targetPos  The melee message containing the cursor's world position.
     * @param attacker   The player who is trying to target someone.
     * @param game       Current game state, including structures.
     * @return The targeted attackable target, or null if nothing is in range.
     */
    public static AttackableTarget findTarget(PlayerMovementMessage targetPos, Player attacker, GameInstance game) {
        float cursorX = targetPos.getX();
        float cursorY = targetPos.getY();

        AttackableTarget closest = null;
        float closestDistance = Float.MAX_VALUE;

        for (Player p : game.getPlayers()) {
            // Do not lock onto targets that this attacker is not allowed to hit.
            if (!FRIENDLY_FIRE.canHit(attacker, p)) continue;

            float dist = distanceToRectangle(cursorX, cursorY, p.getX(), p.getY(), PLAYER_WIDTH_IN_PIXELS, PLAYER_HEIGHT_IN_PIXELS);

            if (dist < closestDistance) {
                closest = p;
                closestDistance = dist;
            }
        }

        for (Minion minion : game.getMinions()) {
            if (minion.getTeam() == attacker.getTeam() || minion.isDestroyed()) {
                continue;
            }

            float dist = distanceToRectangle(
                    cursorX,
                    cursorY,
                    minion.getX(),
                    minion.getY(),
                    MINION_WIDTH_IN_PIXELS,
                    MINION_HEIGHT_IN_PIXELS
            );

            if (dist < closestDistance) {
                closest = minion;
                closestDistance = dist;
            }
        }

        for (Nexus nexus : game.getNexuses()) {
            if (nexus.getTeam() == attacker.getTeam() || nexus.isDestroyed() || !isNexusVulnerable(nexus, game)) {
                continue;
            }

            float dist = distanceToRectangle(
                    cursorX,
                    cursorY,
                    nexus.getX(),
                    nexus.getY(),
                    NEXUS_COLLISION_WIDTH_IN_PIXELS,
                    NEXUS_COLLISION_HEIGHT_IN_PIXELS
            );

            if (dist < closestDistance) {
                closest = nexus;
                closestDistance = dist;
            }
        }

        for (Turret turret : game.getTurrets()) {
            if (turret.getTeam() == attacker.getTeam() || turret.isDestroyed()) {
                continue;
            }

            float dist = distanceToRectangle(
                    cursorX,
                    cursorY,
                    turret.getX(),
                    turret.getY() + TURRET_COLLISION_OFFSET_Y_IN_PIXELS,
                    TURRET_COLLISION_WIDTH_IN_PIXELS,
                    TURRET_COLLISION_HEIGHT_IN_PIXELS
            );

            if (dist < closestDistance) {
                closest = turret;
                closestDistance = dist;
            }
        }

        return closestDistance <= MELEE_RANGE ? closest : null;
    }

    /**
     * Find if target is in melee range of attacker
     * @param attacker attacking Player entity
     * @param target target entity
     * @return boolean
     */
    public static boolean nearTarget(Player attacker, AttackableTarget target) {
        return isTargetInRange(attacker.getX(), attacker.getY(), target, MELEE_RANGE);
    }

    public static boolean isTargetInRange(float attackerX, float attackerY, AttackableTarget target, float range) {
        return distanceToAttackableTarget(attackerX, attackerY, target) <= range;
    }

    public static float distanceToAttackableTarget(float pointX, float pointY, AttackableTarget target) {
        // Half the player's physical width. Subtracting this gives us true EDGE-TO-EDGE distance!
        float playerRadius = PLAYER_COLLISION_WIDTH_IN_PIXELS / 2f;
        float distanceToTarget;

        if (target instanceof Nexus nexus) {
            distanceToTarget = distanceToRectangle(
                    pointX,
                    pointY,
                    nexus.getX(),
                    nexus.getY(),
                    NEXUS_ATTACK_WIDTH_IN_PIXELS,
                    NEXUS_ATTACK_HEIGHT_IN_PIXELS
            );
        } else if (target instanceof Turret turret) {
            distanceToTarget = distanceToRectangle(
                    pointX,
                    pointY,
                    turret.getX(),
                    turret.getY() + TURRET_COLLISION_OFFSET_Y_IN_PIXELS,
                    TURRET_COLLISION_WIDTH_IN_PIXELS,
                    TURRET_COLLISION_HEIGHT_IN_PIXELS
            );
        } else if (target instanceof Minion minion) {
            distanceToTarget = distanceToRectangle(
                    pointX,
                    pointY,
                    minion.getX(),
                    minion.getY(),
                    MINION_COLLISION_WIDTH_IN_PIXELS,
                    MINION_COLLISION_HEIGHT_IN_PIXELS
            );
        } else {
            distanceToTarget = distanceToRectangle(
                    pointX,
                    pointY,
                    target.getX(),
                    target.getY(),
                    PLAYER_COLLISION_WIDTH_IN_PIXELS,
                    PLAYER_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        return Math.max(distanceToTarget - playerRadius, 0f);
    }

    public static float distanceToRectangle(float pointX, float pointY, float centerX, float centerY, float width, float height) {
        float dx = Math.max(Math.abs(pointX - centerX) - width / 2f, 0f);
        float dy = Math.max(Math.abs(pointY - centerY) - height / 2f, 0f);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static boolean isNexusVulnerable(Nexus nexus, GameInstance game) {
        return game.getTurrets().stream()
                .filter(turret -> turret.getTeam() == nexus.getTeam())
                .findFirst()
                .map(Turret::isDestroyed)
                .orElse(true);
    }
}
