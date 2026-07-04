package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import ee.taltech.examplegame.network.ServerConnection;
import com.badlogic.gdx.Input;
import message.PlayerAbilityMessage;
import message.PlayerMovementMessage;
import message.dto.AbilitySlot;

import static constant.Constants.PPM;

/**
 * Listens for user input in the GameScreen regarding player movement and shooting,
 * forwards the corresponding messages to the server.
 */

public class PlayerInputManager {

    private final Viewport viewport;
    private final Arena arena;

    public PlayerInputManager(Viewport viewport, Arena arena) {
        this.viewport = viewport;
        this.arena = arena;
    }

    public void handleMovementInput() {

        // Detect right click press
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            var movementMessage = new PlayerMovementMessage();
            Vector3 coords = unprojectCursor();
            movementMessage.setX((int) (coords.x * PPM));
            movementMessage.setY((int) (coords.y * PPM));
            sendUDP(movementMessage);

            if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                arena.setMovementArrow(coords.x, coords.y);
            }
        }
    }


    public void handleShootingInput() {
        // Detect key presses and send shooting message to the server
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.Q)) sendAbility(AbilitySlot.Q);
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) sendAbility(AbilitySlot.W);
    }

    public void sendAbility(AbilitySlot slot) {
        var abilityMessage = new PlayerAbilityMessage();
        Vector3 coords = unprojectCursor();
        abilityMessage.setAbilitySlot(slot);
        abilityMessage.setTargetX((int) (coords.x * PPM));
        abilityMessage.setTargetY((int) (coords.y * PPM));
        sendUDP(abilityMessage);
    }

    private Vector3 unprojectCursor() {
        // Gets the cursor position
        Vector3 worldCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        // Gets game world coordinates matching the pointer position
        viewport.unproject(worldCoords);
        return worldCoords;
    }

    private void sendUDP(Object message) {
        ServerConnection.getInstance().getClient().sendUDP(message);
    }
}
