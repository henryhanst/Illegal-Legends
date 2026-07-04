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

import static constant.Constants.PPM;
import static constant.Constants.TURRET_COLLISION_HEIGHT_IN_PIXELS;
import static constant.Constants.TURRET_COLLISION_OFFSET_Y_IN_PIXELS;
import static constant.Constants.TURRET_COLLISION_WIDTH_IN_PIXELS;
import static constant.Constants.TURRET_HEIGHT_IN_PIXELS;
import static constant.Constants.TURRET_WIDTH_IN_PIXELS;

@Data
public class Turret {
    private static final float HOVER_COLLISION_PADDING_PIXELS = 30f;

    private float x;
    private float y;
    private int hp;
    private int maxHp;
    private Team team;
    private Team localPlayerTeam;
    private boolean destroyed;

    public void render(SpriteBatch spriteBatch, Camera camera) {
        float width = TURRET_WIDTH_IN_PIXELS / PPM;
        float height = TURRET_HEIGHT_IN_PIXELS / PPM;
        boolean hovered = isCursorOnTurret(camera);
        Texture texture = switch (team) {
            case TEAM_BLUE -> destroyed
                    ? Sprites.turretBlueDestroyedTexture
                    : hovered ? resolveHoveredTexture(Sprites.turretBlueHoveredTexture, Sprites.turretBlueHoveredAllyTexture) : Sprites.turretBlueTexture;
            case TEAM_RED -> destroyed
                    ? Sprites.turretRedDestroyedTexture
                    : hovered ? resolveHoveredTexture(Sprites.turretRedHoveredTexture, Sprites.turretRedHoveredAllyTexture) : Sprites.turretRedTexture;
            case NONE -> destroyed
                    ? Sprites.turretBlueDestroyedTexture
                    : hovered ? Sprites.turretBlueHoveredTexture : Sprites.turretBlueTexture;
        };
        spriteBatch.draw(
                texture,
                x - width / 2f,
                y - height / 2f,
                width,
                height
        );

        if (!destroyed) {
            renderHealthBar(spriteBatch, width, height);
        }
    }

    private void renderHealthBar(SpriteBatch spriteBatch, float width, float height) {
        if (maxHp <= 0) {
            return;
        }

        float barWidth = width * 0.65f;
        float barHeight = 0.07f;
        float barX = x - barWidth / 2f;
        float barY = y + height * 0.36f;

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

    private boolean isCursorOnTurret(Camera camera) {
        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float collisionCenterY = y + TURRET_COLLISION_OFFSET_Y_IN_PIXELS / PPM;
        float halfWidth = (TURRET_COLLISION_WIDTH_IN_PIXELS / 2f + HOVER_COLLISION_PADDING_PIXELS) / PPM;
        float halfHeight = (TURRET_COLLISION_HEIGHT_IN_PIXELS / 2f + HOVER_COLLISION_PADDING_PIXELS) / PPM;

        return mouse.x >= x - halfWidth && mouse.x <= x + halfWidth
                && mouse.y >= collisionCenterY - halfHeight && mouse.y <= collisionCenterY + halfHeight;
    }
}
