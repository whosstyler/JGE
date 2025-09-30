package com.gameengine.server;

import com.gameengine.shared.network.*;
import com.gameengine.shared.physics.PhysicsWorld;
import com.gameengine.server.entity.Player;
import com.gameengine.server.network.ClientConnection;
import com.gameengine.server.network.ConnectionManager;
import com.gameengine.server.network.PacketHandler;
import com.gameengine.server.world.WorldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authoritative game server - coordinator for network, physics, and game state
 */
public class GameServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    private final int port;
    private final AtomicInteger nextPlayerId;
    private final PhysicsWorld physicsWorld;
    private final WorldManager worldManager;
    private final ConnectionManager connectionManager;
    private final PacketHandler packetHandler;
    private final StateManager stateManager;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private int currentTick;

    public GameServer(int port) {
        this.port = port;
        this.nextPlayerId = new AtomicInteger(1);
        this.physicsWorld = new PhysicsWorld();
        this.worldManager = new WorldManager(physicsWorld);
        this.connectionManager = new ConnectionManager(physicsWorld);
        this.packetHandler = new PacketHandler(nextPlayerId, physicsWorld, worldManager, connectionManager);
        this.stateManager = new StateManager(connectionManager);
        this.currentTick = 0;

        // Generate procedural world with noise
        long seed = 12345L; // Fixed seed for reproducible terrain
        worldManager.generateWorld(seed);
        logger.info("Server initialized with procedural terrain (seed: {})", seed);
    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Game server started on port {}", port);

            // Start game loop thread
            Thread gameLoopThread = new Thread(this::gameLoop, "GameLoop");
            gameLoopThread.start();

            // Accept client connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Client connected: {}", clientSocket.getInetAddress());

                    ClientConnection connection = new ClientConnection(clientSocket, this);
                    Thread clientThread = new Thread(connection, "Client-" + clientSocket.getInetAddress());
                    clientThread.start();

                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting client", e);
                    }
                }
            }

            gameLoopThread.interrupt();
            gameLoopThread.join(1000);

        } catch (Exception e) {
            logger.error("Server error", e);
        } finally {
            shutdown();
        }
    }

    private void gameLoop() {
        long lastTick = System.currentTimeMillis();
        long tickDuration = NetworkProtocol.TICK_MS;

        while (running) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastTick;

            if (elapsed >= tickDuration) {
                float deltaTime = elapsed / 1000.0f;
                tick(deltaTime);
                lastTick = now;
                currentTick++;

                // Sleep for remaining time
                long sleepTime = tickDuration - (System.currentTimeMillis() - now);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }

    private void tick(float deltaTime) {
        long currentTime = System.currentTimeMillis();

        // Update all players and check for timeout/violations
        for (Player player : connectionManager.getAllPlayers()) {
            // Check for input timeout (inactive players)
            if (currentTime - player.getLastInputTime() > ServerConfig.INPUT_TIMEOUT_MS) {
                logger.warn("Player {} timed out (no input for {}ms)",
                    player.getId(), ServerConfig.INPUT_TIMEOUT_MS);
                player.setConnected(false);
                continue;
            }

            // Check if player was disconnected due to violations
            if (!player.isConnected()) {
                logger.error("Player {} disconnected due to {} failed validations",
                    player.getId(), player.getFailedValidations());
                connectionManager.kickPlayer(player);
                continue;
            }

            // Run authoritative physics update
            player.update(deltaTime);
        }

        // Broadcast state updates to all clients
        stateManager.broadcastStateUpdate(currentTick, connectionManager.getAllPlayers());
    }

    public void handlePacket(ClientConnection connection, Packet packet) {
        packetHandler.handlePacket(connection, packet);
    }

    public void removePlayer(Player player) {
        connectionManager.removePlayer(player);
    }

    public void start() {
        running = true;
        Thread serverThread = new Thread(this, "ServerMain");
        serverThread.start();
    }

    public void shutdown() {
        if (!running) return;

        logger.info("Shutting down server");
        running = false;

        // Close all connections
        for (ClientConnection connection : connectionManager.getAllConnections()) {
            connection.disconnect();
        }

        // Close server socket
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : NetworkProtocol.DEFAULT_PORT;
        GameServer server = new GameServer(port);
        server.start();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    }
}