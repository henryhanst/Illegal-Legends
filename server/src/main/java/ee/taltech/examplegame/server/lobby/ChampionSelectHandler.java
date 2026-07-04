package ee.taltech.examplegame.server.lobby;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;
import message.PlayerClassMessage;
import message.PlayerLobbyInfo;
import message.dto.ChampionType;
import message.dto.Team;

public class ChampionSelectHandler {

    public void handleChampionSelection(Lobby lobby, Connection conn, PlayerClassMessage msg) {
        PlayerLobbyInfo playerInfo = lobby.getPlayerInfo(conn);
        if (playerInfo == null || playerInfo.isLockedIn()) {
            return;
        }

        ChampionType selectedType = msg.getSelectedType();
        if (selectedType == null || selectedType == ChampionType.NONE) {
            return;
        }

        playerInfo.setChampionType(selectedType);

        if (msg.isLockedIn()) {
            boolean isTaken = lobby.getPlayerMap().values().stream()
                    .filter(p -> p.getTeam() == playerInfo.getTeam() && p.getConnectionId() != conn.getID())
                    .anyMatch(p -> p.isLockedIn() && p.getChampionType() == selectedType);

            if (isTaken) {
                lobby.broadcastChampionSelectUpdate();
                return;
            }

            playerInfo.setLockedIn(true);
        } else {
            playerInfo.setLockedIn(false);
        }

        lobby.broadcastChampionSelectUpdate();
        checkIfAllLocked(lobby);
    }

    private void checkIfAllLocked(Lobby lobby) {
        boolean allLocked = lobby.getPlayerMap().values().stream()
                .allMatch(player -> player.getTeam() != Team.NONE
                        && player.isLockedIn()
                        && player.getChampionType() != null
                        && player.getChampionType() != ChampionType.NONE);

        if (allLocked) {
            Log.info("All players locked in. Starting game for lobby: " + lobby.getLobbyId());
            lobby.startGame();
        }
    }
}
