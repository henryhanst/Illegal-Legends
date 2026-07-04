package ee.taltech.examplegame.server.game.object.ability;

import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.Player;

public abstract class SelfAbility extends Ability {

    protected SelfAbility(long cooldownMs) {
        super(cooldownMs);
    }

    public abstract void execute(Player caster, GameInstance game);
}