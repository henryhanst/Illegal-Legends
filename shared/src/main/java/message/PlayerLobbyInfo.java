package message;

import lombok.Data;
import message.dto.ChampionType;
import message.dto.Team;

@Data
public class PlayerLobbyInfo {
    public int connectionId;
    public String playerName;
    public Team team = Team.NONE;
    public ChampionType championType;
    public boolean lockedIn = false;

    public PlayerLobbyInfo() {}
}
