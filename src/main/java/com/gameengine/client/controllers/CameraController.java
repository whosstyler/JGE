package com.gameengine.client.controllers;

import com.gameengine.client.input.InputHandler;
import com.gameengine.client.renderer.Camera;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles camera positioning, rotation, and view modes
 */
public class CameraController {

    private static final Logger logger = LoggerFactory.getLogger(CameraController.class);

    private final Camera camera;
    private final InputHandler inputHandler;

    private boolean thirdPersonMode = false;
    private boolean f5WasPressed = false;
    private float mouseSensitivity = 0.1f;

    // Third-person camera settings
    private static final float THIRD_PERSON_DISTANCE = 5.0f;
    private static final float THIRD_PERSON_HEIGHT = 2.5f;
    private static final float FIRST_PERSON_HEIGHT = 1.7f; // Eye level

    public CameraController(Camera camera, InputHandler inputHandler) {
        this.camera = camera;
        this.inputHandler = inputHandler;
    }

    /**
     * Update camera every frame for smooth rotation and positioning
     */
    public void update(float deltaTime, Vector3f playerPosition) {
        // Toggle third-person mode with F5
        boolean f5IsPressed = inputHandler.isKeyPressed(GLFW.GLFW_KEY_F5);
        if (f5IsPressed && !f5WasPressed) {
            thirdPersonMode = !thirdPersonMode;
            logger.info("Camera mode: {}", thirdPersonMode ? "Third Person" : "First Person");
        }
        f5WasPressed = f5IsPressed;

        // Mouse input for camera rotation (EVERY FRAME for smooth rotation)
        double mouseDeltaX = inputHandler.getMouseDeltaX();
        double mouseDeltaY = inputHandler.getMouseDeltaY();

        float yawDelta = (float) mouseDeltaX * mouseSensitivity;
        float pitchDelta = (float) mouseDeltaY * mouseSensitivity;

        Vector3f rotation = camera.getRotation();
        float newYaw = rotation.y + yawDelta;
        float newPitch = Math.max(-89, Math.min(89, rotation.x + pitchDelta));

        // Update camera rotation
        camera.setRotation(newPitch, newYaw, 0);

        // Update camera position based on mode
        updateCameraPosition(playerPosition, newYaw);
    }

    /**
     * Update camera position to follow player
     */
    private void updateCameraPosition(Vector3f playerPos, float yaw) {
        if (thirdPersonMode) {
            // Third-person camera: position behind and above the player
            float yawRad = (float) Math.toRadians(yaw);
            float offsetX = (float) Math.sin(yawRad) * THIRD_PERSON_DISTANCE;
            float offsetZ = (float) Math.cos(yawRad) * THIRD_PERSON_DISTANCE;

            camera.setPosition(
                playerPos.x + offsetX,
                playerPos.y + THIRD_PERSON_HEIGHT,
                playerPos.z + offsetZ
            );
        } else {
            // First-person camera: at eye level
            camera.setPosition(
                playerPos.x,
                playerPos.y + FIRST_PERSON_HEIGHT,
                playerPos.z
            );
        }
    }

    public boolean isThirdPersonMode() {
        return thirdPersonMode;
    }

    public float getYaw() {
        return camera.getRotation().y;
    }

    public float getPitch() {
        return camera.getRotation().x;
    }
}
