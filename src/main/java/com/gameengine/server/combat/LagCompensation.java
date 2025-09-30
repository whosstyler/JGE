package com.gameengine.server.combat;

import com.gameengine.server.ServerConfig;
import com.gameengine.server.entity.Player;
import com.gameengine.server.entity.Player.PlayerStateSnapshot;
import com.gameengine.shared.physics.Ray;
import com.gameengine.shared.physics.RaycastHit;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lag Compensation System - Server-side hit validation with time rewind
 *
 * How it works:
 * 1. Client sends input with their local timestamp
 * 2. Server rewinds all players to that timestamp (minus ping/2)
 * 3. Server performs hit detection at that historical point
 * 4. Server restores players to current state
 *
 * This makes hits feel fair for players with higher ping.
 */
public class LagCompensation {

    private static final Logger logger = LoggerFactory.getLogger(LagCompensation.class);

    private final Map<Integer, Player> players;

    // Store original states for restoration after rewind
    private final Map<Integer, PlayerState> savedStates;

    public LagCompensation(Map<Integer, Player> players) {
        this.players = players;
        this.savedStates = new HashMap<>();
    }

    /**
     * Rewind all players to a specific timestamp for hit validation
     * @param timestamp The time to rewind to (client timestamp)
     * @param excludePlayerId Don't rewind this player (the shooter)
     */
    public void rewindToTimestamp(long timestamp, int excludePlayerId) {
        long currentTime = System.currentTimeMillis();

        // Clamp to maximum rewind time
        long maxRewind = currentTime - ServerConfig.MAX_REWIND_TIME_MS;
        if (timestamp < maxRewind) {
            timestamp = maxRewind;
        }

        logger.debug("Rewinding to timestamp {} ({}ms ago) for player {}",
            timestamp, currentTime - timestamp, excludePlayerId);

        savedStates.clear();

        // Rewind all players except the shooter
        for (Player player : players.values()) {
            if (player.getId() == excludePlayerId) {
                continue; // Don't rewind the shooter
            }

            // Save current state
            savedStates.put(player.getId(), new PlayerState(
                new Vector3f(player.getPosition()),
                new Vector3f(player.getVelocity()),
                player.getYaw(),
                player.getPitch()
            ));

            // Get historical state
            PlayerStateSnapshot historicalState = player.getStateAtTime(timestamp);
            if (historicalState != null) {
                // Temporarily set player to historical position
                player.getPosition().set(historicalState.position);
                player.getVelocity().set(historicalState.velocity);

                logger.debug("Rewound player {} from {} to {}",
                    player.getId(), savedStates.get(player.getId()).position, historicalState.position);
            }
        }
    }

    /**
     * Restore all players to their current state after hit validation
     */
    public void restoreToPresent() {
        for (Map.Entry<Integer, PlayerState> entry : savedStates.entrySet()) {
            Player player = players.get(entry.getKey());
            if (player != null) {
                PlayerState state = entry.getValue();
                player.getPosition().set(state.position);
                player.getVelocity().set(state.velocity);

                logger.debug("Restored player {} to {}", player.getId(), state.position);
            }
        }

        savedStates.clear();
    }

    /**
     * Perform a raycast hit check with lag compensation
     *
     * @param shooterPlayer The player shooting
     * @param rayOrigin Origin of the ray (shooter's eye position)
     * @param rayDirection Direction of the ray
     * @param maxDistance Maximum raycast distance
     * @param clientTimestamp When the client fired (for rewind)
     * @return Hit result, or null if no hit
     */
    public RaycastHit performLagCompensatedRaycast(Player shooterPlayer, Vector3f rayOrigin,
                                                    Vector3f rayDirection, float maxDistance,
                                                    long clientTimestamp) {
        // Rewind all other players to when the shooter saw them
        rewindToTimestamp(clientTimestamp, shooterPlayer.getId());

        try {
            // Perform raycast against all players at their historical positions
            RaycastHit closestHit = null;
            float closestDistance = maxDistance;

            for (Player targetPlayer : players.values()) {
                if (targetPlayer.getId() == shooterPlayer.getId()) {
                    continue; // Don't shoot yourself
                }

                // Check if ray hits this player's hitbox
                RaycastHit hit = raycastPlayerHitbox(rayOrigin, rayDirection, targetPlayer, maxDistance);

                if (hit != null && hit.getDistance() < closestDistance) {
                    closestHit = hit;
                    closestDistance = hit.getDistance();
                }
            }

            if (closestHit != null) {
                logger.info("Lag-compensated hit! Shooter {} hit player {} at distance {}",
                    shooterPlayer.getId(),
                    ((Player)closestHit.getCollider().getUserData()).getId(),
                    closestHit.getDistance());
            }

            return closestHit;

        } finally {
            // ALWAYS restore players to present, even if exception occurs
            restoreToPresent();
        }
    }

    /**
     * Raycast against a player's bounding box
     */
    private RaycastHit raycastPlayerHitbox(Vector3f rayOrigin, Vector3f rayDirection,
                                          Player player, float maxDistance) {
        // Player hitbox (simplified AABB)
        Vector3f playerPos = player.getPosition();
        float halfWidth = ServerConfig.PLAYER_RADIUS;  // 0.3m
        float halfHeight = ServerConfig.PLAYER_HEIGHT / 2;  // 0.9m

        Vector3f boxMin = new Vector3f(
            playerPos.x - halfWidth,
            playerPos.y - halfHeight,
            playerPos.z - halfWidth
        );

        Vector3f boxMax = new Vector3f(
            playerPos.x + halfWidth,
            playerPos.y + halfHeight,
            playerPos.z + halfWidth
        );

        // Ray-AABB intersection test
        float tMin = 0.0f;
        float tMax = maxDistance;

        // Check each axis
        for (int i = 0; i < 3; i++) {
            float origin = i == 0 ? rayOrigin.x : (i == 1 ? rayOrigin.y : rayOrigin.z);
            float dir = i == 0 ? rayDirection.x : (i == 1 ? rayDirection.y : rayDirection.z);
            float min = i == 0 ? boxMin.x : (i == 1 ? boxMin.y : boxMin.z);
            float max = i == 0 ? boxMax.x : (i == 1 ? boxMax.y : boxMax.z);

            if (Math.abs(dir) < 0.0001f) {
                // Ray parallel to axis
                if (origin < min || origin > max) {
                    return null; // Miss
                }
            } else {
                float t1 = (min - origin) / dir;
                float t2 = (max - origin) / dir;

                if (t1 > t2) {
                    float temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);

                if (tMin > tMax) {
                    return null; // Miss
                }
            }
        }

        // Hit! Calculate hit point
        Vector3f hitPoint = new Vector3f(rayOrigin).add(
            new Vector3f(rayDirection).mul(tMin)
        );

        // Calculate surface normal (simple - just use closest axis)
        Vector3f normal = new Vector3f(0, 1, 0); // Simplified - should calculate properly

        return new RaycastHit(player.getCollider(), hitPoint, normal, tMin, player);
    }

    /**
     * Helper method to calculate ray direction from yaw/pitch
     */
    public static Vector3f calculateRayDirection(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float x = -(float) (Math.cos(pitchRad) * Math.sin(yawRad));
        float y = -(float) Math.sin(pitchRad);
        float z = (float) (Math.cos(pitchRad) * Math.cos(yawRad));

        return new Vector3f(x, y, z).normalize();
    }

    /**
     * Helper method to get eye position for a player
     */
    public static Vector3f getEyePosition(Player player) {
        return new Vector3f(
            player.getPosition().x,
            player.getPosition().y + ServerConfig.PLAYER_HEIGHT * 0.9f, // Eye level
            player.getPosition().z
        );
    }

    /**
     * Simple struct to store player state
     */
    private static class PlayerState {
        public final Vector3f position;
        public final Vector3f velocity;
        public final float yaw;
        public final float pitch;

        public PlayerState(Vector3f position, Vector3f velocity, float yaw, float pitch) {
            this.position = position;
            this.velocity = velocity;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
