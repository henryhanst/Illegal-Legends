package message;

import lombok.Data;
import message.dto.ChampionType;
@Data
public class PlayerClassMessage {

    private ChampionType selectedType;
    private boolean lockedIn;
    public int playerId;
}
