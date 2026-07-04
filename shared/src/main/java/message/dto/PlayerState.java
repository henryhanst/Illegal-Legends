package message.dto;

import lombok.Data;

@Data
public class PlayerState {
    private int id;
    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private int lives;
    private int killCount;
    private int deathCount;
    private String name;
    private int level;
    private int currentXp;
    private int xpForNextLevel;
    private Team team;
    private ChampionType championType;
    private ActionState actionState;
    private Direction direction = Direction.DOWN;
    private boolean autoAttackActive;
    private int autoAttackSequence;
    private int tankHealSequence;
    private long qCooldownRemainingMs;
    private long qCooldownTotalMs;
    private long wCooldownRemainingMs;
    private long wCooldownTotalMs;
    private String castAbility;
}
