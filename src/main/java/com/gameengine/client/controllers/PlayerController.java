package com.gameengine.client.controllers;

import com.gameengine.client.input.InputHandler;
import com.gameengine.client.network.NetworkClient;
import com.gameengine.client.player.PredictionManager; 
import com.gameengine.client.player.ClientPlayer;
import com.gameengine.shared.network.PlayerInputPacket;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles player input collection and network transmission
 */
public class PlayerController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    private final InputHandler inputHandler;
    private final NetworkClient networkClient;
    private final PredictionManager predictionManager;
    private final CameraController cameraController;

    private static final float MOVE_SPEED = 5.0f;
    private static final float FIXED_DELTA_TIME = 0.05f; // 50ms = 20Hz

    public PlayerController(InputHandler inputHandler, NetworkClient networkClient,
                           PredictionManager predictionManager, CameraController cameraController) {
        this.inputHandler = inputHandler;
        this.networkClient = networkClient;
        this.predictionManager = predictionManager;
        this.cameraController = cameraController;
    }

    /**
     * Send input to server at 20Hz
     */
    public void sendInputToServer() {
        ClientPlayer localPlayer = networkClient.getLocalPlayer();
        if (localPlayer == null) return;

        float currentYaw = cameraController.getYaw();
        float currentPitch = cameraController.getPitch();

        // Collect input keys
        boolean wPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_W);
        boolean aPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_A);
        boolean sPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_S);
        boolean dPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_D);
        boolean spacePressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_SPACE);
        boolean shiftPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT);

        // Build button states bitfield
        byte buttonStates = 0;
        if (wPressed) buttonStates |= PlayerInputPacket.BUTTON_FORWARD;
        if (sPressed) buttonStates |= PlayerInputPacket.BUTTON_BACK;
        if (aPressed) buttonStates |= PlayerInputPacket.BUTTON_LEFT;
        if (dPressed) buttonStates |= PlayerInputPacket.BUTTON_RIGHT;
        if (spacePressed) buttonStates |= PlayerInputPacket.BUTTON_JUMP;
        if (shiftPressed) buttonStates |= PlayerInputPacket.BUTTON_CROUCH;

        // Get sequence number
        int sequence = predictionManager.getNextSequence();

        // Debug logging (only when buttons pressed or every 10th frame)
        if (buttonStates != 0 || sequence % 10 == 0) {
            logger.info("CLIENT #{}: Sending yaw={}, pitch={} | buttonStates={}",
                sequence, currentYaw, currentPitch, buttonStates);
        }

        // Send input to server
        networkClient.sendInput(buttonStates, currentYaw, currentPitch, sequence);

        // Client-side prediction: calculate movement the SAME way server does
        Vector3f movement = calculateMovement(buttonStates, currentYaw);

        // Store pending input for prediction reconciliation
        predictionManager.addPendingInput(sequence, movement.x, movement.y, movement.z,
                                         currentYaw, currentPitch, FIXED_DELTA_TIME);

        // Apply input immediately (client-side prediction)
        localPlayer.predict(movement.x, movement.y, movement.z, FIXED_DELTA_TIME);
    }

    /**
     * Calculate movement vector from button states and yaw (matches server logic)
     */
    private Vector3f calculateMovement(byte buttonStates, float yaw) {
        float inputForward = 0;
        float inputStrafe = 0;

        if ((buttonStates & PlayerInputPacket.BUTTON_FORWARD) != 0) inputForward += 1.0f;
        if ((buttonStates & PlayerInputPacket.BUTTON_BACK) != 0) inputForward -= 1.0f;
        if ((buttonStates & PlayerInputPacket.BUTTON_RIGHT) != 0) inputStrafe += 1.0f;
        if ((buttonStates & PlayerInputPacket.BUTTON_LEFT) != 0) inputStrafe -= 1.0f;

        float yawRad = (float) Math.toRadians(yaw);
        float forwardX = -(float) Math.sin(yawRad);
        float forwardZ = -(float) Math.cos(yawRad);
        float rightX = (float) Math.cos(yawRad);
        float rightZ = -(float) Math.sin(yawRad);

        float moveX = (forwardX * inputForward + rightX * inputStrafe) * MOVE_SPEED;
        float moveZ = (forwardZ * inputForward + rightZ * inputStrafe) * MOVE_SPEED;

        return new Vector3f(moveX, 0, moveZ);
    }
}
