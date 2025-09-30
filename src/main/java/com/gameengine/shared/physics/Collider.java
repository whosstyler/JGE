package com.gameengine.shared.physics;

import org.joml.Vector3f;

/**
 * Base class for all colliders
 */
public abstract class Collider {

    protected Object userData; // Can store Player, Entity, or any game object
    protected boolean enabled;
    protected boolean isTrigger; // If true, detects collisions but doesn't block movement

    public Collider() {
        this.enabled = true;
        this.isTrigger = false;
    }

    /**
     * Get the AABB for this collider
     */
    public abstract AABB getAABB();

    /**
     * Check if this collider intersects with another
     */
    public abstract boolean intersects(Collider other);

    /**
     * Raycast against this collider
     * Returns distance or -1 if no hit
     */
    public abstract float raycast(Ray ray);

    /**
     * Get the center position of this collider
     */
    public abstract Vector3f getCenter();

    public Object getUserData() { return userData; }
    public void setUserData(Object userData) { this.userData = userData; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isTrigger() { return isTrigger; }
    public void setTrigger(boolean trigger) { this.isTrigger = trigger; }
}