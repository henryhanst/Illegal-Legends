package ee.taltech.examplegame.server.game.object.ability.abilities;

import com.badlogic.gdx.math.Vector2;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.Bullet;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.ability.PositionAbility;
import ee.taltech.examplegame.server.util.VectorUtils;

import static constant.Constants.BULLET_HEIGHT_IN_PIXELS;
import static constant.Constants.BULLET_WIDTH_IN_PIXELS;
import static constant.Constants.SKILLSHOT_DMG;
import static constant.Constants.SKILLSHOT_COOLDOWN;

public class SkillshotAbility extends PositionAbility {

    public SkillshotAbility() {
        super(SKILLSHOT_COOLDOWN);
    }

    /**
     * Spawns a bullet that travels from the caster toward the target position
     * @param casterId ID of player who shoots
     * @param casterX player X coord
     * @param casterY player Y coord
     * @param targetX mouse X coord
     * @param targetY mouse Y coord
     * @param game game instance
     */
    @Override
    public void execute(int casterId, float casterX, float casterY, int targetX, int targetY, GameInstance game) {
        var caster = game.findPlayerById(casterId);
        if (caster == null) {
            return;
        }

        markUsed();
        float spawnX = casterX - BULLET_WIDTH_IN_PIXELS / 2f;
        float spawnY = casterY - BULLET_HEIGHT_IN_PIXELS / 2f;

        Vector2 direction = VectorUtils.toUnitVector(
                casterX, casterY, targetX, targetY
        );

        caster.setCastAbility("SkillshotAbility");

        game.addBullet(new Bullet(
                spawnX,
                spawnY,
                direction.x,
                direction.y,
                casterId,
                caster.getTeam(),
                caster.getChampion().getType(),
                SKILLSHOT_DMG
        ));
    }
}
