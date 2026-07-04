package ee.taltech.examplegame.server.game;

import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.object.Bullet;
import ee.taltech.examplegame.server.game.object.Minion;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import ee.taltech.examplegame.server.game.object.ability.FriendlyFire;

import java.util.ArrayList;
import java.util.List;

import static constant.Constants.*;

/**
 * Handles bullet collisions with players, arena boundaries, and map collisions.
 */
public class BulletCollisionHandler implements FriendlyFire {

    /**
     * Checks for collisions between bullets and players, removes bullets that hit players or moved out of bounds.
     *
     * @param bullets The list of bullets in the game.
     * @param players The list of players to check for collisions.
     * @return A list of remaining bullets after handling collisions.
     */
    public List<Bullet> handleCollisions(List<Bullet> bullets, List<Player> players, List<Nexus> nexuses, List<Turret> turrets, List<Minion> minions) {
        List<Bullet> bulletsToBeRemoved = new ArrayList<>();

        for (Player player : players) {
            if (player.isDestroyed()) {
                continue;
            }

            Rectangle hitBox = constructPlayerHitBox(player);
            for (Bullet bullet : bullets) {
                if (hitBox.contains(bullet.getX(), bullet.getY())
                        && canHit(bullet.getShotById(), bullet.getShotByTeam(), player)) {
                    Player attacker = findAttackingPlayer(bullet, players);
                    player.takeDamage(bullet.getDamage(), attacker);

                    if (bullet.getOnHitEffect() != null) {
                        player.applyEffect(bullet.getOnHitEffect());
                    }
                    bulletsToBeRemoved.add(bullet);
                    Log.info("Player with id " + player.getId() + " was hit. " + player.getLives() + " lives left.");
                }
            }
        }

        for (Nexus nexus : nexuses) {
            if (nexus.isDestroyed() || !isNexusVulnerable(nexus, turrets)) {
                continue;
            }

            Rectangle hitBox = constructNexusHitBox(nexus);
            for (Bullet bullet : bullets) {
                if (bullet.getShotByTeam() == nexus.getTeam()) {
                    continue;
                }

                if (hitBox.contains(bullet.getX(), bullet.getY())) {
                    nexus.takeDamage(bullet.getDamage());
                    bulletsToBeRemoved.add(bullet);
                    Log.info("Nexus for team " + nexus.getTeam() + " was hit. " + nexus.getHp() + " hp left.");
                }
            }
        }

        for (Turret turret : turrets) {
            if (turret.isDestroyed()) {
                continue;
            }

            Rectangle hitBox = constructTurretHitBox(turret);
            for (Bullet bullet : bullets) {
                if (bullet.getShotByTeam() == turret.getTeam()) {
                    continue;
                }

                if (hitBox.contains(bullet.getX(), bullet.getY())) {
                    turret.takeDamage(bullet.getDamage());
                    bulletsToBeRemoved.add(bullet);
                    Log.info("Turret for team " + turret.getTeam() + " was hit. " + turret.getHp() + " hp left.");
                }
            }
        }

        for (Minion minion : minions) {
            if (minion.isDestroyed()) {
                continue;
            }
            Rectangle hitBox = constructMinionHitBox(minion);
            for (Bullet bullet : bullets) {
                if (bullet.getShotByTeam() == minion.getTeam() || bullet.getOnHitEffect() != null) {
                    continue;
                }

                if (hitBox.contains(bullet.getX(), bullet.getY())) {
                    minion.takeDamage(bullet.getDamage());
                    Player attacker = findAttackingPlayer(bullet, players);
                    if (attacker != null) {
                        minion.onAttackedBy(attacker);
                    }
                    bulletsToBeRemoved.add(bullet);
                }
            }
        }

        bulletsToBeRemoved.addAll(findOutOfRangeBullets(bullets));
        bullets.removeAll(bulletsToBeRemoved);  // remove bullets that hit a player or move out of bounds
        return bullets;
    }

    /**
     * Finds bullets that are out of the arena bounds.
     *
     * @param bullets All active bullets.
     * @return Bullets that are out of bounds.
     */
    private List<Bullet> findOutOfRangeBullets(List<Bullet> bullets) {
        List<Bullet> outOfRangeBullets = new ArrayList<>();
        for (Bullet bullet : bullets) {
            if (bullet.hasExceededRange()) {
                outOfRangeBullets.add(bullet);
            }
        }
        return outOfRangeBullets;
    }

    /**
     * Constructs a rectangular hitbox for a player based on their position.
     * A hitbox is essential for detecting collisions between players and bullets.
     * Only bullets that visually overlap with the player's sprite register as hits.
     */
    private Rectangle constructPlayerHitBox(Player player) {
        return
            new Rectangle(
                (int) (player.getX() - PLAYER_WIDTH_IN_PIXELS / 2f),
                (int) (player.getY() - PLAYER_HEIGHT_IN_PIXELS / 2f),
                (int) PLAYER_WIDTH_IN_PIXELS,  // rectangle width
                (int) PLAYER_HEIGHT_IN_PIXELS  // rectangle height
            );
    }

    private Rectangle constructNexusHitBox(Nexus nexus) {
        return new Rectangle(
                nexus.getX() - NEXUS_COLLISION_WIDTH_IN_PIXELS / 2f,
                nexus.getY() - NEXUS_COLLISION_HEIGHT_IN_PIXELS / 2f,
                NEXUS_COLLISION_WIDTH_IN_PIXELS,
                NEXUS_COLLISION_HEIGHT_IN_PIXELS
        );
    }

    private Rectangle constructTurretHitBox(Turret turret) {
        float collisionCenterY = turret.getY() + TURRET_COLLISION_OFFSET_Y_IN_PIXELS;
        return new Rectangle(
                turret.getX() - TURRET_COLLISION_WIDTH_IN_PIXELS / 2f,
                collisionCenterY - TURRET_COLLISION_HEIGHT_IN_PIXELS / 2f,
                TURRET_COLLISION_WIDTH_IN_PIXELS,
                TURRET_COLLISION_HEIGHT_IN_PIXELS
        );
    }
    private Rectangle constructMinionHitBox(Minion minion) {
        return new Rectangle(
                minion.getX() - MINION_COLLISION_WIDTH_IN_PIXELS / 2f,
                minion.getY() - MINION_COLLISION_HEIGHT_IN_PIXELS / 2f,
                MINION_COLLISION_WIDTH_IN_PIXELS,
                MINION_COLLISION_HEIGHT_IN_PIXELS
        );
    }

    /**
     * Resolves the player that spawned the bullet so minions can swap aggro to that attacker.
     */
    private Player findAttackingPlayer(Bullet bullet, List<Player> players) {
        for (Player player : players) {
            if (player.getId() == bullet.getShotById()) {
                return player;
            }
        }

        return null;
    }

    private boolean isNexusVulnerable(Nexus nexus, List<Turret> turrets) {
        return turrets.stream()
                .filter(turret -> turret.getTeam() == nexus.getTeam())
                .findFirst()
                .map(Turret::isDestroyed)
                .orElse(true);
    }
}
