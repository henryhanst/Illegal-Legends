package message;

import lombok.Data;
import message.dto.BulletState;
import message.dto.MinionState;
import message.dto.NexusState;
import message.dto.PlayerState;
import message.dto.TurretState;

import java.util.List;

@Data
public class GameStateMessage {
    private List<PlayerState> playerStates;
    private List<MinionState> minionStates;
    private List<BulletState> bulletStates;
    private List<NexusState> nexusStates;
    private List<TurretState> turretStates;
    private int gameTime;
    private boolean allPlayersHaveJoined;
}
