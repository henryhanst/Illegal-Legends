package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import ee.taltech.examplegame.util.Sprites;
import lombok.Data;
import message.dto.ActionState;
import message.dto.Direction;
import message.dto.Team;

import static constant.Constants.MINION_HEIGHT_IN_PIXELS;
import static constant.Constants.MINION_WIDTH_IN_PIXELS;
import static constant.Constants.PPM;

@Data
public class Minion {
    private final int id;
    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private Team team;
    private Team localPlayerTeam;
    private Direction direction = Direction.DOWN;
    private ActionState actionState = ActionState.IDLE;
    private float walkingAnimationTime;
    private float attackAnimationTime = -1f;
    private float deathAnimationTime = -1f;
    private static final float FEET_ANCHOR_Y = 0.2f;

    public void render(SpriteBatch spriteBatch, Camera camera) {
        float width = MINION_WIDTH_IN_PIXELS / PPM;
        float height = MINION_HEIGHT_IN_PIXELS / PPM;
        boolean hovered = actionState != ActionState.DEAD && isCursorOnMinion(camera, width, height);
        boolean alliedHover = hovered && team != null && team == localPlayerTeam;
        TextureRegion texture = resolveTexture(hovered, alliedHover);

        if (!hovered && team == Team.TEAM_BLUE) {
            spriteBatch.setColor(0.75f, 0.9f, 1f, 1f);
        } else if (!hovered && team == Team.TEAM_RED) {
            spriteBatch.setColor(1f, 0.75f, 0.75f, 1f);
        }

        spriteBatch.draw(
                texture,
                x - width / 2f,
                y - height * FEET_ANCHOR_Y,
                width,
                height
        );
        spriteBatch.setColor(Color.WHITE);

        if (actionState != ActionState.DEAD) {
            renderHealthBar(spriteBatch);
        }
    }

    private TextureRegion resolveTexture(boolean hovered, boolean alliedHover) {
        TextureRegion deathTexture = resolveDeathTexture();
        if (deathTexture != null) {
            return deathTexture;
        }

        TextureRegion attackTexture = resolveAttackTexture(hovered, alliedHover);
        if (attackTexture != null) {
            return attackTexture;
        }

        Animation<TextureRegion> walkingAnimation = Sprites.getMinionWalkingAnimation(direction, hovered, alliedHover);
        if (actionState == ActionState.MOVING) {
            TextureRegion animationFrame = walkingAnimation.getKeyFrame(walkingAnimationTime, true);
            walkingAnimationTime += Gdx.graphics.getDeltaTime();
            return animationFrame;
        }

        walkingAnimationTime = 0f;
        return walkingAnimation.getKeyFrame(0f);
    }

    private TextureRegion resolveAttackTexture(boolean hovered, boolean alliedHover) {
        if (actionState != ActionState.AUTO_ATTACKING) {
            attackAnimationTime = -1f;
            return null;
        }

        if (attackAnimationTime < 0f) {
            attackAnimationTime = 0f;
        }

        Animation<TextureRegion> attackAnimation = Sprites.getMinionAttackAnimation(direction, hovered, alliedHover);
        TextureRegion animationFrame = attackAnimation.getKeyFrame(attackAnimationTime, true);
        attackAnimationTime += Gdx.graphics.getDeltaTime();
        return animationFrame;
    }

    private TextureRegion resolveDeathTexture() {
        if (actionState != ActionState.DEAD) {
            deathAnimationTime = -1f;
            return null;
        }

        if (deathAnimationTime < 0f) {
            deathAnimationTime = 0f;
        }

        Animation<TextureRegion> deathAnimation = Sprites.getMinionDeathAnimation();
        TextureRegion animationFrame = deathAnimation.getKeyFrame(deathAnimationTime, false);
        if (deathAnimationTime < deathAnimation.getAnimationDuration()) {
            deathAnimationTime += Gdx.graphics.getDeltaTime();
        }
        return animationFrame;
    }

    public void setActionState(ActionState actionState) {
        if (this.actionState != ActionState.DEAD && actionState == ActionState.DEAD) {
            deathAnimationTime = 0f;
        }
        if (this.actionState != ActionState.AUTO_ATTACKING && actionState == ActionState.AUTO_ATTACKING) {
            attackAnimationTime = 0f;
        }
        if (actionState != ActionState.MOVING) {
            walkingAnimationTime = 0f;
        }

        this.actionState = actionState;
    }

    private boolean isCursorOnMinion(Camera camera, float width, float height) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        camera.unproject(mouse);

        float left = x - width / 2f;
        float right = x + width / 2f;
        float bottom = y - height * FEET_ANCHOR_Y;
        float top = bottom + height;

        return mouse.x >= left && mouse.x <= right
                && mouse.y >= bottom && mouse.y <= top;
    }

    private void renderHealthBar(SpriteBatch spriteBatch) {
        float barWidth = 0.48f;
        float barHeight = 0.04f;
        float barX = x - barWidth / 2f;
        float barY = y + 0.30f;

        spriteBatch.setColor(Color.RED);
        spriteBatch.draw(Sprites.whitePixel, barX, barY, barWidth, barHeight);

        float healthFraction = maxHp <= 0 ? 0f : (float) hp / maxHp;
        spriteBatch.setColor(Color.GREEN);
        spriteBatch.draw(Sprites.whitePixel, barX, barY, barWidth * healthFraction, barHeight);
        spriteBatch.setColor(Color.WHITE);
    }
}
