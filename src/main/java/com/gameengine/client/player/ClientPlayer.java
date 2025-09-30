package com.gameengine.client.player;

import org.joml.Vector3f;

/**
 * Client-side player representation with prediction
 */
public class ClientPlayer {

    private final int id;
    private final String name;
    private final Vector3f position;          // Server authoritative position
    private final Vector3f renderedPosition;  // Smoothly interpolated display position
    private final Vector3f predictedPosition;
    private final Vector3f velocity;
    private float yaw;
    private float pitch;
    private float renderedYaw;   // Interpolated yaw for smooth rotation
    private float renderedPitch; // Interpolated pitch for smooth rotation
    private int lastReceivedInputSequence;

    public ClientPlayer(int id, String name) {
        this.id = id;
        this.name = name;
        this.position = new Vector3f();
        this.renderedPosition = new Vector3f();
        this.predictedPosition = new Vector3f();
        this.velocity = new Vector3f();
        this.lastReceivedInputSequence = 0;
        this.renderedYaw = 0;
        this.renderedPitch = 0;
    }

    public void updateServerState(float x, float y, float z, float yaw, float pitch, int lastProcessedInput) {
        // Update server authoritative state
        position.set(x, y, z);

        // Initialize rendered position on first update
        if (renderedPosition.lengthSquared() == 0) {
            renderedPosition.set(position);
            renderedYaw = yaw;
            renderedPitch = pitch;
        }

        // Debug: Log rotation updates (only when it changes significantly)
        if (Math.abs(this.yaw - yaw) > 1.0f || Math.abs(this.pitch - pitch) > 1.0f) {
            System.out.println(String.format("CLIENT received rotation update: yaw=%.1f -> %.1f, pitch=%.1f -> %.1f",
                this.yaw, yaw, this.pitch, pitch));
        }

        this.yaw = yaw;
        this.pitch = pitch;
        this.lastReceivedInputSequence = lastProcessedInput;
    }

    /**
     * Interpolate rendered position/rotation toward server state for smooth display
     * Call this every frame with deltaTime
     */
    public void interpolate(float deltaTime) {
        // Interpolation speed: higher = snappier, lower = smoother
        // 10.0 = catch up in ~100ms, 20.0 = ~50ms
        float positionSpeed = 10.0f;
        float rotationSpeed = 15.0f;

        // Lerp rendered position toward server position
        float alpha = Math.min(1.0f, positionSpeed * deltaTime);
        renderedPosition.lerp(position, alpha);

        // Lerp rotation (handle wraparound for yaw)
        renderedYaw = lerpAngle(renderedYaw, yaw, rotationSpeed * deltaTime);
        renderedPitch = lerpAngle(renderedPitch, pitch, rotationSpeed * deltaTime);
    }

    /**
     * Lerp between two angles, handling wraparound (e.g., -179 to 179)
     */
    private float lerpAngle(float from, float to, float t) {
        float delta = to - from;

        // Normalize to [-180, 180]
        while (delta > 180) delta -= 360;
        while (delta < -180) delta += 360;

        return from + delta * Math.min(1.0f, t);
    }

    public void predict(float moveX, float moveY, float moveZ, float deltaTime) {
        predictedPosition.set(position);
        predictedPosition.add(moveX * deltaTime, moveY * deltaTime, moveZ * deltaTime);
    }

    /**
     * Force position update from server correction (rubber-banding)
     */
    public void forcePosition(float x, float y, float z) {
        position.set(x, y, z);
        renderedPosition.set(x, y, z);
        predictedPosition.set(x, y, z);
        velocity.set(0, 0, 0);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Vector3f getPosition() { return position; }
    public Vector3f getRenderedPosition() { return renderedPosition; }
    public Vector3f getPredictedPosition() { return predictedPosition; }
    public Vector3f getVelocity() { return velocity; }
    public float getYaw() { return yaw; }
    public float getRenderedYaw() { return renderedYaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public float getRenderedPitch() { return renderedPitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public int getLastReceivedInputSequence() { return lastReceivedInputSequence; }
}