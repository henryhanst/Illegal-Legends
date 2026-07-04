package message.dto;

import lombok.Data;

@Data
public class TurretState {
    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private Team team;
    private boolean destroyed;
}
