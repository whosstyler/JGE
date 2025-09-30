package com.gameengine.shared.network;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * Base packet class for network communication
 */
public abstract class Packet {

    protected int sequenceNumber;
    protected long timestamp;

    public Packet() {
        this.timestamp = System.currentTimeMillis();
    }

    public abstract byte getType();

    public abstract void write(ByteBuffer buffer) throws IOException;

    public abstract void read(ByteBuffer buffer) throws IOException;

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Serialize packet to byte array
     */
    public byte[] serialize() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(NetworkProtocol.MAX_PACKET_SIZE);
        buffer.put(getType());
        buffer.putInt(sequenceNumber);
        write(buffer);

        byte[] data = new byte[buffer.position()];
        buffer.flip();
        buffer.get(data);
        return data;
    }

    /**
     * Deserialize packet from byte array
     */
    public static Packet deserialize(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte type = buffer.get();
        int sequence = buffer.getInt();

        Packet packet = createPacket(type);
        if (packet != null) {
            packet.setSequenceNumber(sequence);
            packet.read(buffer);
        }
        return packet;
    }

    /**
     * Factory method to create packet by type
     */
    private static Packet createPacket(byte type) {
        return switch (type) {
            case NetworkProtocol.PACKET_CONNECT -> new ConnectPacket();
            case NetworkProtocol.PACKET_CONNECT_SUCCESS -> new ConnectSuccessPacket();
            case NetworkProtocol.PACKET_DISCONNECT -> new DisconnectPacket();
            case NetworkProtocol.PACKET_PLAYER_INPUT -> new PlayerInputPacket();
            case NetworkProtocol.PACKET_STATE_UPDATE -> new StateUpdatePacket();
            case NetworkProtocol.PACKET_PLAYER_JOIN -> new PlayerJoinPacket();
            case NetworkProtocol.PACKET_PLAYER_LEAVE -> new PlayerLeavePacket();
            case NetworkProtocol.PACKET_HEARTBEAT -> new HeartbeatPacket();
            case NetworkProtocol.PACKET_POSITION_CORRECTION -> new PositionCorrectionPacket();
            case NetworkProtocol.PACKET_MAP_DATA -> new MapDataPacket();
            case NetworkProtocol.PACKET_TERRAIN_SYNC -> new TerrainSyncPacket();
            default -> null;
        };
    }
}