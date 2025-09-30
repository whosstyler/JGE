package com.gameengine.shared.physics;

import org.joml.Vector3f;

/**
 * Ray for raycasting (origin + direction)
 */
public class Ray {

    private final Vector3f origin;
    private final Vector3f direction;
    private float maxDistance;

    public Ray(Vector3f origin, Vector3f direction) {
        this.origin = new Vector3f(origin);
        this.direction = new Vector3f(direction).normalize();
        this.maxDistance = Float.MAX_VALUE;
    }

    public Ray(Vector3f origin, Vector3f direction, float maxDistance) {
        this.origin = new Vector3f(origin);
        this.direction = new Vector3f(direction).normalize();
        this.maxDistance = maxDistance;
    }

    /**
     * Create ray from player's view (for hitscan weapons, interaction, etc.)
     */
    public static Ray fromView(Vector3f position, float yaw, float pitch, float maxDistance) {
        // Calculate direction from yaw and pitch
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        Vector3f direction = new Vector3f(
            -(float) (Math.cos(pitchRad) * Math.sin(yawRad)),
            -(float) Math.sin(pitchRad),
            -(float) (Math.cos(pitchRad) * Math.cos(yawRad))
        );

        return new Ray(position, direction, maxDistance);
    }

    /**
     * Get point along the ray at distance t
     */
    public Vector3f getPoint(float t) {
        return new Vector3f(origin).add(new Vector3f(direction).mul(t));
    }

    public Vector3f getOrigin() { return new Vector3f(origin); }
    public Vector3f getDirection() { return new Vector3f(direction); }
    public float getMaxDistance() { return maxDistance; }

    @Override
    public String toString() {
        return String.format("Ray[origin=(%.2f,%.2f,%.2f), dir=(%.2f,%.2f,%.2f)]",
            origin.x, origin.y, origin.z, direction.x, direction.y, direction.z);
    }
}