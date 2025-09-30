package com.gameengine.server.network;

import com.gameengine.shared.network.*;
import com.gameengine.shared.physics.PhysicsWorld;
import com.gameengine.server.entity.Player;
import com.gameengine.server.world.WorldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles all incoming packets from clients
 */
public class PacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(PacketHandler.class);

    private final AtomicInteger nextPlayerId;
    private final PhysicsWorld physicsWorld;
    private final WorldManager worldManager;
    private final ConnectionManager connectionManager;

    public PacketHandler(AtomicInteger nextPlayerId, PhysicsWorld physicsWorld,
                        WorldManager worldManager, ConnectionManager connectionManager) {
        this.nextPlayerId = nextPlayerId;
        this.physicsWorld = physicsWorld;
        this.worldManager = worldManager;
        this.connectionManager = connectionManager;
    }

    public void handlePacket(ClientConnection connection, Packet packet) {
        try {
            switch (packet.getType()) {
                case NetworkProtocol.PACKET_CONNECT -> handleConnect(connection, (ConnectPacket) packet);
                case NetworkProtocol.PACKET_DISCONNECT -> handleDisconnect(connection);
                case NetworkProtocol.PACKET_PLAYER_INPUT -> handlePlayerInput(connection, (PlayerInputPacket) packet);
                case NetworkProtocol.PACKET_HEARTBEAT -> handleHeartbeat(connection);
            }
        } catch (Exception e) {
            logger.error("Error handling packet", e);
        }
    }

    private void handleConnect(ClientConnection connection, ConnectPacket packet) {
        if (packet.getProtocolVersion() != NetworkProtocol.PROTOCOL_VERSION) {
            logger.warn("Client protocol version mismatch");
            connection.disconnect();
            return;
        }

        // Create player
        int playerId = nextPlayerId.getAndIncrement();
        Player player = new Player(playerId, packet.getPlayerName());
        player.setPhysicsWorld(physicsWorld);
        connectionManager.addPlayer(connection, player);

        logger.info("Player {} joined: {}", playerId, packet.getPlayerName());

        // Send connect success to new client (tells them their player ID)
        ConnectSuccessPacket successPacket = new ConnectSuccessPacket(playerId, packet.getPlayerName());
        connection.sendPacket(successPacket);

        // Send terrain generation parameters so client can generate identical terrain
        TerrainSyncPacket terrainPacket = new TerrainSyncPacket(
            worldManager.getSeed(),
            worldManager.getTerrainWidth(),
            worldManager.getTerrainDepth(),
            worldManager.isFlat(),
            worldManager.getFlatHeight()
        );
        connection.sendPacket(terrainPacket);
        logger.info("Player {} connected - sent terrain sync (seed: {}, flat: {})",
            playerId, worldManager.getSeed(), worldManager.isFlat());

        // Send all existing players to the new joiner
        for (Player existingPlayer : connectionManager.getAllPlayers()) {
            if (existingPlayer.getId() != playerId) {
                PlayerJoinPacket existingPacket = new PlayerJoinPacket(existingPlayer.getId(), existingPlayer.getName());
                connection.sendPacket(existingPacket);
            }
        }

        // Broadcast new player to all clients (including themselves for consistency)
        PlayerJoinPacket joinPacket = new PlayerJoinPacket(playerId, packet.getPlayerName());
        connectionManager.broadcastToAll(joinPacket);
    }

    private void handleDisconnect(ClientConnection connection) {
        Player player = connection.getPlayer();
        if (player != null) {
            connectionManager.removePlayer(player);
        }
        connection.disconnect();
    }

    private void handlePlayerInput(ClientConnection connection, PlayerInputPacket packet) {
        Player player = connection.getPlayer();
        if (player == null) return;

        // Use Player's comprehensive validation system
        boolean inputValid = player.processInput(
            packet.getButtonStates(),
            packet.getYaw(),
            packet.getPitch(),
            packet.getInputSequence()
        );

        // If validation failed, check if we need to send position correction
        if (!inputValid) {
            logger.warn("Player {} failed input validation - seq: {}, violations: {}",
                player.getId(), packet.getInputSequence(), player.getFailedValidations());

            // Send position correction to client (rubber-banding)
            PositionCorrectionPacket correction = new PositionCorrectionPacket(
                player.getId(),
                player.getPosition().x,
                player.getPosition().y,
                player.getPosition().z,
                player.getLastProcessedInputSequence()
            );
            connection.sendPacket(correction);
        }
    }

    private void handleHeartbeat(ClientConnection connection) {
        // Heartbeat received - connection is alive
        // Note: lastInputTime is updated by processInput(), not here
    }
}
