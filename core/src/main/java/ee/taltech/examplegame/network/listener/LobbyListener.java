package ee.taltech.examplegame.network.listener;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.taltech.examplegame.network.ServerConnection;
import message.ChampionSelectUpdate;
import message.LobbyExitMessage;
import message.LobbyUpdateMessage;
import message.StartGameMessage;

public class LobbyListener extends Listener {

    private final ServerConnection serverConnection;

    public LobbyListener(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object == null) return;

        // All UI in one Runnable
        Gdx.app.postRunnable(() -> {
            if (object instanceof LobbyUpdateMessage update) {
                serverConnection.handleLobbyUpdate(update);
            } else if (object instanceof StartGameMessage start) {
                serverConnection.handleStartGame(start);
            } else if (object instanceof LobbyExitMessage exit) {
                serverConnection.handleLobbyExit(exit);
            } else if (object instanceof ChampionSelectUpdate update) {
                serverConnection.handleChampionSelectUpdate(update);
            }
        });
    }
}
