package com.gameengine.shared.network;

import com.gameengine.shared.world.WorldData;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Server sends map data to client after connection
 */
public class MapDataPacket extends Packet {

    private static final Gson gson = new Gson();
    private WorldData worldData;

    public MapDataPacket() {}

    public MapDataPacket(WorldData worldData) {
        this.worldData = worldData;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_MAP_DATA;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        // Serialize WorldData to JSON and send
        String json = gson.toJson(worldData);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(jsonBytes.length);
        buffer.put(jsonBytes);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        // Deserialize WorldData from JSON
        int jsonLength = buffer.getInt();
        byte[] jsonBytes = new byte[jsonLength];
        buffer.get(jsonBytes);
        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        worldData = gson.fromJson(json, WorldData.class);
    }

    public WorldData getWorldData() {
        return worldData;
    }
}
