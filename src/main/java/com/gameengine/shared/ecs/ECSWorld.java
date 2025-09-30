package com.gameengine.shared.ecs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ECS World - Manages all entities and systems
 */
public class ECSWorld {

    private static final Logger logger = LoggerFactory.getLogger(ECSWorld.class);

    private final List<Entity> entities;
    private final List<System> systems;
    private final List<Entity> entitiesToRemove;

    public ECSWorld() {
        this.entities = new CopyOnWriteArrayList<>();
        this.systems = new ArrayList<>();
        this.entitiesToRemove = new ArrayList<>();
    }

    /**
     * Create a new entity
     */
    public Entity createEntity() {
        Entity entity = new Entity();
        entities.add(entity);
        logger.debug("Created entity {}", entity.getId());
        return entity;
    }

    /**
     * Remove an entity
     */
    public void removeEntity(Entity entity) {
        entity.setActive(false);
        entitiesToRemove.add(entity);
    }

    /**
     * Add a system to the world
     */
    public void addSystem(System system) {
        system.setWorld(this);
        systems.add(system);
        logger.info("Added system: {}", system.getClass().getSimpleName());
    }

    /**
     * Update all systems
     */
    public void update(float deltaTime) {
        // Update all systems
        for (System system : systems) {
            system.update(deltaTime);
        }

        // Remove inactive entities
        if (!entitiesToRemove.isEmpty()) {
            entities.removeAll(entitiesToRemove);
            logger.debug("Removed {} entities", entitiesToRemove.size());
            entitiesToRemove.clear();
        }
    }

    /**
     * Get all entities
     */
    public List<Entity> getAllEntities() {
        return entities;
    }

    /**
     * Get entities that have all specified component types
     */
    @SafeVarargs
    public final List<Entity> getEntitiesWithComponents(Class<? extends Component>... componentClasses) {
        return entities.stream()
                .filter(Entity::isActive)
                .filter(entity -> entity.hasComponents(componentClasses))
                .collect(Collectors.toList());
    }

    /**
     * Get entity by ID
     */
    public Entity getEntity(int id) {
        return entities.stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get entity count
     */
    public int getEntityCount() {
        return entities.size();
    }
}
