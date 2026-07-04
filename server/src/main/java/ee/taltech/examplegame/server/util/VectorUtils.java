package ee.taltech.examplegame.server.util;


import com.badlogic.gdx.math.Vector2;

public class VectorUtils {
    private VectorUtils() {}

    /**
     * Returns a unit direction vector pointing from (x, y) to (targetX, targetY).
     * The vector has length 1, ensuring uniform movement speed in all directions.
     *
     * @param x player x coordinate
     * @param y player y coordinate
     * @param targetX destination x coordinate
     * @param targetY destination y coordinate
     * @return a float array containing the normalized direction vector {dx, dy}
     * where the vector length is 1
     */
    public static Vector2 toUnitVector(float x, float y, float targetX, float targetY) {

        float xDistance = targetX - x;
        float yDistance = targetY - y;

        float distanceToTarget = (float) Math.hypot(xDistance, yDistance);

        if (distanceToTarget == 0f || Float.isNaN(distanceToTarget) || Float.isInfinite(distanceToTarget)) {
            return new Vector2(0f,0f);
        }

        float directionX = xDistance/distanceToTarget;
        float directionY = yDistance/distanceToTarget;

        return new Vector2(directionX, directionY);
    }
}
