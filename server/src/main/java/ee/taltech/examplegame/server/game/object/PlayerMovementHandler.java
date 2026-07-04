package ee.taltech.examplegame.server.game.object;

import com.badlogic.gdx.math.Vector2;
import ee.taltech.examplegame.server.game.AStarPathfinder;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import ee.taltech.examplegame.server.util.VectorUtils;
import message.PlayerMovementMessage;
import message.dto.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import static constant.Constants.*;
import static message.dto.ActionState.IDLE;

public class PlayerMovementHandler {
    private final Player player;
    private final GameInstance game;

    public PlayerMovementHandler(Player player, GameInstance game) {
        this.player = player;
        this.game = game;
    }

    /**
     * Advances the player one tick along their queued path.
     */
    public void updateMovement() {
        Queue<Vector2> playerPath = player.getPathQueue();

        if (playerPath.isEmpty()) {
            player.setActionState(IDLE);
            return;
        }

        Vector2 currentDestination = playerPath.peek();
        player.setDirection(determineDirection(currentDestination.x, currentDestination.y));

        Vector2 destinationUnitVector = VectorUtils.toUnitVector(
                player.getX(),
                player.getY(),
                currentDestination.x,
                currentDestination.y
        );

        float speed = PLAYER_SPEED * player.getSpeedMultiplier();

        Vector2 step = new Vector2(destinationUnitVector.x * speed, destinationUnitVector.y * speed);
        changePlayerPosition(step);

        if (hasArrivedAt(currentDestination)) {
            playerPath.poll(); // Remove reached waypoint
            if (playerPath.isEmpty()) {
                player.setActionState(IDLE);
            }
        }
    }

    /**
     * Builds a movement path from the player's current position to the requested destination.
     * Incorporates nearest-walkable fallback, line-of-sight optimization, and A* pathfinding.
     */
    public List<Vector2> buildPath(PlayerMovementMessage request) {
        int[] startGrid = worldToGrid(player.getX(), player.getY());
        int[] rawTargetGrid = worldToGrid(request.getX(), request.getY());

        // Resolve actual target (fallback to nearest walkable grid tile if clicked on a collision tile)
        int[] resolvedTargetGrid = game.getAStarPathfinder().findNearestWalkable(rawTargetGrid[0], rawTargetGrid[1]);

        // Edge case: No walkable tile found anywhere (e.g., completely boxed in map)
        if (resolvedTargetGrid[0] == -1) {
            return Collections.emptyList();
        }

        // If clicked on walkable tile - keep mouse coordinates for the destination
        // else get center of the nearest walkable grid tile
        boolean targetWasShifted = (resolvedTargetGrid[0] != rawTargetGrid[0] || resolvedTargetGrid[1] != rawTargetGrid[1]);
        Vector2 finalWorldDestination = targetWasShifted
                ? gridToWorld(resolvedTargetGrid[0], resolvedTargetGrid[1])
                : new Vector2(request.getX(), request.getY());

        // Direct line of sight path
        if (game.getAStarPathfinder().isLineOfSightClear(startGrid[0], startGrid[1], resolvedTargetGrid[0], resolvedTargetGrid[1])) {
            return List.of(finalWorldDestination);
        }

        // A* pathfinding
        List<AStarPathfinder.Node> rawGridPath = game.getAStarPathfinder().findPath(
                startGrid[0], startGrid[1],
                resolvedTargetGrid[0], resolvedTargetGrid[1]
        );

        if (rawGridPath.isEmpty()) {
            return Collections.emptyList();
        }

        return formatPathForMovement(rawGridPath, startGrid, finalWorldDestination);
    }

    /**
     * Combat movement should also path to a valid attack point instead of blindly chasing the
     * target's raw center, especially for structures with offset collision boxes.
     */
    public List<Vector2> buildCombatPath(AttackableTarget target) {
        Vector2 combatDestination = resolveCombatDestination(target);

        int[] startGrid = worldToGrid(player.getX(), player.getY());
        int[] rawTargetGrid = worldToGrid(combatDestination.x, combatDestination.y);
        int[] resolvedTargetGrid = game.getAStarPathfinder().findNearestWalkable(rawTargetGrid[0], rawTargetGrid[1]);

        if (resolvedTargetGrid[0] == -1) {
            return Collections.emptyList();
        }

        if (game.getAStarPathfinder().isLineOfSightClear(startGrid[0], startGrid[1], resolvedTargetGrid[0], resolvedTargetGrid[1])) {
            return List.of(combatDestination);
        }

        List<AStarPathfinder.Node> rawGridPath = game.getAStarPathfinder().findPath(
                startGrid[0], startGrid[1],
                resolvedTargetGrid[0], resolvedTargetGrid[1]
        );

        if (rawGridPath.isEmpty()) {
            return Collections.emptyList();
        }

        return formatPathForMovement(rawGridPath, startGrid, combatDestination);
    }


    /**
     * Converts raw grid nodes into smooth world-space coordinates, handling edge cases.
     */
    private List<Vector2> formatPathForMovement(List<AStarPathfinder.Node> gridPath, int[] startGrid, Vector2 finalDestination) {
        List<Vector2> worldPath = new ArrayList<>(gridPath.size());

        for (int i = 0; i < gridPath.size(); i++) {
            AStarPathfinder.Node node = gridPath.get(i);

            // Strip the first node if it's the exact tile the player is currently inside
            if (i == 0 && node.x == startGrid[0] && node.y == startGrid[1]) {
                continue;
            }

            // If it's the final node, use the precise calculated world destination
            if (i == gridPath.size() - 1) {
                worldPath.add(finalDestination);
            } else {
                // Otherwise, navigate to the center of the grid tile
                worldPath.add(gridToWorld(node.x, node.y));
            }
        }

        return worldPath;
    }

    private boolean hasArrivedAt(Vector2 target) {
        float dx = player.getX() - target.x;
        float dy = player.getY() - target.y;
        return (dx * dx + dy * dy) < (ARRIVE_RADIUS * ARRIVE_RADIUS);
    }

    /** Returns the cardinal direction the player should face to reach the destination. */
    private Direction determineDirection(float destinationX, float destinationY) {
        float dx = destinationX - player.getX();
        float dy = destinationY - player.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        } else if (Math.abs(dy) > 0f) {
            return dy >= 0 ? Direction.UP : Direction.DOWN;
        }
        return Direction.DOWN;
    }

    private Vector2 resolveCombatDestination(AttackableTarget target) {
        if (target instanceof Turret turret) {
            // Structures are attacked against their collision box, not their sprite center.
            return resolveStructureApproachPoint(
                    turret.getX(),
                    turret.getY() + TURRET_COLLISION_OFFSET_Y_IN_PIXELS,
                    TURRET_COLLISION_WIDTH_IN_PIXELS,
                    TURRET_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        if (target instanceof Nexus nexus) {
            return resolveStructureApproachPoint(
                    nexus.getX(),
                    nexus.getY(),
                    NEXUS_COLLISION_WIDTH_IN_PIXELS,
                    NEXUS_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        return new Vector2(target.getX(), target.getY());
    }

    private Vector2 resolveStructureApproachPoint(float centerX, float centerY, float width, float height) {
        float minX = centerX - width / 2f;
        float maxX = centerX + width / 2f;
        float minY = centerY - height / 2f;
        float maxY = centerY + height / 2f;

        float clampedX = Math.max(minX, Math.min(maxX, player.getX()));
        float clampedY = Math.max(minY, Math.min(maxY, player.getY()));

        if (clampedX == player.getX() && clampedY == player.getY()) {
            float distanceToLeft = Math.abs(player.getX() - minX);
            float distanceToRight = Math.abs(maxX - player.getX());
            float distanceToBottom = Math.abs(player.getY() - minY);
            float distanceToTop = Math.abs(maxY - player.getY());

            float nearestEdge = Math.min(
                    Math.min(distanceToLeft, distanceToRight),
                    Math.min(distanceToBottom, distanceToTop)
            );

            if (nearestEdge == distanceToLeft) {
                clampedX = minX;
            } else if (nearestEdge == distanceToRight) {
                clampedX = maxX;
            } else if (nearestEdge == distanceToBottom) {
                clampedY = minY;
            } else {
                clampedY = maxY;
            }
        }

        return new Vector2(clampedX, clampedY);
    }

    /** Moves the player by the given velocity step, falling back to sliding on player collision. */
    public void changePlayerPosition(Vector2 step) {
        float stepSeconds = 1f / GAME_TICK_RATE;
        Vector2 currentPosition = player.getBody().getPosition();

        Vector2 fullStep = new Vector2(
                currentPosition.x + step.x * stepSeconds,
                currentPosition.y + step.y * stepSeconds
        );

        if (!wouldOverlapAnotherPlayer(fullStep.x, fullStep.y)) {
            player.getBody().setLinearVelocity(step);
        } else {
            handleCollisionSliding(step, stepSeconds, currentPosition);
        }
    }

    /** Attempts to slide along one axis when the full step is blocked by another player. */
    private void handleCollisionSliding(Vector2 step, float stepSeconds, Vector2 currentPosition) {
        Vector2 xOnlyStep = new Vector2(currentPosition.x + step.x * stepSeconds, currentPosition.y);
        Vector2 yOnlyStep = new Vector2(currentPosition.x, currentPosition.y + step.y * stepSeconds);

        boolean canMoveX = !wouldOverlapAnotherPlayer(xOnlyStep.x, xOnlyStep.y);
        boolean canMoveY = !wouldOverlapAnotherPlayer(yOnlyStep.x, yOnlyStep.y);

        if (canMoveX && !canMoveY) {
            player.getBody().setLinearVelocity(step.x, 0f);
        } else if (!canMoveX && canMoveY) {
            player.getBody().setLinearVelocity(0f, step.y);
        } else {
            player.getBody().setLinearVelocity(0f, 0f);
        }
    }

    /** Returns true if moving to (nextX, nextY) would overlap another living player's hitbox. */
    private boolean wouldOverlapAnotherPlayer(float nextX, float nextY) {
        float halfWidth = PLAYER_COLLISION_WIDTH_IN_PIXELS / (2f * PPM);
        float halfHeight = PLAYER_COLLISION_HEIGHT_IN_PIXELS / (2f * PPM);
        float minionHalfWidth = MINION_COLLISION_WIDTH_IN_PIXELS / (2f * PPM);
        float minionHalfHeight = MINION_COLLISION_HEIGHT_IN_PIXELS / (2f * PPM);

        for (Player other : game.getPlayers()) {
            if (other == player || other.getBody() == null || other.isDestroyed()) {
                continue;
            }

            Vector2 otherPosition = other.getBody().getPosition();
            boolean overlapsX = Math.abs(nextX - otherPosition.x) < halfWidth * 2f;
            boolean overlapsY = Math.abs(nextY - otherPosition.y) < halfHeight * 2f;

            if (overlapsX && overlapsY) {
                return true;
            }
        }

        // Minion checks live outside the player loop so they still apply even when no other
        // players are nearby. This matters a lot around waves and structures.
        for (Minion minion : game.getMinions()) {
            if (minion.getBody() == null || minion.isDestroyed()) {
                continue;
            }

            Vector2 minionPosition = minion.getBody().getPosition();
            boolean minionOverlapsX = Math.abs(nextX - minionPosition.x) < halfWidth + minionHalfWidth;
            boolean minionOverlapsY = Math.abs(nextY - minionPosition.y) < halfHeight + minionHalfHeight;

            if (minionOverlapsX && minionOverlapsY) {
                return true;
            }
        }
        return false;
    }


    private int[] worldToGrid(float x, float y) {
        return game.getBox2dWorldGenerator().mapCoordinatesToGrid(x, y);
    }

    private Vector2 gridToWorld(int gridX, int gridY) {
        return game.getBox2dWorldGenerator().gridToMapCoordinates(gridX, gridY);
    }
}
