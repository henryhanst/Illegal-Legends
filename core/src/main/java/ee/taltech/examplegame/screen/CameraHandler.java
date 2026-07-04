package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import static constant.Constants.EDGE_THRESHOLD;
import static constant.Constants.CAMERA_SPEED;


public class CameraHandler {
    private final OrthographicCamera camera;
    private boolean isLocked = true;

    public CameraHandler(OrthographicCamera camera) {
        this.camera = camera;
    }

    public void update(Vector3 cameraPos, float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            isLocked = !isLocked;
        }

        if (isLocked) {
            camera.position.set(cameraPos);
        } else {
            handleEdgeScroll(delta);
        }
    }


    /**
     * Move camera accordingly when cursor is at the edge of windogame window
     * @param delta time in seconds since last frame was rendered
     */
    public void handleEdgeScroll(float delta) {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        if (mouseX < EDGE_THRESHOLD) camera.position.x -= CAMERA_SPEED * delta;
        else if (mouseX > screenW - EDGE_THRESHOLD) camera.position.x += CAMERA_SPEED * delta;
        if (mouseY < EDGE_THRESHOLD) camera.position.y += CAMERA_SPEED * delta;
        else if (mouseY > screenH - EDGE_THRESHOLD) camera.position.y -= CAMERA_SPEED * delta;
    }
}
