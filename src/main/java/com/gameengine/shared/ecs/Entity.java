package com.gameengine.shared.ecs;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity - Just an ID with attached components
 */
public class Entity {

    private static int nextId = 1;

    private final int id;
    private final Map<Class<? extends Component>, Component> components;
    private boolean active;

    public Entity() {
        this.id = nextId++;
        this.components = new HashMap<>();
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Add a component to this entity
     */
    public <T extends Component> void addComponent(T component) {
        components.put(component.getClass(), component);
    }

    /**
     * Get a component of specific type
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    /**
     * Check if entity has a component type
     */
    public boolean hasComponent(Class<? extends Component> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * Remove a component from this entity
     */
    public void removeComponent(Class<? extends Component> componentClass) {
        components.remove(componentClass);
    }

    /**
     * Check if entity has all specified component types
     */
    @SafeVarargs
    public final boolean hasComponents(Class<? extends Component>... componentClasses) {
        for (Class<? extends Component> componentClass : componentClasses) {
            if (!hasComponent(componentClass)) {
                return false;
            }
        }
        return true;
    }
}
