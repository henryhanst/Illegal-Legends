package ee.taltech.examplegame.server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import ee.taltech.examplegame.server.game.object.Player;
import message.PlayerMovementMessage;

public class PlayerMovementListener extends Listener {
    private final Player player;

    public PlayerMovementListener(Player player) {
        this.player = player;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof PlayerMovementMessage playerMovementMessage) {
            Log.info("Player " + connection.getID() + " moved to X: " + playerMovementMessage.getX() + "Y: "+playerMovementMessage.getY());
            player.move(playerMovementMessage);
        }

        super.received(connection, object);
    }
}
