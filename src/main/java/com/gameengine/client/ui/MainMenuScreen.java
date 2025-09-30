package com.gameengine.client.ui;

import com.gameengine.client.renderer.Renderer;
import com.gameengine.client.renderer.TextStyle;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;

/**
 * Main menu screen displayed before connecting to server
 */
public class MainMenuScreen {

    private static final Logger logger = LoggerFactory.getLogger(MainMenuScreen.class);

    private final long window;
    private final Renderer renderer;
    private String statusText = "Press ENTER to connect";
    private boolean connecting = false;
    private float animationTime = 0;
    private boolean enterWasPressed = false;

    public MainMenuScreen(long window, Renderer renderer) {
        this.window = window;
        this.renderer = renderer;
    }

    /**
     * Render the main menu
     * @return true if user wants to connect, false otherwise
     */
    public boolean render(float deltaTime) {
        animationTime += deltaTime;

        // Pulsing effect
        float pulse = (float) (Math.sin(animationTime * 2) * 0.3 + 0.5);

        // Set background color with pulse
        if (connecting) {
            // Connecting - lighter background
            glClearColor(0.1f + pulse * 0.05f, 0.1f + pulse * 0.05f, 0.12f + pulse * 0.08f, 1.0f);
        } else {
            // Waiting - subtle pulse
            glClearColor(0.1f + pulse * 0.02f, 0.1f + pulse * 0.02f, 0.12f + pulse * 0.03f, 1.0f);
        }

        // Clear screen
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Get screen dimensions (assuming 1280x720 for now - could be improved)
        int screenWidth = 1280;
        int screenHeight = 720;

        // Render title
        String title = "GAME ENGINE";
        float titleScale = 3.0f;
        float titleWidth = renderer.getTextWidth(title, titleScale);
        float titleX = (screenWidth - titleWidth) / 2;
        float titleY = 150;

        int titleStyle = TextStyle.combine(TextStyle.BOLD, TextStyle.OUTLINE);
        Vector3f titleColor = new Vector3f(0.2f, 0.6f, 1.0f); // Blue
        renderer.renderText(title, titleX, titleY, titleScale, titleColor, titleStyle);

        // Render status text (centered, pulsing)
        float statusScale = 1.5f;
        float statusWidth = renderer.getTextWidth(statusText, statusScale);
        float statusX = (screenWidth - statusWidth) / 2;
        float statusY = 400;

        Vector3f statusColor = new Vector3f(pulse, pulse, pulse); // Pulsing white
        int statusStyle = connecting ? TextStyle.BOLD.getFlag() : TextStyle.NORMAL.getFlag();
        renderer.renderText(statusText, statusX, statusY, statusScale, statusColor, statusStyle);

        // Render instructions if not connecting
        if (!connecting) {
            String instructions = "W A S D - Move | SPACE - Jump | F5 - Camera | F6 - Effects";
            float instScale = 0.8f;
            float instWidth = renderer.getTextWidth(instructions, instScale);
            float instX = (screenWidth - instWidth) / 2;
            float instY = 550;

            Vector3f instColor = new Vector3f(0.6f, 0.6f, 0.6f); // Gray
            renderer.renderText(instructions, instX, instY, instScale, instColor);
        }

        // Check for enter key (with debouncing)
        boolean enterIsPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;

        if (enterIsPressed && !enterWasPressed && !connecting) {
            connecting = true;
            statusText = "Connecting to server...";
            enterWasPressed = true;
            return true;
        }

        if (!enterIsPressed) {
            enterWasPressed = false;
        }

        return false;
    }


    public void setStatusText(String text) {
        this.statusText = text;
    }

    public boolean isConnecting() {
        return connecting;
    }
}