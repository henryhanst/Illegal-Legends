package ee.taltech.examplegame.util;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import message.dto.ChampionType;
import message.dto.Direction;

public class Sprites {
    private static final int ABILITY_ICON_GRID_SIZE = 5;

    // Minion textures
    public static final Texture minionTextureSheet = new Texture("sprites/minion/minionRat.png");
    public static final Texture minionHoveredTextureSheet = new Texture("sprites/minion/minionRatHovered.png");
    public static final Texture minionHoveredAllyTextureSheet = new Texture("sprites/minion/minionRatHoveredAlly.png");
    private static final Animation<TextureRegion>[] minionWalkingAnimations = createDirectionalAnimations(minionTextureSheet, 32, 32, 8, 9, 0.1f);
    private static final Animation<TextureRegion>[] minionHoveredWalkingAnimations = createDirectionalAnimations(minionHoveredTextureSheet, 32, 32, 8, 9, 0.1f);
    private static final Animation<TextureRegion>[] minionHoveredAllyWalkingAnimations = createDirectionalAnimations(minionHoveredAllyTextureSheet, 32, 32, 8, 9, 0.1f);
    private static final Animation<TextureRegion>[] minionAttackAnimations = createDirectionalAnimations(minionTextureSheet, 32, 32, 12, 6, 0.1f);
    private static final Animation<TextureRegion>[] minionHoveredAttackAnimations = createDirectionalAnimations(minionHoveredTextureSheet, 32, 32, 12, 6, 0.1f);
    private static final Animation<TextureRegion>[] minionHoveredAllyAttackAnimations = createDirectionalAnimations(minionHoveredAllyTextureSheet, 32, 32, 12, 6, 0.1f);
    public static final Animation<TextureRegion> minionDeathAnimation = createRowAnimation(minionTextureSheet, 32, 32, 20, 6, 0.16f);

    // Fighter textures
    public static final Texture fighterTextureSheet = new Texture("sprites/fighter/fighter-walking.png");
    public static final TextureRegion fighterTexture = new TextureRegion(fighterTextureSheet, 0, 0, 64, 64);
    private static final Animation<TextureRegion>[] fighterWalkingAnimations = createDirectionalAnimations(fighterTextureSheet, 64, 64, 9, 0.1f);
    public static final Texture fighterAttackTextureSheet = new Texture("sprites/fighter/fighter-attack.png");
    private static final Animation<TextureRegion>[] fighterAttackAnimations = createDirectionalAnimations(fighterAttackTextureSheet, 64, 64, 6, 0.14f);
    public static final Texture fighterDeathTextureSheet = new Texture("sprites/fighter/fighter-death.png");
    public static final Animation<TextureRegion> fighterDeathAnimation = createRowAnimation(fighterDeathTextureSheet, 64, 64, 0, 6, 0.18f);
    public static final TextureRegion fighterTextureSelectedRegion = new TextureRegion(fighterTextureSheet, 0, 0, 64, 64);

    // Ranged textures
    public static final Texture rangedTextureSheet = new Texture("sprites/ranged/ranged-walking.png");
    public static final TextureRegion rangedTexture = new TextureRegion(rangedTextureSheet, 0, 0, 64, 64);
    private static final Animation<TextureRegion>[] rangedWalkingAnimations = createDirectionalAnimations(rangedTextureSheet, 64, 64, 8, 0.1f);
    public static final Texture rangedAttackTextureSheet = new Texture("sprites/ranged/ranged-attack.png");
    private static final Animation<TextureRegion>[] rangedAttackAnimations = createDirectionalAnimations(rangedAttackTextureSheet, 64, 64, 13, 0.14f);
    public static final Texture rangedDeathTextureSheet = new Texture("sprites/ranged/ranged-death.png");
    public static final Animation<TextureRegion> rangedDeathAnimation = createRowAnimation(rangedDeathTextureSheet, 64, 64, 0, 6, 0.18f);
    public static final TextureRegion rangedTextureSelectedRegion = new TextureRegion(rangedTextureSheet, 0, 0, 64, 64);

    // Tank textures
    public static final Texture tankTextureSheet = new Texture("sprites/tank/tank-walking.png");
    public static final TextureRegion tankTexture = new TextureRegion(tankTextureSheet, 0, 0, 64, 64);
    private static final Animation<TextureRegion>[] tankWalkingAnimations = createDirectionalAnimations(tankTextureSheet, 64, 64, 9, 0.1f);
    public static final Texture tankAttackTextureSheet = new Texture("sprites/tank/tank-attack.png");
    private static final Animation<TextureRegion>[] tankAttackAnimations = createDirectionalAnimations(tankAttackTextureSheet, 128, 128, 6, 0.16f);
    public static final Texture tankSpellcastSheet = new Texture("sprites/tank/tank-spellcast.png");
    public static final Animation<TextureRegion> tankSpellcastAnimation = createRowAnimation(tankSpellcastSheet, 64, 64, 2, 7, 0.18f);
    public static final Texture tankDeathTextureSheet = new Texture("sprites/tank/tank-death.png");
    public static final Animation<TextureRegion> tankDeathAnimation = createRowAnimation(tankDeathTextureSheet, 64, 64, 0, 6, 0.18f);
    public static final TextureRegion tankTextureSelectedRegion = new TextureRegion(tankTextureSheet, 0, 0, 64, 64);

    // Nexus textures
    public static final Texture nexusBlueTexture = new Texture("sprites/nexus/fountainBlue.png");
    public static final Texture nexusBlueHoveredTexture = new Texture("sprites/nexus/fountainBlueHovered.png");
    public static final Texture nexusBlueHoveredAllyTexture = new Texture("sprites/nexus/fountainBlueHoveredAlly.png");
    public static final Texture nexusBlueDestroyedTexture = new Texture("sprites/nexus/fountainBlueDestroyed.png");
    public static final Texture nexusRedTexture = new Texture("sprites/nexus/fountainRed.png");
    public static final Texture nexusRedDestroyedTexture = new Texture("sprites/nexus/fountainRedDestroyed.png");

    // Turret textures
    public static final Texture turretBlueTexture = new Texture("sprites/turret/turretBlue.png");
    public static final Texture turretBlueHoveredTexture = new Texture("sprites/turret/turretBlueHovered.png");
    public static final Texture turretBlueHoveredAllyTexture = new Texture("sprites/turret/turretBlueHoveredAlly.png");
    public static final Texture turretBlueDestroyedTexture = new Texture("sprites/turret/turretBlueDestroyed.png");

    public static final Texture nexusRedHoveredTexture = new Texture("sprites/nexus/fountainRedHovered.png");
    public static final Texture nexusRedHoveredAllyTexture = new Texture("sprites/nexus/fountainRedHoveredAlly.png");
    public static final Texture turretRedTexture = new Texture("sprites/turret/turretRed.png");
    public static final Texture turretRedHoveredTexture = new Texture("sprites/turret/turretRedHovered.png");
    public static final Texture turretRedHoveredAllyTexture = new Texture("sprites/turret/turretRedHoveredAlly.png");
    public static final Texture turretRedDestroyedTexture = new Texture("sprites/turret/turretRedDestroyed.png");

    // Other or util textures
    public static final Texture whitePixel = createWhitePixel();
    public static final Texture defaultBulletTexture = new Texture("sprites/bullet.png");
    public static final Texture movementArrow = new Texture("sprites/movementArrow.png");

    // Bullet or skillshot textures
    public static final Texture fighterBulletTexture = new Texture("sprites/fighter/FighterAbilitySpriteW.png");
    public static final Texture rangedBulletTexture = new Texture("sprites/ranged/RangedAmmoOutline.png");
    public static final Texture tankBulletTexture = defaultBulletTexture;

    // Effect textures

    // Fighter stun granade
    public static final Texture granadeTextureSheet = new Texture("sprites/effects/granadeEffect.png");
    private static final Animation<TextureRegion> granadeExplosionEffect = createFullAnimation(granadeTextureSheet, 120, 109, 5, 6, 0.1f);

    // Ranged teleport
    public static final Texture teleportTextureSheet = new Texture("sprites/effects/teleportEffect.png");
    private static final Animation<TextureRegion> teleportExplosionEffect = createFullAnimation(teleportTextureSheet,
            120, 109, 2, 6, 0.1f);


    private static Texture createWhitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public static Animation<TextureRegion> getWalkingAnimation(ChampionType championType, Direction direction) {
        int rowIndex = getRowIndex(direction);

        return switch (championType) {
            case FIGHTER -> fighterWalkingAnimations[rowIndex];
            case RANGED -> rangedWalkingAnimations[rowIndex];
            case TANK -> tankWalkingAnimations[rowIndex];
            case NONE -> null;
        };
    }

    public static Animation<TextureRegion> getMinionWalkingAnimation(Direction direction, boolean hovered, boolean alliedHover) {
        int rowIndex = getRowIndex(direction);
        if (!hovered) {
            return minionWalkingAnimations[rowIndex];
        }

        return alliedHover
                ? minionHoveredAllyWalkingAnimations[rowIndex]
                : minionHoveredWalkingAnimations[rowIndex];
    }

    public static Animation<TextureRegion> getMinionAttackAnimation(Direction direction, boolean hovered, boolean alliedHover) {
        int rowIndex = getRowIndex(direction);
        if (!hovered) {
            return minionAttackAnimations[rowIndex];
        }

        return alliedHover
                ? minionHoveredAllyAttackAnimations[rowIndex]
                : minionHoveredAttackAnimations[rowIndex];
    }

    public static Animation<TextureRegion> getMinionDeathAnimation() {
        return minionDeathAnimation;
    }

    public static Animation<TextureRegion> getAttackAnimation(ChampionType championType, Direction direction) {
        int rowIndex = getRowIndex(direction);

        return switch (championType) {
            case FIGHTER -> fighterAttackAnimations[rowIndex];
            case RANGED -> rangedAttackAnimations[rowIndex];
            case TANK -> tankAttackAnimations[rowIndex];
            case NONE -> null;
        };
    }

    public static Texture getBulletTexture(ChampionType championType) {
        return switch (championType) {
            case FIGHTER -> fighterBulletTexture;
            case RANGED -> rangedBulletTexture;
            case TANK -> tankBulletTexture;
            case NONE -> defaultBulletTexture;
        };
    }

    private static Animation<TextureRegion>[] createDirectionalAnimations(
            Texture texture,
            int frameWidth,
            int frameHeight,
            int frameCount,
            float frameDuration
    ) {
        return createDirectionalAnimations(texture, frameWidth, frameHeight, 0, frameCount, frameDuration);
    }

    private static Animation<TextureRegion>[] createDirectionalAnimations(
            Texture texture,
            int frameWidth,
            int frameHeight,
            int startRow,
            int frameCount,
            float frameDuration
    ) {
        @SuppressWarnings("unchecked")
        Animation<TextureRegion>[] animations = new Animation[4];

        for (int rowIndex = 0; rowIndex < animations.length; rowIndex++) {
            animations[rowIndex] = createRowAnimation(
                    texture,
                    frameWidth,
                    frameHeight,
                    startRow + rowIndex,
                    frameCount,
                    frameDuration
            );
        }

        return animations;
    }

    private static int getRowIndex(Direction direction) {
        return switch (direction) {
            case UP -> 0;
            case LEFT -> 1;
            case DOWN -> 2;
            case RIGHT -> 3;
        };
    }

    private static Animation<TextureRegion> createRowAnimation(Texture texture, int frameWidth, int frameHeight, int rowIndex, int frameCount, float frameDuration) {
        TextureRegion[][] frames = TextureRegion.split(texture, frameWidth, frameHeight);
        TextureRegion[] rowFrames = new TextureRegion[frameCount];

        for (int i = 0; i < frameCount; i++) {
            rowFrames[i] = frames[rowIndex][i];
        }

        return new Animation<>(frameDuration, rowFrames);
    }

    // Used for effects
    public static Animation<TextureRegion> createFullAnimation(
            Texture texture,
            int frameWidth,
            int frameHeight,
            int rows,
            int cols,
            float frameDuration
    ) {
        TextureRegion[][] frames = TextureRegion.split(texture, frameWidth, frameHeight);

        Array<TextureRegion> allFrames = new Array<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                allFrames.add(frames[row][col]);
            }
        }

        return new Animation<>(frameDuration, allFrames);
    }
}
