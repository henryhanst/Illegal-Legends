package ee.taltech.examplegame.server.game.object.champion;

import message.dto.ChampionType;

public class ChampionFactory {

    private ChampionFactory() {
        // static utility class, no instantiation
    }

    public static Champion create(ChampionType type) {
        return switch (type) {
            case FIGHTER -> new Fighter();
            case RANGED  -> new Ranged();
            case TANK    -> new Tank();
            case NONE -> null;
        };
    }
}