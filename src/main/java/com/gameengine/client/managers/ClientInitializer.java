package com.gameengine.client.managers;

import com.gameengine.client.controllers.CameraController;
import com.gameengine.client.controllers.PlayerController;
import com.gameengine.client.debug.DebugRenderer;
import com.gameengine.client.demo.ECSDemo;
import com.gameengine.client.input.InputHandler;
import com.gameengine.client.network.NetworkClient;
import com.gameengine.client.player.PredictionManager;
import com.gameengine.client.renderer.Mesh;
import com.gameengine.client.renderer.Renderer;
import com.gameengine.client.ui.MainMenuScreen;
import com.gameengine.client.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles initialization of all client subsystems
 */
public class ClientInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ClientInitializer.class);

    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final NetworkClient networkClient;
    private final World world;
    private final String playerName;

    // Initialized subsystems
    private CameraController cameraController;
    private PlayerController playerController;
    private PredictionManager predictionManager;
    private DebugRenderer debugRenderer;
    private MainMenuScreen mainMenuScreen;
    private ECSDemo ecsDemo;
    private Mesh playerMesh;

    public ClientInitializer(Renderer renderer, InputHandler inputHandler,
                           NetworkClient networkClient, World world, String playerName) {
        this.renderer = renderer;
        this.inputHandler = inputHandler;
        this.networkClient = networkClient;
        this.world = world;
        this.playerName = playerName;
    }

    /**
     * Initialize all client subsystems
     */
    public void initialize() throws Exception {
        // Initialize renderer
        renderer.init();
        inputHandler.init(renderer.getWindow());

        // Initialize controllers
        cameraController = new CameraController(renderer.getCamera(), inputHandler);
        predictionManager = new PredictionManager();
        playerController = new PlayerController(inputHandler, networkClient, predictionManager, cameraController);
        debugRenderer = new DebugRenderer(renderer, inputHandler);

        // Initialize main menu
        mainMenuScreen = new MainMenuScreen(renderer.getWindow(), renderer);

        // Initialize debug renderer
        debugRenderer.init();

        // Create player visualization mesh
        playerMesh = Mesh.createCube(0.2f, 0.6f, 1.0f); // Blue cubes for players

        // Initialize ECS Demo
        ecsDemo = new ECSDemo(renderer);
        logger.info("Press F7 to toggle ECS Demo (spinning cubes + asset loading test)");

        logger.info("Client subsystems initialized");
    }

    public CameraController getCameraController() { return cameraController; }
    public PlayerController getPlayerController() { return playerController; }
    public PredictionManager getPredictionManager() { return predictionManager; }
    public DebugRenderer getDebugRenderer() { return debugRenderer; }
    public MainMenuScreen getMainMenuScreen() { return mainMenuScreen; }
    public ECSDemo getEcsDemo() { return ecsDemo; }
    public Mesh getPlayerMesh() { return playerMesh; }
}
