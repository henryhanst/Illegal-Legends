package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.component.LabelComponents;
import ee.taltech.examplegame.component.TextFieldComponents;
import ee.taltech.examplegame.util.ScreenStyle;
import ee.taltech.examplegame.util.ViewportConfig;

import static ee.taltech.examplegame.component.ButtonComponents.getButton;

/**
 * TitleScreen represents the main menu of the game, where players can choose to start the game or exit.
 * It listens for user input and directs user to different screens, for example TitleScreen -> GameScreen.
 */
public class TitleScreen extends ScreenAdapter {
    private final Game game;
    private final Stage stage;
    private TextField nameField;
    public static String savedPlayerName = "";

    public TitleScreen(Game game) {
        this.game = game;
        stage = ViewportConfig.createUiStage();
        stage.addActor(ScreenStyle.createScreenBackground());

        // Create nameField first (needed in createUI)
        nameField = TextFieldComponents.getTextField("", 18);
        nameField.setText(Main.savedPlayerName);
        nameField.setMessageText("Enter name...");
        nameField.setTextFieldFilter((textField, c) -> textField.getText().length() < 12);

        // Create UI elements
        createUI();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        ((Main) game).getAudioManager().playMenuMusic();
    }

    private void createUI() {
        // Main title
        Label titleLabel = LabelComponents.getLabel("ILLEGAL LEGENDS", 48);
        titleLabel.setColor(ScreenStyle.ACCENT_GOLD);
        titleLabel.setAlignment(Align.center);

//        // Subtitle
//        Label subtitleLabel = LabelComponents.getLabel("Urban Battle Arena", 24);
//        subtitleLabel.setColor(ScreenStyle.TEXT_LIGHT);
//        subtitleLabel.setAlignment(Align.center);

        // Menu buttons
        var startButton = getButton(20, "Start", () -> {
            String playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                int randomId = new java.util.Random().nextInt(6) + 1;
                playerName = "Player" + randomId;
            }
            if (playerName.length() > 12) {
                playerName = playerName.substring(0, 12);
            }
            Main.savedPlayerName = playerName;
            stage.dispose();
            game.setScreen(new ChooseLobbyScreen(game));
        });
        var settingsButton = getButton(20, "Settings", () -> {
            Screen settingsScreen = new SettingsScreen(game, this);
            game.setScreen(settingsScreen);
        });
        var exitButton = getButton(20, "Exit", () -> Gdx.app.exit());

        // Main layout table
        Table mainTable = new Table();
        mainTable.setFillParent(true);

        // Top section - Title
        mainTable.top().center().pad(40);
        mainTable.add(titleLabel).center().padBottom(10).row();

        // Spacer to push buttons to middle
        mainTable.add().expand().row();

        // Center section - Buttons
        Table buttonsTable = new Table();
        buttonsTable.add(startButton).width(200).height(60).padBottom(30).row();
        buttonsTable.add(settingsButton).width(200).height(60).padBottom(30).row();
        buttonsTable.add(exitButton).width(200).height(60);

        mainTable.add(buttonsTable).center().row();

        // Spacer
        mainTable.add().expand().row();

        stage.addActor(mainTable);

        // Table for top right corner - Name field
        Table topTable = new Table();
        topTable.setFillParent(true);
        topTable.top().right().pad(20);

        Label nameLabel = LabelComponents.getLabel("Player Name: ", 18);
        nameLabel.setColor(ScreenStyle.TEXT_LIGHT);
        topTable.add(nameLabel).padRight(10);
        topTable.add(nameField).width(150).height(40);

        stage.addActor(topTable);
    }

    /**
     * Renders the TitleScreen, clearing it and drawing the buttons.
     *
     * @param delta time since last frame.
     */
    @Override
    public void render(float delta) {
        super.render(delta);
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

    @Override
    public void dispose() {
        stage.dispose();
    }
}
