package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DisconnectPacket extends Packet {

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_DISCONNECT;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        // No additional data
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        // No additional data
    }
}