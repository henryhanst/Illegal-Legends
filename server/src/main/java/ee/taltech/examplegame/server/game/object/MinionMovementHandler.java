package ee.taltech.examplegame.server.game.object;

import com.badlogic.gdx.math.Vector2;
import ee.taltech.examplegame.server.game.AStarPathfinder;
import ee.taltech.examplegame.server.game.GameInstance;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import ee.taltech.examplegame.server.util.VectorUtils;
import message.dto.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import static constant.Constants.*;
import static message.dto.ActionState.IDLE;
import static message.dto.ActionState.MOVING;

public class MinionMovementHandler {
    private static final float STRUCTURE_APPROACH_PADDING = 8f;

    private final Minion minion;
    private final GameInstance game;

    public MinionMovementHandler(Minion minion, GameInstance game) {
        this.minion = minion;
        this.game = game;
    }

    public void moveToward(float targetX, float targetY) {
        rebuildPathIfNeeded(targetX, targetY);

        Queue<Vector2> path = minion.getPathQueue();
        if (path.isEmpty()) {
            minion.setActionState(IDLE);
            minion.getBody().setLinearVelocity(0f, 0f);
            return;
        }

        minion.setActionState(MOVING);
        Vector2 waypoint = path.peek();
        minion.setDirection(determineDirection(waypoint.x, waypoint.y));

        Vector2 velocityDirection = VectorUtils.toUnitVector(minion.getX(), minion.getY(), waypoint.x, waypoint.y);
        Vector2 step = new Vector2(velocityDirection.x * MINION_SPEED, velocityDirection.y * MINION_SPEED);
        changeMinionPosition(step);

        if (hasArrivedAt(waypoint)) {
            path.poll();
        }
    }

    public List<Vector2> buildPath(float targetX, float targetY) {
        AStarPathfinder pathfinder = createStructureAwarePathfinder();
        int[] rawStart = worldToGrid(minion.getX(), minion.getY());
        int[] start = pathfinder.findNearestWalkable(rawStart[0], rawStart[1]);
        int[] rawEnd = worldToGrid(targetX, targetY);
        int[] end = pathfinder.findNearestWalkable(rawEnd[0], rawEnd[1]);

        if (start[0] == -1 || end[0] == -1) {
            return Collections.emptyList();
        }

        Vector2 resolvedDestination = (rawEnd[0] == end[0] && rawEnd[1] == end[1])
                ? new Vector2(targetX, targetY)
                : gridToWorld(end[0], end[1]);

        if (pathfinder.isLineOfSightClear(start[0], start[1], end[0], end[1])) {
            // If the raw target sits inside blocked space near a turret/base, follow the nearest
            // walkable destination instead of trying to force the last step into an unreachable point.
            return List.of(resolvedDestination);
        }

        List<AStarPathfinder.Node> rawGridPath = pathfinder.findPath(start[0], start[1], end[0], end[1]);
        if (rawGridPath.isEmpty()) {
            return Collections.emptyList();
        }

        return formatPathForMovement(rawGridPath, start, resolvedDestination);
    }

    public Vector2 resolveMovementTarget(AttackableTarget target) {
        if (target instanceof Turret turret) {
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
                    NEXUS_ATTACK_WIDTH_IN_PIXELS,
                    NEXUS_ATTACK_HEIGHT_IN_PIXELS
            );
        }

        if (target instanceof Player player) {
            // For moving units we only want the closest point on the hitbox, not an extra attack-gap.
            // This helps minions keep chasing around blocked spawn/turret areas instead of stalling
            // at a nearest-walkable point behind the target.
            return resolveUnitApproachPoint(
                    player.getX(),
                    player.getY(),
                    PLAYER_COLLISION_WIDTH_IN_PIXELS,
                    PLAYER_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        if (target instanceof Minion otherMinion) {
            return resolveUnitApproachPoint(
                    otherMinion.getX(),
                    otherMinion.getY(),
                    MINION_COLLISION_WIDTH_IN_PIXELS,
                    MINION_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        return new Vector2(target.getX(), target.getY());
    }

    private void rebuildPathIfNeeded(float targetX, float targetY) {
        long now = System.currentTimeMillis();
        if (!minion.getPathQueue().isEmpty() && now - minion.getLastRepathTime() < MINION_REPATH_INTERVAL_MS) {
            return;
        }

        minion.getPathQueue().clear();
        minion.getPathQueue().addAll(buildPath(targetX, targetY));
        trimArrivedWaypoints();
        minion.setLastRepathTime(now);
    }

    private List<Vector2> formatPathForMovement(List<AStarPathfinder.Node> gridPath, int[] startGrid, Vector2 finalDestination) {
        List<Vector2> worldPath = new ArrayList<>(gridPath.size());

        for (int i = 0; i < gridPath.size(); i++) {
            AStarPathfinder.Node node = gridPath.get(i);

            if (i == 0 && node.x == startGrid[0] && node.y == startGrid[1]) {
                continue;
            }

            if (i == gridPath.size() - 1) {
                worldPath.add(finalDestination);
            } else {
                worldPath.add(gridToWorld(node.x, node.y));
            }
        }

        return worldPath;
    }

    private boolean hasArrivedAt(Vector2 target) {
        float dx = minion.getX() - target.x;
        float dy = minion.getY() - target.y;
        return (dx * dx + dy * dy) < (ARRIVE_RADIUS * ARRIVE_RADIUS);
    }

    private void trimArrivedWaypoints() {
        while (!minion.getPathQueue().isEmpty()) {
            Vector2 next = minion.getPathQueue().peek();
            if (hasArrivedAt(next)) {
                minion.getPathQueue().poll();
            } else {
                return;
            }
        }
    }

    private Direction determineDirection(float destinationX, float destinationY) {
        float dx = destinationX - minion.getX();
        float dy = destinationY - minion.getY();

        if (Math.abs(dx) > Math.abs(dy)) {
            return dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        } else if (Math.abs(dy) > 0f) {
            return dy >= 0 ? Direction.UP : Direction.DOWN;
        }
        return Direction.DOWN;
    }

    private Vector2 resolveStructureApproachPoint(float centerX, float centerY, float width, float height) {
        // Stand a little outside the structure hitbox so the minion does not try to sit exactly
        // on the collision edge and get pinned there by small movement/collision corrections.
        float minX = centerX - width / 2f - STRUCTURE_APPROACH_PADDING;
        float maxX = centerX + width / 2f + STRUCTURE_APPROACH_PADDING;
        float minY = centerY - height / 2f - STRUCTURE_APPROACH_PADDING;
        float maxY = centerY + height / 2f + STRUCTURE_APPROACH_PADDING;

        float clampedX = Math.max(minX, Math.min(maxX, minion.getX()));
        float clampedY = Math.max(minY, Math.min(maxY, minion.getY()));

        if (clampedX == minion.getX() && clampedY == minion.getY()) {
            float distanceToLeft = Math.abs(minion.getX() - minX);
            float distanceToRight = Math.abs(maxX - minion.getX());
            float distanceToBottom = Math.abs(minion.getY() - minY);
            float distanceToTop = Math.abs(maxY - minion.getY());

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

    private Vector2 resolveUnitApproachPoint(float centerX, float centerY, float width, float height) {
        float minX = centerX - width / 2f;
        float maxX = centerX + width / 2f;
        float minY = centerY - height / 2f;
        float maxY = centerY + height / 2f;

        float clampedX = Math.max(minX, Math.min(maxX, minion.getX()));
        float clampedY = Math.max(minY, Math.min(maxY, minion.getY()));

        if (clampedX == minion.getX() && clampedY == minion.getY()) {
            return new Vector2(centerX, centerY);
        }

        return new Vector2(clampedX, clampedY);
    }

    private void changeMinionPosition(Vector2 step) {
        float stepSeconds = 1f / GAME_TICK_RATE;
        Vector2 currentPosition = minion.getBody().getPosition();

        Vector2 fullStep = new Vector2(
                currentPosition.x + step.x * stepSeconds,
                currentPosition.y + step.y * stepSeconds
        );

        if (!wouldOverlapAnotherUnit(fullStep.x, fullStep.y)) {
            minion.getBody().setLinearVelocity(step);
        } else {
            handleCollisionSliding(step, stepSeconds, currentPosition);
        }
    }

    private void handleCollisionSliding(Vector2 step, float stepSeconds, Vector2 currentPosition) {
        Vector2 xOnlyStep = new Vector2(currentPosition.x + step.x * stepSeconds, currentPosition.y);
        Vector2 yOnlyStep = new Vector2(currentPosition.x, currentPosition.y + step.y * stepSeconds);

        boolean canMoveX = !wouldOverlapAnotherUnit(xOnlyStep.x, xOnlyStep.y);
        boolean canMoveY = !wouldOverlapAnotherUnit(yOnlyStep.x, yOnlyStep.y);

        if (canMoveX && !canMoveY) {
            minion.getBody().setLinearVelocity(step.x, 0f);
        } else if (!canMoveX && canMoveY) {
            minion.getBody().setLinearVelocity(0f, step.y);
        } else {
            minion.getBody().setLinearVelocity(0f, 0f);
        }
    }

    private boolean wouldOverlapAnotherUnit(float nextX, float nextY) {
        float minionHalfWidth = MINION_COLLISION_WIDTH_IN_PIXELS / (2f * PPM);
        float minionHalfHeight = MINION_COLLISION_HEIGHT_IN_PIXELS / (2f * PPM);

        for (Minion other : game.getMinions()) {
            if (other == minion || other.getBody() == null || other.isDestroyed()) {
                continue;
            }

            // Let allied minions phase through each other logically; otherwise waves deadlock
            // when several units want the same approach point near a spawn or structure.
            if (other.getTeam() == minion.getTeam()) {
                continue;
            }

            Vector2 otherPosition = other.getBody().getPosition();
            boolean overlapsX = Math.abs(nextX - otherPosition.x) < minionHalfWidth * 2f;
            boolean overlapsY = Math.abs(nextY - otherPosition.y) < minionHalfHeight * 2f;

            if (overlapsX && overlapsY) {
                return true;
            }
        }

        float playerHalfWidth = PLAYER_COLLISION_WIDTH_IN_PIXELS / (2f * PPM);
        float playerHalfHeight = PLAYER_COLLISION_HEIGHT_IN_PIXELS / (2f * PPM);

        for (Player player : game.getPlayers()) {
            if (player.getBody() == null || player.isDestroyed()) {
                continue;
            }

            if (player.getTeam() == minion.getTeam()) {
                continue;
            }

            Vector2 playerPosition = player.getBody().getPosition();
            boolean overlapsX = Math.abs(nextX - playerPosition.x) < minionHalfWidth + playerHalfWidth;
            boolean overlapsY = Math.abs(nextY - playerPosition.y) < minionHalfHeight + playerHalfHeight;

            if (overlapsX && overlapsY) {
                return true;
            }
        }

        return false;
    }

    private AStarPathfinder createStructureAwarePathfinder() {
        int[][] grid = game.getBox2dWorldGenerator().getPathfindingGrid();

        for (Turret turret : game.getTurrets()) {
            if (turret.isDestroyed()) {
                continue;
            }

            markBlockedRectangle(
                    grid,
                    turret.getX(),
                    turret.getY() + TURRET_COLLISION_OFFSET_Y_IN_PIXELS,
                    TURRET_COLLISION_WIDTH_IN_PIXELS,
                    TURRET_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        for (Nexus nexus : game.getNexuses()) {
            if (nexus.isDestroyed()) {
                continue;
            }

            markBlockedRectangle(
                    grid,
                    nexus.getX(),
                    nexus.getY(),
                    NEXUS_COLLISION_WIDTH_IN_PIXELS,
                    NEXUS_COLLISION_HEIGHT_IN_PIXELS
            );
        }

        return new AStarPathfinder(grid);
    }

    private void markBlockedRectangle(int[][] grid, float centerX, float centerY, float width, float height) {
        if (grid.length == 0 || grid[0].length == 0) {
            return;
        }

        int[] topLeft = clampToGrid(worldToGrid(centerX - width / 2f, centerY + height / 2f), grid);
        int[] topRight = clampToGrid(worldToGrid(centerX + width / 2f, centerY + height / 2f), grid);
        int[] bottomLeft = clampToGrid(worldToGrid(centerX - width / 2f, centerY - height / 2f), grid);
        int[] bottomRight = clampToGrid(worldToGrid(centerX + width / 2f, centerY - height / 2f), grid);

        int minCol = Math.min(Math.min(topLeft[0], topRight[0]), Math.min(bottomLeft[0], bottomRight[0]));
        int maxCol = Math.max(Math.max(topLeft[0], topRight[0]), Math.max(bottomLeft[0], bottomRight[0]));
        int minRow = Math.min(Math.min(topLeft[1], topRight[1]), Math.min(bottomLeft[1], bottomRight[1]));
        int maxRow = Math.max(Math.max(topLeft[1], topRight[1]), Math.max(bottomLeft[1], bottomRight[1]));

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                grid[row][col] = 1;
            }
        }
    }

    private int[] clampToGrid(int[] cell, int[][] grid) {
        int clampedX = Math.max(0, Math.min(grid[0].length - 1, cell[0]));
        int clampedY = Math.max(0, Math.min(grid.length - 1, cell[1]));
        return new int[]{clampedX, clampedY};
    }

    private int[] worldToGrid(float x, float y) {
        return game.getBox2dWorldGenerator().mapCoordinatesToGrid(x, y);
    }

    private Vector2 gridToWorld(int gridX, int gridY) {
        return game.getBox2dWorldGenerator().gridToMapCoordinates(gridX, gridY);
    }
}
