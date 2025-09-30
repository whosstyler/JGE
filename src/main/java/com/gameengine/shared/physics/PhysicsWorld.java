package com.gameengine.shared.physics;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Physics world that manages all colliders and performs raycasts/collision queries
 */
public class PhysicsWorld {

    private static final Logger logger = LoggerFactory.getLogger(PhysicsWorld.class);

    private final List<Collider> colliders;
    private final List<Collider> staticColliders; // Non-moving colliders (walls, floors, etc.)

    public PhysicsWorld() {
        this.colliders = new CopyOnWriteArrayList<>();
        this.staticColliders = new CopyOnWriteArrayList<>();
    }

    /**
     * Add a collider to the physics world
     */
    public void addCollider(Collider collider) {
        colliders.add(collider);
        logger.debug("Added collider: {}", collider);
    }

    /**
     * Add a static collider (for level geometry)
     */
    public void addStaticCollider(Collider collider) {
        staticColliders.add(collider);
        logger.debug("Added static collider: {}", collider);
    }

    /**
     * Remove a collider from the physics world
     */
    public void removeCollider(Collider collider) {
        colliders.remove(collider);
        staticColliders.remove(collider);
    }

    /**
     * Perform a raycast and return the closest hit
     */
    public RaycastHit raycast(Ray ray) {
        return raycast(ray, null);
    }

    /**
     * Perform a raycast, ignoring a specific collider (useful for player raycasts)
     */
    public RaycastHit raycast(Ray ray, Collider ignore) {
        RaycastHit closestHit = null;
        float closestDistance = ray.getMaxDistance();

        // Check all colliders
        for (Collider collider : colliders) {
            if (collider == ignore || !collider.isEnabled()) continue;

            float distance = collider.raycast(ray);
            if (distance >= 0 && distance < closestDistance) {
                closestDistance = distance;
                Vector3f hitPoint = ray.getPoint(distance);
                Vector3f normal = calculateNormal(collider, hitPoint);
                closestHit = new RaycastHit(collider, hitPoint, normal, distance, collider.getUserData());
            }
        }

        // Check static colliders
        for (Collider collider : staticColliders) {
            if (collider == ignore || !collider.isEnabled()) continue;

            float distance = collider.raycast(ray);
            if (distance >= 0 && distance < closestDistance) {
                closestDistance = distance;
                Vector3f hitPoint = ray.getPoint(distance);
                Vector3f normal = calculateNormal(collider, hitPoint);
                closestHit = new RaycastHit(collider, hitPoint, normal, distance, collider.getUserData());
            }
        }

        return closestHit;
    }

    /**
     * Perform a raycast and return all hits
     */
    public List<RaycastHit> raycastAll(Ray ray) {
        List<RaycastHit> hits = new ArrayList<>();

        for (Collider collider : colliders) {
            if (!collider.isEnabled()) continue;

            float distance = collider.raycast(ray);
            if (distance >= 0 && distance <= ray.getMaxDistance()) {
                Vector3f hitPoint = ray.getPoint(distance);
                Vector3f normal = calculateNormal(collider, hitPoint);
                hits.add(new RaycastHit(collider, hitPoint, normal, distance, collider.getUserData()));
            }
        }

        for (Collider collider : staticColliders) {
            if (!collider.isEnabled()) continue;

            float distance = collider.raycast(ray);
            if (distance >= 0 && distance <= ray.getMaxDistance()) {
                Vector3f hitPoint = ray.getPoint(distance);
                Vector3f normal = calculateNormal(collider, hitPoint);
                hits.add(new RaycastHit(collider, hitPoint, normal, distance, collider.getUserData()));
            }
        }

        // Sort by distance
        hits.sort((a, b) -> Float.compare(a.getDistance(), b.getDistance()));

        return hits;
    }

    /**
     * Check if a collider overlaps with any other colliders
     */
    public List<Collider> overlapCollider(Collider collider) {
        List<Collider> overlapping = new ArrayList<>();

        for (Collider other : colliders) {
            if (other == collider || !other.isEnabled()) continue;
            if (collider.intersects(other)) {
                overlapping.add(other);
            }
        }

        for (Collider other : staticColliders) {
            if (other == collider || !other.isEnabled()) continue;
            if (collider.intersects(other)) {
                overlapping.add(other);
            }
        }

        return overlapping;
    }

    /**
     * Check if a box overlaps with anything in the world
     */
    public boolean checkBox(Vector3f center, Vector3f halfExtents) {
        return checkBox(center, halfExtents, null);
    }

    /**
     * Check if a box overlaps with anything in the world, excluding a specific collider
     */
    public boolean checkBox(Vector3f center, Vector3f halfExtents, Collider exclude) {
        AABB box = AABB.fromCenterExtents(center, halfExtents);

        for (Collider collider : colliders) {
            if (collider == exclude) continue; // Skip the excluded collider
            if (!collider.isEnabled() || collider.isTrigger()) continue;
            if (box.intersects(collider.getAABB())) {
                return true;
            }
        }

        for (Collider collider : staticColliders) {
            if (!collider.isEnabled() || collider.isTrigger()) continue;
            if (box.intersects(collider.getAABB())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sweep a collider along a direction and find the first collision
     * Returns the fraction of movement possible (0-1)
     */
    public float sweepCollider(Collider collider, Vector3f direction, float maxDistance) {
        AABB aabb = collider.getAABB();
        float minTime = 1.0f;

        for (Collider other : colliders) {
            if (other == collider || !other.isEnabled() || other.isTrigger()) continue;

            float time = aabb.sweepAABB(other.getAABB(), new Vector3f(direction).mul(maxDistance));
            if (time >= 0 && time < minTime) {
                minTime = time;
            }
        }

        for (Collider other : staticColliders) {
            if (!other.isEnabled() || other.isTrigger()) continue;

            float time = aabb.sweepAABB(other.getAABB(), new Vector3f(direction).mul(maxDistance));
            if (time >= 0 && time < minTime) {
                minTime = time;
            }
        }

        return minTime;
    }

    /**
     * Calculate surface normal at hit point (simplified for AABB)
     */
    private Vector3f calculateNormal(Collider collider, Vector3f hitPoint) {
        AABB aabb = collider.getAABB();
        Vector3f center = aabb.getCenter();
        Vector3f toHit = new Vector3f(hitPoint).sub(center);

        // Find the axis with the largest component
        float absX = Math.abs(toHit.x);
        float absY = Math.abs(toHit.y);
        float absZ = Math.abs(toHit.z);

        if (absX > absY && absX > absZ) {
            return new Vector3f(Math.signum(toHit.x), 0, 0);
        } else if (absY > absZ) {
            return new Vector3f(0, Math.signum(toHit.y), 0);
        } else {
            return new Vector3f(0, 0, Math.signum(toHit.z));
        }
    }

    /**
     * Get all colliders
     */
    public List<Collider> getColliders() {
        List<Collider> all = new ArrayList<>(colliders);
        all.addAll(staticColliders);
        return all;
    }

    /**
     * Clear all colliders
     */
    public void clear() {
        colliders.clear();
        staticColliders.clear();
    }
}