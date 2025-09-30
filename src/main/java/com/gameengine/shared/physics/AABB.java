package com.gameengine.shared.physics;

import org.joml.Vector3f;

/**
 * Axis-Aligned Bounding Box for collision detection
 */
public class AABB {

    private final Vector3f min;
    private final Vector3f max;
    private final Vector3f center;
    private final Vector3f extents;

    /**
     * Create AABB from min/max corners
     */
    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
        this.center = new Vector3f();
        this.extents = new Vector3f();
        calculateCenterAndExtents();
    }

    /**
     * Create AABB from center and half-extents
     */
    public static AABB fromCenterExtents(Vector3f center, Vector3f halfExtents) {
        Vector3f min = new Vector3f(center).sub(halfExtents);
        Vector3f max = new Vector3f(center).add(halfExtents);
        return new AABB(min, max);
    }

    /**
     * Create AABB for a player (typical character controller)
     */
    public static AABB forPlayer(Vector3f position, float width, float height) {
        float halfWidth = width / 2.0f;
        Vector3f min = new Vector3f(
            position.x - halfWidth,
            position.y - height / 2.0f,
            position.z - halfWidth
        );
        Vector3f max = new Vector3f(
            position.x + halfWidth,
            position.y + height / 2.0f,
            position.z + halfWidth
        );
        return new AABB(min, max);
    }

    private void calculateCenterAndExtents() {
        center.set(max).add(min).mul(0.5f);
        extents.set(max).sub(min).mul(0.5f);
    }

    /**
     * Check if this AABB intersects with another AABB
     */
    public boolean intersects(AABB other) {
        return (min.x <= other.max.x && max.x >= other.min.x) &&
               (min.y <= other.max.y && max.y >= other.min.y) &&
               (min.z <= other.max.z && max.z >= other.min.z);
    }

    /**
     * Check if a point is inside this AABB
     */
    public boolean contains(Vector3f point) {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z;
    }

    /**
     * Get the closest point on this AABB to a given point
     */
    public Vector3f closestPoint(Vector3f point) {
        return new Vector3f(
            Math.max(min.x, Math.min(point.x, max.x)),
            Math.max(min.y, Math.min(point.y, max.y)),
            Math.max(min.z, Math.min(point.z, max.z))
        );
    }

    /**
     * Expand this AABB by a given amount
     */
    public AABB expand(float amount) {
        Vector3f newMin = new Vector3f(min).sub(amount, amount, amount);
        Vector3f newMax = new Vector3f(max).add(amount, amount, amount);
        return new AABB(newMin, newMax);
    }

    /**
     * Translate this AABB by a vector
     */
    public AABB translate(Vector3f offset) {
        Vector3f newMin = new Vector3f(min).add(offset);
        Vector3f newMax = new Vector3f(max).add(offset);
        return new AABB(newMin, newMax);
    }

    /**
     * Check intersection with a ray and return the distance
     * Returns -1 if no intersection
     */
    public float raycast(Ray ray) {
        Vector3f invDir = new Vector3f(
            1.0f / ray.getDirection().x,
            1.0f / ray.getDirection().y,
            1.0f / ray.getDirection().z
        );

        float t1 = (min.x - ray.getOrigin().x) * invDir.x;
        float t2 = (max.x - ray.getOrigin().x) * invDir.x;
        float t3 = (min.y - ray.getOrigin().y) * invDir.y;
        float t4 = (max.y - ray.getOrigin().y) * invDir.y;
        float t5 = (min.z - ray.getOrigin().z) * invDir.z;
        float t6 = (max.z - ray.getOrigin().z) * invDir.z;

        float tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        // Ray is intersecting AABB, but the whole AABB is behind us
        if (tmax < 0) {
            return -1;
        }

        // Ray doesn't intersect AABB
        if (tmin > tmax) {
            return -1;
        }

        // If tmin is negative, we are inside the box
        return tmin < 0 ? tmax : tmin;
    }

    /**
     * Sweep this AABB along a direction and check for collision with another AABB
     * Returns the time of impact (0-1) or -1 if no collision
     */
    public float sweepAABB(AABB other, Vector3f direction) {
        // Minkowski difference
        AABB minkowski = new AABB(
            new Vector3f(other.min).sub(this.max),
            new Vector3f(other.max).sub(this.min)
        );

        // Raycast against the Minkowski difference
        Ray ray = new Ray(new Vector3f(), direction);
        return minkowski.raycast(ray);
    }

    public Vector3f getMin() { return new Vector3f(min); }
    public Vector3f getMax() { return new Vector3f(max); }
    public Vector3f getCenter() { return new Vector3f(center); }
    public Vector3f getExtents() { return new Vector3f(extents); }

    @Override
    public String toString() {
        return String.format("AABB[min=(%.2f,%.2f,%.2f), max=(%.2f,%.2f,%.2f)]",
            min.x, min.y, min.z, max.x, max.y, max.z);
    }
}