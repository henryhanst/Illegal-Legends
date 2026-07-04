package ee.taltech.examplegame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import ee.taltech.examplegame.util.Sprites;
import lombok.Data;
import message.dto.Team;

import static constant.Constants.NEXUS_COLLISION_HEIGHT_IN_PIXELS;
import static constant.Constants.NEXUS_COLLISION_OFFSET_X_IN_PIXELS;
import static constant.Constants.NEXUS_COLLISION_OFFSET_Y_IN_PIXELS;
import static constant.Constants.NEXUS_COLLISION_WIDTH_IN_PIXELS;
import static constant.Constants.NEXUS_HEIGHT_IN_PIXELS;
import static constant.Constants.NEXUS_WIDTH_IN_PIXELS;
import static constant.Constants.PPM;

@Data
public class Nexus {
    private static final float HOVER_COLLISION_PADDING_PIXELS = 30f;

    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private Team team;
    private Team localPlayerTeam;
    private boolean destroyed;
    private boolean isTurretDestroyed;

    public void render(SpriteBatch spriteBatch, Camera camera) {
        float width = NEXUS_WIDTH_IN_PIXELS / PPM;
        float height = NEXUS_HEIGHT_IN_PIXELS / PPM;
        boolean hovered = isCursorOnNexus(camera);
        Texture texture = switch (team) {
            case TEAM_BLUE -> destroyed
                    ? Sprites.nexusBlueDestroyedTexture
                    : hovered ? resolveHoveredTexture(Sprites.nexusBlueHoveredTexture, Sprites.nexusBlueHoveredAllyTexture) : Sprites.nexusBlueTexture;
            case TEAM_RED -> destroyed
                    ? Sprites.nexusRedDestroyedTexture
                    : hovered ? resolveHoveredTexture(Sprites.nexusRedHoveredTexture, Sprites.nexusRedHoveredAllyTexture) : Sprites.nexusRedTexture;
            case NONE -> destroyed
                    ? Sprites.nexusBlueDestroyedTexture
                    : hovered ? Sprites.nexusBlueHoveredTexture : Sprites.nexusBlueTexture;
        };
        spriteBatch.draw(
                texture,
                x - width / 2f,
                y - height / 2f,
                width,
                height
        );
        if (!destroyed && isTurretDestroyed) { // If turret is destroyed only then shows Nexus HP
            renderHealthBar(spriteBatch, width, height);
        }
    }

    private void renderHealthBar(SpriteBatch spriteBatch, float width, float height) {
        if (maxHp <= 0) {
            return;
        }

        float barWidth = width * 0.4f;
        float barHeight = 0.08f;
        float barX = x - barWidth / 2f;
        float barY = y + height * 0.22f;

        spriteBatch.setColor(Color.RED);
        spriteBatch.draw(Sprites.whitePixel, barX, barY, barWidth, barHeight);

        float healthFraction = Math.max(0f, (float) hp / maxHp);
        spriteBatch.setColor(Color.GREEN);
        spriteBatch.draw(Sprites.whitePixel, barX, barY, barWidth * healthFraction, barHeight);
        spriteBatch.setColor(Color.WHITE);
    }

    private Texture resolveHoveredTexture(Texture enemyHoveredTexture, Texture allyHoveredTexture) {
        return team == localPlayerTeam ? allyHoveredTexture : enemyHoveredTexture;
    }

    private boolean isCursorOnNexus(Camera camera) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float collisionCenterX = x + NEXUS_COLLISION_OFFSET_X_IN_PIXELS / PPM;
        float collisionCenterY = y + NEXUS_COLLISION_OFFSET_Y_IN_PIXELS / PPM;
        float localX = mouse.x - collisionCenterX;
        float localY = mouse.y - collisionCenterY;

        float angleRad = (float) Math.toRadians(45f);
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);

        float unrotatedX = localX * cos + localY * sin;
        float unrotatedY = -localX * sin + localY * cos;
        float halfWidth = (NEXUS_COLLISION_WIDTH_IN_PIXELS / 2f + HOVER_COLLISION_PADDING_PIXELS) / PPM;
        float halfHeight = (NEXUS_COLLISION_HEIGHT_IN_PIXELS / 2f + HOVER_COLLISION_PADDING_PIXELS) / PPM;

        return Math.abs(unrotatedX) <= halfWidth
                && Math.abs(unrotatedY) <= halfHeight;
    }
}
