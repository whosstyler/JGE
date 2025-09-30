package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Server response telling client their assigned player ID
 */
public class ConnectSuccessPacket extends Packet {

    private int yourPlayerId;
    private String yourPlayerName;

    public ConnectSuccessPacket() {}

    public ConnectSuccessPacket(int yourPlayerId, String yourPlayerName) {
        this.yourPlayerId = yourPlayerId;
        this.yourPlayerName = yourPlayerName;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_CONNECT_SUCCESS;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putInt(yourPlayerId);
        byte[] nameBytes = yourPlayerName.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        yourPlayerId = buffer.getInt();
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        yourPlayerName = new String(nameBytes, StandardCharsets.UTF_8);
    }

    public int getYourPlayerId() {
        return yourPlayerId;
    }

    public String getYourPlayerName() {
        return yourPlayerName;
    }
}