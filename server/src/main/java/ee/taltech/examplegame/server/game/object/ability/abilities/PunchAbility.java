package ee.taltech.examplegame.server.game.object.ability.abilities;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Minion;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.TargetedAbility;

import static constant.Constants.*;

public class PunchAbility extends TargetedAbility {
    public PunchAbility() {
        super(PUNCH_COOLDOWN);
    }

    @Override
    public void execute(Player caster, AttackableTarget enemy, GameInstance game) {
        int damage = caster.getAutoAttackDamage();
        // Melee abilities use the shared friendly-fire check from the base ability class.
        if (!canHit(caster, enemy)) {
            return;
        }

        int trueDamage = (int) (damage * caster.getDamageMultiplier());

        if (enemy instanceof Player opponent) {
            opponent.takeDamage(trueDamage, caster);
        } else {
            enemy.takeDamage(trueDamage);
        }
        if (enemy instanceof Minion minion) {
            // Melee hits should also force the minion to answer the player who just struck it.
            minion.onAttackedBy(caster);
        }
        markUsed();
    }
}

