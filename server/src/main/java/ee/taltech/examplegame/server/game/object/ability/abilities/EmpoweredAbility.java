package ee.taltech.examplegame.server.game.object.ability.abilities;

import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.PositionAbility;
import ee.taltech.examplegame.server.game.object.ability.TimedEffect;

import static constant.Constants.EMPOWERED_DURATION;
import static constant.Constants.EMPOWERED_COOLDOWN;
import static constant.Constants.EMPOWERED_ATTACK_DMG_MULTIPLIER;

public class EmpoweredAbility extends TimedEffect {
    public EmpoweredAbility() {
        super(EMPOWERED_COOLDOWN, EMPOWERED_DURATION);
    }

    @Override
    public void onApply(Player caster) {
        markUsed();
        caster.setDamageMultiplier(EMPOWERED_ATTACK_DMG_MULTIPLIER);
        caster.setCastAbility("EmpoweredAbility");
    }

    @Override
    public void onExpire(Player caster) {
        caster.setDamageMultiplier(1f);
    }

}
