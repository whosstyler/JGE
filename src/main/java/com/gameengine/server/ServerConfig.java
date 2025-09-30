package com.gameengine.server;

/**
 * Server configuration and game rules
 */
public class ServerConfig {

    // Physics constants
    public static final float GRAVITY = -20.0f; // Units per second squared
    public static final float GROUND_LEVEL = 0.0f;
    public static final float PLAYER_HEIGHT = 1.8f;
    public static final float PLAYER_RADIUS = 0.3f;

    // Movement validation
    public static final float MAX_HORIZONTAL_SPEED = 10.0f; // Units per second
    public static final float MAX_VERTICAL_SPEED = 15.0f; // Units per second (allows jump + gravity)
    public static final float MAX_SPEED_TOLERANCE = 1.5f; // 50% tolerance for lag/precision

    // Jump mechanics
    public static final float JUMP_VELOCITY = 8.0f;
    public static final boolean ALLOW_AIR_CONTROL = true;
    public static final float AIR_CONTROL_FACTOR = 0.5f; // 50% movement in air

    // Anti-cheat thresholds
    public static final float MAX_POSITION_CORRECTION = 5.0f; // Max units to rubber-band
    public static final int MAX_FAILED_VALIDATIONS = 10; // Kick after this many failures
    public static final long INPUT_TIMEOUT_MS = 5000; // Kick if no input for 5 seconds

    // World boundaries
    public static final float WORLD_MIN_X = -1000f;
    public static final float WORLD_MAX_X = 1000f;
    public static final float WORLD_MIN_Y = 0f;
    public static final float WORLD_MAX_Y = 500f;
    public static final float WORLD_MIN_Z = -1000f;
    public static final float WORLD_MAX_Z = 1000f;

    // Lag compensation
    public static final int MAX_REWIND_TIME_MS = 200; // Max rewind for lag comp
    public static final int STATE_HISTORY_SIZE = 20; // 20 ticks * 50ms = 1000ms history

    // Server performance
    public static final int TICK_RATE = 20; // Ticks per second
    public static final int TICK_MS = 1000 / TICK_RATE;

    private ServerConfig() {}
}