package ee.taltech.examplegame.server.game.object;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import lombok.Getter;
import message.dto.NexusState;
import message.dto.Team;

import static constant.Constants.PPM;

@Getter
public abstract class Destructible implements AttackableTarget {
    protected final World world;
    protected final Team team;
    protected final float x;
    protected final float y;
    protected final int maxHp;

    protected int hp;
    protected boolean destroyed;
    protected Body body;

    protected Destructible(World world, Team team, float x, float y, int maxHp) {
        this.world = world;
        this.team = team;
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    // Used for nexus
    protected void createStaticBody(
            float collisionWidthPixels,
            float collisionHeightPixels,
            short categoryBits,
            short maskBits)
    {
        createStaticBody(
                collisionWidthPixels,
                collisionHeightPixels,
                0f,
                0f,
                categoryBits,
                maskBits,
                0f
        );
    }

    // Without rotation
    protected void createStaticBody(
            float collisionWidthPixels,
            float collisionHeightPixels,
            float offsetXPixels,
            float offsetYPixels,
            short categoryBits,
            short maskBits
    ) {
        createStaticBody(
                collisionWidthPixels,
                collisionHeightPixels,
                offsetXPixels,
                offsetYPixels,
                categoryBits,
                maskBits,
                0f
        );
    }

    // Used for towers, because it needs offsetpixels
    protected void createStaticBody(
            float collisionWidthPixels,
            float collisionHeightPixels,
            float offsetXPixels,
            float offsetYPixels,
            short categoryBits,
            short maskBits,
            float angleDeg
    )

    {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set((x + offsetXPixels) / PPM, (y + offsetYPixels) / PPM);

        bodyDef.angle = (float) Math.toRadians(angleDeg); // Rotates collision

        body = world.createBody(bodyDef);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.filter.categoryBits = categoryBits;
        fixtureDef.filter.maskBits = maskBits;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(collisionWidthPixels / (2f * PPM), collisionHeightPixels / (2f * PPM));
        fixtureDef.shape = shape;
        body.createFixture(fixtureDef);
        body.setUserData(this);
        shape.dispose();
    }

    public void takeDamage(int damageAmount) {
        if (destroyed || damageAmount <= 0) {
            return;
        }

        hp = Math.max(0, hp - damageAmount);
        if (hp == 0) {
            destroyed = true;
        }
    }

    public NexusState getState() {
        NexusState nexusState = new NexusState();
        nexusState.setX(x);
        nexusState.setY(y);
        nexusState.setHp(hp);
        nexusState.setMaxHp(maxHp);
        nexusState.setTeam(team);
        nexusState.setDestroyed(destroyed);
        return nexusState;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
}
