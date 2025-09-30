package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class StateUpdatePacket extends Packet {

    private int serverTick;
    private List<PlayerState> playerStates;

    public StateUpdatePacket() {
        this.playerStates = new ArrayList<>();
    }

    public StateUpdatePacket(int serverTick) {
        this();
        this.serverTick = serverTick;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_STATE_UPDATE;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putInt(serverTick);
        buffer.putInt(playerStates.size());

        for (PlayerState state : playerStates) {
            buffer.putInt(state.playerId);
            buffer.putFloat(state.x);
            buffer.putFloat(state.y);
            buffer.putFloat(state.z);
            buffer.putFloat(state.yaw);
            buffer.putFloat(state.pitch);
            buffer.putInt(state.lastProcessedInput);
        }
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        serverTick = buffer.getInt();
        int count = buffer.getInt();

        playerStates.clear();
        for (int i = 0; i < count; i++) {
            PlayerState state = new PlayerState();
            state.playerId = buffer.getInt();
            state.x = buffer.getFloat();
            state.y = buffer.getFloat();
            state.z = buffer.getFloat();
            state.yaw = buffer.getFloat();
            state.pitch = buffer.getFloat();
            state.lastProcessedInput = buffer.getInt();
            playerStates.add(state);
        }
    }

    public void addPlayerState(int playerId, float x, float y, float z, float yaw, float pitch, int lastProcessedInput) {
        PlayerState state = new PlayerState();
        state.playerId = playerId;
        state.x = x;
        state.y = y;
        state.z = z;
        state.yaw = yaw;
        state.pitch = pitch;
        state.lastProcessedInput = lastProcessedInput;
        playerStates.add(state);
    }

    public int getServerTick() {
        return serverTick;
    }

    public List<PlayerState> getPlayerStates() {
        return playerStates;
    }

    public static class PlayerState {
        public int playerId;
        public float x, y, z;
        public float yaw, pitch;
        public int lastProcessedInput;
    }
}