package ee.taltech.examplegame.server.game.object.champion;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.Ability;
import ee.taltech.examplegame.server.game.object.ability.abilities.FlashAbility;
import ee.taltech.examplegame.server.game.object.ability.abilities.PunchAbility;
import ee.taltech.examplegame.server.game.object.ability.abilities.SkillshotAbility;
import message.dto.ChampionType;

import static constant.Constants.*;


public class Ranged extends Champion {
    private final SkillshotAbility skillshotAbility = new SkillshotAbility();
    private final PunchAbility punchAbility = new PunchAbility();
    private final FlashAbility flashAbility = new FlashAbility();

    public Ranged() {
        super(RANGED_HP, RANGED_HP_PER_LEVEL, RANGED_DMG, RANGED_DMG_PER_LEVEL, RANGED_SPEED, RANGED_HP_REGEN_PER_SECOND);
    }

    @Override
    public ChampionType getType() {
        return ChampionType.RANGED;
    }

    @Override
    public boolean isQReady() {
        return skillshotAbility.isReady();
    }

    @Override
    public boolean isWReady() {
        return flashAbility.isReady();
    }

    @Override
    public long getQCooldownRemainingMs() {
        return skillshotAbility.getRemainingCooldownMs();
    }

    @Override
    public long getQCooldownTotalMs() {
        return skillshotAbility.getCooldownMs();
    }

    @Override
    public long getWCooldownRemainingMs() {
        return flashAbility.getRemainingCooldownMs();
    }

    @Override
    public long getWCooldownTotalMs() {
        return flashAbility.getCooldownMs();
    }

    @Override
    public void useQ(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        if (skillshotAbility.isReady()) {
            skillshotAbility.execute(casterId, casterX, casterY, targetX, targetY, game);
        }
    }

    @Override
    public void useW(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        if (flashAbility.isReady()) {
            flashAbility.execute(casterId, casterX, casterY, targetX, targetY, game);
        }
    }

    @Override
    public void autoAttack(Player caster, AttackableTarget enemy, GameInstance game) {
        if (punchAbility.isReady()) {
            punchAbility.execute(caster, enemy, game);
        }
    }

    @Override
    public Ability getQAbility() { return skillshotAbility; }

    @Override
    public Ability getWAbility() { return flashAbility; }

}
