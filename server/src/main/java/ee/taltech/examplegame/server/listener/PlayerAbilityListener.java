package ee.taltech.examplegame.server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.object.Player;
import message.PlayerAbilityMessage;
import message.PlayerShootingMessage;

import java.time.Duration;
import java.time.LocalTime;

import static constant.Constants.BULLET_TIMEOUT_IN_MILLIS;

public class PlayerAbilityListener extends Listener {
    private final Player player;

    public PlayerAbilityListener(Player player) {
        this.player = player;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof PlayerAbilityMessage msg) {
            Log.info("Player " + connection.getID() + " used " + msg.getAbilitySlot() + "ability in direction: X: " + msg.getTargetX() + "Y: "+ msg.getTargetY());

            player.useAbility(msg.getAbilitySlot(), msg.getTargetX(), msg.getTargetY());
        }

        super.received(connection, object);
    }
}
