package ee.taltech.examplegame.server.game.object.ability.abilities;

import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.TimedEffect;

import static constant.Constants.STUN_COOLDOWN;
import static constant.Constants.STUN_DURATION;
import static message.dto.ActionState.IDLE;
import static message.dto.ActionState.STUNNED;

public class StunEffect extends TimedEffect {

    public StunEffect() {
        super(STUN_COOLDOWN, STUN_DURATION);
    }

    @Override
    public void onApply(Player player) {
        player.setActionState(STUNNED);
    }

    @Override
    public void onExpire(Player player) {
        if (player.getActionState() == STUNNED) {
            player.setActionState(IDLE);
        }
    }
}