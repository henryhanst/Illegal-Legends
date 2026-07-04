package ee.taltech.examplegame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class Font {
    private static final float BASE_UI_WIDTH = 1280f;
    private static final float BASE_UI_HEIGHT = 720f;
    private static final float MAX_FONT_UPSCALE = 1.3f;
    private static final FreeTypeFontGenerator beaufortFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Beaufort.ttf"));

    private static int getScaledFontSize(int fontSize) {
        float widthScale = Gdx.graphics.getWidth() / BASE_UI_WIDTH;
        float heightScale = Gdx.graphics.getHeight() / BASE_UI_HEIGHT;
        float currentScale = Math.min(widthScale, heightScale);

        if (currentScale >= 1f) {
            return fontSize;
        }

        float inverseScale = 1f / Math.max(currentScale, 0.01f);
        float multiplier = Math.min(MAX_FONT_UPSCALE, inverseScale);
        return Math.max(1, Math.round(fontSize * multiplier));
    }

    public static BitmapFont getBeaufortFont(int fontSize) {
        var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = getScaledFontSize(fontSize);

        return beaufortFontGenerator.generateFont(parameter);
    }

}
