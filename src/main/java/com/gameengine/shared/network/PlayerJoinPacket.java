package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PlayerJoinPacket extends Packet {

    private int playerId;
    private String playerName;

    public PlayerJoinPacket() {}

    public PlayerJoinPacket(int playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_PLAYER_JOIN;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.putInt(playerId);
        byte[] nameBytes = playerName.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        playerId = buffer.getInt();
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        playerName = new String(nameBytes, StandardCharsets.UTF_8);
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }
}