package message;

import lombok.Data;
import message.dto.Team;

@Data
public class TeamRequestMessage {
    private Team requestedTeam;
    public TeamRequestMessage() {}
}
