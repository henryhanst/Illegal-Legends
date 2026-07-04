package ee.taltech.examplegame.server.game.object.ability;

import ee.taltech.examplegame.server.game.object.Player;

public abstract class TimedEffect extends Ability {

    private long endTime;
    private long durationMs;

    protected TimedEffect(long cooldownMs, long durationMs) {
        super(cooldownMs);
        this.durationMs = durationMs;
    }

    public void startTimer() {
        this.endTime = System.currentTimeMillis() + durationMs;
    }

    public abstract void onApply(Player player);
    public abstract void onExpire(Player player);

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime;
    }
}
