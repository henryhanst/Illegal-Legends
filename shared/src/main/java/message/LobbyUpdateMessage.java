package message;

import lombok.Data;

import java.util.List;

@Data
public class LobbyUpdateMessage {
    private List<PlayerLobbyInfo> players;
    private int lobbyId;

    public LobbyUpdateMessage() {}
}
