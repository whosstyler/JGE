package com.gameengine.client.network;

import com.gameengine.shared.network.*;
import com.gameengine.client.player.ClientPlayer;
import com.gameengine.shared.world.WorldData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Client network handler
 */
public class NetworkClient implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);

    private final String host;
    private final int port;
    private final String playerName;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private final BlockingQueue<Packet> outgoingPackets;
    private final Map<Integer, ClientPlayer> players;
    private ClientPlayer localPlayer;
    private int myPlayerId = -1;  // Server-assigned player ID
    private WorldData worldData;  // Map data from server (deprecated)
    private volatile boolean mapReceived = false;  // Deprecated

    // Terrain sync data
    private long terrainSeed;
    private int terrainWidth;
    private int terrainDepth;
    private boolean terrainIsFlat;
    private float terrainFlatHeight;
    private volatile boolean terrainReceived = false;

    private volatile boolean running;
    private int sequenceNumber;

    public NetworkClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.outgoingPackets = new LinkedBlockingQueue<>();
        this.players = new ConcurrentHashMap<>();
        this.running = false;
        this.sequenceNumber = 0;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        running = true;

        logger.info("Connected to server: {}:{}", host, port);

        // Send connect packet
        ConnectPacket connectPacket = new ConnectPacket(playerName);
        sendPacket(connectPacket);

        // Start threads
        Thread receiverThread = new Thread(this::receiveLoop, "ClientReceiver");
        Thread senderThread = new Thread(this::sendLoop, "ClientSender");
        receiverThread.start();
        senderThread.start();
    }

    @Override
    public void run() {
        receiveLoop();
    }

    private void receiveLoop() {
        while (running && !socket.isClosed()) {
            try {
                int packetSize = input.readInt();
                if (packetSize <= 0 || packetSize > NetworkProtocol.MAX_PACKET_SIZE) {
                    logger.warn("Invalid packet size: {}", packetSize);
                    break;
                }

                byte[] packetData = new byte[packetSize];
                input.readFully(packetData);

                Packet packet = Packet.deserialize(packetData);
                if (packet != null) {
                    handlePacket(packet);
                }

            } catch (IOException e) {
                if (running) {
                    logger.error("Error receiving packet", e);
                }
                break;
            }
        }
    }

    private void sendLoop() {
        while (running && !socket.isClosed()) {
            try {
                Packet packet = outgoingPackets.take();
                packet.setSequenceNumber(sequenceNumber++);

                byte[] data = packet.serialize();
                synchronized (output) {
                    output.writeInt(data.length);
                    output.write(data);
                    output.flush();
                }

            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                if (running) {
                    logger.error("Error sending packet", e);
                }
                break;
            }
        }
    }

    private void handlePacket(Packet packet) {
        try {
            switch (packet.getType()) {
                case NetworkProtocol.PACKET_CONNECT_SUCCESS -> handleConnectSuccess((ConnectSuccessPacket) packet);
                case NetworkProtocol.PACKET_MAP_DATA -> handleMapData((MapDataPacket) packet);
                case NetworkProtocol.PACKET_TERRAIN_SYNC -> handleTerrainSync((TerrainSyncPacket) packet);
                case NetworkProtocol.PACKET_STATE_UPDATE -> handleStateUpdate((StateUpdatePacket) packet);
                case NetworkProtocol.PACKET_PLAYER_JOIN -> handlePlayerJoin((PlayerJoinPacket) packet);
                case NetworkProtocol.PACKET_PLAYER_LEAVE -> handlePlayerLeave((PlayerLeavePacket) packet);
                case NetworkProtocol.PACKET_POSITION_CORRECTION -> handlePositionCorrection((PositionCorrectionPacket) packet);
            }
        } catch (Exception e) {
            logger.error("Error handling packet", e);
        }
    }

    private void handleConnectSuccess(ConnectSuccessPacket packet) {
        myPlayerId = packet.getYourPlayerId();
        logger.info("Connected successfully! You are player ID: {} ({})", myPlayerId, packet.getYourPlayerName());
    }

    private void handleMapData(MapDataPacket packet) {
        worldData = packet.getWorldData();
        mapReceived = true;
        logger.info("Received map from server: {} with {} spawn points, {} geometry",
            worldData.getName(),
            worldData.getSpawnPoints().size(),
            worldData.getStaticGeometry().size());
    }

    private void handleTerrainSync(TerrainSyncPacket packet) {
        terrainSeed = packet.getSeed();
        terrainWidth = packet.getWidth();
        terrainDepth = packet.getDepth();
        terrainIsFlat = packet.isFlat();
        terrainFlatHeight = packet.getFlatHeight();
        terrainReceived = true;
        logger.info("Received terrain sync from server: seed={}, size={}x{}, flat={}, height={}",
            terrainSeed, terrainWidth, terrainDepth, terrainIsFlat, terrainFlatHeight);
    }

    private void handleStateUpdate(StateUpdatePacket packet) {
        for (StateUpdatePacket.PlayerState state : packet.getPlayerStates()) {
            ClientPlayer player = players.get(state.playerId);
            if (player != null) {
                player.updateServerState(
                        state.x, state.y, state.z,
                        state.yaw, state.pitch,
                        state.lastProcessedInput
                );
            }
        }
    }

    private void handlePlayerJoin(PlayerJoinPacket packet) {
        ClientPlayer player = new ClientPlayer(packet.getPlayerId(), packet.getPlayerName());
        players.put(packet.getPlayerId(), player);

        // Check if this is the local player by comparing IDs
        if (packet.getPlayerId() == myPlayerId) {
            localPlayer = player;
            logger.info("YOU joined the game: {} (ID: {})", packet.getPlayerName(), packet.getPlayerId());
        } else {
            logger.info("Player joined: {} (ID: {})", packet.getPlayerName(), packet.getPlayerId());
        }
    }

    private void handlePlayerLeave(PlayerLeavePacket packet) {
        ClientPlayer player = players.remove(packet.getPlayerId());
        if (player != null) {
            logger.info("Player left: {} (ID: {})", player.getName(), player.getId());
        }
    }

    private void handlePositionCorrection(PositionCorrectionPacket packet) {
        ClientPlayer player = players.get(packet.getPlayerId());
        if (player != null && player == localPlayer) {
            logger.warn("Server corrected position to ({}, {}, {}) - last valid seq: {}",
                packet.getX(), packet.getY(), packet.getZ(), packet.getLastValidInputSequence());

            // Apply server correction (rubber-banding)
            player.forcePosition(packet.getX(), packet.getY(), packet.getZ());
            player.updateServerState(
                packet.getX(), packet.getY(), packet.getZ(),
                player.getYaw(), player.getPitch(),
                packet.getLastValidInputSequence()
            );
        }
    }

    public void sendInput(byte buttonStates, float yaw, float pitch, int inputSequence) {
        PlayerInputPacket packet = new PlayerInputPacket(buttonStates, yaw, pitch, inputSequence);
        sendPacket(packet);
    }

    public void sendPacket(Packet packet) {
        if (running && !socket.isClosed()) {
            outgoingPackets.offer(packet);
        }
    }

    public void disconnect() {
        if (!running) return;

        running = false;
        sendPacket(new DisconnectPacket());

        try {
            Thread.sleep(100); // Give time for disconnect packet to send
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            socket.close();
        } catch (IOException e) {
            logger.error("Error closing socket", e);
        }

        logger.info("Disconnected from server");
    }

    public Map<Integer, ClientPlayer> getPlayers() {
        return players;
    }

    public ClientPlayer getLocalPlayer() {
        return localPlayer;
    }

    public WorldData getWorldData() {
        return worldData;
    }

    public boolean isMapReceived() {
        return mapReceived;
    }

    // Terrain sync getters
    public boolean isTerrainReceived() {
        return terrainReceived;
    }

    public long getTerrainSeed() {
        return terrainSeed;
    }

    public int getTerrainWidth() {
        return terrainWidth;
    }

    public int getTerrainDepth() {
        return terrainDepth;
    }

    public boolean isTerrainFlat() {
        return terrainIsFlat;
    }

    public float getTerrainFlatHeight() {
        return terrainFlatHeight;
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }
}