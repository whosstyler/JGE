package com.gameengine.shared.physics;

import org.joml.Vector3f;

/**
 * Box (AABB) collider
 */
public class BoxCollider extends Collider {

    private Vector3f center;
    private Vector3f size;
    private AABB aabb;

    public BoxCollider(Vector3f center, Vector3f size) {
        super();
        this.center = new Vector3f(center);
        this.size = new Vector3f(size);
        updateAABB();
    }

    /**
     * Create a box collider for a player
     */
    public static BoxCollider forPlayer(Vector3f position) {
        return new BoxCollider(position, new Vector3f(0.6f, 1.8f, 0.6f));
    }

    private void updateAABB() {
        Vector3f halfSize = new Vector3f(size).mul(0.5f);
        this.aabb = AABB.fromCenterExtents(center, halfSize);
    }

    /**
     * Update position (for moving objects)
     */
    public void setCenter(Vector3f center) {
        this.center.set(center);
        updateAABB();
    }

    public void setSize(Vector3f size) {
        this.size.set(size);
        updateAABB();
    }

    @Override
    public AABB getAABB() {
        return aabb;
    }

    @Override
    public boolean intersects(Collider other) {
        if (!enabled || !other.isEnabled()) return false;
        return aabb.intersects(other.getAABB());
    }

    @Override
    public float raycast(Ray ray) {
        if (!enabled) return -1;
        return aabb.raycast(ray);
    }

    @Override
    public Vector3f getCenter() {
        return new Vector3f(center);
    }

    public Vector3f getSize() {
        return new Vector3f(size);
    }

    @Override
    public String toString() {
        return String.format("BoxCollider[center=(%.2f,%.2f,%.2f), size=(%.2f,%.2f,%.2f)]",
            center.x, center.y, center.z, size.x, size.y, size.z);
    }
}