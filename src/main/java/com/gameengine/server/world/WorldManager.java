package com.gameengine.server.world;

import com.gameengine.shared.physics.PhysicsWorld;
import com.gameengine.shared.world.TerrainData;
import com.gameengine.shared.world.TerrainGenerator;
import com.gameengine.shared.world.WorldData;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side world manager - generates procedural terrain and manages world state
 */
public class WorldManager {

    private static final Logger logger = LoggerFactory.getLogger(WorldManager.class);

    private TerrainData terrainData;
    private TerrainGenerator terrainGenerator;
    private final PhysicsWorld physicsWorld;

    // World settings
    private static final int TERRAIN_WIDTH = 1000; // 1000x1000 grid - big and huge
    private static final int TERRAIN_DEPTH = 1000;

    // Terrain generation parameters (for syncing with clients)
    private long seed;
    private boolean isFlat;
    private float flatHeight;

    public WorldManager(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
    }

    /**
     * Generate procedural terrain world
     */
    public void generateWorld(long seed) {
        logger.info("Generating procedural world with seed: {}", seed);

        this.seed = seed;
        this.isFlat = false;
        this.flatHeight = 0;

        // Create terrain generator
        terrainGenerator = new TerrainGenerator(seed);

        // Generate terrain heightmap
        terrainData = terrainGenerator.generate(TERRAIN_WIDTH, TERRAIN_DEPTH);

        // TODO: Build collision from terrain heightmap
        // For now, just flat ground collision

        logger.info("Procedural world generated successfully");
    }

    /**
     * Generate world with random seed
     */
    public void generateWorld() {
        long seed = System.currentTimeMillis();
        generateWorld(seed);
    }

    /**
     * Generate flat test world
     */
    public void generateFlatWorld(float height) {
        logger.info("Generating flat test world at height {}", height);

        this.seed = System.currentTimeMillis();
        this.isFlat = true;
        this.flatHeight = height;

        terrainGenerator = new TerrainGenerator(seed);
        terrainData = terrainGenerator.generateFlat(TERRAIN_WIDTH, TERRAIN_DEPTH, height);

        logger.info("Flat world generated");
    }

    /**
     * Get random spawn point (for now, just return flat positions)
     */
    public Vector3f getRandomSpawnPoint() {
        // Random position on the terrain
        float x = (float) (Math.random() * 40 - 20); // -20 to 20
        float z = (float) (Math.random() * 40 - 20); // -20 to 20

        // Get height at this position
        float y = 0;
        if (terrainData != null) {
            y = terrainData.getHeightAtWorldPos(x, z) + 2.0f; // 2 units above terrain
        }

        return new Vector3f(x, y, z);
    }

    /**
     * Get terrain height at world position
     */
    public float getTerrainHeight(float x, float z) {
        if (terrainData == null) {
            return 0;
        }
        return terrainData.getHeightAtWorldPos(x, z);
    }

    public TerrainData getTerrainData() {
        return terrainData;
    }

    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    /**
     * DEPRECATED - JSON map loading (old system)
     */
    @Deprecated
    public WorldData getWorldData() {
        // Return null - we're using procedural generation now
        logger.warn("getWorldData() called but using procedural generation");
        return null;
    }

    // Terrain sync getters
    public long getSeed() {
        return seed;
    }

    public boolean isFlat() {
        return isFlat;
    }

    public float getFlatHeight() {
        return flatHeight;
    }

    public int getTerrainWidth() {
        return TERRAIN_WIDTH;
    }

    public int getTerrainDepth() {
        return TERRAIN_DEPTH;
    }
}
