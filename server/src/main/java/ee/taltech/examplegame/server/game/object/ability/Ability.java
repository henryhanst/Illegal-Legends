package ee.taltech.examplegame.server.game.object.ability;

import lombok.Getter;

public abstract class Ability {
    @Getter
    private final long cooldownMs;
    private long lastUsed = 0;

    protected Ability(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public boolean isReady() {
        return System.currentTimeMillis() - lastUsed >= cooldownMs;
    }

    public long getRemainingCooldownMs() {
        return Math.max(0L, cooldownMs - (System.currentTimeMillis() - lastUsed));
    }

    public void markUsed() {
        lastUsed = System.currentTimeMillis();
    }
}
