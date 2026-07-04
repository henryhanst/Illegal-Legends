package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector3;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.util.DamageIndicator;
import ee.taltech.examplegame.util.Font;
import ee.taltech.examplegame.util.Sprites;
import lombok.Data;
import message.dto.ActionState;
import message.dto.ChampionType;
import message.dto.Direction;
import message.dto.Team;

import java.util.ArrayList;
import java.util.List;

import static constant.Constants.*;

@Data
public class Player {
    private String name = "Player";
    private int level = 1;
    private final int id;
    private Team team;
    private Team localPlayerTeam;
    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private int lives;
    private ChampionType championType;
    private ActionState actionState = ActionState.IDLE;
    private Direction direction = Direction.DOWN;
    private final BitmapFont nameFont = Font.getBeaufortFont(20);
    private float fighterWalkingAnimationTime;
    private float rangedWalkingAnimationTime;
    private float tankWalkingAnimationTime;
    private float attackAnimationTime = -1f;
    private int queuedAutoAttacks;
    private float tankSpellcastAnimationTime = -1f;
    private int tankHealSequence;
    private int autoAttackSequence = -1;
    private boolean autoAttackActive;
    private float deathAnimationTime = -1f;
    private long lastDeathAnimationFrameId = -1L;
    private static final float SPRITE_BASE_FRAME_SIZE = 64f;
    private static final float SPRITE_FEET_ANCHOR_Y = 0.2f;
    private static final float TANK_ATTACK_FEET_ANCHOR_Y = 0.34f;
    private final List<DamageIndicator> damageIndicators = new ArrayList<>();

    public Player(int id) {
        this.id = id;
    }

    public void render(SpriteBatch spriteBatch, Camera camera) {
        float w = PLAYER_WIDTH_IN_PIXELS / PPM;
        float h = PLAYER_HEIGHT_IN_PIXELS / PPM;
        TextureRegion texture = resolveTexture(false);
        float drawWidth = getDrawWidth(texture, w);
        float drawHeight = getDrawHeight(texture, h);
        boolean hovered = isCursorOnPlayer(camera, drawWidth, drawHeight);
        texture = applyHoverTexture(texture, hovered);
        drawWidth = getDrawWidth(texture, w);
        drawHeight = getDrawHeight(texture, h);
        float feetAnchorY = getFeetAnchorY(texture);
        float drawX = x - drawWidth / 2f;
        float drawY = y - drawHeight * feetAnchorY;
        spriteBatch.draw(
                texture,
                drawX,
                drawY,
                drawWidth,
                drawHeight
        );
        renderHealthBar(spriteBatch);
        renderDamageIndicators(spriteBatch);
        renderLevel(spriteBatch, w, h, camera);
        renderName(spriteBatch, w, h, camera);
    }

    private void renderHealthBar(SpriteBatch spriteBatch) {
        if (maxHp <= 0) {
            return;
        }

        float barWidth = 64f / PPM;
        float barHeight = 8f / PPM;
        float border = 1.2f / PPM;
        float tickWidth = 1.5f / PPM;
        float barX = x - barWidth / 2f;
        float barY = y + 0.38f;
        float innerX = barX + border;
        float innerY = barY + border;
        float innerWidth = Math.max(0f, barWidth - border * 2f);
        float innerHeight = Math.max(0f, barHeight - border * 2f);

        spriteBatch.setColor(Color.BLACK);
        spriteBatch.draw(Sprites.whitePixel, barX, barY, barWidth, barHeight);

        float healthFraction = Math.max(0f, Math.min(1f, hp / (float) maxHp));
        spriteBatch.setColor(0.16f, 0.16f, 0.16f, 1f);
        spriteBatch.draw(Sprites.whitePixel, innerX, innerY, innerWidth, innerHeight);
        Color healthColor;
        if (id == Main.myID) {
            healthColor = new Color(0.12f, 0.75f, 0.18f, 1f); // own = green
        } else if (team == localPlayerTeam) {
            healthColor = new Color(0.20f, 0.45f, 0.95f, 1f); // ally = blue
        } else {
            healthColor = new Color(0.85f, 0.18f, 0.18f, 1f); // enemy = red
        }

        spriteBatch.setColor(healthColor);
        spriteBatch.draw(Sprites.whitePixel, innerX, innerY, innerWidth * healthFraction, innerHeight);

        spriteBatch.setColor(0f, 0f, 0f, 0.8f);
        for (int hpMark = 100; hpMark < maxHp; hpMark += 100) {
            float tickProgress = hpMark / (float) maxHp;
            float tickX = innerX + innerWidth * tickProgress - (tickWidth / 2f);
            spriteBatch.draw(Sprites.whitePixel, tickX, innerY, tickWidth, innerHeight);
        }

        spriteBatch.setColor(Color.WHITE);
    }

    private TextureRegion applyHoverTexture(TextureRegion currentTexture, boolean hovered) {
        if (!hovered) {
            return currentTexture;
        }

        return switch (championType) {
            case FIGHTER -> actionState == ActionState.IDLE ? Sprites.fighterTextureSelectedRegion : currentTexture;
            case RANGED -> actionState == ActionState.IDLE ? Sprites.rangedTextureSelectedRegion : currentTexture;
            case TANK -> actionState == ActionState.IDLE ? Sprites.tankTextureSelectedRegion : currentTexture;
            case NONE -> currentTexture;
        };
    }

    private TextureRegion resolveTexture(boolean hovered) {
        TextureRegion deathTexture = resolveDeathTexture();
        if (deathTexture != null) {
            return deathTexture;
        }

        TextureRegion attackTexture = resolveAttackTexture();
        if (attackTexture != null) {
            return attackTexture;
        }

        return switch (championType) {
            case FIGHTER -> resolveWalkingTexture(
                    hovered,
                    Sprites.getWalkingAnimation(championType, direction),
                    fighterWalkingAnimationTime,
                    Sprites.fighterTextureSelectedRegion,
                    Sprites.fighterTexture
            );
            case RANGED  -> resolveWalkingTexture(
                    hovered,
                    Sprites.getWalkingAnimation(championType, direction),
                    rangedWalkingAnimationTime,
                    Sprites.rangedTextureSelectedRegion,
                    Sprites.rangedTexture
            );
            case TANK    -> resolveTankWalkingTexture(hovered);
            case NONE    -> Sprites.rangedTexture;
        };
    }

    private TextureRegion resolveDeathTexture() {
        if (actionState != ActionState.DEAD) {
            deathAnimationTime = -1f;
            lastDeathAnimationFrameId = -1L;
            return null;
        }

        if (deathAnimationTime < 0f) {
            deathAnimationTime = 0f;
        }

        Animation<TextureRegion> deathAnimation = switch (championType) {
            case FIGHTER -> Sprites.fighterDeathAnimation;
            case RANGED -> Sprites.rangedDeathAnimation;
            case TANK -> Sprites.tankDeathAnimation;
            case NONE -> null;
        };

        if (deathAnimation == null) {
            return null;
        }

        TextureRegion animationFrame = deathAnimation.getKeyFrame(deathAnimationTime, false);
        long frameId = Gdx.graphics.getFrameId();
        if (frameId != lastDeathAnimationFrameId && deathAnimationTime < deathAnimation.getAnimationDuration()) {
            deathAnimationTime += Gdx.graphics.getDeltaTime();
            lastDeathAnimationFrameId = frameId;
        }
        return animationFrame;
    }

    private TextureRegion resolveAttackTexture() {
        if (!autoAttackActive) {
            attackAnimationTime = -1f;
            queuedAutoAttacks = 0;
            return null;
        }

        if (attackAnimationTime < 0f) {
            return null;
        }

        var attackAnimation = Sprites.getAttackAnimation(championType, direction);
        if (attackAnimationTime >= attackAnimation.getAnimationDuration()) {
            if (queuedAutoAttacks > 0) {
                queuedAutoAttacks--;
                attackAnimationTime = 0f;
            } else {
                attackAnimationTime = -1f;
                return null;
            }
        }

        TextureRegion animationFrame = attackAnimation.getKeyFrame(attackAnimationTime, false);
        attackAnimationTime += Gdx.graphics.getDeltaTime();
        return animationFrame;
    }

    private TextureRegion resolveWalkingTexture(
            boolean hovered,
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> walkingAnimation,
            float animationTime,
            TextureRegion selectedTexture,
            TextureRegion defaultTexture
    ) {
        if (actionState == ActionState.MOVING || (actionState == ActionState.AUTO_ATTACKING && !autoAttackActive)) {
            TextureRegion animationFrame = walkingAnimation.getKeyFrame(animationTime, true);
            updateWalkingAnimationTime(animationTime + Gdx.graphics.getDeltaTime());
            return animationFrame;
        }

        updateWalkingAnimationTime(0f);
        return hovered ? selectedTexture : defaultTexture;
    }

    private TextureRegion resolveTankWalkingTexture(boolean hovered) {
        if (tankSpellcastAnimationTime >= 0f
                && tankSpellcastAnimationTime < Sprites.tankSpellcastAnimation.getAnimationDuration()) {
            TextureRegion animationFrame = Sprites.tankSpellcastAnimation.getKeyFrame(tankSpellcastAnimationTime, false);
            tankSpellcastAnimationTime += Gdx.graphics.getDeltaTime();
            return animationFrame;
        }

        return resolveWalkingTexture(
                hovered,
                Sprites.getWalkingAnimation(championType, direction),
                tankWalkingAnimationTime,
                Sprites.tankTextureSelectedRegion,
                Sprites.tankTexture
        );
    }

    private void updateWalkingAnimationTime(float animationTime) {
        switch (championType) {
            case FIGHTER -> fighterWalkingAnimationTime = animationTime;
            case RANGED -> rangedWalkingAnimationTime = animationTime;
            case TANK -> tankWalkingAnimationTime = animationTime;
        }
    }

    public void setActionState(ActionState actionState) {
        if (this.actionState != ActionState.DEAD && actionState == ActionState.DEAD) {
            deathAnimationTime = 0f;
            lastDeathAnimationFrameId = -1L;
        }

        if (this.actionState != actionState && actionState != ActionState.MOVING) {
            fighterWalkingAnimationTime = 0f;
            rangedWalkingAnimationTime = 0f;
            tankWalkingAnimationTime = 0f;
        }

        this.actionState = actionState;
    }

    public void setAutoAttackSequence(int autoAttackSequence) {
        if (this.autoAttackSequence != -1 && this.autoAttackSequence != autoAttackSequence) {
            if (attackAnimationTime < 0f) {
                attackAnimationTime = 0f;
            } else {
                queuedAutoAttacks++;
            }
        }

        this.autoAttackSequence = autoAttackSequence;
    }

    public void setTankHealSequence(int tankHealSequence) {
        if (this.tankHealSequence != tankHealSequence) {
            tankSpellcastAnimationTime = 0f;
        }

        this.tankHealSequence = tankHealSequence;
    }

    private void renderName(SpriteBatch spriteBatch, float w, float h, Camera camera) {
        // Anchor the name to the same compact width used by the health bar.
        float barWidth = 0.62f;
        float nameWorldX = x - barWidth / 2f + 0.01f;
        float nameWorldY = y + 0.60f;

        // Scale the font down to world units so it stays proportional to the player sprite.
        nameFont.getData().setScale(0.0058f);
        nameFont.setUseIntegerPositions(false); // Keeps text smooth when moving
        nameFont.setColor(Color.WHITE);

        String displayName = (this.id == Main.myID) ? Main.savedPlayerName : this.name;
        if (displayName != null) {
            nameFont.draw(spriteBatch, displayName, nameWorldX, nameWorldY);
        }
    }

    private void renderLevel(SpriteBatch spriteBatch, float w, float h, Camera camera) {
        float barWidth = 0.62f;
        float boxSize = 0.12f;
        float gap = 0.015f;

        float levelX = x - barWidth / 2f - boxSize - gap;
        float levelY = y + 0.375f;

        // Draw the level box just to the left of the health bar.
        spriteBatch.setColor(Color.BLACK);
        spriteBatch.draw(Sprites.whitePixel, levelX, levelY, boxSize, boxSize);

        spriteBatch.setColor(0.22f, 0.16f, 0.08f, 1f);
        spriteBatch.draw(
                Sprites.whitePixel,
                levelX + 0.008f,
                levelY + 0.008f,
                boxSize - 0.016f,
                boxSize - 0.016f
        );
        nameFont.getData().setScale(0.005f);
        nameFont.setUseIntegerPositions(false);

        String lvlStr = String.valueOf(level);
        GlyphLayout layout = new GlyphLayout(nameFont, lvlStr);

        // Center the level text inside the world-space box.
        float textX = levelX + (boxSize / 2f) - (layout.width / 2f);
        float textY = levelY + (boxSize / 2f) + (layout.height / 2f);

        nameFont.setColor(Color.WHITE);
        nameFont.draw(spriteBatch, lvlStr, textX, textY);

        spriteBatch.setColor(Color.WHITE);
    }

    private void renderDamageIndicators(SpriteBatch spriteBatch) {
        damageIndicators.removeIf(d -> d.update(Gdx.graphics.getDeltaTime()));
        for (DamageIndicator d : damageIndicators) {
            nameFont.getData().setScale(0.007f);
            nameFont.setUseIntegerPositions(false);
            nameFont.setColor(1f, 0.25f, 0.25f, d.alpha);
            nameFont.draw(spriteBatch, d.text, d.x, d.y);
        }
        nameFont.setColor(Color.WHITE);
    }

    public boolean isCursorOnPlayer(Camera camera, float width, float height) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float feetAnchorY = getFeetAnchorY(resolveTexture(false));

        float left   = x - width / 2f;
        float right  = x + width / 2f;
        float bottom = y - height * feetAnchorY;
        float top    = bottom + height;

        return mouse.x >= left && mouse.x <= right
                && mouse.y >= bottom && mouse.y <= top;
    }

    public void spawnDamageIndicator(int amount) {
        damageIndicators.add(new DamageIndicator(x, y, amount));
    }

    private float getDrawWidth(TextureRegion texture, float baseWidth) {
        return baseWidth * texture.getRegionWidth() / SPRITE_BASE_FRAME_SIZE;
    }

    private float getDrawHeight(TextureRegion texture, float baseHeight) {
        return baseHeight * texture.getRegionHeight() / SPRITE_BASE_FRAME_SIZE;
    }

    private float getFeetAnchorY(TextureRegion texture) {
        if (championType == ChampionType.TANK && texture.getRegionHeight() > SPRITE_BASE_FRAME_SIZE) {
            return TANK_ATTACK_FEET_ANCHOR_Y;
        }

        return SPRITE_FEET_ANCHOR_Y;
    }
}
