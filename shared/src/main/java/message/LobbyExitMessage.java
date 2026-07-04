package message;

import lombok.Data;

@Data
public class LobbyExitMessage {
    private int lobbyId;

    public LobbyExitMessage() {
    } // Required for KryoNet serialization

}
