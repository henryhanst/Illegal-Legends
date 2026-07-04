package ee.taltech.examplegame.server.game.object.ability;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Player;

public abstract class TargetedAbility extends Ability implements FriendlyFire {

    protected TargetedAbility(long cooldownMs) {
        super(cooldownMs);
    }

    public abstract void execute(Player caster, AttackableTarget target, GameInstance game);
}
