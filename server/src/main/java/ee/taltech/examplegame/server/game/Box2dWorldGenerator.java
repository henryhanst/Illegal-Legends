package ee.taltech.examplegame.server.game;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import ee.taltech.examplegame.server.TMXLoaders.HijackedTmxLoader;
import ee.taltech.examplegame.server.TMXLoaders.MyServer;
import lombok.Getter;

import java.util.Arrays;

import static constant.Constants.PPM;

public class Box2dWorldGenerator {

    // Configuration Constants
    private static final String DEFAULT_MAP_PATH = "shared/src/main/java/TMXAssets/level1.tmx";

    private static final float DEBUG_ISO_ROTATION_DEGREES = 135f;
    private static final float COLLISION_OFFSET_X = 26.5f;
    private static final float COLLISION_OFFSET_Y = 11.93f;

    // Grid properties
    private static final float GRID_CELL_SIZE = 20f;

    // Pathfinding padding (Expands walls by the player's half-size so they don't clip corners)
    private static final float PATHFINDING_PADDING_X = 25f;
    private static final float PATHFINDING_PADDING_Y = 30f;

    // Cached State
    @Getter
    private final TiledMap map;

    // Cached Map Dimensions (Pixels)
    private final float mapWidthPixels;
    private final float mapHeightPixels;

    // Cached Isometric Transforms (Pre-computed for maximum performance)
    private final Matrix4 isoTransform;
    private final Matrix4 inverseIsoTransform;

    /**
     * Initializes the generator, loads the map into memory, and pre-computes often used mathematical transforms.
     */
    public Box2dWorldGenerator() {
        HijackedTmxLoader.Parameters parameters = new HijackedTmxLoader.Parameters();
        parameters.flipY = true;
        parameters.convertObjectToTileSpace = false;

        this.map = new HijackedTmxLoader(new MyServer.MyFileHandleResolver()).load(DEFAULT_MAP_PATH, parameters);

        // Cache map dimensions to avoid expensive property lookups during active gameplay
        MapProperties props = map.getProperties();
        int mapWidthTiles = props.get("width", Integer.class);
        int mapHeightTiles = props.get("height", Integer.class);
        int tileWidth = props.get("tilewidth", Integer.class);
        int tileHeight = props.get("tileheight", Integer.class);

        this.mapWidthPixels = mapWidthTiles * tileWidth;
        this.mapHeightPixels = mapHeightTiles * tileHeight;

        // Pre-compute the isometric transformation matrices once
        this.isoTransform = buildIsometricTransform(mapHeightTiles, tileWidth, tileHeight);
        this.inverseIsoTransform = new Matrix4(this.isoTransform).inv();
    }


    /**
     * Reads the "Collisions" layer from the Tiled map and injects static polygon bodies into the provided Box2D world.
     *
     * @param world the Box2D physics world.
     */
    public void initializeWorld(World world) {
        MapLayer mapLayer = map.getLayers().get("Collisions");
        if (mapLayer == null) return;

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        PolygonShape shape = new PolygonShape();
        FixtureDef fdef = new FixtureDef();

        for (RectangleMapObject object : mapLayer.getObjects().getByType(RectangleMapObject.class)) {
            Rectangle rectangle = object.getRectangle();
            float width = rectangle.getWidth() / PPM;
            float height = rectangle.getHeight() / PPM;

            if (width <= 0f || height <= 0f) continue;

            Vector2 rawCenter = tileToWorld(
                    rectangle.getX() + rectangle.getWidth() / 2f,
                    rectangle.getY() + rectangle.getHeight() / 2f
            );

            Vector2 worldPosition = applyIsometricTransform(rawCenter.x, rawCenter.y);
            bdef.position.set(worldPosition);

            float halfWidth = width / 2f;
            float halfHeight = height / 2f;
            float[] vertices = new float[8];

            setVertex(vertices, 0, rawCenter.x - halfWidth, rawCenter.y - halfHeight, worldPosition);
            setVertex(vertices, 2, rawCenter.x + halfWidth, rawCenter.y - halfHeight, worldPosition);
            setVertex(vertices, 4, rawCenter.x + halfWidth, rawCenter.y + halfHeight, worldPosition);
            setVertex(vertices, 6, rawCenter.x - halfWidth, rawCenter.y + halfHeight, worldPosition);

            Body body = world.createBody(bdef);
            shape.set(vertices);
            fdef.shape = shape;
            body.createFixture(fdef);
        }

        shape.dispose();
    }


    /**
     * Parses the map collision objects into an optimized 2D integer array for A* pathfinding.
     * Expands the size of all walls by the player's dimensions so pathfinding inherently avoids corner-snagging.
     *
     * @return 2D array where 0 = Walkable, 1 = Wall/Blocked.
     */
    public int[][] getPathfindingGrid() {
        MapLayer mapLayer = map.getLayers().get("Collisions");
        if (mapLayer == null) return new int[0][0];

        int gridW = (int) (mapWidthPixels / GRID_CELL_SIZE);
        int gridH = (int) (mapHeightPixels / GRID_CELL_SIZE);

        int[][] grid = new int[gridH][gridW];
        for (int[] row : grid) {
            Arrays.fill(row, 0);
        }

        for (RectangleMapObject object : mapLayer.getObjects().getByType(RectangleMapObject.class)) {
            Rectangle rect = object.getRectangle();

            // Expand each wall rectangle by the player's half-size on every side
            float expandedX = rect.x - PATHFINDING_PADDING_X;
            float expandedY = rect.y - PATHFINDING_PADDING_Y;
            float expandedWidth = rect.width + (PATHFINDING_PADDING_X * 2);
            float expandedHeight = rect.height + (PATHFINDING_PADDING_Y * 2);

            float flippedY = mapHeightPixels - expandedY - expandedHeight;

            int rawStartCol = (int) (expandedX / GRID_CELL_SIZE);
            int rawStartRow = (int) (flippedY / GRID_CELL_SIZE);
            int rawEndCol = rawStartCol + Math.max(1, (int) (expandedWidth / GRID_CELL_SIZE));
            int rawEndRow = rawStartRow + Math.max(1, (int) (expandedHeight / GRID_CELL_SIZE));

            int startCol = Math.max(0, rawStartCol);
            int startRow = Math.max(0, rawStartRow);
            int endCol = Math.min(gridW, rawEndCol);
            int endRow = Math.min(gridH, rawEndRow);

            for (int row = startRow; row < endRow; row++) {
                for (int col = startCol; col < endCol; col++) {
                    grid[row][col] = 1;
                }
            }
        }

        return grid;
    }


    /**
     * Converts a Grid Cell (col, row) back into visual Isometric Map coordinates.
     */
    public Vector2 gridToMapCoordinates(int col, int row) {
        // Grid cell -> Tiled pixel space
        float pixelX = col * GRID_CELL_SIZE + (GRID_CELL_SIZE / 2f);
        float pixelY = mapHeightPixels - (row * GRID_CELL_SIZE + (GRID_CELL_SIZE / 2f));

        // Tiled pixels -> Box2D world space
        Vector2 rawWorld = tileToWorld(pixelX, pixelY);
        Vector2 worldPos = applyIsometricTransform(rawWorld.x, rawWorld.y);

        // Scale up to map coordinates based on PPM
        return new Vector2(worldPos.x * PPM, worldPos.y * PPM);
    }

    /**
     * Converts a visual Isometric Map coordinate into a Pathfinding Grid Cell [col, row].
     */
    public int[] mapCoordinatesToGrid(float mapX, float mapY) {
        // Remove PPM scaling
        float worldX = mapX / PPM;
        float worldY = mapY / PPM;

        // Invert the isometric transform
        Vector3 raw = new Vector3(worldX, worldY, 0f).mul(inverseIsoTransform);

        // Invert tileToWorld logic
        float pixelX = mapWidthPixels - (raw.x + COLLISION_OFFSET_X) * PPM;
        float pixelY = mapHeightPixels - (raw.y + COLLISION_OFFSET_Y) * PPM;

        // Map Pixels -> Grid Cell indices
        int col = (int) (pixelX / GRID_CELL_SIZE);
        int row = (int) ((mapHeightPixels - pixelY) / GRID_CELL_SIZE);

        return new int[]{col, row};
    }


    /**
     * Converts a point given in map pixels into Box2D world coordinates (meters).
     */
    private Vector2 tileToWorld(float pixelX, float pixelY) {
        float x = (mapWidthPixels - pixelX) / PPM;
        float y = (mapHeightPixels - pixelY) / PPM;

        x -= COLLISION_OFFSET_X;
        y -= COLLISION_OFFSET_Y;

        return new Vector2(x, y);
    }


    /**
     * Applies the pre-computed Isometric Matrix to an X,Y coordinate.
     */
    private Vector2 applyIsometricTransform(float x, float y) {
        Vector3 localVector = new Vector3(x, y, 0f).mul(isoTransform);
        return new Vector2(localVector.x, localVector.y);
    }


    private void setVertex(float[] vertices, int index, float x, float y, Vector2 center) {
        Vector2 transformed = applyIsometricTransform(x, y);
        vertices[index] = transformed.x - center.x;
        vertices[index + 1] = transformed.y - center.y;
    }


    /**
     * Builds the Isometric Matrix used to map straight physics math to isometric diamond visuals.
     */
    private Matrix4 buildIsometricTransform(int mapHeightTiles, int tileWidth, int tileHeight) {
        float originX = (mapHeightTiles * tileWidth / 2f) / PPM;
        float scaleX = (float) Math.sqrt(2f);
        float scaleY = (float) Math.sqrt(2f);

        if (tileWidth > tileHeight) {
            scaleY *= (float) tileHeight / tileWidth;
        } else {
            scaleX *= (float) tileWidth / tileHeight;
        }

        return new Matrix4().idt()
                .translate(originX, 0f, 0f)
                .scale(scaleX, scaleY, 1f)
                .rotate(0f, 0f, 1f, DEBUG_ISO_ROTATION_DEGREES);
    }
}
