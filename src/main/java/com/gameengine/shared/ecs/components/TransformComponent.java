package com.gameengine.shared.ecs.components;

import com.gameengine.shared.ecs.Component;
import org.joml.Vector3f;

/**
 * Transform component - Position, rotation, scale
 */
public class TransformComponent implements Component {

    public Vector3f position;
    public Vector3f rotation; // Euler angles (pitch, yaw, roll)
    public Vector3f scale;

    public TransformComponent() {
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
    }

    public TransformComponent(Vector3f position) {
        this();
        this.position = new Vector3f(position);
    }

    public TransformComponent(float x, float y, float z) {
        this();
        this.position = new Vector3f(x, y, z);
    }
}
