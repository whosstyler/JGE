package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HeartbeatPacket extends Packet {

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_HEARTBEAT;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putLong(timestamp);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        timestamp = buffer.getLong();
    }
}