package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Sends terrain generation parameters from server to client
 * Client will generate identical terrain using the same seed
 */
public class TerrainSyncPacket extends Packet {

    private long seed;
    private int width;
    private int depth;
    private boolean isFlat;
    private float flatHeight;

    public TerrainSyncPacket() {
    }

    public TerrainSyncPacket(long seed, int width, int depth, boolean isFlat, float flatHeight) {
        this.seed = seed;
        this.width = width;
        this.depth = depth;
        this.isFlat = isFlat;
        this.flatHeight = flatHeight;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_TERRAIN_SYNC;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putLong(seed);
        buffer.putInt(width);
        buffer.putInt(depth);
        buffer.put((byte) (isFlat ? 1 : 0));
        buffer.putFloat(flatHeight);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        seed = buffer.getLong();
        width = buffer.getInt();
        depth = buffer.getInt();
        isFlat = buffer.get() == 1;
        flatHeight = buffer.getFloat();
    }

    public long getSeed() {
        return seed;
    }

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isFlat() {
        return isFlat;
    }

    public float getFlatHeight() {
        return flatHeight;
    }
}
