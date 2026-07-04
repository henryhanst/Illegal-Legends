package ee.taltech.examplegame.server.game.object;

import message.dto.Team;

public interface AttackableTarget {
    float getX();
    float getY();
    Team getTeam();
    void takeDamage(int damageAmount);
    boolean isDestroyed();
}
