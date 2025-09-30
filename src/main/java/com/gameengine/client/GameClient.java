package com.gameengine.client;

import com.gameengine.client.input.InputHandler;
import com.gameengine.client.managers.ClientInitializer;
import com.gameengine.client.managers.RenderManager;
import com.gameengine.client.managers.UpdateManager;
import com.gameengine.client.network.NetworkClient;
import com.gameengine.client.renderer.Renderer;
import com.gameengine.client.ui.MainMenuScreen;
import com.gameengine.client.world.World;
import com.gameengine.shared.network.NetworkProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main game client - coordinates all subsystems
 */
public class GameClient {

    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);

    // Core systems
    private final Renderer renderer;
    private final InputHandler inputHandler;
    private final NetworkClient networkClient;
    private final World world;
    private final String playerName;

    // Managers
    private ClientInitializer initializer;
    private UpdateManager updateManager;
    private RenderManager renderManager;

    // State
    private volatile boolean running;
    private boolean inGame;

    public GameClient(String host, int port, String playerName) {
        this.renderer = new Renderer(1280, 720, "Game Client");
        this.inputHandler = new InputHandler();
        this.networkClient = new NetworkClient(host, port, playerName);
        this.world = new World();
        this.playerName = playerName;
        this.inGame = false;
    }

    public void start() throws Exception {
        // Initialize all subsystems
        initializer = new ClientInitializer(renderer, inputHandler, networkClient, world, playerName);
        initializer.initialize();

        // Create managers
        updateManager = new UpdateManager(
            networkClient, world,
            initializer.getCameraController(),
            initializer.getPlayerController(),
            initializer.getPredictionManager(),
            renderer, inputHandler,
            initializer.getEcsDemo()
        );

        renderManager = new RenderManager(
            renderer, world, networkClient,
            initializer.getDebugRenderer(),
            initializer.getCameraController(),
            initializer.getEcsDemo(),
            initializer.getPlayerMesh()
        );

        running = true;
        run();
    }

    private void run() {
        long lastTime = System.currentTimeMillis();
        long lastInputTime = lastTime;
        int frames = 0;
        long fpsTimer = lastTime;

        while (running && !renderer.shouldClose()) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastTime) / 1000.0f;
            lastTime = currentTime;

            if (!inGame) {
                // Show main menu
                MainMenuScreen mainMenu = initializer.getMainMenuScreen();
                boolean shouldConnect = mainMenu.render(deltaTime);

                if (shouldConnect) {
                    try {
                        logger.info("Connecting to server...");
                        networkClient.connect();
                        inGame = true;
                        logger.info("Connected! Starting game...");
                    } catch (Exception e) {
                        logger.error("Failed to connect to server", e);
                        mainMenu.setStatusText("Connection failed! Press ENTER to retry");
                    }
                }
            } else {
                // In-game logic
                updateManager.update(deltaTime, currentTime, lastInputTime);

                // Update last input time if we sent input
                if (currentTime - lastInputTime >= 50) { // 20 Hz = 50ms
                    lastInputTime = currentTime;
                }

                // Render
                renderManager.render(updateManager.isWorldInitialized(), updateManager.isEcsDemoEnabled());

                // FPS counter
                frames++;
                if (currentTime - fpsTimer >= 1000) {
                    logger.debug("FPS: {}", frames);
                    frames = 0;
                    fpsTimer = currentTime;
                }
            }

            renderer.pollEvents();
            renderer.swapBuffers();
        }

        cleanup();
    }

    private void cleanup() {
        logger.info("Shutting down client");
        running = false;

        if (networkClient.isConnected()) {
            networkClient.disconnect();
        }

        if (initializer.getPlayerMesh() != null) {
            initializer.getPlayerMesh().cleanup();
        }

        if (initializer.getEcsDemo() != null) {
            initializer.getEcsDemo().cleanup();
        }

        initializer.getDebugRenderer().cleanup();
        world.cleanup();
        renderer.cleanup();
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : NetworkProtocol.DEFAULT_PORT;
        String playerName = args.length > 2 ? args[2] : "Player" + System.currentTimeMillis() % 10000;

        GameClient client = new GameClient(host, port, playerName);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));

        client.start();
    }
}
