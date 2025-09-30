package com.gameengine.server.combat;

import com.gameengine.server.ServerConfig;
import com.gameengine.server.entity.Player;
import com.gameengine.shared.physics.RaycastHit;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Example: How to use lag compensation for hit validation
 *
 * This demonstrates server-side hit validation with time rewind.
 * When a player shoots, the server rewinds all other players to when
 * the shooter saw them on their screen, performs the hit check, then
 * restores everyone to the present.
 */
public class HitValidationExample {

    private static final Logger logger = LoggerFactory.getLogger(HitValidationExample.class);

    private final LagCompensation lagCompensation;

    public HitValidationExample(Map<Integer, Player> players) {
        this.lagCompensation = new LagCompensation(players);
    }

    /**
     * Example: Player fires a hitscan weapon (gun, laser, etc.)
     *
     * @param shooterId The player who fired
     * @param players All players in the game
     * @param clientTimestamp When the client fired (from input packet)
     */
    public void handlePlayerShoot(int shooterId, Map<Integer, Player> players, long clientTimestamp) {
        Player shooter = players.get(shooterId);
        if (shooter == null) {
            logger.warn("Shooter {} not found!", shooterId);
            return;
        }

        logger.info("Player {} fired weapon at client time {}", shooterId, clientTimestamp);

        // Calculate ray from player's eye position in direction they're looking
        Vector3f eyePosition = LagCompensation.getEyePosition(shooter);
        Vector3f rayDirection = LagCompensation.calculateRayDirection(shooter.getYaw(), shooter.getPitch());

        // Maximum weapon range (e.g., 100 meters)
        float maxRange = 100.0f;

        // Perform lag-compensated raycast
        RaycastHit hit = lagCompensation.performLagCompensatedRaycast(
            shooter,
            eyePosition,
            rayDirection,
            maxRange,
            clientTimestamp
        );

        if (hit != null) {
            // HIT!
            Player targetPlayer = (Player) hit.getCollider().getUserData();

            logger.info("HIT CONFIRMED! Shooter {} hit player {} at distance {} meters",
                shooterId, targetPlayer.getId(), hit.getDistance());

            logger.info("Hit position: {}", hit.getPoint());
            logger.info("Hit normal: {}", hit.getNormal());

            // Apply damage, send feedback to clients, etc.
            applyDamage(shooter, targetPlayer, hit);
        } else {
            // MISS
            logger.info("Player {} missed", shooterId);
        }
    }

    /**
     * Apply damage to the hit player
     */
    private void applyDamage(Player shooter, Player target, RaycastHit hit) {
        // Example damage calculation
        float baseDamage = 25.0f;

        // You could implement:
        // - Headshot detection (check hit.getPoint().y vs player head height)
        // - Distance falloff (damage decreases with distance)
        // - Armor/shield systems
        // - Critical hits
        // etc.

        logger.info("Applying {} damage to player {}", baseDamage, target.getId());

        // TODO: Implement health system and apply damage
        // target.takeDamage(baseDamage);

        // TODO: Send hit confirmation to shooter (for hit markers)
        // sendHitConfirmation(shooter.getId(), target.getId(), hit);

        // TODO: Send damage packet to victim (for damage indicators, screen effects)
        // sendDamageNotification(target.getId(), baseDamage, shooter.getId());
    }

    /**
     * Example: How to use lag compensation from the main game loop
     */
    public static void exampleUsage() {
        // In GameServer.java or PacketHandler.java:
        /*

        // When you receive a shoot packet from a client:
        public void handleShootPacket(int playerId, PlayerInputPacket inputPacket) {
            long clientTimestamp = inputPacket.getClientTimestamp();

            HitValidationExample hitValidator = new HitValidationExample(players);
            hitValidator.handlePlayerShoot(playerId, players, clientTimestamp);
        }

        */
    }

    /**
     * Example: Advanced - Projectile hit validation
     *
     * For slow-moving projectiles (rockets, arrows), you might want to
     * check where the projectile is NOW vs where players WERE when it
     * would have hit them.
     */
    public void handleProjectileHit(int projectileOwnerId, Vector3f projectilePosition,
                                     Map<Integer, Player> players, long projectileSpawnTime) {
        // Calculate how long the projectile has been flying
        long currentTime = System.currentTimeMillis();
        long travelTime = currentTime - projectileSpawnTime;

        // Rewind to when the projectile would have reached this position
        long rewindTime = currentTime - (travelTime / 2); // Simplified

        logger.info("Checking projectile hit with rewind to {}", rewindTime);

        // Check collision at historical positions
        lagCompensation.rewindToTimestamp(rewindTime, projectileOwnerId);

        try {
            // Check if projectile position overlaps any player hitbox
            for (Player player : players.values()) {
                if (player.getId() == projectileOwnerId) {
                    continue; // Don't hit yourself
                }

                float distance = player.getPosition().distance(projectilePosition);
                if (distance < ServerConfig.PLAYER_RADIUS + 0.2f) { // Projectile radius
                    logger.info("Projectile from player {} hit player {}",
                        projectileOwnerId, player.getId());

                    // Apply splash damage, direct hit damage, etc.
                    break;
                }
            }
        } finally {
            lagCompensation.restoreToPresent();
        }
    }
}
