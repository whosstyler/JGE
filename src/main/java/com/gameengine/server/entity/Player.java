package com.gameengine.server.entity;

import com.gameengine.shared.network.PlayerInputPacket;
import com.gameengine.shared.physics.BoxCollider;
import com.gameengine.shared.physics.PhysicsWorld;
import com.gameengine.server.ServerConfig;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Server-side player entity with authoritative physics and validation
 */
public class Player {

    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    private final int id;
    private final String name;
    private final Vector3f position;
    private final Vector3f velocity;
    private final Vector3f lastValidPosition;
    private float yaw;
    private float pitch;

    // State tracking
    private int lastProcessedInputSequence;
    private long lastInputTime;
    private boolean connected;
    private boolean onGround;

    // Anti-cheat tracking
    private int failedValidations;
    private long lastViolationTime;
    private final Deque<PlayerStateSnapshot> stateHistory;

    // Physics state
    private boolean canJump;
    private long lastJumpTime;

    // Collision
    private BoxCollider collider;
    private PhysicsWorld physicsWorld;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        this.position = new Vector3f(0, ServerConfig.GROUND_LEVEL + ServerConfig.PLAYER_HEIGHT / 2, 0);
        this.velocity = new Vector3f(0, 0, 0);
        this.lastValidPosition = new Vector3f(position);
        this.yaw = 0;
        this.pitch = 0;
        this.lastProcessedInputSequence = 0;
        this.lastInputTime = System.currentTimeMillis();
        this.connected = true;
        this.onGround = true; // Start on ground
        this.failedValidations = 0;
        this.lastViolationTime = 0;
        this.stateHistory = new ArrayDeque<>(ServerConfig.STATE_HISTORY_SIZE);
        this.canJump = true;
        this.lastJumpTime = 0;

        // Create collider (0.6m width, 1.8m height - typical player size)
        this.collider = new BoxCollider(new Vector3f(position), new Vector3f(0.6f, 1.8f, 0.6f));
        this.collider.setUserData(this);

        logger.info("Player {} spawned at position {}", id, position);
    }

    /**
     * Set the physics world for collision detection
     */
    public void setPhysicsWorld(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
        if (physicsWorld != null) {
            physicsWorld.addCollider(collider);
        }
    }

    /**
     * Server authoritative physics update
     */
    public void update(float deltaTime) {
        // Save current state for lag compensation
        saveStateSnapshot();

        // Apply gravity
        if (!onGround) {
            velocity.y += ServerConfig.GRAVITY * deltaTime;

            // Terminal velocity cap
            if (velocity.y < -ServerConfig.MAX_VERTICAL_SPEED) {
                velocity.y = -ServerConfig.MAX_VERTICAL_SPEED;
            }
        }

        // Calculate movement delta
        Vector3f movementDelta = new Vector3f(
            velocity.x * deltaTime,
            velocity.y * deltaTime,
            velocity.z * deltaTime
        );

        // Apply collision detection if physics world is available
        if (physicsWorld != null) {
            // Sweep collider to check for collisions
            Vector3f newPosition = new Vector3f(position).add(movementDelta);
            collider.setCenter(newPosition);

            // Check if new position collides (exclude our own collider!)
            if (physicsWorld.checkBox(newPosition, new Vector3f(0.3f, 0.9f, 0.3f), collider)) {
                // Collision detected - try sliding along obstacles
                // Try X movement only
                Vector3f slideX = new Vector3f(position).add(movementDelta.x, 0, 0);
                collider.setCenter(slideX);
                boolean xCollides = physicsWorld.checkBox(slideX, new Vector3f(0.3f, 0.9f, 0.3f), collider);
                if (!xCollides) {
                    position.x = slideX.x;
                }

                // Try Z movement only
                Vector3f slideZ = new Vector3f(position).add(0, 0, movementDelta.z);
                collider.setCenter(slideZ);
                boolean zCollides = physicsWorld.checkBox(slideZ, new Vector3f(0.3f, 0.9f, 0.3f), collider);
                if (!zCollides) {
                    position.z = slideZ.z;
                }

                // Always apply Y movement (gravity/jump)
                position.y += movementDelta.y;
            } else {
                // No collision - apply full movement
                position.add(movementDelta);
            }
        } else {
            // No physics world - just apply movement
            position.add(movementDelta);
        }

        // Ground collision
        if (position.y <= ServerConfig.GROUND_LEVEL + ServerConfig.PLAYER_HEIGHT / 2) {
            position.y = ServerConfig.GROUND_LEVEL + ServerConfig.PLAYER_HEIGHT / 2;
            velocity.y = 0;
            onGround = true;
            canJump = true;
        } else {
            onGround = false;
        }

        // Update collider position
        if (collider != null) {
            collider.setCenter(position);
        }

        // World boundary enforcement
        clampPosition();

        // Note: Friction is NOT applied to player input movement
        // Player input directly sets velocity each frame, so friction would decay movement between inputs
        // Friction should only apply to external forces (knockback, explosions, etc.)

        // Update last valid position if player is stable
        if (failedValidations == 0) {
            lastValidPosition.set(position);
        }
    }

    /**
     * Process and validate player input - NEVER trust the client!
     * Takes button states (like Minecraft/Source engine)
     */
    public boolean processInput(byte buttonStates, float yaw, float pitch, int inputSequence) {
        // Validate input sequence (prevent replay attacks)
        if (inputSequence <= lastProcessedInputSequence) {
            logger.warn("Player {} sent outdated input sequence: {} <= {}", id, inputSequence, lastProcessedInputSequence);
            return false;
        }

        // Validate rotation (prevent spin-bot)
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            logger.warn("Player {} sent invalid rotation values", id);
            recordViolation();
            return false;
        }

        // Clamp pitch to valid range
        pitch = Math.max(-89, Math.min(89, pitch));

        // Update rotation FIRST (before calculating movement)
        // Debug: Log rotation received (every 10th input)
        if (inputSequence % 10 == 0) {
            logger.info("SERVER Input #{}: Received yaw={}, pitch={}", inputSequence, yaw, pitch);
        }

        this.yaw = yaw;
        this.pitch = pitch;

        // Parse button states
        boolean moveForward = (buttonStates & PlayerInputPacket.BUTTON_FORWARD) != 0;
        boolean moveBack = (buttonStates & PlayerInputPacket.BUTTON_BACK) != 0;
        boolean moveLeft = (buttonStates & PlayerInputPacket.BUTTON_LEFT) != 0;
        boolean moveRight = (buttonStates & PlayerInputPacket.BUTTON_RIGHT) != 0;
        boolean jump = (buttonStates & PlayerInputPacket.BUTTON_JUMP) != 0;
        boolean crouch = (buttonStates & PlayerInputPacket.BUTTON_CROUCH) != 0;

        // Calculate movement input (-1 to 1)
        float inputForward = 0;
        float inputStrafe = 0;

        if (moveForward) inputForward += 1.0f;
        if (moveBack) inputForward -= 1.0f;
        if (moveRight) inputStrafe += 1.0f;
        if (moveLeft) inputStrafe -= 1.0f;

        // Calculate movement direction based on player's yaw (server authoritative)
        float yawRad = (float) Math.toRadians(yaw);
        float forwardX = (float) Math.sin(yawRad);   // Fixed: removed negative sign
        float forwardZ = -(float) Math.cos(yawRad);
        float rightX = (float) Math.cos(yawRad);
        float rightZ = (float) Math.sin(yawRad);     // Fixed: removed negative sign

        float moveX = (forwardX * inputForward + rightX * inputStrafe) * ServerConfig.MAX_HORIZONTAL_SPEED;
        float moveZ = (forwardZ * inputForward + rightZ * inputStrafe) * ServerConfig.MAX_HORIZONTAL_SPEED;

        // Handle jump input
        if (jump && onGround && canJump) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastJumpTime > 500) { // Jump cooldown
                velocity.y = ServerConfig.JUMP_VELOCITY;
                onGround = false;
                canJump = false;
                lastJumpTime = currentTime;
            }
        }

        // Apply horizontal movement (server authoritative)
        float moveMultiplier = onGround ? 1.0f : ServerConfig.AIR_CONTROL_FACTOR;
        velocity.x = moveX * moveMultiplier;
        velocity.z = moveZ * moveMultiplier;

        // Update tracking
        lastProcessedInputSequence = inputSequence;
        lastInputTime = System.currentTimeMillis();

        return true;
    }

    /**
     * Validate player position against expected position
     * Used to detect teleportation hacks
     */
    public boolean validatePosition(Vector3f clientPosition) {
        float distance = position.distance(clientPosition);

        if (distance > ServerConfig.MAX_POSITION_CORRECTION) {
            logger.warn("Player {} position mismatch: {} units (max: {})",
                       id, distance, ServerConfig.MAX_POSITION_CORRECTION);
            recordViolation();
            return false;
        }

        return true;
    }

    /**
     * Record anti-cheat violation
     */
    private void recordViolation() {
        failedValidations++;
        lastViolationTime = System.currentTimeMillis();

        if (failedValidations >= ServerConfig.MAX_FAILED_VALIDATIONS) {
            logger.error("Player {} exceeded max violations ({}), should be kicked",
                        id, ServerConfig.MAX_FAILED_VALIDATIONS);
            connected = false; // Will trigger disconnect
        }
    }

    /**
     * Save state snapshot for lag compensation
     */
    private void saveStateSnapshot() {
        PlayerStateSnapshot snapshot = new PlayerStateSnapshot(
            System.currentTimeMillis(),
            new Vector3f(position),
            new Vector3f(velocity),
            yaw,
            pitch,
            onGround
        );

        stateHistory.addLast(snapshot);

        // Limit history size
        while (stateHistory.size() > ServerConfig.STATE_HISTORY_SIZE) {
            stateHistory.removeFirst();
        }
    }

    /**
     * Get player state at a specific time (for lag compensation)
     */
    public PlayerStateSnapshot getStateAtTime(long timestamp) {
        // Don't rewind more than allowed
        long currentTime = System.currentTimeMillis();
        long minTime = currentTime - ServerConfig.MAX_REWIND_TIME_MS;
        timestamp = Math.max(timestamp, minTime);

        // Find closest state
        PlayerStateSnapshot closest = null;
        long closestDiff = Long.MAX_VALUE;

        for (PlayerStateSnapshot snapshot : stateHistory) {
            long diff = Math.abs(snapshot.timestamp - timestamp);
            if (diff < closestDiff) {
                closestDiff = diff;
                closest = snapshot;
            }
        }

        return closest;
    }

    /**
     * Force position correction (rubber-banding)
     */
    public void correctPosition() {
        logger.info("Correcting player {} position from {} to {}", id, position, lastValidPosition);
        position.set(lastValidPosition);
        velocity.set(0, 0, 0);
        failedValidations = 0;
    }

    private void clampPosition() {
        position.x = Math.max(ServerConfig.WORLD_MIN_X, Math.min(ServerConfig.WORLD_MAX_X, position.x));
        position.y = Math.max(ServerConfig.WORLD_MIN_Y, Math.min(ServerConfig.WORLD_MAX_Y, position.y));
        position.z = Math.max(ServerConfig.WORLD_MIN_Z, Math.min(ServerConfig.WORLD_MAX_Z, position.z));
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public Vector3f getPosition() { return position; }
    public Vector3f getVelocity() { return velocity; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public int getLastProcessedInputSequence() { return lastProcessedInputSequence; }
    public long getLastInputTime() { return lastInputTime; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public boolean isOnGround() { return onGround; }
    public int getFailedValidations() { return failedValidations; }
    public BoxCollider getCollider() { return collider; }

    /**
     * Player state snapshot for lag compensation
     */
    public static class PlayerStateSnapshot {
        public final long timestamp;
        public final Vector3f position;
        public final Vector3f velocity;
        public final float yaw;
        public final float pitch;
        public final boolean onGround;

        public PlayerStateSnapshot(long timestamp, Vector3f position, Vector3f velocity,
                                  float yaw, float pitch, boolean onGround) {
            this.timestamp = timestamp;
            this.position = position;
            this.velocity = velocity;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
        }
    }
}