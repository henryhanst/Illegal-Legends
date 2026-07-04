package ee.taltech.examplegame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static constant.Constants.PPM;

/**
 * Shared viewport configuration so menus, HUD and gameplay use consistent virtual sizes.
 */
public final class ViewportConfig {

    public static final float UI_WIDTH = 1280f;
    public static final float UI_HEIGHT = 720f;
    public static final float GAME_WORLD_WIDTH = UI_WIDTH / PPM;
    public static final float GAME_WORLD_HEIGHT = UI_HEIGHT / PPM;

    private ViewportConfig() {
    }

    public static Viewport createUiViewport() {
        return new FitViewport(UI_WIDTH, UI_HEIGHT);
    }

    public static Stage createUiStage() {
        Viewport viewport = createUiViewport();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        return new Stage(viewport);
    }
}
