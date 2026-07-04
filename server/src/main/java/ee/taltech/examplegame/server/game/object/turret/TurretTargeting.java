package ee.taltech.examplegame.server.game.object.turret;

import com.badlogic.gdx.math.Vector2;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.HomingBullet;
import ee.taltech.examplegame.server.game.object.Minion;
import ee.taltech.examplegame.server.game.object.Player;

import static constant.Constants.BULLET_HEIGHT_IN_PIXELS;
import static constant.Constants.BULLET_WIDTH_IN_PIXELS;
import static constant.Constants.TURRET_DMG;
import static constant.Constants.TURRET_RANGE;
import static ee.taltech.examplegame.server.util.VectorUtils.toUnitVector;

public final class TurretTargeting {
    private TurretTargeting() {}

    public static void updateTurretFire(Turret turret, GameInstance game) {
        if (turret.isDestroyed()) {
            return;
        }

        AttackableTarget target = turret.getCurrentTarget();
        if (!isValidTarget(turret, target)) {
            target = acquireTarget(turret, game);
            turret.setCurrentTarget(target);
        }

        if (target == null) {
            turret.clearCurrentTarget();
            return;
        }

        if (!turret.isReadyToShoot()) {
            return;
        }

        Vector2 direction = toUnitVector(turret.getX(), turret.getY(), target.getX(), target.getY());
        float spawnX = turret.getX() - BULLET_WIDTH_IN_PIXELS / 2f;
        float spawnY = turret.getY() - BULLET_HEIGHT_IN_PIXELS / 2f;
        game.addBullet(new HomingBullet(spawnX, spawnY, direction.x, direction.y, -1, turret.getTeam(), target));
        turret.markShot();
    }

    public static boolean isTargetInRange(Turret turret, AttackableTarget target) {
        if (turret == null || target == null) {
            return false;
        }

        float dx = target.getX() - turret.getX();
        float dy = target.getY() - turret.getY();
        return dx * dx + dy * dy <= TURRET_RANGE * TURRET_RANGE;
    }

    private static boolean isValidTarget(Turret turret, AttackableTarget target) {
        return target != null
                && !target.isDestroyed()
                && target.getTeam() != turret.getTeam()
                && isTargetInRange(turret, target);
    }

    private static AttackableTarget acquireTarget(Turret turret, GameInstance game) {
        Minion enemyMinion = findNearestMinion(turret, game);
        if (enemyMinion != null) {
            return enemyMinion;
        }

        return findNearestEnemyPlayer(turret, game);
    }

    private static Player findNearestEnemyPlayer(Turret turret, GameInstance game) {
        Player closest = null;
        float closestDistanceSquared = TURRET_RANGE * TURRET_RANGE;

        for (Player player : game.getPlayers()) {
            if (player.getTeam() == turret.getTeam() || player.isDestroyed()) {
                continue;
            }

            float dx = player.getX() - turret.getX();
            float dy = player.getY() - turret.getY();
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared <= closestDistanceSquared) {
                closest = player;
                closestDistanceSquared = distanceSquared;
            }
        }

        return closest;
    }

    private static Minion findNearestMinion(Turret turret, GameInstance game) {
        Minion closestMinion = null;
        float closestDistanceSquared = TURRET_RANGE * TURRET_RANGE;

        for (Minion minion : game.getMinions()) {
            if (minion.getTeam() == turret.getTeam() || minion.isDestroyed()) {
                continue;
            }
            float dx = minion.getX() - turret.getX();
            float dy = minion.getY() - turret.getY();
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared <= closestDistanceSquared) {
                closestMinion = minion;
                closestDistanceSquared = distanceSquared;
            }
        }
        return closestMinion;
    }
}
