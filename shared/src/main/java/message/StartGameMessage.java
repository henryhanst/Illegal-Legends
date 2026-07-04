package message;

import lombok.Data;

@Data
public class StartGameMessage {
    private int lobbyId;

    // Default constructor required for Kryo serialization
    public StartGameMessage() {
    }
}
