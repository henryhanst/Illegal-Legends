package ee.taltech.examplegame.server.game.object.ability;

import ee.taltech.examplegame.server.game.GameInstance;

public abstract class PositionAbility extends Ability {

    protected PositionAbility(long cooldownMs) {
        super(cooldownMs);
    }

    public abstract void execute(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game);
}