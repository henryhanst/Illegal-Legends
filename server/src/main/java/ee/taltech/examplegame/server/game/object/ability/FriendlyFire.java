package ee.taltech.examplegame.server.game.object.ability;

import constant.Constants;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Player;
import message.dto.Team;

/**
 * Shared helper interface for components that need to decide whether one player
 * is allowed to hit another player based on friendly-fire.
 *
 * <p>The default methods keep the team-check logic in one place while still
 * letting ability base classes reuse it through inheritance.
 */
public interface FriendlyFire {
    /**
     * Checks whether the given attacker is allowed to hit the target.
     *
     * @param attacker the player causing the hit
     * @param target the player receiving the hit
     * @return {@code true} when the hit is valid
     */
    default boolean canHit(Player attacker, Player target) {
        if (attacker == null || target == null) {
            return false;
        }

        return canHit(attacker.getId(), attacker.getTeam(), target);
    }

    /**
     * Checks whether the given attacker is allowed to hit the target.
     * Non-player targets are always valid if they exist.
     *
     * @param attacker the player causing the hit
     * @param target the target receiving the hit
     * @return {@code true} when the hit is valid
     */
    default boolean canHit(Player attacker, AttackableTarget target) {
        if (attacker == null || target == null) {
            return false;
        }

        if (target instanceof Player playerTarget) {
            return canHit(attacker, playerTarget);
        }

        return true;
    }

    /**
     * Checks whether a hit is valid when only attacker identity and team are available.
     *
     * @param attackerId the id of the attacker
     * @param attackerTeam the team of the attacker
     * @param target the player receiving the hit
     * @return {@code true} when the hit is valid
     */
    default boolean canHit(int attackerId, Team attackerTeam, Player target) {
        if (target == null) {
            return false;
        }

        if (attackerId == target.getId()) {
            return false;
        }

        if (!Constants.FRIENDLY_FIRE && attackerTeam != null && attackerTeam == target.getTeam()) {
            return false;
        }

        return true;
    }
}
