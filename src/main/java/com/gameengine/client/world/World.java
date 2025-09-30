package com.gameengine.client.world;

import com.gameengine.client.renderer.Renderer;
import com.gameengine.shared.ecs.Entity;
import com.gameengine.shared.world.WorldData;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * World manager - handles static geometry and entities
 */
public class World {

    private static final Logger logger = LoggerFactory.getLogger(World.class);

    private WorldData worldData;
    private List<StaticGeometry> staticGeometry;
    private List<Entity> entities;

    public World() {
        this.staticGeometry = new ArrayList<>();
        this.entities = new ArrayList<>();
    }

    /**
     * Initialize the world from server-provided map data (PROPER WAY)
     */
    public void init(WorldData worldData) {
        logger.info("Initializing world from server data: {}", worldData.getName());

        this.worldData = worldData;

        // Build static geometry from map data
        buildStaticGeometry();

        logger.info("World initialized: {}", worldData.getName());
    }

    /**
     * Initialize the world from a map file (DEPRECATED - server should send map)
     */
    @Deprecated
    public void init(String mapPath) throws IOException {
        logger.warn("Loading map from file is deprecated - server should send map data");
        logger.info("Initializing world from map: {}", mapPath);

        // Load map data
        worldData = com.gameengine.shared.world.MapLoader.load(mapPath);

        // Build static geometry from map data
        buildStaticGeometry();

        logger.info("World initialized: {}", worldData.getName());
    }

    /**
     * Initialize the world with default test map (DEPRECATED - for fallback only)
     */
    @Deprecated
    public void init() {
        logger.warn("Using default map - server should send map data");
        logger.info("Initializing world with default map");

        // Create default map
        worldData = com.gameengine.shared.world.MapLoader.createDefaultMap();

        // Build static geometry from map data
        buildStaticGeometry();

        logger.info("World initialized with default map");
    }

    /**
     * Initialize world with procedural terrain from server parameters
     */
    public void initTerrain(long seed, int width, int depth, boolean isFlat, float flatHeight) {
        logger.info("Generating terrain: seed={}, size={}x{}, flat={}", seed, width, depth, isFlat);

        // Generate terrain using same algorithm as server
        com.gameengine.shared.world.TerrainGenerator generator = new com.gameengine.shared.world.TerrainGenerator(seed);
        com.gameengine.shared.world.TerrainData terrainData;   

        if (isFlat) {
            terrainData = generator.generateFlat(width, depth, flatHeight);
        } else {
            terrainData = generator.generate(width, depth);
        }

        // For now, just create a simple world data with a large floor
        // Later we can build mesh from heightmap
        worldData = com.gameengine.shared.world.MapLoader.createDefaultMap();

        // Build the geometry meshes from world data
        buildStaticGeometry();

        logger.info("Terrain generated and geometry built successfully");
    }

    /**
     * Build static geometry objects from world data
     */
    private void buildStaticGeometry() {
        for (WorldData.StaticGeometryData data : worldData.getStaticGeometry()) {
            Vector3f position = data.getPosition();
            Vector3f dimensions = new Vector3f(data.getWidth(), data.getHeight(), data.getDepth());

            // Use size for floor/ground if specified
            if (data.getSize() > 0) {
                dimensions.x = data.getSize();
            }

            Vector3f color = data.getColor();

            StaticGeometry geometry = new StaticGeometry(data.getType(), position, dimensions, color);
            staticGeometry.add(geometry);
        }

        logger.info("Built {} static geometry objects", staticGeometry.size());
    }

    /**
     * Update world (for dynamic entities)
     */
    public void update(float deltaTime) {
        // Entities are updated by ECS systems, not here
    }

    /**
     * Render all world geometry and entities
     */
    public void render(Renderer renderer) {
        // Render static geometry
        for (StaticGeometry geometry : staticGeometry) {
            geometry.render(renderer);
        }

        // Entities are rendered by ECS systems, not here
    }

    /**
     * Cleanup all world resources
     */
    public void cleanup() {
        // Cleanup static geometry
        for (StaticGeometry geometry : staticGeometry) {
            geometry.cleanup();
        }

        // Clear entity list
        entities.clear();

        logger.info("World cleaned up");
    }

    /**
     * Get a random spawn point
     */
    public Vector3f getRandomSpawnPoint() {
        if (worldData.getSpawnPoints().isEmpty()) {
            logger.warn("No spawn points defined, using origin");
            return new Vector3f(0, 0, 0);
        }

        int index = (int) (Math.random() * worldData.getSpawnPoints().size());
        return worldData.getSpawnPoints().get(index).getPosition();
    }

    /**
     * Add an entity to the world
     */
    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    /**
     * Remove an entity from the world
     */
    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public WorldData getWorldData() {
        return worldData;
    }

    public List<Entity> getEntities() {
        return entities;
    }
}
