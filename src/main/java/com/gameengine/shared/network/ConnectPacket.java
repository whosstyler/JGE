package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ConnectPacket extends Packet {

    private int protocolVersion;
    private String playerName;

    public ConnectPacket() {
        this.protocolVersion = NetworkProtocol.PROTOCOL_VERSION;
    }

    public ConnectPacket(String playerName) {
        this();
        this.playerName = playerName;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_CONNECT;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putInt(protocolVersion);
        byte[] nameBytes = playerName.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        protocolVersion = buffer.getInt();
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        playerName = new String(nameBytes, StandardCharsets.UTF_8);
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getPlayerName() {
        return playerName;
    }
}