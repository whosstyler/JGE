package com.gameengine.shared.ecs.systems;

import com.gameengine.client.renderer.Renderer;
import com.gameengine.shared.ecs.Entity;
import com.gameengine.shared.ecs.System;
import com.gameengine.shared.ecs.components.RenderableComponent;
import com.gameengine.shared.ecs.components.TransformComponent;

import java.util.List;

/**
 * Render system - Renders all entities with Transform and Renderable components
 */
public class RenderSystem extends System {

    private final Renderer renderer;

    public RenderSystem(Renderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void update(float deltaTime) {
        // Not used - rendering happens in render() method
    }

    /**
     * Render all visible entities
     */
    public void render() {
        List<Entity> entities = getEntitiesWithComponents(TransformComponent.class, RenderableComponent.class);

        for (Entity entity : entities) {
            TransformComponent transform = entity.getComponent(TransformComponent.class);
            RenderableComponent renderable = entity.getComponent(RenderableComponent.class);

            if (renderable.visible && renderable.mesh != null) {
                // Convert rotation from radians to degrees for renderer
                float pitch = (float) Math.toDegrees(transform.rotation.x);
                float yaw = (float) Math.toDegrees(transform.rotation.y);

                renderer.render(
                    renderable.mesh,
                    transform.position.x,
                    transform.position.y,
                    transform.position.z,
                    pitch,
                    yaw
                );
            }
        }
    }
}
