package com.gameengine.client.demo;

import com.gameengine.client.assets.AssetManager;
import com.gameengine.client.renderer.Mesh;
import com.gameengine.client.renderer.Renderer;
import com.gameengine.shared.ecs.ECSWorld;
import com.gameengine.shared.ecs.Entity;
import com.gameengine.shared.ecs.components.RenderableComponent;
import com.gameengine.shared.ecs.components.TransformComponent;
import com.gameengine.shared.ecs.components.VelocityComponent;
import com.gameengine.shared.ecs.systems.MovementSystem;
import com.gameengine.shared.ecs.systems.RenderSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo class to test ECS and Asset Loading systems
 */
public class ECSDemo {

    private static final Logger logger = LoggerFactory.getLogger(ECSDemo.class);

    private final ECSWorld ecsWorld;
    private final RenderSystem renderSystem;
    private final AssetManager assetManager;

    public ECSDemo(Renderer renderer) {
        this.ecsWorld = new ECSWorld();
        this.assetManager = AssetManager.getInstance();

        // Add systems
        ecsWorld.addSystem(new MovementSystem());
        this.renderSystem = new RenderSystem(renderer);
        ecsWorld.addSystem(renderSystem);

        logger.info("ECS Demo initialized");
    }

    /**
     * Create demo entities to test ECS and asset loading
     */
    public void createDemoEntities() {
        logger.info("Creating demo entities...");

        // Test 1: Create spinning cubes with built-in mesh
        createSpinningCube(-5, 2, 0, 0, 1, 0); // Spins around Y axis (yaw)
        createSpinningCube(5, 2, 0, 1, 0, 0);  // Spins around X axis (pitch)
        createSpinningCube(0, 2, 5, 0, 0, 1);  // Spins around Z axis (roll)

        // Test 2: Try to load OBJ model (pyramid)
        try {
            Mesh pyramidMesh = assetManager.loadMesh("assets/models/pyramid.obj");
            if (pyramidMesh != null) {
                createSpinningEntity(pyramidMesh, 0, 5, 0, 0, 2, 0); // Pyramid spinning fast
                logger.info("✅ Asset loading test: SUCCESS - Loaded pyramid.obj");
            }
        } catch (Exception e) {
            logger.warn("⚠️ Asset loading test: Could not load pyramid.obj - {}", e.getMessage());
            logger.warn("   This is OK if you haven't created the file yet");
        }

        // Test 3: Create a stationary entity
        Entity staticCube = ecsWorld.createEntity();
        staticCube.addComponent(new TransformComponent(0, 0, -10));
        staticCube.addComponent(new RenderableComponent(
            Mesh.createCube(0.8f, 0.2f, 0.8f) // Purple cube
        ));

        logger.info("✅ ECS test: Created {} entities", ecsWorld.getEntityCount());
        assetManager.printStats();
    }

    /**
     * Create a spinning cube entity
     */
    private void createSpinningCube(float x, float y, float z, float spinX, float spinY, float spinZ) {
        Entity entity = ecsWorld.createEntity();

        // Transform component
        entity.addComponent(new TransformComponent(x, y, z));

        // Renderable component with colored cube
        float r = (float) Math.random();
        float g = (float) Math.random();
        float b = (float) Math.random();
        entity.addComponent(new RenderableComponent(Mesh.createCube(r, g, b)));

        // Velocity component (angular velocity for spinning)
        VelocityComponent velocity = new VelocityComponent();
        velocity.angular.set(spinX, spinY, spinZ); // Radians per second
        entity.addComponent(velocity);
    }

    /**
     * Create a spinning entity with custom mesh
     */
    private void createSpinningEntity(Mesh mesh, float x, float y, float z, float spinX, float spinY, float spinZ) {
        Entity entity = ecsWorld.createEntity();
        entity.addComponent(new TransformComponent(x, y, z));
        entity.addComponent(new RenderableComponent(mesh));

        VelocityComponent velocity = new VelocityComponent();
        velocity.angular.set(spinX, spinY, spinZ);
        entity.addComponent(velocity);
    }

    /**
     * Update all ECS systems
     */
    public void update(float deltaTime) {
        ecsWorld.update(deltaTime);
    }

    /**
     * Render all entities
     */
    public void render() {
        renderSystem.render();
    }

    /**
     * Get entity count for debugging
     */
    public int getEntityCount() {
        return ecsWorld.getEntityCount();
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        assetManager.cleanup();
    }
}
