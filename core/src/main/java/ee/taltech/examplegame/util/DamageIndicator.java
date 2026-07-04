package ee.taltech.examplegame.util;

public class DamageIndicator {
    public float x;
    public float y;
    public final String text;
    public float lifetime;
    public float alpha;

    private static final float TOTAL_LIFETIME = 1.25f;
    private static final float RISE_SPEED = 0.4f;

    public DamageIndicator(float x, float y, int damage) {
        this.x = x + (float)(Math.random() * 0.2f) - 0.1f; // slight scatter
        this.y = y;
        this.text = "-" + damage;
        this.lifetime = TOTAL_LIFETIME;
        this.alpha = 1f;
    }

    /** @return true when expired */
    public boolean update(float delta) {
        lifetime -= delta;
        y += RISE_SPEED * delta;
        alpha = Math.max(0f, lifetime / TOTAL_LIFETIME);
        return lifetime <= 0f;
    }
}