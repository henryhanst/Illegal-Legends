package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.IsometricTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.FitViewport;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.game.Arena;
import ee.taltech.examplegame.game.GameStateManager;
import ee.taltech.examplegame.game.PlayerInputManager;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.screen.overlay.Hud;
import ee.taltech.examplegame.util.CameraUtil;
import ee.taltech.examplegame.util.ViewportConfig;
import message.GameStateMessage;
import message.LobbyExitMessage;
import message.PlayerLobbyInfo;

import java.util.List;

import static constant.Constants.PPM;

public class GameScreen extends ScreenAdapter {

    private final Game game;
    private final GameStateManager gameStateManager;
    private final PlayerInputManager playerInputManager;

    private final SpriteBatch spriteBatch;
    private final Arena arena;
    private final Hud hud;
    private final int lobbyId;
    private boolean gameOverSoundPlayed;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private TiledMap map;

    private TiledMapRenderer renderer;
    private final CameraHandler cameraHandler;
    private Box2DDebugRenderer debugRenderer;
    private World world;

    private static final boolean USE_ISOMETRIC_DEBUG_MATRIX = true;
    private static final float DEBUG_ISO_ROTATION_DEGREES = 135f;

    private final Matrix4 debugProjection = new Matrix4();
    private final Matrix4 isoDebugTransform = new Matrix4();

    private boolean endScreenShown;

    /**
     * Constructs the main gameplay screen.
     * Initializes rendering systems, HUD, arena, physics world, camera handling,
     * and player input manager. Also loads the map and prepares Box2D collision bodies.
     *
     * @param game the LibGDX Game instance controlling screen transitions
     * @param lobbyId the ID of the multiplayer lobby this game instance belongs to
     * @param lobbyPlayers list of players currently in the lobby used to initialize the HUD
     */
    public GameScreen(Game game, int lobbyId, List<PlayerLobbyInfo> lobbyPlayers) {
        this.game = game;
        gameStateManager = new GameStateManager();

        spriteBatch = new SpriteBatch();
        arena = new Arena(lobbyPlayers);
        hud = new Hud(spriteBatch, lobbyPlayers);
        this.lobbyId = lobbyId;
        box2dWorldGenerator();
        cameraHandler = new CameraHandler(camera);
        playerInputManager = new PlayerInputManager(viewport, arena);
    }

    /**
     * Initializes the Box2D physics world and all map-related rendering systems.
     *
     * This method:
     * - Creates the camera and viewport
     * - Initializes the Box2D physics world
     * - Generates collision bodies from the map
     */
    public void box2dWorldGenerator() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, ViewportConfig.GAME_WORLD_WIDTH, ViewportConfig.GAME_WORLD_HEIGHT);
        this.viewport = new FitViewport(ViewportConfig.GAME_WORLD_WIDTH, ViewportConfig.GAME_WORLD_HEIGHT, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        this.world = new World(new Vector2(0, 0), true);
        this.debugRenderer = new Box2DDebugRenderer(false, false, false, false, false, false);

        TmxMapLoader mapLoader = new TmxMapLoader();
        TmxMapLoader.Parameters mapParameters = new TmxMapLoader.Parameters();
        mapParameters.flipY = true;
        mapParameters.convertObjectToTileSpace = false;
        this.map = mapLoader.load("shared/src/main/java/TMXAssets/level1.tmx", mapParameters);

        buildIsometricDebugTransform();
        initializeCollisionBodies();
        this.renderer = new IsometricTiledMapRenderer(map, 1f / PPM);
    }

    /**
     * Builds the transformation matrix used to correctly display Box2D debug
     * shapes in an isometric coordinate system.
     *
     * This method:
     * - Helps Box2D generate collisions accurately, through rotation.
     */
    private void buildIsometricDebugTransform() {
        MapProperties props = map.getProperties();
        float tileWidth = props.get("tilewidth", Integer.class);
        float tileHeight = props.get("tileheight", Integer.class);
        float mapHeight = props.get("height", Integer.class);

        // Convert transform to world units to match Box2D meters.
        float originX = (mapHeight * tileWidth / 2f) / PPM;
        float scaleX = (float) Math.sqrt(2f);
        float scaleY = (float) Math.sqrt(2f);

        if (tileWidth > tileHeight) {
            scaleY *= tileHeight / tileWidth;
        } else {
            scaleX *= tileWidth / tileHeight;
        }

        isoDebugTransform.idt()
                .translate(originX, 0f, 0f)
                .scale(scaleX, scaleY, 1f)
                .rotate(0f, 0f, 1f, DEBUG_ISO_ROTATION_DEGREES);
    }

    /**
     * Reads the "Collisions" layer from the Tiled map and generates
     * static Box2D bodies for each rectangle object.
     *
     * Each rectangle object is converted into a Box2D polygon shape and
     * added to the physics world.
     */
    private void initializeCollisionBodies() {
        MapLayer collisionLayer = map.getLayers().get("Collisions");
        if (collisionLayer == null) {
            return;
        }

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape shape = new PolygonShape();

        for (RectangleMapObject object : collisionLayer.getObjects().getByType(RectangleMapObject.class)) {
            Rectangle rectangle = object.getRectangle();

            float cx = rectangle.getX() + rectangle.getWidth()  / 2f;
            float cy = rectangle.getY() + rectangle.getHeight() / 2f;
            Vector2 worldPos = tileToWorld(cx, cy);
            float w = rectangle.getWidth()  / PPM;
            float h = rectangle.getHeight() / PPM;

            if (w <= 0f || h <= 0f) {
                continue;
            }
            bodyDef.position.set(worldPos);

            Body body = world.createBody(bodyDef);
            shape.setAsBox(w / 2f, h / 2f);
            fixtureDef.shape = shape;
            body.createFixture(fixtureDef);
        }

        shape.dispose();
    }

    /**
     * Converts a position from Tiled map pixel coordinates into Box2D world
     * coordinates compatible with the isometric projection.
     *
     * The transformation:
     * - Flips the Y-axis because Tiled and LibGDX coordinate systems differ
     * - Converts pixels to meters using the PPM constant
     * - Applies offsets to align physics bodies with rendered isometric tiles
     *
     * @param pixelX x coordinate in map pixel space
     * @param pixelY y coordinate in map pixel space
     * @return the equivalent position in Box2D world coordinates
     */
    private Vector2 tileToWorld(float pixelX, float pixelY) {
        MapProperties props = map.getProperties();
        float tileWidth  = props.get("tilewidth",  Integer.class);
        float tileHeight = props.get("tileheight", Integer.class);
        int   mapHeight  = props.get("height",     Integer.class);
        int   mapWidth   = props.get("width",      Integer.class);

        // Account for flipped Y-axis (flipY = true in TMX loader)
        float mapHeightPixels = mapHeight * tileHeight;
        float mapWidthPixels = mapWidth * tileWidth;

        // Flip X-axis based on map width
        float y = ((mapHeightPixels - pixelY) / PPM );
        float x = ((mapWidthPixels - pixelX) / PPM);

        // Adjustable offsets to align with isometric rendering
        // Increase these values to move collisions up and left more
        float offsetX = 26.5f;  // Offset in meters - adjust to move left/right
        float offsetY = 11.93f;  // Offset in meters - adjust to move up/down
        x -= offsetX;
        y -= offsetY;

        return new Vector2(x, y);
    }

    /**
     * Main render loop executed every frame.
     *
     * Responsible for:
     * - Clearing the screen
     * - Updating the camera position based on player state
     * - Rendering the isometric tile map
     * - Stepping the Box2D physics simulation (step is 1 tick for physics)
     * - Rendering Box2D debug shapes
     * - Processing player input
     * - Updating and rendering game entities and HUD
     *
     * @param delta time elapsed since the last frame
     */
    @Override
    public void render(float delta) {
        super.render(delta);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        GameStateMessage currentGameState = gameStateManager.getLatestGameStateMessage();
        if (!gameOverSoundPlayed && currentGameState.getNexusStates().stream().anyMatch(nexus -> nexus.isDestroyed())) {
            ((Main) game).getAudioManager().playGameOverSound();
            gameOverSoundPlayed = true;
        }

        // Check if game has ended and transition to end screen
        if (!endScreenShown && currentGameState.getNexusStates().stream().anyMatch(nexus -> nexus.isDestroyed())) {
            endScreenShown = true;

            // Determine which team won (the team whose nexus is still standing)
            boolean blueNexusDestroyed = currentGameState.getNexusStates().stream()
                    .anyMatch(nexus -> nexus.isDestroyed() && nexus.getTeam() == message.dto.Team.TEAM_BLUE);
            boolean redNexusDestroyed = currentGameState.getNexusStates().stream()
                    .anyMatch(nexus -> nexus.isDestroyed() && nexus.getTeam() == message.dto.Team.TEAM_RED);

            // Determine player's team and if they won
            message.dto.Team playerTeam = arena.getLocalPlayerTeam();
            boolean playerWon = (blueNexusDestroyed && playerTeam == message.dto.Team.TEAM_RED) ||
                    (redNexusDestroyed && playerTeam == message.dto.Team.TEAM_BLUE);

            game.setScreen(new EndScreen(game, lobbyId, playerWon, playerTeam));
        }
        Vector3 camVector = CameraUtil.getCameraVector(currentGameState, ServerConnection.getInstance().getClient().getID());
        cameraHandler.update(camVector, delta);
        camera.update();
        renderer.setView(camera);
        renderer.render();
        world.step(delta, 6, 2);

        if (USE_ISOMETRIC_DEBUG_MATRIX) {
            debugProjection.set(camera.combined).mul(isoDebugTransform);
            debugRenderer.render(world, debugProjection);
        } else {
            debugRenderer.render(world, camera.combined);
        }

        playerInputManager.handleMovementInput();
        playerInputManager.handleShootingInput();
        handleScreenNavigation(game);

        arena.update(currentGameState);
        arena.updateNexus(currentGameState);
        arena.updateTurret(currentGameState);
        arena.updateMinion(currentGameState);

        if (arena.hasTurretDestroyedThisFrame()) {
            ((Main) game).getAudioManager().playTurretDestroyedSound();
        }
        hud.update(currentGameState);

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        arena.render(spriteBatch, camera);
        spriteBatch.end();

        // Render damage vignette (blood red edges when local player takes damage)
        spriteBatch.begin();
        arena.damageVignette.render(spriteBatch, camera);
        spriteBatch.end();

        hud.render(camera, viewport);
    }


    /**
     * Handles keyboard input for screen navigation.
     *
     * Currently listens for the ESCAPE key which opens the pause menu.
     * The pause menu allows the player to continue, access settings, or exit the game.
     *
     * @param game the LibGDX Game instance used to change screens
     */
    private void handleScreenNavigation(Game game) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new PauseScreen(game, this, lobbyId));
        }
    }


    @Override
    public void show() {
        ((Main) game).getAudioManager().stopBackgroundMusic();
    }

    @Override
    public void dispose() {
        hud.dispose();
        spriteBatch.dispose();
        map.dispose();
        debugRenderer.dispose();
        world.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height);
    }
}
