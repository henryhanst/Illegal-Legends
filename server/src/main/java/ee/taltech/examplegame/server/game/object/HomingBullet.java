package ee.taltech.examplegame.server.game.object;

import com.badlogic.gdx.math.Vector2;
import ee.taltech.examplegame.server.util.VectorUtils;
import message.dto.Team;

import static constant.Constants.HOMING_BULLET_DMG;

public class HomingBullet extends Bullet{
    private final AttackableTarget target;

    public HomingBullet(float x, float y, float directionX, float directionY,
                        int shotByPlayerWithId, Team shotByTeam, AttackableTarget target) {
        super(x, y, directionX, directionY, shotByPlayerWithId, shotByTeam, HOMING_BULLET_DMG);
        this.target = target;
    }

    @Override
    public void update() {
        if (target != null && !target.isDestroyed()) {
            Vector2 targetDirection = VectorUtils.toUnitVector(getX(), getY(), target.getX(), target.getY());
            setDirectionX(targetDirection.x);
            setDirectionY(targetDirection.y);
        }
        super.update();
    }

    @Override
    public boolean hasExceededRange() {
        return false;
    }
}
