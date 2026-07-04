package ee.taltech.examplegame.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Shared colors and helpers for letterboxing and screen backgrounds.
 * Modern urban/cyberpunk theme with dark colors and gold accents.
 */
public final class ScreenStyle {

    public static final Color LETTERBOX_COLOR = new Color(0f, 0f, 0f, 1f);
    // Dark navy blue backround
    public static final Color SCREEN_BACKGROUND_COLOR = new Color(15 / 255f, 25 / 255f, 40 / 255f, 1f);

    // Additional theme colors
    public static final Color PRIMARY_DARK = new Color(20 / 255f, 35 / 255f, 60 / 255f, 1f);
    public static final Color ACCENT_GOLD = new Color(217 / 255f, 160 / 255f, 58 / 255f, 1f);
    public static final Color TEAM_BLUE = new Color(0f, 102 / 255f, 204 / 255f, 1f);
    public static final Color TEAM_RED = new Color(204 / 255f, 0f, 0f, 1f);
    public static final Color TEXT_LIGHT = new Color(235 / 255f, 235 / 255f, 235 / 255f, 1f);

    private ScreenStyle() {
    }

    public static void clearWindow() {
        ScreenUtils.clear(LETTERBOX_COLOR);
    }

    public static Table createScreenBackground() {
        Table background = new Table();
        background.setFillParent(true);
        background.setBackground(createColoredDrawable(SCREEN_BACKGROUND_COLOR));
        return background;
    }

    public static Drawable createColoredDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();
        return drawable;
    }
    public static Drawable createBorderedDrawable(Color fill, Color border, int thickness) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pixmap.setColor(fill);
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
