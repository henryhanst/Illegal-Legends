package ee.taltech.examplegame.server.game.object.champion;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.Ability;
import lombok.Getter;
import message.dto.ChampionType;

/**
 * Base class for all champion types in the game.
 *
 * A champion defines core combat attributes such as health, damage and speed,
 * and provides abstract ability methods (Q, W, auto attack) that concrete
 * champion implementations must define.
 *
 */
@Getter
public abstract class Champion {
    private final int baseHp;
    private final int hpPerLevel;
    private final int baseAutoAttackDamage;
    private final int damagePerLevel;
    private final int speed;
    private final int hpRegenPerSecond;

    public abstract boolean isQReady();
    public abstract boolean isWReady();
    public abstract long getQCooldownRemainingMs();
    public abstract long getQCooldownTotalMs();
    public abstract long getWCooldownRemainingMs();
    public abstract long getWCooldownTotalMs();
    public abstract Ability getQAbility();
    public abstract Ability getWAbility();

    protected Champion(int baseHp, int hpPerLevel, int baseAutoAttackDamage, int damagePerLevel, int speed, int hpRegenPerSecond) {
        this.baseHp = baseHp;
        this.hpPerLevel = hpPerLevel;
        this.baseAutoAttackDamage = baseAutoAttackDamage;
        this.damagePerLevel = damagePerLevel;
        this.speed = speed;
        this.hpRegenPerSecond = hpRegenPerSecond;
    }

    /**
     * Execute the champion's Q ability.
     *
     * @param casterId ID of the player using the ability
     * @param casterX current X coordinate of the caster
     * @param casterY current Y coordinate of the caster
     * @param targetX X coordinate of the target location
     * @param targetY Y coordinate of the target location
     * @param game current game instance
     */
    public abstract void useQ(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game);

    /**
     * Execute the champion's Q ability.
     *
     * @param casterId ID of the player using the ability
     * @param casterX current X coordinate of the caster
     * @param casterY current Y coordinate of the caster
     * @param targetX X coordinate of the target location
     * @param targetY Y coordinate of the target location
     * @param game current game instance
     */
    public abstract void useW(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game);

    /**
     * Performs a basic attack against an enemy (player, minion, tower).
     *
     * @param caster the player performing the attack
     * @param enemy the target player
     * @param game the game instance
     */
    public abstract void autoAttack(Player caster, AttackableTarget enemy, GameInstance game);

    public abstract ChampionType getType();

    public int getMaxHpAtLevel(int level) {
        return baseHp + (level - 1) * hpPerLevel;
    }

    public int getAutoAttackDamageAtLevel(int level) {
        return baseAutoAttackDamage + (level - 1) * damagePerLevel;
    }
}
