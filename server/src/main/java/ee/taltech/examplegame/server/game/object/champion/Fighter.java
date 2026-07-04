package ee.taltech.examplegame.server.game.object.champion;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.Ability;
import ee.taltech.examplegame.server.game.object.ability.abilities.EmpoweredAbility;
import ee.taltech.examplegame.server.game.object.ability.abilities.PunchAbility;
import ee.taltech.examplegame.server.game.object.ability.abilities.StunAbility;
import message.dto.ChampionType;

import static constant.Constants.*;


public class Fighter extends Champion {
    private final PunchAbility punchAbility = new PunchAbility();
    private final EmpoweredAbility empoweredAbility = new EmpoweredAbility();
    private final StunAbility stunAbility = new StunAbility();

    public Fighter() {
        super(FIGHTER_HP, FIGHTER_HP_PER_LEVEL, FIGHTER_DMG, FIGHTER_DMG_PER_LEVEL, FIGHTER_SPEED,  FIGHTER_HP_REGEN_PER_SECOND);
    }

    @Override
    public ChampionType getType() {
        return ChampionType.FIGHTER;
    }

    @Override
    public boolean isQReady() {
        return empoweredAbility.isReady();
    }

    @Override
    public boolean isWReady() {
        return stunAbility.isReady();
    }

    @Override
    public long getQCooldownRemainingMs() {
        return empoweredAbility.getRemainingCooldownMs();
    }

    @Override
    public long getQCooldownTotalMs() {
        return empoweredAbility.getCooldownMs();
    }

    @Override
    public long getWCooldownRemainingMs() {
        return stunAbility.getRemainingCooldownMs();
    }

    @Override
    public long getWCooldownTotalMs() {
        return stunAbility.getCooldownMs();
    }

    @Override
    public void useQ(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        Player caster = game.getPlayers().stream()
                .filter(p -> p.getId() == casterId)
                .findFirst()
                .orElse(null);

        if (empoweredAbility.isReady()) {
            caster.applyEffect(empoweredAbility);
        }
    }

    @Override
    public void useW(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        if (stunAbility.isReady()) {
            stunAbility.execute(casterId, casterX, casterY, targetX, targetY, game);
        }
    }

    @Override
    public void autoAttack(Player caster, AttackableTarget enemy, GameInstance game) {
        if (punchAbility.isReady()) {
            punchAbility.execute(caster, enemy, game);
        }
    }

    @Override
    public Ability getQAbility() { return empoweredAbility; }

    @Override
    public Ability getWAbility() { return stunAbility; }

}
