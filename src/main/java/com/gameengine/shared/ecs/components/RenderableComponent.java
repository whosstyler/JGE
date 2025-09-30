package com.gameengine.shared.ecs.components;

import com.gameengine.client.renderer.Mesh;
import com.gameengine.shared.ecs.Component;
import org.joml.Vector3f;

/**
 * Renderable component - Holds mesh and visual properties
 */
public class RenderableComponent implements Component {

    public Mesh mesh;
    public Vector3f color;
    public boolean visible;

    public RenderableComponent(Mesh mesh) {
        this.mesh = mesh;
        this.color = new Vector3f(1, 1, 1);
        this.visible = true;
    }

    public RenderableComponent(Mesh mesh, Vector3f color) {
        this.mesh = mesh;
        this.color = new Vector3f(color);
        this.visible = true;
    }

    public RenderableComponent(Mesh mesh, float r, float g, float b) {
        this.mesh = mesh;
        this.color = new Vector3f(r, g, b);
        this.visible = true;
    }
}
