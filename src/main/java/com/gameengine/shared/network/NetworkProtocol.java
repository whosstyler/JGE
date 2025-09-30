package com.gameengine.shared.network;

/**
 * Network protocol constants and packet type definitions
 */
public class NetworkProtocol {

    // Protocol version for compatibility checking
    public static final int PROTOCOL_VERSION = 1;

    // Packet Types
    public static final byte PACKET_CONNECT = 0x01;
    public static final byte PACKET_CONNECT_SUCCESS = 0x02;
    public static final byte PACKET_DISCONNECT = 0x03;
    public static final byte PACKET_PLAYER_INPUT = 0x04;
    public static final byte PACKET_STATE_UPDATE = 0x05;
    public static final byte PACKET_PLAYER_JOIN = 0x06;
    public static final byte PACKET_PLAYER_LEAVE = 0x07;
    public static final byte PACKET_HEARTBEAT = 0x08;
    public static final byte PACKET_POSITION_CORRECTION = 0x09;
    public static final byte PACKET_MAP_DATA = 0x0A;
    public static final byte PACKET_TERRAIN_SYNC = 0x0B;

    // Network configuration
    public static final int DEFAULT_PORT = 7777;
    public static final int TICK_RATE = 20; // Server ticks per second
    public static final int TICK_MS = 1000 / TICK_RATE;
    public static final int CLIENT_UPDATE_RATE = 60; // Client render updates per second

    // Validation constants
    public static final float MAX_MOVE_SPEED = 10.0f; // Units per second
    public static final float MAX_POSITION_DELTA = MAX_MOVE_SPEED / TICK_RATE * 2; // Allow 2x for lag

    // Packet size limits
    public static final int MAX_PACKET_SIZE = 8192;
    public static final int HEADER_SIZE = 5; // 1 byte type + 4 bytes sequence

    private NetworkProtocol() {}
}