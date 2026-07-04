package message.dto;

import lombok.Data;

@Data
public class BulletState {
    private float x;
    private float y;
    private float directionX;
    private float directionY;
    private ChampionType championType = ChampionType.NONE;
}
