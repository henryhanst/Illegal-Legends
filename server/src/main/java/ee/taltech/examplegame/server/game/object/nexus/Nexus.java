package ee.taltech.examplegame.server.game.object.nexus;

import com.badlogic.gdx.physics.box2d.World;
import ee.taltech.examplegame.server.game.object.Destructible;
import message.dto.Team;

import static constant.Constants.*;

public class Nexus extends Destructible {

    public Nexus(World world, Team team, float x, float y) {
        super(world, team, x, y, NEXUS_HP);
        createStaticBody(
                NEXUS_COLLISION_WIDTH_IN_PIXELS,
                NEXUS_COLLISION_HEIGHT_IN_PIXELS,
                NEXUS_COLLISION_OFFSET_X_IN_PIXELS,
                NEXUS_COLLISION_OFFSET_Y_IN_PIXELS,
                WALL_CATEGORY,
                PLAYER_MASK,
                45f
        );
    }
}
