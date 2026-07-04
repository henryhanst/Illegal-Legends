package message;

import lombok.Data;

@Data
public class GameJoinMessage {
    private String playerName;
    private int lobbyId;
    public GameJoinMessage(String playerName, int lobbyId) {
        this.playerName = playerName;
        this.lobbyId = lobbyId;
    }
    public String getPlayerName() { return playerName; }
    public int getLobbyId() { return lobbyId; }
    public GameJoinMessage() {}
}
