package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerLeavePacket extends Packet {

    private int playerId;

    public PlayerLeavePacket() {}

    public PlayerLeavePacket(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_PLAYER_LEAVE;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putInt(playerId);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        playerId = buffer.getInt();
    }

    public int getPlayerId() {
        return playerId;
    }
}