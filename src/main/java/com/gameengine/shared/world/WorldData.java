package com.gameengine.shared.world;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all data for a game map/world
 */
public class WorldData {

    private String name;
    private List<SpawnPoint> spawnPoints;
    private List<StaticGeometryData> staticGeometry;
    private List<EntityData> entities;

    public WorldData() {
        this.spawnPoints = new ArrayList<>();
        this.staticGeometry = new ArrayList<>();
        this.entities = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public void setSpawnPoints(List<SpawnPoint> spawnPoints) {
        this.spawnPoints = spawnPoints;
    }

    public List<StaticGeometryData> getStaticGeometry() {
        return staticGeometry;
    }

    public void setStaticGeometry(List<StaticGeometryData> staticGeometry) {
        this.staticGeometry = staticGeometry;
    }

    public List<EntityData> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityData> entities) {
        this.entities = entities;
    }

    /**
     * Spawn point definition
     */
    public static class SpawnPoint {
        private float x, y, z;

        public SpawnPoint() {}

        public SpawnPoint(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vector3f getPosition() {
            return new Vector3f(x, y, z);
        }

        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }

        public void setX(float x) { this.x = x; }
        public void setY(float y) { this.y = y; }
        public void setZ(float z) { this.z = z; }
    }

    /**
     * Static geometry definition (walls, floors, props)
     */
    public static class StaticGeometryData {
        private String type; // "wall", "floor", "cube", "sphere"
        private float x, y, z;
        private float width, height, depth;
        private float size; // For floor/plane
        private float r, g, b; // Color

        public StaticGeometryData() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public float getX() { return x; }
        public void setX(float x) { this.x = x; }

        public float getY() { return y; }
        public void setY(float y) { this.y = y; }

        public float getZ() { return z; }
        public void setZ(float z) { this.z = z; }

        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }

        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }

        public float getDepth() { return depth; }
        public void setDepth(float depth) { this.depth = depth; }

        public float getSize() { return size; }
        public void setSize(float size) { this.size = size; }

        public float getR() { return r; }
        public void setR(float r) { this.r = r; }

        public float getG() { return g; }
        public void setG(float g) { this.g = g; }

        public float getB() { return b; }
        public void setB(float b) { this.b = b; }

        public Vector3f getPosition() {
            return new Vector3f(x, y, z);
        }

        public Vector3f getColor() {
            return new Vector3f(r, g, b);
        }
    }

    /**
     * Entity definition (pickups, doors, NPCs)
     */
    public static class EntityData {
        private String type; // "health_pack", "weapon_spawn", "door"
        private float x, y, z;
        private String data; // Additional entity-specific data (JSON string)

        public EntityData() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public float getX() { return x; }
        public void setX(float x) { this.x = x; }

        public float getY() { return y; }
        public void setY(float y) { this.y = y; }

        public float getZ() { return z; }
        public void setZ(float z) { this.z = z; }

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }

        public Vector3f getPosition() {
            return new Vector3f(x, y, z);
        }
    }
}
