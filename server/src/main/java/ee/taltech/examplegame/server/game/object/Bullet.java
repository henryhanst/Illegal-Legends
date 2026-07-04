package ee.taltech.examplegame.server.game.object;

import ee.taltech.examplegame.server.game.object.ability.TimedEffect;
import lombok.Getter;
import lombok.Setter;
import message.dto.BulletState;
import message.dto.ChampionType;
import message.dto.Team;

import static constant.Constants.BULLET_RANGE;
import static constant.Constants.SKILLSHOT_DMG;
import static constant.Constants.BULLET_SPEED;

@Getter
public class Bullet {
    private float x;
    private float y;
    private int shotById;
    private Team shotByTeam;
    private ChampionType championType;

    @Setter
    private float directionX;
    @Setter
    private float directionY;

    private final TimedEffect onHitEffect;
    private final float spawnX;
    private final float spawnY;
    private final int damage;

    public Bullet(float x, float y, float directionX, float directionY, int shotByPlayerWithId, Team shotByTeam) {
        this(x, y, directionX, directionY, shotByPlayerWithId, shotByTeam, ChampionType.NONE, SKILLSHOT_DMG);
    }

    public Bullet(float x, float y, float dirX, float dirY, int shotById, Team team, int damage) {
        this(x, y, dirX, dirY, shotById, team, ChampionType.NONE, damage, null);
    }

    public Bullet(float x, float y, float dirX, float dirY, int shotById, Team team, int damage, TimedEffect onHitEffect) {
        this(x, y, dirX, dirY, shotById, team, ChampionType.NONE, damage, onHitEffect);
    }

    public Bullet(float x, float y, float dirX, float dirY, int shotById, Team team, ChampionType championType, int damage) {
        this(x, y, dirX, dirY, shotById, team, championType, damage, null);
    }

    public Bullet(float x, float y, float dirX, float dirY, int shotById, Team team, ChampionType championType, int damage, TimedEffect onHitEffect) {
        this.x = this.spawnX = x;
        this.y = this.spawnY = y;
        this.directionX = dirX;
        this.directionY = dirY;
        this.shotById = shotById;
        this.shotByTeam = team;
        this.championType = championType;
        this.damage = damage;
        this.onHitEffect = onHitEffect;
    }

    public void update() {
        x += directionX * BULLET_SPEED;
        y += directionY * BULLET_SPEED;
    }

    public boolean hasExceededRange() {
        // Distance from bullet spawn to current bullet position
        float distanceX = x - spawnX;
        float distanceY = y - spawnY;
        // Avoiding sqrt to improve optimization :)
        return distanceX * distanceX + distanceY * distanceY >= BULLET_RANGE * BULLET_RANGE;
    }

    public BulletState getState() {
        BulletState bulletState = new BulletState();
        bulletState.setX(x);
        bulletState.setY(y);
        bulletState.setDirectionX(directionX);
        bulletState.setDirectionY(directionY);
        bulletState.setChampionType(championType);
        return bulletState;
    }
}
