package constant;

public class Constants {

    // --- PPM based on your tiledmap design
    // Box2D 100px = 1 meter
    public static final float PPM = 100f;
    public static final float GRAVITY = 0;
    // --- networking constants ---
    public static final int PORT_TCP = 8086;
    public static final int PORT_UDP = 8087;

    // The original university-hosted server address has been replaced
    // with localhost for the public GitHub version.
    public static final String SERVER_IP = "localhost";

    // --- player constants ---
    public static final float PLAYER_SPEED = 2.0f;
    public static final float PLAYER_HEIGHT_IN_PIXELS = 64;
    public static final float PLAYER_WIDTH_IN_PIXELS = 64;
    public static final float PLAYER_COLLISION_WIDTH_IN_PIXELS = 24;
    public static final float PLAYER_COLLISION_HEIGHT_IN_PIXELS = 24;
    public static final int ARRIVE_RADIUS = 15;
    public static final short WALL_CATEGORY = 0x0001;
    public static final short PLAYER_CATEGORY = 0x0002;
    public static final short PLAYER_MASK = -1;

    // --- bullet constants ---
    public static final float BULLET_SPEED = 9.0f;
    public static final long BULLET_TIMEOUT_IN_MILLIS = 200;
    public static final float BULLET_WIDTH_IN_PIXELS = 10;
    public static final float BULLET_HEIGHT_IN_PIXELS = 10;
    public static final float BULLET_RANGE = 250;

    // --- game constants ---
    public static final int GAME_TICK_RATE = 60;
    public static final int PLAYER_COUNT_IN_GAME = 6;
    // Can toggle whether allies can damage each other.
    public static final boolean FRIENDLY_FIRE = false;
    // current values enforce the bounds at the edges of a default-size LibGDX window (defined in Lwjgl3Launcher)
    // changing these constants won't modify the size of the GameScreen
    public static final int ARENA_LOWER_BOUND_X = 0;
    public static final int ARENA_UPPER_BOUND_X = 1920;
    public static final int ARENA_LOWER_BOUND_Y = 0;
    public static final int ARENA_UPPER_BOUND_Y = 1920;

    // --- camera constants ---
    public static final int EDGE_THRESHOLD = 20;
    public static final int CAMERA_SPEED = 20;

    // --- lobby constants ---
    public static final int LOBBY_LIMIT = 6;

    // --- champions constants ---
    public static final int FIGHTER_HP = 600;
    public static final int FIGHTER_HP_PER_LEVEL = 30;
    public static final int FIGHTER_DMG = 40;
    public static final int FIGHTER_DMG_PER_LEVEL = 10;
    public static final int FIGHTER_SPEED = 10;

    public static final int RANGED_HP = 400;
    public static final int RANGED_HP_PER_LEVEL = 10;
    public static final int RANGED_DMG = 50;
    public static final int RANGED_DMG_PER_LEVEL = 10;
    public static final int RANGED_SPEED = 15;

    public static final int TANK_HP = 800;
    public static final int TANK_HP_PER_LEVEL = 45;
    public static final int TANK_DMG = 20;
    public static final int TANK_DMG_PER_LEVEL = 5;
    public static final int TANK_SPEED = 5;

    // --- minion constants ---
    public static final int MINION_HP = 110;
    public static final int MINION_DMG = 5;
    public static final float MINION_SPEED = 1.4f;
    public static final float MINION_WIDTH_IN_PIXELS = 48;
    public static final float MINION_HEIGHT_IN_PIXELS = 48;
    public static final float MINION_COLLISION_WIDTH_IN_PIXELS = 18;
    public static final float MINION_COLLISION_HEIGHT_IN_PIXELS = 18;
    public static final float MINION_ATTACK_RANGE = 26f;
    public static final float MINION_AGGRO_RANGE = 260f;
    public static final long MINION_ATTACK_COOLDOWN = 1000L;
    public static final long MINION_REPATH_INTERVAL_MS = 300L;
    public static final long MINION_SPAWN_INTERVAL_MS = 30_000L;
    public static final int MINIONS_PER_WAVE = 3;

    public static final float MINION_XP_SHARE_RANGE = 325f;
    // --- leveling constants ---
    public static final int XP_PER_MINION = 30;
    public static final int MAX_PLAYER_LEVEL = 18;
    public static final int BASE_XP_TO_LEVEL = 100;
    public static final int XP_TO_LEVEL_GROWTH = 50;

    // --- ability constants ---
    public static final long SKILLSHOT_COOLDOWN = 2000;
    public static final int SKILLSHOT_DMG = 60;

    public static final float MELEE_RANGE = 35;

    public static final long PUNCH_COOLDOWN = 800;

    public static final long ACTION_DELAY = 100;

    public static final int HEAL_AMOUNT = 100;
    public static final long HEAL_COOLDOWN = 10000;

    public static final int FLASH_DISTANCE = 100;
    public static final int FLASH_COOLDOWN = 10000;

    public static final int RAGE_DURATION = 2000;
    public static final int RAGE_COOLDOWN = 6000;
    public static final float RAGE_SPEED_MULTIPLIER = 1.5f;

    public static final int EMPOWERED_DURATION = 2000;
    public static final int EMPOWERED_COOLDOWN = 8000;
    public static final float EMPOWERED_ATTACK_DMG_MULTIPLIER = 2.5f;

    public static final int STUN_DURATION = 4000;
    public static final int STUN_COOLDOWN = 8000;

    // --- Regen constants ---
    public static final int FIGHTER_HP_REGEN_PER_SECOND = 5;
    public static final int RANGED_HP_REGEN_PER_SECOND = 3;
    public static final int TANK_HP_REGEN_PER_SECOND = 7;

    // --- nexus constants ---
    public static final int NEXUS_HP = 1200;
    public static final float NEXUS_WIDTH_IN_PIXELS = 384;
    public static final float NEXUS_HEIGHT_IN_PIXELS = 256;
    public static final float NEXUS_COLLISION_WIDTH_IN_PIXELS = 85; // Actually height
    public static final float NEXUS_COLLISION_HEIGHT_IN_PIXELS = 85; // Actually width
    public static final float NEXUS_COLLISION_OFFSET_Y_IN_PIXELS = -10;
    public static final float NEXUS_COLLISION_OFFSET_X_IN_PIXELS = -3;
    public static final float NEXUS_ATTACK_WIDTH_IN_PIXELS = 90;
    public static final float NEXUS_ATTACK_HEIGHT_IN_PIXELS = 90;

    public static final int TURRET_HP = 2000;
    public static final float TURRET_WIDTH_IN_PIXELS = 256;
    public static final float TURRET_HEIGHT_IN_PIXELS = 256;
    public static final float TURRET_COLLISION_WIDTH_IN_PIXELS = 30;
    public static final float TURRET_COLLISION_HEIGHT_IN_PIXELS = 30;
    public static final float TURRET_COLLISION_OFFSET_Y_IN_PIXELS = -80;
    public static final float TURRET_RANGE = 350f;
    public static final long TURRET_COOLDOWN = 1200;
    public static final int TURRET_DMG = 50;
    public static final int HOMING_BULLET_DMG = 50;
}
