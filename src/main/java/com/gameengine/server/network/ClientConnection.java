package com.gameengine.server.network;

import com.gameengine.shared.network.Packet;
import com.gameengine.server.entity.Player;
import com.gameengine.server.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a client connection on the server side
 */
public class ClientConnection implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientConnection.class);

    private final Socket socket;
    private final GameServer server;
    private final BlockingQueue<Packet> outgoingPackets;
    private DataInputStream input;
    private DataOutputStream output;
    private Player player;
    private volatile boolean running;
    private int sequenceNumber;

    public ClientConnection(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        this.outgoingPackets = new LinkedBlockingQueue<>();
        this.running = true;
        this.sequenceNumber = 0;
    }

    @Override
    public void run() {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            // Start sender thread
            Thread senderThread = new Thread(this::sendLoop, "ClientSender-" + socket.getInetAddress());
            senderThread.start();

            // Receiver loop
            receiveLoop();

            senderThread.interrupt();
            senderThread.join(1000);

        } catch (Exception e) {
            logger.error("Client connection error", e);
        } finally {
            disconnect();
        }
    }

    private void receiveLoop() {
        while (running && !socket.isClosed()) {
            try {
                // Read packet size
                int packetSize = input.readInt();
                if (packetSize <= 0 || packetSize > 8192) {
                    logger.warn("Invalid packet size: {}", packetSize);
                    break;
                }

                // Read packet data
                byte[] packetData = new byte[packetSize];
                input.readFully(packetData);

                // Deserialize and handle packet
                Packet packet = Packet.deserialize(packetData);
                if (packet != null) {
                    server.handlePacket(this, packet);
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

    public void sendPacket(Packet packet) {
        if (running && !socket.isClosed()) {
            outgoingPackets.offer(packet);
        }
    }

    public void disconnect() {
        if (!running) return;

        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            logger.error("Error closing socket", e);
        }

        if (player != null) {
            server.removePlayer(player);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public boolean isRunning() {
        return running;
    }
}