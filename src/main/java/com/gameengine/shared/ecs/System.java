package com.gameengine.shared.ecs;

import java.util.List;

/**
 * System - Operates on entities with specific components
 * Each system processes entities that have required components
 */
public abstract class System {

    protected ECSWorld world;

    public void setWorld(ECSWorld world) {
        this.world = world;
    }

    /**
     * Update this system with given delta time
     */
    public abstract void update(float deltaTime);

    /**
     * Get entities that have all specified component types
     */
    @SafeVarargs
    protected final List<Entity> getEntitiesWithComponents(Class<? extends Component>... componentClasses) {
        return world.getEntitiesWithComponents(componentClasses);
    }
}
