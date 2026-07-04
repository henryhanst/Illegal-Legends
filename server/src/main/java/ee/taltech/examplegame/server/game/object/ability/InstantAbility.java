package ee.taltech.examplegame.server.game.object.ability;

public interface InstantAbility {

    /**
     * Class which implements this returns true
     * @return true
     */
    default boolean isInstantAbility() {
        return true;
    }
}
