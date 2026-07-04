package ee.taltech.examplegame.server.game;

import lombok.Getter;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class AStarPathfinder {

    @Getter
    private final int maxX;
    @Getter
    private final int maxY;

    private final int[][] grid;
    private final int[][] neighbours = {
            {-1, 0},   // left
            {0, -1},   // up
            {1, 0},    // right
            {0, 1},     // down
            {-1, -1},  // up-left
            { 1, -1},  // up-right
            { 1,  1},  // down-right
            {-1,  1}   // down-left
    };

    public AStarPathfinder(int[][] grid) {
        this.grid = grid;
        this.maxX = grid[0].length;
        this.maxY = grid.length;
    }

    public class Node {

        public int x;
        public int y;
        int gScore;
        int hScore;
        Node parent;

        Node(int x, int y) {
            this.x = x;
            this.y = y;
            this.gScore = 0;
            this.hScore = 0;
            this.parent = null;
        }

        void updateHScore(int dstX, int dstY) {
            this.hScore = Math.abs(x - dstX) + Math.abs(y - dstY);
        }

        int getFScore() {
            return this.gScore + this.hScore;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node node)) return false;
            return x == node.x &&
                    y == node.y;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(x + (y * maxX));
        }
    }

    /**
     * Finds the shortest path between two points on the grid using the A* algorithm.
     *
     * <p>Supports both cardinal (cost: 10) and diagonal (cost: 14) movement. Diagonal moves
     * are blocked if either adjacent cardinal tile is a wall, preventing corner-cutting.
     *
     * @param srcX the X coordinate of the starting tile
     * @param srcY the Y coordinate of the starting tile
     * @param dstX the X coordinate of the destination tile
     * @param dstY the Y coordinate of the destination tile
     * @return an ordered {@link List} of {@link Node} objects from source to destination
     *         (inclusive), or an empty list if no path exists
     */
    public List<Node> findPath(int srcX, int srcY, int dstX, int dstY) {
        List<Node> path = new ArrayList<>();
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(AStarPathfinder.Node::getFScore));
        openSet.add(new Node(srcX, srcY));
        Set<Node> closedSet = new HashSet<>();
        while (!openSet.isEmpty()) {
            AStarPathfinder.Node current = openSet.poll();

            if (current.x == dstX && current.y == dstY) {
                while (current != null) {
                    path.add(current);
                    current = current.parent;
                }
                Collections.reverse(path);
                return path;
            }

            closedSet.add(current);

            for (int[] neighbour : neighbours) {
                int x = current.x + neighbour[0];
                int y = current.y + neighbour[1];

                if (x < 0 || x >= maxX || y < 0 || y >= maxY || grid[y][x] == 1) continue;

                boolean isDiagonal = (neighbour[0] != 0 && neighbour[1] != 0);
                if (isDiagonal && (grid[current.y][x] == 1 || grid[y][current.x] == 1)) continue;

                AStarPathfinder.Node neighbor = new AStarPathfinder.Node(x, y);
                int newGScore = current.gScore + (isDiagonal ? 14 : 10);
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                if (!openSet.contains(neighbor) || newGScore < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = newGScore;
                    neighbor.updateHScore(dstX, dstY);
                    openSet.add(neighbor);
                }
            }
        }
        return new ArrayList<>();
    }


    /**
     * Determines whether there is an unobstructed line of sight between two grid tiles.
     *
     * <p>Traces a ray from the start to the target using Bresenham's line algorithm.
     * A tile blocks sight if it is a wall ({@code grid[y][x] == 1}) or out of bounds.
     * Additionally, the ray is blocked when it would pass diagonally between two adjacent
     * walls (a "diagonal squeeze"), preventing sight from slipping through wall corners.
     *
     * @param startX  the X coordinate of the observer's tile
     * @param startY  the Y coordinate of the observer's tile
     * @param targetX the X coordinate of the target tile
     * @param targetY the Y coordinate of the target tile
     * @return {@code true} if the line of sight is clear all the way to the target;
     *         {@code false} if any wall or boundary obstructs the path
     */
    public boolean isLineOfSightClear(int startX, int startY, int targetX, int targetY) {
        int currentX = startX;
        int currentY = startY;

        int deltaX = Math.abs(targetX - currentX);
        int deltaY = Math.abs(targetY - currentY);

        int stepX = currentX < targetX ? 1 : -1;
        int stepY = currentY < targetY ? 1 : -1;

        int error = deltaX - deltaY;

        while (true) {
            // If the current tile is a wall or out of bounds, sight is blocked
            if (!isWalkable(currentX, currentY)) {
                return false;
            }

            // If we safely reached the target, sight is clear
            if (currentX == targetX && currentY == targetY) {
                return true;
            }

            int doubleError = 2 * error;
            int previousX = currentX;
            int previousY = currentY;

            // Move horizontally if needed
            if (doubleError > -deltaY) {
                error -= deltaY;
                currentX += stepX;
            }

            // Move vertically if needed
            if (doubleError < deltaX) {
                error += deltaX;
                currentY += stepY;
            }

            // Prevent squeezing diagonally between two walls
            if (isDiagonalSqueeze(currentX, currentY, previousX, previousY)) {
                return false;
            }
        }
    }

    /**
     * Finds the nearest walkable grid cell to the given coordinates.
     *
     * <p>If the target cell is already walkable, it is returned immediately.
     * If it is a wall or out of bounds, the coordinates are first clamped to the
     * grid edge, then a BFS expands outward until the closest walkable cell is found.
     *
     * @param dstX the target X grid coordinate (may be out of bounds)
     * @param dstY the target Y grid coordinate (may be out of bounds)
     * @return int[]{x, y} of the nearest walkable cell, or {-1, -1} if none exists
     */
    public int[] findNearestWalkable(int dstX, int dstY) {
        // Clamp out-of-bounds clicks to the grid edge before searching
        int clampedX = Math.max(0, Math.min(dstX, maxX - 1));
        int clampedY = Math.max(0, Math.min(dstY, maxY - 1));

        if (isWalkable(clampedX, clampedY)) return new int[]{clampedX, clampedY};

        Queue<int[]> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();

        queue.add(new int[]{clampedX, clampedY});
        visited.add((long) clampedX + (long) clampedY * maxX);

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            for (int[] n : neighbours) {
                int nx = cell[0] + n[0];
                int ny = cell[1] + n[1];
                if (nx < 0 || nx >= maxX || ny < 0 || ny >= maxY) continue;
                long key = (long) nx + (long) ny * maxX;
                if (visited.contains(key)) continue;
                visited.add(key);
                if (isWalkable(nx, ny)) return new int[]{nx, ny};
                queue.add(new int[]{nx, ny});
            }
        }
        return new int[]{-1, -1};
    }

    /**
     * Checks if a specific grid cell is within bounds and is not a wall.
     */
    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= maxX || y < 0 || y >= maxY) {
            return false;
        }
        return grid[y][x] == 0;
    }

    /**
     * Prevents the player from walking diagonally through the corner of two adjacent walls.
     */
    private boolean isDiagonalSqueeze(int currentX, int currentY, int previousX, int previousY) {
        // If we only moved on one axis, it wasn't a diagonal move.
        if (currentX == previousX || currentY == previousY) {
            return false;
        }

        // If it was diagonal, check the two shared corners. If either is a wall, it's a squeeze.
        return !isWalkable(currentX, previousY) || !isWalkable(previousX, currentY);
    }
}