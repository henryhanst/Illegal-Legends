package ee.taltech.examplegame.server.game.object.ability.abilities;

import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.InstantAbility;
import ee.taltech.examplegame.server.game.object.ability.PositionAbility;

import static constant.Constants.FLASH_COOLDOWN;
import static constant.Constants.FLASH_DISTANCE;

public class FlashAbility extends PositionAbility implements InstantAbility {
    public FlashAbility() {
        super(FLASH_COOLDOWN);
    }

    @Override
    public void execute(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        Player caster = game.getPlayers().stream()
                .filter(p -> p.getId() == casterId)
                .findFirst()
                .orElse(null);
        markUsed();

        if (caster == null) return;

        caster.setCastAbility("FlashAbility");

        float dx = casterX - targetX;
        float dy = casterY - targetY;
        float distance = (float) Math.hypot(dx, dy);
        Log.info(distance + "the distance is");

        if (distance > FLASH_DISTANCE) {
            float ratio = FLASH_DISTANCE / distance;
            float newX = casterX - dx * ratio;
            float newY = casterY - dy * ratio;
            caster.teleportTo(newX, newY);
            return;
        }


        caster.teleportTo(targetX, targetY);
    }
}
