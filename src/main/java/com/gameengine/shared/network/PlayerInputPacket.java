package com.gameengine.shared.network;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerInputPacket extends Packet {

    // Button states (bit flags)
    private byte buttonStates;  // W, A, S, D, Space, Shift, etc.
    private float yaw;
    private float pitch;
    private int inputSequence;
    private long clientTimestamp; // When client generated this input (for lag compensation)

    // Button bit flags
    public static final byte BUTTON_FORWARD = 1 << 0;  // W
    public static final byte BUTTON_BACK = 1 << 1;     // S
    public static final byte BUTTON_LEFT = 1 << 2;     // A
    public static final byte BUTTON_RIGHT = 1 << 3;    // D
    public static final byte BUTTON_JUMP = 1 << 4;     // Space
    public static final byte BUTTON_CROUCH = 1 << 5;   // Shift

    public PlayerInputPacket() {}

    public PlayerInputPacket(byte buttonStates, float yaw, float pitch, int inputSequence, long clientTimestamp) {
        this.buttonStates = buttonStates;
        this.yaw = yaw;
        this.pitch = pitch;
        this.inputSequence = inputSequence;
        this.clientTimestamp = clientTimestamp;
    }

    @Override
    public byte getType() {
        return NetworkProtocol.PACKET_PLAYER_INPUT;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        buffer.put(buttonStates);
        buffer.putFloat(yaw);
        buffer.putFloat(pitch);
        buffer.putInt(inputSequence);
        buffer.putLong(clientTimestamp);
    }

    @Override
    public void read(ByteBuffer buffer) throws IOException {
        buttonStates = buffer.get();
        yaw = buffer.getFloat();
        pitch = buffer.getFloat();
        inputSequence = buffer.getInt();
        clientTimestamp = buffer.getLong();
    }

    public byte getButtonStates() { return buttonStates; }
    public boolean isButtonPressed(byte button) { return (buttonStates & button) != 0; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public int getInputSequence() { return inputSequence; }
    public long getClientTimestamp() { return clientTimestamp; }
}