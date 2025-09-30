package com.gameengine.client.managers;

import com.gameengine.client.controllers.CameraController;
import com.gameengine.client.debug.DebugRenderer;
import com.gameengine.client.demo.ECSDemo;
import com.gameengine.client.network.NetworkClient;
import com.gameengine.client.player.ClientPlayer;
import com.gameengine.client.renderer.Mesh;
import com.gameengine.client.renderer.Renderer;
import com.gameengine.client.world.World;
import org.joml.Vector3f;
import com.gameengine.client.player.ClientPlayer;
/**
 * Manages rendering of all game elements
 */
public class RenderManager {

    private final Renderer renderer;
    private final World world;
    private final NetworkClient networkClient;
    private final DebugRenderer debugRenderer;
    private final CameraController cameraController;
    private final ECSDemo ecsDemo;
    private final Mesh playerMesh;

    public RenderManager(Renderer renderer, World world, NetworkClient networkClient,
                        DebugRenderer debugRenderer, CameraController cameraController,
                        ECSDemo ecsDemo, Mesh playerMesh) {
        this.renderer = renderer;
        this.world = world;
        this.networkClient = networkClient;
        this.debugRenderer = debugRenderer;
        this.cameraController = cameraController;
        this.ecsDemo = ecsDemo;
        this.playerMesh = playerMesh;
    }

    /**
     * Render game frame
     */
    public void render(boolean worldInitialized, boolean ecsDemoEnabled) {
        // Begin frame - binds framebuffer and clears
        renderer.beginFrame();

        // Render world (terrain, walls, structures) - only if initialized
        if (worldInitialized) {
            world.render(renderer);
        }

        // Render all players
        for (ClientPlayer player : networkClient.getPlayers().values()) {
            boolean isLocalPlayer = player == networkClient.getLocalPlayer();

            // For local player: use predicted position
            // For other players: use interpolated position
            Vector3f renderPos = isLocalPlayer ?
                player.getPredictedPosition() :
                player.getRenderedPosition();

            renderer.render(playerMesh, renderPos.x, renderPos.y + 0.5f, renderPos.z);

            // Debug visualization for local player
            if (isLocalPlayer) {
                debugRenderer.renderPlayerDebug(player);
            }
        }

        // Render ECS demo entities if enabled
        if (ecsDemoEnabled) {
            ecsDemo.render();
        }

        // End frame - applies post-processing to 3D scene
        renderer.endFrame();

        // Render UI/text AFTER post-processing (directly to screen)
        ClientPlayer localPlayer = networkClient.getLocalPlayer();
        debugRenderer.renderDebugText(localPlayer, renderer.getCamera(), cameraController);
    }
}
