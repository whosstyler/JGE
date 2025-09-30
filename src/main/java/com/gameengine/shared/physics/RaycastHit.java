package com.gameengine.shared.physics;

import org.joml.Vector3f;

/**
 * Result of a raycast operation
 */
public class RaycastHit {

    private final Collider collider;
    private final Vector3f point;
    private final Vector3f normal;
    private final float distance;
    private final Object userData;

    public RaycastHit(Collider collider, Vector3f point, Vector3f normal, float distance, Object userData) {
        this.collider = collider;
        this.point = new Vector3f(point);
        this.normal = new Vector3f(normal);
        this.distance = distance;
        this.userData = userData;
    }

    public Collider getCollider() { return collider; }
    public Vector3f getPoint() { return new Vector3f(point); }
    public Vector3f getNormal() { return new Vector3f(normal); }
    public float getDistance() { return distance; }
    public Object getUserData() { return userData; }

    @Override
    public String toString() {
        return String.format("RaycastHit[distance=%.2f, point=(%.2f,%.2f,%.2f), collider=%s]",
            distance, point.x, point.y, point.z, collider);
    }
}