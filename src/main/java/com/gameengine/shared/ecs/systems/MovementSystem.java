package com.gameengine.shared.ecs.systems;

import com.gameengine.shared.ecs.Entity;
import com.gameengine.shared.ecs.System;
import com.gameengine.shared.ecs.components.TransformComponent;
import com.gameengine.shared.ecs.components.VelocityComponent;

import java.util.List;

/**
 * Movement system - Applies velocity to transform
 */
public class MovementSystem extends System {

    @Override
    public void update(float deltaTime) {
        List<Entity> entities = getEntitiesWithComponents(TransformComponent.class, VelocityComponent.class);

        for (Entity entity : entities) {
            TransformComponent transform = entity.getComponent(TransformComponent.class);
            VelocityComponent velocity = entity.getComponent(VelocityComponent.class);

            // Apply linear velocity
            transform.position.x += velocity.linear.x * deltaTime;
            transform.position.y += velocity.linear.y * deltaTime;
            transform.position.z += velocity.linear.z * deltaTime;

            // Apply angular velocity (rotation)
            transform.rotation.x += velocity.angular.x * deltaTime;
            transform.rotation.y += velocity.angular.y * deltaTime;
            transform.rotation.z += velocity.angular.z * deltaTime;
        }
    }
}
