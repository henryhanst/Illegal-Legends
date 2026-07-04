package ee.taltech.examplegame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class DamageVignette {
    private static final float FADE_DURATION = 0.8f;
    private static final float PEAK_ALPHA = 0.55f;
    private float elapsed = FADE_DURATION;

    // Create these ONCE to prevent memory leaks and lag
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Color outerColor = new Color(1f, 0f, 0f, 0f);
    private final Color innerColor = new Color(1f, 0f, 0f, 0f); // Always fully transparent

    public void trigger() { elapsed = 0f; }

    public void render(SpriteBatch batch, Camera camera) {
        if (elapsed < FADE_DURATION) {
            elapsed = Math.min(elapsed + Gdx.graphics.getDeltaTime(), FADE_DURATION);
        }
        if (elapsed >= FADE_DURATION) return;

        // Calculate transparency
        float alpha = PEAK_ALPHA * (1f - (elapsed / FADE_DURATION));
        outerColor.a = alpha; // Update the color's transparency

        // Get camera dimensions and position
        float camW = camera.viewportWidth;
        float camH = camera.viewportHeight;
        if (camera instanceof OrthographicCamera) {
            OrthographicCamera ortho = (OrthographicCamera) camera;
            camW *= ortho.zoom;
            camH *= ortho.zoom;
        }
        float camX = camera.position.x - camW / 2f;
        float camY = camera.position.y - camH / 2f;

        // Gradient thickness (15% of the screen size makes a nice, wide, soft fade)
        float t = Math.min(camW, camH) * 0.08f;

        // 1. Pause SpriteBatch so we can draw custom shapes safely
        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // -- DRAWING 4 EDGES WITH GRADIENTS --
        // rect(x, y, width, height, bottomLeft, bottomRight, topRight, topLeft colors)

        // Left Edge
        shapeRenderer.rect(camX, camY + t, t, camH - 2 * t, outerColor, innerColor, innerColor, outerColor);
        // Right Edge
        shapeRenderer.rect(camX + camW - t, camY + t, t, camH - 2 * t, innerColor, outerColor, outerColor, innerColor);
        // Bottom Edge
        shapeRenderer.rect(camX + t, camY, camW - 2 * t, t, outerColor, outerColor, innerColor, innerColor);
        // Top Edge
        shapeRenderer.rect(camX + t, camY + camH - t, camW - 2 * t, t, innerColor, innerColor, outerColor, outerColor);

        // -- FILLING THE 4 CORNERS TO MAKE IT SEAMLESS --

        // Bottom-Left
        shapeRenderer.rect(camX, camY, t, t, outerColor, outerColor, innerColor, outerColor);
        // Bottom-Right
        shapeRenderer.rect(camX + camW - t, camY, t, t, outerColor, outerColor, outerColor, innerColor);
        // Top-Right
        shapeRenderer.rect(camX + camW - t, camY + camH - t, t, t, innerColor, outerColor, outerColor, outerColor);
        // Top-Left
        shapeRenderer.rect(camX, camY + camH - t, t, t, outerColor, innerColor, outerColor, outerColor);

        shapeRenderer.end();

        // 2. Resume the SpriteBatch so your game continues drawing normally
        batch.begin();
    }
}