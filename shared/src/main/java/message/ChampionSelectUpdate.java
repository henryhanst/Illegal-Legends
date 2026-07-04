package message;

import lombok.Data;
import java.util.List;

@Data
public class ChampionSelectUpdate {
    private List<PlayerLobbyInfo> players;
    private int lobbyId;

    public ChampionSelectUpdate() {}
    public ChampionSelectUpdate(List<PlayerLobbyInfo> players) {
        this.players = players;
        this.lobbyId = 0;
    }
}
