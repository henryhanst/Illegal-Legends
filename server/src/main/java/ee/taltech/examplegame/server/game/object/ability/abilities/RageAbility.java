package ee.taltech.examplegame.server.game.object.ability.abilities;

import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.TimedEffect;

import static constant.Constants.RAGE_COOLDOWN;
import static constant.Constants.RAGE_DURATION;
import static constant.Constants.RAGE_SPEED_MULTIPLIER;

public class RageAbility extends TimedEffect {
    public RageAbility() {
        super(RAGE_COOLDOWN, RAGE_DURATION);
    }

    @Override
    public void onApply(Player caster) {
        markUsed();
        caster.setSpeedMultiplier(RAGE_SPEED_MULTIPLIER);
        caster.setCastAbility("RageAbility");
    }

    @Override
    public void onExpire(Player caster) {
        caster.setSpeedMultiplier(1f);
    }
}
