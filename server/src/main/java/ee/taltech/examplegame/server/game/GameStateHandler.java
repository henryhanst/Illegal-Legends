package ee.taltech.examplegame.server.game;

import ee.taltech.examplegame.server.game.object.Bullet;
import ee.taltech.examplegame.server.game.object.Minion;
import ee.taltech.examplegame.server.game.object.nexus.Nexus;
import ee.taltech.examplegame.server.game.object.Player;
import ee.taltech.examplegame.server.game.object.turret.Turret;
import lombok.Getter;
import lombok.Setter;
import message.GameStateMessage;
import message.dto.BulletState;
import message.dto.MinionState;
import message.dto.NexusState;
import message.dto.PlayerState;
import message.dto.TurretState;

import java.util.ArrayList;
import java.util.List;

import static constant.Constants.GAME_TICK_RATE;

@Getter
@Setter
public class GameStateHandler {

    private boolean allPlayersHaveJoined = false;
    private float gameTime = 0;

    public void incrementGameTimeIfPlayersPresent() {
        if (allPlayersHaveJoined) {
            gameTime += 1f / GAME_TICK_RATE;
        }
    }

    public GameStateMessage getGameStateMessage(List<Player> players, List<Minion> minions, List<Bullet> bullets, List<Nexus> nexuses, List<Turret> turrets) {
        // get the state of all players
        var playerStates = new ArrayList<PlayerState>();
//        players.forEach(player -> playerStates.add(player.getState()));
        players.forEach(player -> {
            PlayerState state = player.getState();
            playerStates.add(state);
        });

        var minionStates = new ArrayList<MinionState>();
        minions.forEach(minion -> minionStates.add(minion.getState()));

        // get state of all bullets
        var bulletStates = new ArrayList<BulletState>();
        bullets.forEach(bullet -> bulletStates.add(bullet.getState()));

        var nexusStates = new ArrayList<NexusState>();
        nexuses.forEach(nexus -> nexusStates.add(nexus.getState()));

        var turretStates = new ArrayList<TurretState>();
        turrets.forEach(turret -> turretStates.add(turret.getTurretState()));

        // construct gameStateMessage
        var gameStateMessage = new GameStateMessage();
        gameStateMessage.setPlayerStates(playerStates);
        gameStateMessage.setMinionStates(minionStates);
        gameStateMessage.setBulletStates(bulletStates);
        gameStateMessage.setNexusStates(nexusStates);
        gameStateMessage.setTurretStates(turretStates);
        gameStateMessage.setGameTime(Math.round(gameTime));
        gameStateMessage.setAllPlayersHaveJoined(allPlayersHaveJoined);

        return gameStateMessage;
    }


}
