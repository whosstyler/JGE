package com.gameengine.shared.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads map data from JSON files
 */
public class MapLoader {

    private static final Logger logger = LoggerFactory.getLogger(MapLoader.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load a map from JSON file
     */
    public static WorldData load(String filePath) throws IOException {
        logger.info("Loading map from: {}", filePath);

        String json = new String(Files.readAllBytes(Paths.get(filePath)));
        WorldData worldData = gson.fromJson(json, WorldData.class);

        logger.info("Loaded map '{}' with {} spawn points, {} static geometry, {} entities",
            worldData.getName(),
            worldData.getSpawnPoints().size(),
            worldData.getStaticGeometry().size(),
            worldData.getEntities().size());

        return worldData;
    }

    /**
     * Save a map to JSON file (for map editor later)
     */
    public static void save(String filePath, WorldData worldData) throws IOException {
        logger.info("Saving map to: {}", filePath);

        String json = gson.toJson(worldData);
        Files.write(Paths.get(filePath), json.getBytes());

        logger.info("Map saved successfully");
    }

    /**
     * Create a default test map programmatically - huge flat open world
     */
    public static WorldData createDefaultMap() {
        WorldData worldData = new WorldData();
        worldData.setName("Flat Open World");

        // Add spawn points spread out
        worldData.getSpawnPoints().add(new WorldData.SpawnPoint(0, 2, 0));
        worldData.getSpawnPoints().add(new WorldData.SpawnPoint(10, 2, 10));
        worldData.getSpawnPoints().add(new WorldData.SpawnPoint(-10, 2, -10));
        worldData.getSpawnPoints().add(new WorldData.SpawnPoint(20, 2, 0));

        // Add huge flat floor (1000x1000) - bright green grass color
        WorldData.StaticGeometryData floor = new WorldData.StaticGeometryData();
        floor.setType("floor");
        floor.setX(0);
        floor.setY(0);
        floor.setZ(0);
        floor.setSize(1000);
        floor.setR(0.4f);  // Bright green grass
        floor.setG(0.8f);
        floor.setB(0.3f);
        worldData.getStaticGeometry().add(floor);

        // No walls - completely open world

        logger.info("Created default flat open world (1000x1000)");
        return worldData;
    }
}
