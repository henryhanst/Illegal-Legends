package message.dto;

import lombok.Data;

@Data
public class MinionState {
    private int id;
    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private Team team;
    private Direction direction = Direction.DOWN;
    private ActionState actionState = ActionState.IDLE;
    private boolean destroyed;
}
