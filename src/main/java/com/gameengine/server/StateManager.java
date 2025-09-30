package com.gameengine.server;

import com.gameengine.shared.network.StateUpdatePacket;
import com.gameengine.server.entity.Player;
import com.gameengine.server.network.ConnectionManager;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Manages game state broadcasting to clients
 */
public class StateManager {

    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);

    private final ConnectionManager connectionManager;

    public StateManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Broadcast current game state to all clients
     */
    public void broadcastStateUpdate(int currentTick, Collection<Player> players) {
        StateUpdatePacket packet = new StateUpdatePacket(currentTick);

        for (Player player : players) {
            Vector3f pos = player.getPosition();

            // Debug: Log broadcast rotation (only when it changes significantly)
            if (player.getId() == 1 && currentTick % 20 == 0) { // Every second
                logger.debug("Broadcasting player {} rotation: yaw={}, pitch={}",
                    player.getId(), player.getYaw(), player.getPitch());
            }

            packet.addPlayerState(
                    player.getId(),
                    pos.x, pos.y, pos.z,
                    player.getYaw(),
                    player.getPitch(),
                    player.getLastProcessedInputSequence()
            );
        }

        // Send to all connections
        connectionManager.broadcastToAll(packet);
    }
}
