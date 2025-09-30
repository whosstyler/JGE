package com.gameengine.client.assets;

import com.gameengine.client.renderer.Mesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Asset Manager - Loads and caches game assets
 * Singleton pattern for global access
 */
public class AssetManager {

    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    private static AssetManager instance;

    private final Map<String, Mesh> meshCache;
    private final Map<String, Integer> textureCache; // Texture ID cache
    private final Map<String, Object> soundCache;   // Sound cache (TBD)

    private AssetManager() {
        this.meshCache = new HashMap<>();
        this.textureCache = new HashMap<>();
        this.soundCache = new HashMap<>();
        logger.info("AssetManager initialized");
    }

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    /**
     * Load a mesh from file (OBJ format)
     */
    public Mesh loadMesh(String path) {
        // Check cache first
        if (meshCache.containsKey(path)) {
            logger.debug("Mesh '{}' loaded from cache", path);
            return meshCache.get(path);
        }

        // Load mesh
        logger.info("Loading mesh: {}", path);
        try {
            Mesh mesh = ModelLoader.loadOBJ(path);
            meshCache.put(path, mesh);
            return mesh;
        } catch (Exception e) {
            logger.error("Failed to load mesh: {}", path, e);
            return null;
        }
    }

    /**
     * Load a texture from file
     */
    public int loadTexture(String path) {
        // Check cache first
        if (textureCache.containsKey(path)) {
            logger.debug("Texture '{}' loaded from cache", path);
            return textureCache.get(path);
        }

        // Load texture
        logger.info("Loading texture: {}", path);
        try {
            int textureId = TextureLoader.loadTexture(path);
            textureCache.put(path, textureId);
            return textureId;
        } catch (Exception e) {
            logger.error("Failed to load texture: {}", path, e);
            return -1;
        }
    }

    /**
     * Get a cached mesh
     */
    public Mesh getMesh(String path) {
        return meshCache.get(path);
    }

    /**
     * Get a cached texture
     */
    public Integer getTexture(String path) {
        return textureCache.get(path);
    }

    /**
     * Cleanup all loaded assets
     */
    public void cleanup() {
        logger.info("Cleaning up assets...");

        // Cleanup meshes
        for (Mesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();

        // Cleanup textures
        for (int textureId : textureCache.values()) {
            TextureLoader.deleteTexture(textureId);
        }
        textureCache.clear();

        logger.info("Assets cleaned up");
    }

    /**
     * Get asset statistics
     */
    public void printStats() {
        logger.info("Asset Statistics:");
        logger.info("  Meshes loaded: {}", meshCache.size());
        logger.info("  Textures loaded: {}", textureCache.size());
    }
}
