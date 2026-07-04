package network;

import com.esotericsoftware.kryo.Kryo;
import message.PlayerShootingMessage;
import message.dto.*;

import java.util.ArrayList;

public class KryoHelper {

    public static void registerClasses(Kryo kryo) {
        // all classes that you want to send over the network
        // must be registered here. To make the handling of these classes
        // easier they are all stored in the "messages" package
        kryo.register(message.GameJoinMessage.class);
        kryo.register(message.PlayerMovementMessage.class);
        kryo.register(Direction.class);
        kryo.register(ArrayList.class);
        kryo.register(message.GameStateMessage.class);
        kryo.register(PlayerState.class);
        kryo.register(MinionState.class);
        kryo.register(BulletState.class);
        kryo.register(NexusState.class);
        kryo.register(TurretState.class);
        kryo.register(ChampionType.class);
        kryo.register(ActionState.class);
        kryo.register(AbilitySlot.class);
        kryo.register(PlayerShootingMessage.class);
        kryo.register(message.LobbyUpdateMessage.class);
        kryo.register(message.LobbyExitMessage.class);
        kryo.register(message.StartGameMessage.class);
        kryo.register(message.dto.Team.class);
        kryo.register(message.PlayerLobbyInfo.class);
        kryo.register(message.TeamRequestMessage.class);
        kryo.register(message.PlayerAbilityMessage.class);
        kryo.register(message.PlayerClassMessage.class);
        kryo.register(message.ChampionSelectUpdate.class);
    }
}
