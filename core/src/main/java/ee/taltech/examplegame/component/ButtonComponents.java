package ee.taltech.examplegame.component;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.screen.GameScreen;

import static ee.taltech.examplegame.util.Font.getBeaufortFont;
import ee.taltech.examplegame.util.ScreenStyle;


/**
 * Utility class for creating consistently styled UI buttons.
 *
 * Responsibilities:
 * - Create visual style (colors, shape)
 * - Apply interaction feedback (hover, click)
 * - Play UI sound on click
 */
public class ButtonComponents {

    /**
     * Functional interface for button click behavior.
     */
    public interface OnClickHandler {
        void handleClick();  // Defines the action (function) to be triggered when the button is clicked.
    }

    /**
     * Creates a styled TextButton with hover and click animations.
     */
    public static TextButton getButton(int fontSize, String text, OnClickHandler onClickHandler) {

        TextButtonStyle style = createButtonStyle(fontSize);
        TextButton button = createButtonWithPadding(text, style);

        button.setTransform(true); // Required for scale animations

        button.addListener(new ClickListener() {

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                // Slight scale-up on hover for visual feedback
                button.clearActions();
                button.addAction(Actions.scaleTo(1.03f, 1.03f, 0.08f, Interpolation.fade));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                // Reset scale when hover ends
                button.clearActions();
                button.addAction(Actions.scaleTo(1f, 1f, 0.08f, Interpolation.fade));
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                playClickSound();
                onClickHandler.handleClick();

                // Small "press" animation
                button.addAction(Actions.sequence(
                        Actions.scaleTo(0.97f, 0.97f, 0.05f),
                        Actions.scaleTo(1f, 1f, 0.08f)
                ));

                super.clicked(event, x, y);
            }
        });

        return button;
    }

    /**
     * Defines the visual style for buttons.
     * Uses simple color variations for different states.
     */
    private static TextButtonStyle createButtonStyle(int fontSize) {
        TextButtonStyle style = new TextButtonStyle();
        style.font = getBeaufortFont(fontSize);
        style.fontColor = ScreenStyle.ACCENT_GOLD;

        // Base, hover and pressed colors
        style.up = createDrawable(new Color(24 / 255f, 28 / 255f, 38 / 255f, 1f));
        style.over = createDrawable(new Color(34 / 255f, 38 / 255f, 58 / 255f, 1f));
        style.down = createDrawable(new Color(18 / 255f, 22 / 255f, 28 / 255f, 1f));

        return style;
    }

    /**
     * Creates a rounded rectangle drawable without borders.
     * Keeps visuals clean and avoids rendering artifacts.
     */
    private static Drawable createDrawable(Color color) {
        int width = 220;
        int height = 54;
        int radius = 4;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(color);
        fillRoundedRect(pixmap, 0, 0, width, height, radius);

        TextureRegionDrawable drawable = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();

        return drawable;
    }

    /**
     * Fills a rounded rectangle using rectangles + circles.
     * This avoids sharp corners without needing textures.
     */
    private static void fillRoundedRect(Pixmap pixmap, int x, int y, int w, int h, int r) {
        pixmap.fillRectangle(x + r, y, w - 2 * r, h);
        pixmap.fillRectangle(x, y + r, w, h - 2 * r);

        pixmap.fillCircle(x + r, y + r, r);
        pixmap.fillCircle(x + w - r - 1, y + r, r);
        pixmap.fillCircle(x + r, y + h - r - 1, r);
        pixmap.fillCircle(x + w - r - 1, y + h - r - 1, r);
    }

    /**
     * Applies consistent padding to buttons.
     */
    private static TextButton createButtonWithPadding(String text, TextButtonStyle style) {
        TextButton button = new TextButton(text, style);
        button.pad(6);
        button.padLeft(10);
        button.padRight(10);
        return button;
    }

    /**
     * Plays UI click sound (disabled during gameplay screen).
     */
    private static void playClickSound() {
        Main game = Main.getInstance();
        if (game == null || game.getScreen() instanceof GameScreen) return;
        game.getAudioManager().playSoundEffect();
    }
}