package ee.taltech.examplegame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import ee.taltech.examplegame.Main;
import ee.taltech.examplegame.component.ButtonComponents;
import ee.taltech.examplegame.component.LabelComponents;
import ee.taltech.examplegame.network.ServerConnection;
import ee.taltech.examplegame.util.ScreenStyle;
import ee.taltech.examplegame.util.Sprites;
import ee.taltech.examplegame.util.ViewportConfig;
import message.LobbyExitMessage;
import message.LobbyUpdateMessage;
import message.PlayerClassMessage;
import message.PlayerLobbyInfo;
import message.StartGameMessage;
import message.dto.ChampionType;
import message.dto.Direction;
import message.dto.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * ChampionSelectScreen handles the champion selection phase in the lobby.
 * Players can view their teammates, select a champion from three options (Tank, Fighter, Ranged),
 * and lock in their selection. The screen displays team information on both sides and champion cards
 * in the center with gold borders and modern styling.
 */
public class ChampionSelectScreen extends ScreenAdapter {
    // Team information display tables
    private final Stage stage;
    private final Table leftTeamTable;
    private final Table rightTeamTable;
    private final Table championCardsTable;

    // Game state
    private ChampionType mySelection = ChampionType.NONE;
    private boolean lockedIn = false;
    private List<PlayerLobbyInfo> playerList;

    // Screen references
    private final Game game;
    private final int lobbyId;

    // UI components
    private final Label selectionLabel;

    /**
     * Constructs a ChampionSelectScreen with default empty state.
     *
     * @param game the main game instance
     */
    public ChampionSelectScreen(Game game) {
        this(game, -1, new ArrayList<>());
    }

    /**
     * Constructs a ChampionSelectScreen with lobby information and initial player list.
     *
     * @param game the main game instance
     * @param lobbyId the ID of the lobby
     * @param initialPlayers the initial list of players in the lobby
     */
    public ChampionSelectScreen(Game game, int lobbyId, List<PlayerLobbyInfo> initialPlayers) {
        this.game = game;
        this.lobbyId = lobbyId;
        this.playerList = new ArrayList<>(initialPlayers);
        this.stage = ViewportConfig.createUiStage();
        this.leftTeamTable = new Table();
        this.rightTeamTable = new Table();
        this.championCardsTable = new Table();
        this.selectionLabel = LabelComponents.getLabel("Selected Champion: None", 18);
        setupUI();
        updatePlayers(this.playerList);
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        root.setBackground(ScreenStyle.createColoredDrawable(ScreenStyle.SCREEN_BACKGROUND_COLOR));

        Table centerTable = new Table();
        centerTable.defaults().pad(10);

        // Main title - larger and more prominent
        Label titleLabel = LabelComponents.getLabel("PICK YOUR CHAMPION", 32);
        titleLabel.setAlignment(Align.center);
        titleLabel.setColor(ScreenStyle.ACCENT_GOLD);

        // Champion cards layout
        championCardsTable.defaults().pad(16);
        championCardsTable.add(createChampCard("TANK", ChampionType.TANK)).width(170).height(320);
        championCardsTable.add(createChampCard("FIGHTER", ChampionType.FIGHTER)).width(170).height(320);
        championCardsTable.add(createChampCard("RANGED", ChampionType.RANGED)).width(170).height(320);

        selectionLabel.setAlignment(Align.center);
        selectionLabel.setColor(ScreenStyle.TEXT_LIGHT);

        // --- LOCK IN BUTTON ---
        TextButton lockInButton = ButtonComponents.getButton(18, "LOCK IN", () -> {
            if (!lockedIn && mySelection != ChampionType.NONE) {
                sendSelection(true);
            }
        });

        // --- QUIT BUTTON ---
        TextButton quitButton = ButtonComponents.getButton(18, "QUIT", () -> {
            if (lobbyId > 0) {
                LobbyExitMessage exitMessage = new LobbyExitMessage();
                exitMessage.setLobbyId(lobbyId);
                ServerConnection.getInstance().getClient().sendTCP(exitMessage);
            }
            game.setScreen(new ChooseLobbyScreen(game));
        });

        centerTable.add(titleLabel).center().padBottom(24).row();
        centerTable.add(championCardsTable).center().row();
        centerTable.add(selectionLabel).padTop(20).padBottom(10).row();

        Table mainContent = new Table();
        mainContent.add(leftTeamTable).width(220).top().left().padRight(30).padTop(40);
        mainContent.add(centerTable).width(620).expand().top().center();
        mainContent.add(rightTeamTable).width(220).top().right().padLeft(10).padTop(40);

        // Main content in the top row of root
        root.add(mainContent).width(1200).expand().top().center().row();
        
        // Bottom row with LOCK IN (centered) and QUIT (right-aligned)
        Table bottomRow = new Table();
        bottomRow.add(lockInButton).width(180).height(52).center().expandX().padLeft(170);
        bottomRow.add(quitButton).width(150).height(52).right().padRight(12);
        root.add(bottomRow).width(1200).height(80).expandX().bottom();

        stage.addActor(root);
    }

    public void updatePlayers(List<PlayerLobbyInfo> players) {
        this.playerList = new ArrayList<>(players);
        leftTeamTable.clear();
        rightTeamTable.clear();

        leftTeamTable.add(createTeamHeader("Blue Team")).left().padBottom(24).row();
        rightTeamTable.add(createTeamHeader("Red Team")).right().padBottom(24).row();

        for (PlayerLobbyInfo player : players) {
            if (player.getConnectionId() == ServerConnection.getInstance().getClient().getID()) {
                Main.myID = player.getConnectionId();
                lockedIn = player.isLockedIn();
                mySelection = player.getChampionType() == null ? ChampionType.NONE : player.getChampionType();
            }

            if (player.getTeam() == Team.TEAM_BLUE) {
                leftTeamTable.add(createPlayerInfoCard(player, true)).width(190).left().padBottom(34).row();
            } else if (player.getTeam() == Team.TEAM_RED) {
                rightTeamTable.add(createPlayerInfoCard(player, false)).width(190).right().padBottom(34).row();
            }
        }

        // Rebuild champion cards to reflect selection state
        championCardsTable.clear();
        championCardsTable.add(createChampCard("TANK", ChampionType.TANK)).width(170).height(320);
        championCardsTable.add(createChampCard("FIGHTER", ChampionType.FIGHTER)).width(170).height(320);
        championCardsTable.add(createChampCard("RANGED", ChampionType.RANGED)).width(170).height(320);

        selectionLabel.setText("Selected Champion: " + getChampionText(mySelection));
    }

    private Label createTeamHeader(String text) {
        if (text.contains("Blue")) {
            Label labelTeamBlue = LabelComponents.createLabel("Team BLUE", ScreenStyle.TEAM_BLUE, 20);
            return labelTeamBlue;
        } else if (text.contains("Red")) {
           Label labelTeamRed = LabelComponents.createLabel("Team RED", ScreenStyle.TEAM_RED, 20);
            return labelTeamRed;
        }
        return null;
    }

    private Table createPlayerInfoCard(PlayerLobbyInfo player, boolean leftAligned) {
        Table infoCard = new Table();
        infoCard.pad(6);

        String playerName = player.getPlayerName() == null ? "Name" : player.getPlayerName();
        String chosenChampion = getChampionText(player.getChampionType());
        String championLine = player.isLockedIn()
                ? chosenChampion + " - " + getChampionText(player.getChampionType())
                : chosenChampion;

        Label nameLabel;
        if (player.getConnectionId() == ServerConnection.getInstance().getClient().getID()) {
            nameLabel = LabelComponents.createLabel(playerName, ScreenStyle.ACCENT_GOLD, 16);
        } else {
            nameLabel = LabelComponents.getLabel(playerName, 16);
        }
        Label championLabel = LabelComponents.getLabel("Selected Champion: " + championLine, 14);
        championLabel.setWrap(true);
        if (player.isLockedIn()) {
            championLabel.setColor(ScreenStyle.ACCENT_GOLD);
        }

        infoCard.add(nameLabel).growX().align(leftAligned ? Align.left : Align.right).row();
        infoCard.add(championLabel).width(180).padTop(12).align(leftAligned ? Align.left : Align.right);
        return infoCard;
    }

    private Table createChampCard(String name, ChampionType type) {
        boolean selected = mySelection == type;

        Table card = new Table();
        // Dark background for cards with gold border style
        Color cardBg = new Color(20 / 255f, 30 / 255f, 50 / 255f, 1f);
        card.setBackground(tinted(cardBg, selected ? ScreenStyle.ACCENT_GOLD : new Color(80 / 255f, 80 / 255f, 80 / 255f, 1f), selected ? 4 : 2));
        card.pad(14);

        // Title with gold highlight if selected
        Label titleLabel = LabelComponents.getLabel(name, 20);
        titleLabel.setAlignment(Align.center);
        titleLabel.setColor(selected ? ScreenStyle.ACCENT_GOLD : ScreenStyle.TEXT_LIGHT);

        // Portrait frame with gold border
        Table portraitHolder = new Table();
        Color portraitBg = new Color(15 / 255f, 20 / 255f, 35 / 255f, 1f);
        portraitHolder.setBackground(tinted(portraitBg, ScreenStyle.ACCENT_GOLD, 3));
        portraitHolder.pad(8);

        Image championImage = new Image(new TextureRegionDrawable(getChampionTexture(type, selected)));
        portraitHolder.add(championImage).size(110);

        // State label - more visible
        Label stateLabel = LabelComponents.getLabel(selected ? "SELECTED" : "SELECT", 16);
        stateLabel.setAlignment(Align.center);
        stateLabel.setColor(selected ? ScreenStyle.ACCENT_GOLD : new Color(150 / 255f, 150 / 255f, 150 / 255f, 1f));

        // Layout
        card.add(titleLabel).center().padBottom(12).row();
        card.add(portraitHolder).size(126).padBottom(12).row();
        card.add(stateLabel).center();

        // Make entire card clickable
        card.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!lockedIn) {
                    mySelection = type;
                    selectionLabel.setText("Selected Champion: " + getChampionText(mySelection));
                    sendSelection(false);
                    updatePlayers(playerList);
                }
            }
        });

        return card;
    }

    private TextureRegion getChampionTexture(ChampionType type, boolean selected) {
        return switch (type) {
            case TANK -> getPreviewRegion(ChampionType.TANK);
            case FIGHTER -> getPreviewRegion(ChampionType.FIGHTER);
            case RANGED -> getPreviewRegion(ChampionType.RANGED);
            default -> new TextureRegion(Sprites.minionTextureSheet);
        };
    }

    private TextureRegion getPreviewRegion(ChampionType championType) {
        var animation = Sprites.getWalkingAnimation(championType, Direction.DOWN);
        if (animation != null) {
            return animation.getKeyFrame(0f, true);
        }

        return switch (championType) {
            case TANK -> Sprites.tankTexture;
            case FIGHTER -> Sprites.fighterTexture;
            case RANGED -> Sprites.rangedTexture;
            case NONE -> new TextureRegion(Sprites.minionTextureSheet);
        };
    }

    private String getChampionText(ChampionType championType) {
        if (championType == null || championType == ChampionType.NONE) {
            return "None";
        }
        return switch (championType) {
            case TANK -> "Tank";
            case FIGHTER -> "Fighter";
            case RANGED -> "Ranged";
            case NONE -> "None";
        };
    }

    private TextureRegionDrawable tinted(Color fillColor, Color borderColor, int borderThickness) {
        int width = 170;
        int height = 320;
        int radius = 6;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(borderColor);
        fillRoundedRect(pixmap, 0, 0, width, height, radius);

        pixmap.setColor(fillColor);
        fillRoundedRect(pixmap, borderThickness, borderThickness, width - 2 * borderThickness, height - 2 * borderThickness, radius);

        TextureRegionDrawable drawable = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();

        return drawable;
    }

    private TextureRegionDrawable tinted(Color color) {
        return tinted(color, new Color(0, 0, 0, 0), 0);
    }

    private void fillRoundedRect(Pixmap pixmap, int x, int y, int w, int h, int r) {
        pixmap.fillRectangle(x + r, y, w - 2 * r, h);
        pixmap.fillRectangle(x, y + r, w, h - 2 * r);

        pixmap.fillCircle(x + r, y + r, r);
        pixmap.fillCircle(x + w - r - 1, y + r, r);
        pixmap.fillCircle(x + r, y + h - r - 1, r);
        pixmap.fillCircle(x + w - r - 1, y + h - r - 1, r);
    }

    private void sendSelection(boolean lockIn) {
        PlayerClassMessage msg = new PlayerClassMessage();
        msg.setSelectedType(mySelection);
        msg.setLockedIn(lockIn);
        ServerConnection.getInstance().getClient().sendTCP(msg);
        if (lockIn) {
            lockedIn = true;
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        ((Main) game).getAudioManager().playChampionSelectMusic();

        ServerConnection connection = ServerConnection.getInstance();
        connection.setLobbyUpdateListener((LobbyUpdateMessage msg) -> {
            if (msg.getLobbyId() == lobbyId) {
                Gdx.app.postRunnable(() -> updatePlayers(msg.getPlayers()));
            }
        });
        connection.setStartGameListener((StartGameMessage msg) -> {
            if (msg.getLobbyId() == lobbyId) {
                Gdx.app.postRunnable(() -> game.setScreen(new GameScreen(game, lobbyId, playerList)));
            }
        });
        connection.setLobbyExitListener(msg -> {
            if (msg.getLobbyId() == lobbyId) {
                Gdx.app.postRunnable(() -> game.setScreen(new ChooseLobbyScreen(game)));
            }
        });
    }

    @Override
    public void hide() {
        ServerConnection.getInstance().setLobbyUpdateListener(null);
        ServerConnection.getInstance().setStartGameListener(null);
        ServerConnection.getInstance().setLobbyExitListener(null);
    }

    @Override
    public void render(float delta) {
        ScreenStyle.clearWindow();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
