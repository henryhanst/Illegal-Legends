package ee.taltech.examplegame.server.game.object.champion;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.Ability;
import ee.taltech.examplegame.server.game.object.ability.abilities.HealAbility;
import ee.taltech.examplegame.server.game.object.ability.abilities.PunchAbility;
import ee.taltech.examplegame.server.game.object.ability.abilities.RageAbility;
import message.dto.ChampionType;

import static constant.Constants.*;


public class Tank extends Champion {
    private final PunchAbility punchAbility = new PunchAbility();
    private final HealAbility healAbility = new HealAbility();
    private final RageAbility rageAbility = new RageAbility();

    public Tank() {
        super(TANK_HP, TANK_HP_PER_LEVEL, TANK_DMG, TANK_DMG_PER_LEVEL, TANK_SPEED, TANK_HP_REGEN_PER_SECOND);
    }

    @Override
    public ChampionType getType() {
        return ChampionType.TANK;
    }

    @Override
    public boolean isQReady() {
        return rageAbility.isReady();
    }

    @Override
    public boolean isWReady() {
        return healAbility.isReady();
    }

    @Override
    public long getQCooldownRemainingMs() {
        return rageAbility.getRemainingCooldownMs();
    }

    @Override
    public long getQCooldownTotalMs() {
        return rageAbility.getCooldownMs();
    }

    @Override
    public long getWCooldownRemainingMs() {
        return healAbility.getRemainingCooldownMs();
    }

    @Override
    public long getWCooldownTotalMs() {
        return healAbility.getCooldownMs();
    }

    @Override
    public void useQ(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        Player caster = game.getPlayers().stream()
                .filter(p -> p.getId() == casterId)
                .findFirst()
                .orElse(null);

        if (rageAbility.isReady()) {
            caster.applyEffect(rageAbility);
        }
    }

    @Override
    public void useW(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        tryUseHeal(casterId, game);
    }

    public boolean tryUseHeal(int casterId, GameInstance game) {
        if (healAbility.isReady()) {
            Player caster = game.getPlayers().stream()
                    .filter(p -> p.getId() == casterId)
                    .findFirst()
                    .orElse(null);

            healAbility.execute(caster, game);
            return true;
        }

        return false;
    }

    @Override
    public void autoAttack(Player caster, AttackableTarget enemy, GameInstance game) {
        if (punchAbility.isReady()) {
            punchAbility.execute(caster, enemy, game);
        }
    }

    @Override
    public Ability getQAbility() { return rageAbility; }

    @Override
    public Ability getWAbility() { return healAbility; }

}
