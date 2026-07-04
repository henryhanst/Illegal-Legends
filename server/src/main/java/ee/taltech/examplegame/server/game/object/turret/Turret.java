package ee.taltech.examplegame.server.game.object.turret;

import com.badlogic.gdx.physics.box2d.World;
import ee.taltech.examplegame.server.game.object.AttackableTarget;
import ee.taltech.examplegame.server.game.object.Destructible;
import ee.taltech.examplegame.server.game.object.Player;
import lombok.Getter;
import lombok.Setter;
import message.dto.Team;
import message.dto.TurretState;

import static constant.Constants.PLAYER_MASK;
import static constant.Constants.TURRET_COLLISION_HEIGHT_IN_PIXELS;
import static constant.Constants.TURRET_COLLISION_OFFSET_Y_IN_PIXELS;
import static constant.Constants.TURRET_COLLISION_WIDTH_IN_PIXELS;
import static constant.Constants.TURRET_HP;
import static constant.Constants.WALL_CATEGORY;

public class Turret extends Destructible {
    private long lastShotTime;
    @Setter
    @Getter
    private AttackableTarget currentTarget;

    public Turret(World world, Team team, float x, float y) {
        super(world, team, x, y, TURRET_HP);
        createStaticBody(
                TURRET_COLLISION_WIDTH_IN_PIXELS,
                TURRET_COLLISION_HEIGHT_IN_PIXELS,
                0f,
                TURRET_COLLISION_OFFSET_Y_IN_PIXELS,
                WALL_CATEGORY,
                PLAYER_MASK,
                0f
        );
    }

    public TurretState getTurretState() {
        TurretState turretState = new TurretState();
        turretState.setX(x);
        turretState.setY(y);
        turretState.setHp(hp);
        turretState.setMaxHp(maxHp);
        turretState.setTeam(team);
        turretState.setDestroyed(destroyed);
        return turretState;
    }

    public boolean isReadyToShoot() {
        return System.currentTimeMillis() - lastShotTime >= constant.Constants.TURRET_COOLDOWN;
    }

    public void markShot() {
        lastShotTime = System.currentTimeMillis();
    }

    public void clearCurrentTarget() {
        currentTarget = null;
    }

    public void forcePlayerAggro(Player attacker) {
        currentTarget = attacker;
    }
}
