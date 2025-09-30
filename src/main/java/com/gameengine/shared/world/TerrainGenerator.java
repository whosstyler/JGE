package com.gameengine.shared.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Procedural terrain generator using Simplex noise
 */
public class TerrainGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TerrainGenerator.class);

    // Terrain parameters
    private final long seed;
    private final SimplexNoise noise;

    // Generation settings
    private static final double BASE_SCALE = 0.01; // Frequency of terrain features
    private static final int OCTAVES = 4; // Detail levels
    private static final double PERSISTENCE = 0.5; // How much each octave contributes
    private static final double AMPLITUDE = 20.0; // Max height variation
    private static final double BASE_HEIGHT = 5.0; // Base terrain height

    public TerrainGenerator(long seed) {
        this.seed = seed;
        this.noise = new SimplexNoise(seed);
        logger.info("Terrain generator initialized with seed: {}", seed);
    }

    /**
     * Generate terrain heightmap
     */
    public TerrainData generate(int width, int depth) {
        logger.info("Generating {}x{} terrain with seed {}...", width, depth, seed);
        long startTime = System.currentTimeMillis();

        TerrainData terrain = new TerrainData(width, depth, seed);

        // Generate heightmap
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                // Center the coordinates
                double worldX = x - width / 2.0;
                double worldZ = z - depth / 2.0;

                // Generate height using octave noise
                double noiseValue = noise.octaveNoise(
                    worldX,
                    worldZ,
                    OCTAVES,
                    PERSISTENCE,
                    BASE_SCALE
                );

                // Map noise (-1 to 1) to height
                float height = (float) (BASE_HEIGHT + noiseValue * AMPLITUDE);

                // Ensure minimum height (don't go below ground)
                height = Math.max(0, height);

                terrain.setHeight(x, z, height);
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Terrain generated in {}ms", endTime - startTime);

        return terrain;
    }

    /**
     * Generate terrain with custom parameters
     */
    public TerrainData generateCustom(int width, int depth,
                                     double scale, int octaves,
                                     double persistence, double amplitude) {
        logger.info("Generating custom {}x{} terrain...", width, depth);

        TerrainData terrain = new TerrainData(width, depth, seed);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                double worldX = x - width / 2.0;
                double worldZ = z - depth / 2.0;

                double noiseValue = noise.octaveNoise(
                    worldX,
                    worldZ,
                    octaves,
                    persistence,
                    scale
                );

                float height = (float) (BASE_HEIGHT + noiseValue * amplitude);
                height = Math.max(0, height);

                terrain.setHeight(x, z, height);
            }
        }

        return terrain;
    }

    /**
     * Generate flat terrain (for testing)
     */
    public TerrainData generateFlat(int width, int depth, float height) {
        logger.info("Generating flat {}x{} terrain at height {}", width, depth, height);

        TerrainData terrain = new TerrainData(width, depth, seed);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                terrain.setHeight(x, z, height);
            }
        }

        return terrain;
    }

    public long getSeed() {
        return seed;
    }
}
