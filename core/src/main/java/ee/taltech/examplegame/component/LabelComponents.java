package ee.taltech.examplegame.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.utils.Align;
import ee.taltech.examplegame.util.Font;
import ee.taltech.examplegame.util.ScreenStyle;

import static ee.taltech.examplegame.util.Font.getBeaufortFont;

/**
 * Utility class for creating different types of labels for the HUD.
 * Provides methods to generate standard labels or labels with a background.
 */
public class LabelComponents {

    private static final int LABEL_BACKGROUND_PADDING = 10;
    private static final int MIN_LABEL_BACKGROUND_WIDTH = 75;

    /**
     * Creates a simple label with the specified text, color, and font size.
     */
    public static Label createLabel(String text, Color textColor, int fontSize) {
        BitmapFont font = getBeaufortFont(fontSize);
        LabelStyle labelStyle = new LabelStyle(font, textColor);
        return new Label(text, labelStyle);
    }

    /**
     * Creates a label with a dark gray background, centered text, and specified font properties.
     * Quite hacky, but it works.
     */
    public static Label createLabelWithBackground(String text, Color textColor, int fontSize) {
        BitmapFont font = getBeaufortFont(fontSize);
        LabelStyle labelStyle = new LabelStyle(font, textColor);

        Label label = new Label(text, labelStyle);
        label.setAlignment(Align.center); // Align text to the center of the background

        // Create background
        Pixmap labelBackground = new Pixmap(
            Math.max(MIN_LABEL_BACKGROUND_WIDTH, (int) label.getWidth() + LABEL_BACKGROUND_PADDING),
            (int) label.getHeight(),
            Pixmap.Format.RGB888
        );
        labelBackground.setColor(Color.DARK_GRAY);
        labelBackground.fill();

        label.getStyle().background = new Image(new Texture(labelBackground)).getDrawable();
        labelBackground.dispose();  // Dispose to prevent unnecessary RAM usage (small leaks will accumulate quickly)
        return label;
    }
    public static Label getLabel(String text, int fontSize) {
        // Label style with light text color for dark backgrounds
        Label.LabelStyle style = new Label.LabelStyle();
        style.font = Font.getBeaufortFont(fontSize);
        style.fontColor = ScreenStyle.TEXT_LIGHT;

        return new Label(text, style);
    }

}
