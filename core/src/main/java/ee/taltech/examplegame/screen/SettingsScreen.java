package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.component.LabelComponents;
import ee.taltech.examplegame.util.Font;
import ee.taltech.examplegame.util.ScreenStyle;
import ee.taltech.examplegame.util.SettingsManager;
import ee.taltech.examplegame.util.ViewportConfig;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;

/**
 * Dedicated settings screen for desktop options.
 * Handles menu UI for resolution, window mode, and a placeholder sound setting.
 */
public class SettingsScreen extends ScreenAdapter {
    private final Game game;
    private final Screen previousScreen;
    private final Stage stage;
    private final SelectBox<String> resolutionSelectBox;
    private final SelectBox<String> windowModeSelectBox;
    private final Slider soundSlider;
    private final Label soundValueLabel;

    public SettingsScreen(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.stage = ViewportConfig.createUiStage();
        resolutionSelectBox = createSelectBox();
        windowModeSelectBox = createSelectBox();
        soundSlider = createSlider();
        soundValueLabel = LabelComponents.getLabel("", 18);
        SettingsManager.SettingsData currentSettings = SettingsManager.load();

        populateFields(currentSettings);
        createLayout();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Renders the TitleScreen, clearing it and drawing the buttons.
     *
     * @param delta time since last frame.
     */
    @Override
    public void render(float delta) {
        ScreenStyle.clearWindow();

        // draw the buttons
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
    }

    /**
     * Builds the layout for the settings panel and action buttons.
     */
    private void createLayout() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        root.setBackground(ScreenStyle.createColoredDrawable(ScreenStyle.SCREEN_BACKGROUND_COLOR));

        Table panel = new Table();
        panel.setBackground(getColoredDrawable(1, 1, new Color(30 / 255f, 40 / 255f, 60 / 255f, 1f)));
        panel.pad(24);

        Label titleLabel = LabelComponents.getLabel("Settings", 28);
        titleLabel.setColor(ScreenStyle.ACCENT_GOLD);
        panel.add(titleLabel).left().colspan(2).padBottom(24);
        panel.row();

        Label resolutionLabel = LabelComponents.getLabel("Resolution", 20);
        resolutionLabel.setColor(ScreenStyle.TEXT_LIGHT);
        panel.add(resolutionLabel).left().padRight(20).padBottom(16);
        panel.add(resolutionSelectBox).width(280).height(42).left().padBottom(16);
        panel.row();

        Label windowLabel = LabelComponents.getLabel("Window Mode", 20);
        windowLabel.setColor(ScreenStyle.TEXT_LIGHT);
        panel.add(windowLabel).left().padRight(20).padBottom(16);
        panel.add(windowModeSelectBox).width(280).height(42).left().padBottom(16);
        panel.row();

        Label volumeLabel = LabelComponents.getLabel("Master Volume", 20);
        volumeLabel.setColor(ScreenStyle.TEXT_LIGHT);
        panel.add(volumeLabel).left().padRight(20).padBottom(12);

        Table soundTable = new Table();
        soundTable.add(soundSlider).width(280).left().padRight(12);
        soundValueLabel.setColor(ScreenStyle.TEXT_LIGHT);
        soundTable.add(soundValueLabel).width(50).left();

        panel.add(soundTable).left().padBottom(12);
        panel.row();

        Label noteLabel = LabelComponents.getLabel("Can connect the sound setting with this slider later.", 14);
        noteLabel.setColor(ScreenStyle.TEXT_LIGHT);
        panel.add(noteLabel)
                .left()
                .colspan(2)
                .padBottom(24);
        panel.row();

        Table buttonRow = new Table();
        buttonRow.add(getButton(18, "Back", this::goBack)).padRight(16);
        buttonRow.add(getButton(18, "Apply", this::applySettings));
        panel.add(buttonRow).colspan(2).center();

        root.add(panel).width(560);
        stage.addActor(root);
    }

    /**
     * Fills the widgets with the current saved settings.
     */
    private void populateFields(SettingsManager.SettingsData currentSettings) {
        resolutionSelectBox.setItems(SettingsManager.getAvailableResolutionLabels());
        resolutionSelectBox.setSelected(currentSettings.getResolutionLabel());

        windowModeSelectBox.setItems(
                SettingsManager.WindowMode.WINDOWED.getLabel(),
                SettingsManager.WindowMode.FULLSCREEN.getLabel()
        );
        windowModeSelectBox.setSelected(currentSettings.windowMode.getLabel());

        soundSlider.setValue(currentSettings.soundVolume);
        updateSoundValueLabel(currentSettings.soundVolume);

        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                updateSoundValueLabel(Math.round(soundSlider.getValue()));
            }
        });
    }

    /**
     * Saves the current UI selections and applies them immediately.
     */
    private void applySettings() {
        SettingsManager.SettingsData updatedSettings = SettingsManager.fromSelections(
                resolutionSelectBox.getSelected(),
                windowModeSelectBox.getSelected(),
                Math.round(soundSlider.getValue())
        );
        SettingsManager.save(updatedSettings);
        SettingsManager.apply(updatedSettings);
        ((Main) game).getAudioManager().setMasterVolume(updatedSettings.soundVolume / 100f);
    }

    /**
     * Returns to the previous screen, which is currently the title screen.
     */
    private void goBack() {
        game.setScreen(previousScreen);
    }

    private void updateSoundValueLabel(int value) {
        soundValueLabel.setText(value + "%");
    }

    /**
     * Creates a simple styled dropdown used by the resolution and window mode controls.
     */
    private SelectBox<String> createSelectBox() {
        BitmapFont font = Font.getBeaufortFont(18);
        Drawable background = getColoredDrawable(1, 1, new Color(50 / 255f, 50 / 255f, 50 / 255f, 1f));
        Drawable highlighted = getColoredDrawable(1, 1, new Color(100 / 255f, 130 / 255f, 180 / 255f, 1f));
        Drawable dropdownBackground = getColoredDrawable(1, 1, new Color(40 / 255f, 40 / 255f, 50 / 255f, 1f));

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = ScreenStyle.ACCENT_GOLD;
        listStyle.fontColorUnselected = ScreenStyle.TEXT_LIGHT;
        listStyle.selection = highlighted;
        listStyle.background = dropdownBackground;

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = dropdownBackground;

        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle();
        style.font = font;
        style.fontColor = ScreenStyle.TEXT_LIGHT;
        style.background = background;
        style.backgroundOpen = background;
        style.backgroundOver = background;
        style.scrollStyle = scrollPaneStyle;
        style.listStyle = listStyle;
        style.scrollStyle.vScrollKnob = getColoredDrawable(1, 1, ScreenStyle.ACCENT_GOLD);
        style.scrollStyle.vScroll = getColoredDrawable(1, 1, new Color(60 / 255f, 60 / 255f, 60 / 255f, 1f));

        return new SelectBox<>(style);
    }

    /**
     * Creates the sound slider UI.
     * The slider already stores a value even though no audio system is connected yet.
     */
    private Slider createSlider() {
        Slider.SliderStyle style = new Slider.SliderStyle();
        style.background = getColoredDrawable(1, 1, new Color(60 / 255f, 60 / 255f, 60 / 255f, 1f));
        style.knobBefore = getColoredDrawable(1, 1, ScreenStyle.ACCENT_GOLD);
        style.knob = new TextureRegionDrawable(new TextureRegion(new Texture("sprites/slider.png")));

        return new Slider(0, 100, 1, false, style);
    }

    /**
     * Creates a flat-colored drawable so the screen does not need extra texture assets for simple panels.
     */
    private Drawable getColoredDrawable(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();
        return drawable;
    }
}
