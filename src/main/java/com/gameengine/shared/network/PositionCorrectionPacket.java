package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Server sends position correction to client when validation fails (rubber-banding)
 */
public class PositionCorrectionPacket extends Packet {

    private int playerId;
    private float x, y, z;
    private int lastValidInputSequence;

    public PositionCorrectionPacket() {
        super();
    }

    public PositionCorrectionPacket(int playerId, float x, float y, float z, int lastValidInputSequence) {
        super();
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lastValidInputSequence = lastValidInputSequence;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_POSITION_CORRECTION;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putInt(playerId);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putInt(lastValidInputSequence);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        playerId = buffer.getInt();
        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();
        lastValidInputSequence = buffer.getInt();
    }

    public int getPlayerId() { return playerId; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public int getLastValidInputSequence() { return lastValidInputSequence; }
}