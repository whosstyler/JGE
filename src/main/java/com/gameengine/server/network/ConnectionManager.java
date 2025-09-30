package com.gameengine.server.network;

import com.gameengine.shared.network.Packet;
import com.gameengine.shared.network.PlayerLeavePacket;
import com.gameengine.shared.physics.PhysicsWorld;
import com.gameengine.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client connections and player lifecycle
 */
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<Integer, Player> players;
    private final Map<ClientConnection, Player> connectionPlayerMap;
    private final PhysicsWorld physicsWorld;

    public ConnectionManager(PhysicsWorld physicsWorld) {
        this.players = new ConcurrentHashMap<>();
        this.connectionPlayerMap = new ConcurrentHashMap<>();
        this.physicsWorld = physicsWorld;
    }

    /**
     * Add new player to the game
     */
    public void addPlayer(ClientConnection connection, Player player) {
        players.put(player.getId(), player);
        connectionPlayerMap.put(connection, player);
        connection.setPlayer(player);
    }

    /**
     * Remove player from the game
     */
    public void removePlayer(Player player) {
        players.remove(player.getId());
        connectionPlayerMap.values().remove(player);

        // Remove from physics world
        if (player.getCollider() != null) {
            physicsWorld.removeCollider(player.getCollider());
        }

        logger.info("Player {} left: {}", player.getId(), player.getName());

        // Notify all clients
        PlayerLeavePacket leavePacket = new PlayerLeavePacket(player.getId());
        broadcastToAll(leavePacket);
    }

    /**
     * Kick player due to violations or timeout
     */
    public void kickPlayer(Player player) {
        // Find connection for this player
        ClientConnection connectionToKick = null;
        for (Map.Entry<ClientConnection, Player> entry : connectionPlayerMap.entrySet()) {
            if (entry.getValue().equals(player)) {
                connectionToKick = entry.getKey();
                break;
            }
        }

        if (connectionToKick != null) {
            connectionToKick.disconnect();
        }

        removePlayer(player);
    }

    /**
     * Get all active players
     */
    public Collection<Player> getAllPlayers() {
        return players.values();
    }

    /**
     * Get all active connections
     */
    public Collection<ClientConnection> getAllConnections() {
        return connectionPlayerMap.keySet();
    }

    /**
     * Broadcast packet to all connected clients
     */
    public void broadcastToAll(Packet packet) {
        for (ClientConnection connection : connectionPlayerMap.keySet()) {
            connection.sendPacket(packet);
        }
    }
}
