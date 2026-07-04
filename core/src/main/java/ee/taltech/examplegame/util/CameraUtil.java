package ee.taltech.examplegame.util;

import com.badlogic.gdx.math.Vector3;
import message.GameStateMessage;
import message.dto.PlayerState;
import static constant.Constants.PPM;

public class CameraUtil {
    private CameraUtil() {}

    /**
     * Find current PlayerState object from GameStateMessage and player id
     * @param gameState GameStateMessage object
     * @param playerId int containing id
     * @return PlayerState object
     */
    public static PlayerState getCurrentPlayer(GameStateMessage gameState, int playerId) {
        PlayerState[] playerStates = gameState.getPlayerStates().toArray(new PlayerState[0]);

        for (PlayerState playerState : playerStates) {
            if (playerState.getId() == playerId) {
                return playerState;
            }
        }
        return null;
    }

    /**
     * Create a Vector3 to center the camera over the player
     * @param gamestate GameStateMessage object
     * @param playerId int containing id
     * @return Vector3 containing player coordinates
     */
    public static Vector3 getCameraVector(GameStateMessage gamestate, int playerId) {
        PlayerState playerState = getCurrentPlayer(gamestate, playerId);
        if (playerState == null) {
            return new Vector3(0,0,0);
        }
        return new Vector3(playerState.getX() / PPM, playerState.getY() / PPM, 0f);
    }
}
