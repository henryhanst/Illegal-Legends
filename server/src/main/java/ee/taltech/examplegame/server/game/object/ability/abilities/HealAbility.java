package ee.taltech.examplegame.server.game.object.ability.abilities;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.SelfAbility;

import static constant.Constants.FIGHTER_HP;
import static constant.Constants.RANGED_HP;
import static constant.Constants.TANK_HP;
import static constant.Constants.HEAL_COOLDOWN;
import static constant.Constants.HEAL_AMOUNT;

public class HealAbility extends SelfAbility {
    public HealAbility() {
        super(HEAL_COOLDOWN);
    }

    @Override
    public void execute(Player caster, GameInstance game) {
        if (caster == null) return;

        int maxHealth = switch(caster.getChampion().getType()) {
            case FIGHTER -> FIGHTER_HP;
            case RANGED  -> RANGED_HP;
            case TANK    -> TANK_HP;
            case NONE -> caster.getLives();
        };

        caster.setCastAbility("HealAbility");

        caster.setLives(Math.min(maxHealth, caster.getLives() + HEAL_AMOUNT));
        markUsed();
    }
}

