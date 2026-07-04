package ee.taltech.examplegame.component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import ee.taltech.examplegame.util.ScreenStyle;
import static ee.taltech.examplegame.util.Font.getBeaufortFont;

public class TextFieldComponents {

    public static TextField getTextField(String initialText, int fontSize) {
        TextFieldStyle style = new TextFieldStyle();
        style.font = getBeaufortFont(fontSize);
        // Light text for visibility on dark backgrounds
        style.fontColor = ScreenStyle.TEXT_LIGHT;

        style.background = getColoredDrawable(1, 1, new Color(50 / 255f, 50 / 255f, 50 / 255f, 1f)); // Dark gray background
        style.cursor = getColoredDrawable(2, 1, ScreenStyle.ACCENT_GOLD); // Gold cursor
        style.selection = getColoredDrawable(1, 1, new Color(217 / 255f, 160 / 255f, 58 / 255f, 0.3f)); // Gold selection
        return new TextField(initialText, style);
    }

    // Helper method to create textures without PNG files
    private static Drawable getColoredDrawable(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();
        return drawable;
    }
}