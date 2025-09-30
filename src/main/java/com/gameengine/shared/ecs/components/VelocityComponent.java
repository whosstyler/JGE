package com.gameengine.shared.ecs.components;

import com.gameengine.shared.ecs.Component;
import org.joml.Vector3f;

/**
 * Velocity component - Linear and angular velocity shared
 */
public class VelocityComponent implements Component {

    public Vector3f linear;  // Linear velocity (m/s)
    public Vector3f angular; // Angular velocity (rad/s)

    public VelocityComponent() {
        this.linear = new Vector3f(0, 0, 0);
        this.angular = new Vector3f(0, 0, 0);
    }

    public VelocityComponent(Vector3f linear) {
        this.linear = new Vector3f(linear);
        this.angular = new Vector3f(0, 0, 0);
    }

    public VelocityComponent(float vx, float vy, float vz) {
        this.linear = new Vector3f(vx, vy, vz);
        this.angular = new Vector3f(0, 0, 0);
    }
}
