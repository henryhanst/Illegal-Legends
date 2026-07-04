package message.dto;

import lombok.Data;

@Data
public class NexusState {
    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private Team team;
    private boolean destroyed;
}
