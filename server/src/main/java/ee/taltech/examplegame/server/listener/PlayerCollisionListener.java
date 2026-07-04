package ee.taltech.examplegame.server.listener;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;

import static constant.Constants.PLAYER_CATEGORY;

/**
 * Prevent Box2D from applying impulse resolution between players.
 * Player blocking is handled manually in Player movement logic.
 */
public class PlayerCollisionListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
    }

    @Override
    public void endContact(Contact contact) {
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        if (fixtureA.getFilterData().categoryBits == PLAYER_CATEGORY
                && fixtureB.getFilterData().categoryBits == PLAYER_CATEGORY) {
            contact.setEnabled(false);
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
}
