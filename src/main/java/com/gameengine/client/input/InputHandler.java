package com.gameengine.client.input;

import org.lwjgl.glfw.GLFW;

/**
 * Input handling and state tracking
 */
public class InputHandler {

    private long window;
    private double mouseX, mouseY;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse;

    public InputHandler() {
        firstMouse = true;
    }

    public void init(long window) {
        this.window = window;

        // Mouse callback
        GLFW.glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }
            mouseX = xpos;
            mouseY = ypos;
        });

        // Capture mouse
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    /**
     * Poll key state directly (more reliable for game input)
     */
    public boolean isKeyPressed(int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public double getMouseDeltaX() {
        double delta = mouseX - lastMouseX;
        lastMouseX = mouseX;
        return delta;
    }

    public double getMouseDeltaY() {
        double delta = mouseY - lastMouseY;
        lastMouseY = mouseY;
        return delta;
    }

    public void reset() {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
}