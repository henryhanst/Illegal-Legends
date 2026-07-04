package ee.taltech.examplegame.screen.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.util.ViewportConfig;
import message.GameStateMessage;
import message.PlayerLobbyInfo;
import message.dto.ChampionType;
import message.dto.NexusState;
import message.dto.PlayerState;
import message.dto.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static ee.taltech.examplegame.component.LabelComponents.createLabel;
import static constant.Constants.MAX_PLAYER_LEVEL;

/**
 * In-game overlay for top status information and the custom bottom HUD cluster.
 *
 * The bottom HUD currently contains the champion portrait, level badge, HP/XP bars,
 * ability placeholders.
 */
public class Hud {

    private static final int LABEL_SIZE = 14;
    private static final int HP_LABEL_SIZE = 12;
    private static final int COOLDOWN_LABEL_SIZE = 16;
    private static final int ABILITY_ICON_GRID_SIZE = 5;
    private static final int TABLE_PADDING_TOP = 0;
    private static final float BOTTOM_HUD_PAD_BOTTOM = 14f;
    public static final int GAME_STATUS_LABEL_PADDING_TOP = 300;
    private static final float BASE_UI_WIDTH = 640f;
    private static final float BASE_UI_HEIGHT = 480f;
    private final Stage stage;
    private final Viewport viewport;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final int localPlayerId;
    private final List<Texture> textures = new ArrayList<>();
    private final List<PlayerState> latestPlayerStates = new ArrayList<>();

    private final Texture whitePixelTexture = createSolidTexture(Color.WHITE);
    private final Texture portraitOuterCircleTexture = createCircleTexture(64, Color.BLACK);
    private final Texture portraitInnerCircleTexture = createCircleTexture(64, new Color(0.12f, 0.12f, 0.12f, 1f));
    private final Texture levelOuterCircleTexture = createCircleTexture(64, Color.BLACK);
    private final Texture levelInnerCircleTexture = createCircleTexture(64, new Color(0.22f, 0.16f, 0.08f, 1f));
    private final Texture frameOuterTexture = createSolidTexture(Color.BLACK);
    private final Texture frameInnerTexture = createSolidTexture(new Color(0.12f, 0.12f, 0.12f, 1f));
    private final Texture topInfoOuterTexture = createSolidTexture(Color.BLACK);
    private final Texture topInfoInnerTexture = createSolidTexture(new Color(0.1f, 0.18f, 0.34f, 0.92f));
    private final Texture abilityPlaceholderTexture = loadTexture("sprites/bullet.png");
    private final Texture fighterQAbilityTexture = createAbilityPicture("sprites/AbilityIcons.png", 4, 0);
    private final Texture fighterWAbilityTexture = createAbilityPicture("sprites/AbilityIcons.png", 0, 1);
    private final Texture rangedQAbilityTexture = createAbilityPicture("sprites/AbilityIcons.png", 1, 1);
    private final Texture rangedWAbilityTexture = createAbilityPicture("sprites/AbilityIcons.png", 0, 3);
    private final Texture tankQAbilityTexture = createAbilityPicture("sprites/AbilityIcons.png", 1, 2);
    private final Texture tankWAbilityTexture = createAbilityPicture("sprites/AbilityIcons.png", 4, 4);

    private final Texture fighterPortraitTexture = createCircularPortrait("sprites/fighter/fighter-walking.png", 64, 64, 2, 0);
    private final Texture rangedPortraitTexture = createCircularPortrait("sprites/ranged/ranged-walking.png", 64, 64, 2, 0);
    private final Texture tankPortraitTexture = createCircularPortrait("sprites/tank/tank-walking.png", 64, 64, 2, 0);
    private Team localPlayerTeam;
    private ChampionType localChampionType = ChampionType.NONE;
    private int localKillCount;
    private int localDeathCount;
    private float displayedQCooldownRemainingMs;
    private float displayedQCooldownTotalMs;
    private float displayedWCooldownRemainingMs;
    private float displayedWCooldownTotalMs;
    private long lastServerQCooldownRemainingMs = -1L;
    private long lastServerQCooldownTotalMs = -1L;
    private long lastServerWCooldownRemainingMs = -1L;
    private long lastServerWCooldownTotalMs = -1L;

    private final Label timeLabel = createLabel("0:00", Color.WHITE, LABEL_SIZE);
    private final Label killDeathLabel = createLabel("0 / 0", Color.WHITE, LABEL_SIZE);
    private final Label gameStatusLabel = createLabel("Waiting for other player...", Color.BLACK, LABEL_SIZE);
    private final Label localPlayerHpLabel = createLabel("0 / 0", Color.BLACK, HP_LABEL_SIZE);
    private final Label localPlayerLevelLabel = createLabel("1", Color.WHITE, HP_LABEL_SIZE);
    private final Label firstAbilityLabel = createLabel("Q", Color.WHITE, HP_LABEL_SIZE);
    private final Label secondAbilityLabel = createLabel("W", Color.WHITE, HP_LABEL_SIZE);
    private final Label firstAbilityCooldownLabel = createLabel("", Color.WHITE, COOLDOWN_LABEL_SIZE);
    private final Label secondAbilityCooldownLabel = createLabel("", Color.WHITE, COOLDOWN_LABEL_SIZE);

    private final HudBar localPlayerHpBar = new HudBar(
        whitePixelTexture,
        Color.BLACK,
        new Color(0.3f, 0.08f, 0.08f, 1f),
        new Color(0.12f, 0.75f, 0.18f, 1f)
    );
    private final HudBar localPlayerXpBar = new HudBar(
        whitePixelTexture,
        Color.BLACK,
        new Color(0.04f, 0.04f, 0.04f, 1f),
        new Color(0.53f, 0.21f, 0.74f, 1f)
    );
    private final FramedTextureActor championPortrait = new FramedTextureActor(
        portraitOuterCircleTexture,
        portraitInnerCircleTexture,
        this::getPortraitTexture
    );
    private final FramedTextureActor levelBadgeBackground = new FramedTextureActor(
        levelOuterCircleTexture,
        levelInnerCircleTexture,
        null
    );
    private final FramedTextureActor firstAbilityPlaceholder = new FramedTextureActor(
        frameOuterTexture,
        frameInnerTexture,
        this::getQAbilityTexture
    );
    private final FramedTextureActor secondAbilityPlaceholder = new FramedTextureActor(
        frameOuterTexture,
        frameInnerTexture,
        this::getWAbilityTexture
    );
    private final CooldownOverlayActor firstAbilityCooldownOverlay = new CooldownOverlayActor(whitePixelTexture);
    private final CooldownOverlayActor secondAbilityCooldownOverlay = new CooldownOverlayActor(whitePixelTexture);

    private final float widthScale = ViewportConfig.UI_WIDTH / BASE_UI_WIDTH;
    private final float heightScale = ViewportConfig.UI_HEIGHT / BASE_UI_HEIGHT;

    /**
     * Simple manually drawn fill bar used for health and XP.
     * Using a custom actor keeps thickness and colors under our control.
     */
    private static class HudBar extends Actor {
        private static final float BORDER = 1.2f;

        private final Texture pixel;
        private final Color outerColor;
        private final Color innerColor;
        private final Color fillColor;
        private float progress;

        private HudBar(Texture pixel, Color outerColor, Color innerColor, Color fillColor) {
            this.pixel = pixel;
            this.outerColor = new Color(outerColor);
            this.innerColor = new Color(innerColor);
            this.fillColor = new Color(fillColor);
        }

        public void setProgress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color previousColor = batch.getColor().cpy();
            float innerX = getX() + BORDER;
            float innerY = getY() + BORDER;
            float innerWidth = Math.max(0f, getWidth() - BORDER * 2f);
            float innerHeight = Math.max(0f, getHeight() - BORDER * 2f);

            batch.setColor(outerColor.r, outerColor.g, outerColor.b, parentAlpha);
            batch.draw(pixel, getX(), getY(), getWidth(), getHeight());

            batch.setColor(innerColor.r, innerColor.g, innerColor.b, parentAlpha);
            batch.draw(pixel, innerX, innerY, innerWidth, innerHeight);

            batch.setColor(fillColor.r, fillColor.g, fillColor.b, parentAlpha);
            batch.draw(pixel, innerX, innerY, innerWidth * progress, innerHeight);

            batch.setColor(previousColor);
        }
    }

    /**
     * Reusable framed actor for square/circular HUD elements such as portrait and ability placeholders.
     */
    private static class FramedTextureActor extends Actor {
        private static final float BORDER = 2f;

        private final Texture outerTexture;
        private final Texture innerTexture;
        private final Supplier<Texture> contentSupplier;

        private FramedTextureActor(Texture outerTexture, Texture innerTexture, Supplier<Texture> contentSupplier) {
            this.outerTexture = outerTexture;
            this.innerTexture = innerTexture;
            this.contentSupplier = contentSupplier;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color previousColor = batch.getColor().cpy();
            float innerX = getX() + BORDER;
            float innerY = getY() + BORDER;
            float innerWidth = Math.max(0f, getWidth() - BORDER * 2f);
            float innerHeight = Math.max(0f, getHeight() - BORDER * 2f);

            batch.setColor(1f, 1f, 1f, parentAlpha);
            batch.draw(outerTexture, getX(), getY(), getWidth(), getHeight());
            batch.draw(innerTexture, innerX, innerY, innerWidth, innerHeight);

            if (contentSupplier != null) {
                Texture contentTexture = contentSupplier.get();
                if (contentTexture != null) {
                    batch.draw(contentTexture, innerX, innerY, innerWidth, innerHeight);
                }
            }

            batch.setColor(previousColor);
        }
    }

    /**
     * Draws a dark fill over an ability icon while that ability is on cooldown.
     */
    private class CooldownOverlayActor extends Actor {
        private static final float OVERLAY_INSET = 2f;
        private static final float OVERLAY_ALPHA = 0.7f;

        private float progress;

        private CooldownOverlayActor(Texture pixel) {
        }

        public void setProgress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (progress <= 0f) {
                return;
            }

            float left = getX() + OVERLAY_INSET;
            float bottom = getY() + OVERLAY_INSET;
            float width = Math.max(0f, getWidth() - OVERLAY_INSET * 2f);
            float height = Math.max(0f, getHeight() - OVERLAY_INSET * 2f);
            if (width <= 0f || height <= 0f) {
                return;
            }

            List<float[]> boundaryPoints = buildSquareBoundaryPath(1f - progress, left, bottom, width, height);
            float centerX = left + width / 2f;
            float centerY = bottom + height / 2f;

            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, OVERLAY_ALPHA * parentAlpha);
            drawSquareCooldownOverlay(centerX, centerY, boundaryPoints);
            shapeRenderer.end();
            batch.begin();
        }

        private List<float[]> buildSquareBoundaryPath(float startProgress, float left, float bottom, float width, float height) {
            float clampedStart = Math.max(0f, Math.min(1f, startProgress));
            List<float[]> points = new ArrayList<>();
            points.add(getPointOnSquarePerimeter(clampedStart, left, bottom, width, height));

            float[] cornerProgresses = new float[] {1f / 8f, 3f / 8f, 5f / 8f, 7f / 8f, 1f};
            for (float cornerProgress : cornerProgresses) {
                if (cornerProgress > clampedStart && cornerProgress < 1f) {
                    points.add(getPointOnSquarePerimeter(cornerProgress, left, bottom, width, height));
                }
            }

            points.add(getPointOnSquarePerimeter(1f, left, bottom, width, height));
            return points;
        }

        private float[] getPointOnSquarePerimeter(float progress, float left, float bottom, float width, float height) {
            float top = bottom + height;
            float centerX = left + width / 2f;
            float perimeterDistance = Math.max(0f, Math.min(1f, progress)) * (width * 4f);
            float halfTopEdge = width / 2f;

            if (perimeterDistance <= halfTopEdge) {
                return new float[] {centerX + perimeterDistance, top};
            }

            perimeterDistance -= halfTopEdge;
            if (perimeterDistance <= height) {
                return new float[] {left + width, top - perimeterDistance};
            }

            perimeterDistance -= height;
            if (perimeterDistance <= width) {
                return new float[] {left + width - perimeterDistance, bottom};
            }

            perimeterDistance -= width;
            if (perimeterDistance <= height) {
                return new float[] {left, bottom + perimeterDistance};
            }

            perimeterDistance -= height;
            return new float[] {left + perimeterDistance, top};
        }

        private void drawSquareCooldownOverlay(float centerX, float centerY, List<float[]> boundaryPoints) {
            for (int i = 0; i < boundaryPoints.size() - 1; i++) {
                float[] first = boundaryPoints.get(i);
                float[] second = boundaryPoints.get(i + 1);
                shapeRenderer.triangle(centerX, centerY, first[0], first[1], second[0], second[1]);
            }
        }
    }

    public Hud(SpriteBatch spriteBatch, List<PlayerLobbyInfo> initialPlayers) {
        this.localPlayerId = ServerConnection.getInstance().getClient().getID();
        for (PlayerLobbyInfo playerLobbyInfo : initialPlayers) {
            if (playerLobbyInfo.getConnectionId() == localPlayerId) {
                this.localPlayerTeam = playerLobbyInfo.getTeam();
                break;
            }
        }

        viewport = new FitViewport(ViewportConfig.UI_WIDTH, ViewportConfig.UI_HEIGHT, new OrthographicCamera());
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        // Create a stage to render the HUD content
        stage = new Stage(viewport, spriteBatch);

        Table rootTable = createRootTable();
        rootTable.setDebug(false);
        stage.addActor(rootTable);
    }

    /**
     * Root layout: top match info and bottom gameplay HUD.
     */
    private Table createRootTable() {
        Table rootTable = new Table();
        rootTable.setFillParent(true);

        rootTable.add(createTopHudTable()).expandX().fillX().top();
        rootTable.row();
        rootTable.add(createBottomHudTable()).expand().fill().bottom();

        return rootTable;
    }

    /**
     * Top overlay with names, timer and match status text.
     */
    private Table createTopHudTable() {
        Table table = new Table();
        table.top();
        table.padTop(scaleY(TABLE_PADDING_TOP));

        killDeathLabel.setAlignment(Align.center);
        Stack topInfoChip = createTopInfoChip(killDeathLabel, scaleX(150f), scaleY(24f));
        table.add().expandX();
        table.add(topInfoChip).right().top().padRight(0f);
        table.row().expandX();

        table.add(gameStatusLabel)
            .colspan(3)
            .padTop(scaleY(GAME_STATUS_LABEL_PADDING_TOP))
            .align(Align.center);

        return table;
    }

    /**
     * Bottom HUD cluster with portrait, bars and abilities.
     */
    private Table createBottomHudTable() {
        Table bottomTable = new Table();
        bottomTable.bottom();

        Stack hpStack = new Stack();
        localPlayerHpLabel.setAlignment(Align.center);
        hpStack.add(localPlayerHpBar);
        hpStack.add(localPlayerHpLabel);

        Table abilitiesTable = new Table();
        abilitiesTable.defaults().size(scaleY(28f), scaleY(28f));
        // Small ability placeholders sit above the HP/XP bars similar to a MOBA HUD.
        Stack firstAbilityStack = new Stack();
        firstAbilityLabel.setAlignment(Align.bottomRight);
        firstAbilityCooldownLabel.setAlignment(Align.center);
        firstAbilityStack.add(firstAbilityPlaceholder);
        firstAbilityStack.add(firstAbilityCooldownOverlay);
        firstAbilityStack.add(firstAbilityCooldownLabel);
        firstAbilityStack.add(firstAbilityLabel);

        Stack secondAbilityStack = new Stack();
        secondAbilityLabel.setAlignment(Align.bottomRight);
        secondAbilityCooldownLabel.setAlignment(Align.center);
        secondAbilityStack.add(secondAbilityPlaceholder);
        secondAbilityStack.add(secondAbilityCooldownOverlay);
        secondAbilityStack.add(secondAbilityCooldownLabel);
        secondAbilityStack.add(secondAbilityLabel);

        abilitiesTable.add(firstAbilityStack).padRight(scaleX(4f));
        abilitiesTable.add(secondAbilityStack);

        Table barsTable = new Table();
        barsTable.add(abilitiesTable).left().padBottom(scaleY(4f));
        barsTable.row();
        barsTable.add(hpStack).width(scaleX(300f)).height(scaleY(15f)).center();
        barsTable.row();
        barsTable.add(localPlayerXpBar).width(scaleX(300f)).height(scaleY(8f)).padTop(scaleY(3f)).center();

        Stack portraitStack = new Stack();
        portraitStack.add(championPortrait);

        Stack levelStack = new Stack();
        localPlayerLevelLabel.setAlignment(Align.center);
        levelStack.add(levelBadgeBackground);
        levelStack.add(localPlayerLevelLabel);

        Table levelOverlay = new Table();
        levelOverlay.bottom().right();
        levelOverlay.add(levelStack).size(scaleY(18f), scaleY(18f));
        portraitStack.add(levelOverlay);

        Table hudCluster = new Table();
        hudCluster.add(portraitStack).size(scaleY(55f), scaleY(55f)).padRight(scaleX(6f)).bottom();
        hudCluster.add(barsTable).bottom();

        bottomTable.add(hudCluster).expandX().left().padLeft(scaleX(120f)).padBottom(scaleY(BOTTOM_HUD_PAD_BOTTOM));

        return bottomTable;
    }

    /**
     * Creates a compact framed background behind top-right info labels.
     */
    private Stack createTopInfoChip(Label label, float width, float height) {
        Stack stack = new Stack();
        stack.add(new FramedTextureActor(topInfoOuterTexture, topInfoInnerTexture, null));
        Table contentTable = new Table();
        contentTable.add(label)
            .padLeft(scaleX(8f))
            .padRight(scaleX(8f))
            .padTop(scaleY(2f))
            .padBottom(scaleY(2f));
        stack.add(contentTable);
        stack.setSize(width, height);
        return stack;
    }

    /**
     * Scales old 640-based HUD X measurements to the configured UI width.
     */
    private float scaleX(float value) {
        return value * widthScale;
    }

    /**
     * Scales old 480-based HUD Y measurements to the configured UI height.
     */
    private float scaleY(float value) {
        return value * heightScale;
    }

    /**
     * Selects the portrait texture that matches the local player's current champion.
     */
    private Texture getPortraitTexture() {
        return switch (localChampionType) {
            case FIGHTER -> fighterPortraitTexture;
            case RANGED -> rangedPortraitTexture;
            case TANK -> tankPortraitTexture;
            case NONE -> null;
        };
    }

    private Texture getQAbilityTexture() {
        return switch (localChampionType) {
            case FIGHTER -> fighterQAbilityTexture;
            case RANGED -> rangedQAbilityTexture;
            case TANK -> tankQAbilityTexture;
            case NONE -> abilityPlaceholderTexture;
        };
    }

    private Texture getWAbilityTexture() {
        return switch (localChampionType) {
            case FIGHTER -> fighterWAbilityTexture;
            case RANGED -> rangedWAbilityTexture;
            case TANK -> tankWAbilityTexture;
            case NONE -> abilityPlaceholderTexture;
        };
    }

    /**
     * Creates a 1-color texture that can be stretched for borders/backgrounds.
     */
    private Texture createSolidTexture(Color color) {
        Pixmap pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        textures.add(texture);
        return texture;
    }

    /**
     * Loads a regular texture and tracks it for disposal with the HUD.
     */
    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        textures.add(texture);
        return texture;
    }

    /**
     * Crops one icon cell from the shared 5x5 ability icon sheet.
     * Row and column indexes are zero-based.
     */
    private Texture createAbilityPicture(String filePath, int rowIndex, int columnIndex) {
        Pixmap sourcePixmap = new Pixmap(Gdx.files.internal(filePath));
        int iconWidth = sourcePixmap.getWidth() / ABILITY_ICON_GRID_SIZE;
        int iconHeight = sourcePixmap.getHeight() / ABILITY_ICON_GRID_SIZE;
        Pixmap iconPixmap = new Pixmap(iconWidth, iconHeight, Pixmap.Format.RGBA8888);

        int srcX = columnIndex * iconWidth;
        int srcY = rowIndex * iconHeight;
        iconPixmap.drawPixmap(
            sourcePixmap,
            0,
            0,
            srcX,
            srcY,
            iconWidth,
            iconHeight
        );

        Texture texture = new Texture(iconPixmap);
        iconPixmap.dispose();
        sourcePixmap.dispose();
        textures.add(texture);
        return texture;
    }

    /**
     * Crops a chosen sprite frame into a circular portrait texture.
     * The selected row/column currently uses the champion facing downward.
     */
    private Texture createCircularPortrait(String filePath, int frameWidth, int frameHeight, int rowIndex, int columnIndex) {
        Pixmap sourcePixmap = new Pixmap(Gdx.files.internal(filePath));
        Pixmap portraitPixmap = new Pixmap(frameWidth, frameHeight, Pixmap.Format.RGBA8888);
        portraitPixmap.setColor(0f, 0f, 0f, 0f);
        portraitPixmap.fill();

        int srcX = columnIndex * frameWidth;
        int srcY = rowIndex * frameHeight;
        float radius = Math.min(frameWidth, frameHeight) / 2f;
        float centerX = (frameWidth - 1) / 2f;
        float centerY = (frameHeight - 1) / 2f;

        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                float dx = x - centerX;
                float dy = y - centerY;
                if ((dx * dx) + (dy * dy) <= radius * radius) {
                    portraitPixmap.drawPixel(x, y, sourcePixmap.getPixel(srcX + x, srcY + y));
                }
            }
        }

        Texture texture = new Texture(portraitPixmap);
        portraitPixmap.dispose();
        sourcePixmap.dispose();
        textures.add(texture);
        return texture;
    }

    /**
     * Creates a filled circle texture for round HUD frames/badges.
     */
    private Texture createCircleTexture(int size, Color color) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillCircle(size / 2, size / 2, size / 2);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        textures.add(texture);
        return texture;
    }

    /**
     * Draws the HUD stage every frame.
     */
    public void render(OrthographicCamera worldCamera, Viewport worldViewport) {
        tickDisplayedCooldowns(Gdx.graphics.getDeltaTime());
        refreshAbilityCooldowns();
        stage.draw();
    }

    /**
     * Keeps the HUD viewport aligned to the current window size.
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * Releases the stage and all textures created by this HUD.
     */
    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
        for (Texture texture : textures) {
            texture.dispose();
        }
    }

    /**
     * Applies the latest networked game state to the HUD.
     */
    public void update(GameStateMessage gameStateMessage) {
        updatePlayers(gameStateMessage.getPlayerStates());
        updateTime(gameStateMessage.getGameTime());
        updateGameStatus(gameStateMessage);
    }

    /**
     * Updates local and remote player display values used by the HUD.
     */
    private void updatePlayers(List<PlayerState> players) {
        latestPlayerStates.clear();
        latestPlayerStates.addAll(players);

        for (PlayerState player : players) {
            if (player.getId() == localPlayerId) {
                localChampionType = player.getChampionType();
                localPlayerLevelLabel.setText(String.valueOf(player.getLevel()));

                int maxHp = Math.max(1, player.getMaxHp());
                int hp = Math.max(0, Math.min(player.getHp(), maxHp));
                localPlayerHpLabel.setText(hp + " / " + maxHp);
                localPlayerHpBar.setProgress(hp / (float) maxHp);
                localKillCount = player.getKillCount();
                localDeathCount = player.getDeathCount();
                syncCooldownFromServer(player);

                int xpForNextLevel = Math.max(1, player.getXpForNextLevel());
                int currentXp = Math.max(0, Math.min(player.getCurrentXp(), xpForNextLevel));
                boolean isMaxLevel = player.getLevel() >= MAX_PLAYER_LEVEL && currentXp >= xpForNextLevel;
                localPlayerXpBar.setProgress(isMaxLevel ? 1f : currentXp / (float) xpForNextLevel);
            }
        }
    }

    /**
     * Formats game time as mm:ss for the top-center label.
     */
    private void updateTime(int gameTime) {
        int minutes = Math.floorDiv(gameTime, 60);
        int seconds = gameTime % 60;
        timeLabel.setText(minutes + ":" + String.format("%02d", seconds));
        killDeathLabel.setText(localKillCount + " / " + localDeathCount + "    " + timeLabel.getText());
    }

    private void tickDisplayedCooldowns(float deltaSeconds) {
        float elapsedMs = Math.max(0f, deltaSeconds) * 1000f;
        displayedQCooldownRemainingMs = Math.max(0f, displayedQCooldownRemainingMs - elapsedMs);
        displayedWCooldownRemainingMs = Math.max(0f, displayedWCooldownRemainingMs - elapsedMs);
    }

    private void syncCooldownFromServer(PlayerState player) {
        displayedQCooldownRemainingMs = mergeServerCooldown(
            displayedQCooldownRemainingMs,
            displayedQCooldownTotalMs,
            player.getQCooldownRemainingMs(),
            player.getQCooldownTotalMs(),
            lastServerQCooldownRemainingMs,
            lastServerQCooldownTotalMs
        );
        displayedQCooldownTotalMs = player.getQCooldownTotalMs();
        lastServerQCooldownRemainingMs = player.getQCooldownRemainingMs();
        lastServerQCooldownTotalMs = player.getQCooldownTotalMs();

        displayedWCooldownRemainingMs = mergeServerCooldown(
            displayedWCooldownRemainingMs,
            displayedWCooldownTotalMs,
            player.getWCooldownRemainingMs(),
            player.getWCooldownTotalMs(),
            lastServerWCooldownRemainingMs,
            lastServerWCooldownTotalMs
        );
        displayedWCooldownTotalMs = player.getWCooldownTotalMs();
        lastServerWCooldownRemainingMs = player.getWCooldownRemainingMs();
        lastServerWCooldownTotalMs = player.getWCooldownTotalMs();

        refreshAbilityCooldowns();
    }

    private float mergeServerCooldown(
        float displayedRemainingMs,
        float displayedTotalMs,
        long serverRemainingMs,
        long serverTotalMs,
        long previousServerRemainingMs,
        long previousServerTotalMs
    ) {
        if (serverRemainingMs <= 0L || serverTotalMs <= 0L) {
            return 0f;
        }

        boolean cooldownRestarted = previousServerRemainingMs < 0L
            || serverTotalMs != previousServerTotalMs
            || serverRemainingMs > previousServerRemainingMs + 100L;
        if (cooldownRestarted) {
            return serverRemainingMs;
        }

        if (displayedTotalMs != serverTotalMs) {
            return serverRemainingMs;
        }

        float driftMs = Math.abs(displayedRemainingMs - serverRemainingMs);
        if (driftMs > 120f) {
            return serverRemainingMs;
        }

        return displayedRemainingMs;
    }

    private void refreshAbilityCooldowns() {
        updateAbilityCooldown(
            firstAbilityCooldownOverlay,
            firstAbilityCooldownLabel,
            firstAbilityLabel,
            Math.round(displayedQCooldownRemainingMs),
            Math.round(displayedQCooldownTotalMs)
        );
        updateAbilityCooldown(
            secondAbilityCooldownOverlay,
            secondAbilityCooldownLabel,
            secondAbilityLabel,
            Math.round(displayedWCooldownRemainingMs),
            Math.round(displayedWCooldownTotalMs)
        );
    }

    private void updateAbilityCooldown(
        CooldownOverlayActor overlay,
        Label cooldownLabel,
        Label keyLabel,
        long remainingCooldownMs,
        long totalCooldownMs
    ) {
        float progress = totalCooldownMs <= 0L ? 0f : remainingCooldownMs / (float) totalCooldownMs;
        boolean isCoolingDown = remainingCooldownMs > 0L;

        overlay.setProgress(progress);
        cooldownLabel.setText(isCoolingDown ? formatCooldownText(remainingCooldownMs) : "");
        keyLabel.getStyle().fontColor = isCoolingDown ? Color.LIGHT_GRAY : Color.WHITE;
    }

    private String formatCooldownText(long remainingCooldownMs) {
        if (remainingCooldownMs >= 10_000L) {
            return String.valueOf((int) Math.ceil(remainingCooldownMs / 1000f));
        }

        return String.format("%.1f", remainingCooldownMs / 1000f);
    }

    /**
     * Shows waiting/victory/defeat text based on the latest nexus state.
     */
    private void updateGameStatus(GameStateMessage gameState) {
        if (gameState.isAllPlayersHaveJoined()) {
            gameStatusLabel.setText("");
        }

        for (NexusState nexus : gameState.getNexusStates()) {
            if (!nexus.isDestroyed()) {
                continue;
            }

            if (nexus.getTeam() == localPlayerTeam) {
                gameStatusLabel.getStyle().fontColor = Color.RED;
                gameStatusLabel.setText("DEFEAT");
            } else {
                gameStatusLabel.getStyle().fontColor = Color.GREEN;
                gameStatusLabel.setText("VICTORY");
            }
            return;
        }
    }
}
