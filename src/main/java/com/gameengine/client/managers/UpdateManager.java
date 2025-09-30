package com.gameengine.client.managers;

import com.gameengine.client.controllers.CameraController;
import com.gameengine.client.controllers.PlayerController;
import com.gameengine.client.demo.ECSDemo;
import com.gameengine.client.input.InputHandler;
import com.gameengine.client.network.NetworkClient;
import com.gameengine.client.player.ClientPlayer;
import com.gameengine.client.player.PredictionManager;
import com.gameengine.client.renderer.Renderer;
import com.gameengine.client.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages game update logic
 */
public class UpdateManager {

    private static final Logger logger = LoggerFactory.getLogger(UpdateManager.class);

    private final NetworkClient networkClient;
    private final World world;
    private final CameraController cameraController;
    private final PlayerController playerController;
    private final PredictionManager predictionManager;
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final ECSDemo ecsDemo;

    // State
    private boolean worldInitialized = false;
    private boolean ecsDemoEnabled = false;
    private boolean f7WasPressed = false;
    private boolean postProcessingEnabled = true;
    private boolean f6WasPressed = false;

    public UpdateManager(NetworkClient networkClient, World world, CameraController cameraController,
                        PlayerController playerController, PredictionManager predictionManager,
                        Renderer renderer, InputHandler inputHandler, ECSDemo ecsDemo) {
        this.networkClient = networkClient;
        this.world = world;
        this.cameraController = cameraController;
        this.playerController = playerController;
        this.predictionManager = predictionManager;
        this.renderer = renderer;
        this.inputHandler = inputHandler;
        this.ecsDemo = ecsDemo;
    }

    /**
     * Update game logic
     */
    public void update(float deltaTime, long currentTime, long lastInputTime) {
        // Wait for terrain sync from server before initializing world
        if (!worldInitialized && networkClient.isTerrainReceived()) {
            world.initTerrain(
                networkClient.getTerrainSeed(),
                networkClient.getTerrainWidth(),
                networkClient.getTerrainDepth(),
                networkClient.isTerrainFlat(),
                networkClient.getTerrainFlatHeight()
            );
            worldInitialized = true;
            logger.info("World initialized with server terrain");
        }

        // Can't update until world is ready
        if (!worldInitialized) {
            return;
        }

        ClientPlayer localPlayer = networkClient.getLocalPlayer();
        if (localPlayer == null) return;

        // Update world (entities, etc.)
        world.update(deltaTime);

        // Update camera every frame for smooth rotation
        cameraController.update(deltaTime, localPlayer.getPredictedPosition());

        // Send input to server at fixed rate (20 Hz)
        if (currentTime - lastInputTime >= 50) {
            playerController.sendInputToServer();
        }

        // Update client-side prediction
        predictionManager.update(deltaTime, localPlayer);

        // Interpolate OTHER players only (not local player)
        for (ClientPlayer player : networkClient.getPlayers().values()) {
            if (player != localPlayer) {
                player.interpolate(deltaTime);
            }
        }

        // Toggle post-processing with F6
        boolean f6IsPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_F6);
        if (f6IsPressed && !f6WasPressed) {
            postProcessingEnabled = !postProcessingEnabled;
            renderer.setPostProcessingEnabled(postProcessingEnabled);
        }
        f6WasPressed = f6IsPressed;

        // Toggle ECS Demo with F7
        boolean f7IsPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_F7);
        if (f7IsPressed && !f7WasPressed) {
            ecsDemoEnabled = !ecsDemoEnabled;
            if (ecsDemoEnabled) {
                ecsDemo.createDemoEntities();
                logger.info("✅ ECS Demo ENABLED - {} entities", ecsDemo.getEntityCount());
            } else {
                logger.info("❌ ECS Demo DISABLED");
            }
        }
        f7WasPressed = f7IsPressed;

        // Update ECS demo if enabled
        if (ecsDemoEnabled) {
            ecsDemo.update(deltaTime);
        }
    }

    public boolean isWorldInitialized() { return worldInitialized; }
    public boolean isEcsDemoEnabled() { return ecsDemoEnabled; }
}
