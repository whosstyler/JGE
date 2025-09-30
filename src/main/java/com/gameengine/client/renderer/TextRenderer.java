package com.gameengine.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * Advanced text rendering with custom fonts and styling
 */
public class TextRenderer {

    private static final Logger logger = LoggerFactory.getLogger(TextRenderer.class);

    private final ShaderProgram textShader;
    private final Map<String, Font> fonts;
    private Font currentFont;
    private int vao;
    private int vbo;
    private Matrix4f projectionMatrix;

    public TextRenderer(int screenWidth, int screenHeight) throws Exception {
        this.fonts = new HashMap<>();
        this.projectionMatrix = new Matrix4f().ortho(0, screenWidth, screenHeight, 0, -1, 1);

        // Load text shader
        textShader = ShaderProgram.fromFiles(
            "src/main/java/com/gameengine/client/renderer/shaders/text_vertex.glsl",
            "src/main/java/com/gameengine/client/renderer/shaders/text_fragment.glsl"
        );

        // Setup VAO/VBO for text rendering
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES, GL_DYNAMIC_DRAW); // 6 vertices, 4 floats each

        // Position attribute
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Texture coordinate attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Load default font
        currentFont = Font.createDefault(24);
        fonts.put("default", currentFont);

        logger.info("TextRenderer initialized");
    }

    /**
     * Load a custom font
     */
    public void loadFont(String name, String fontPath, int size) throws Exception {
        Font font = new Font(fontPath, size);
        fonts.put(name, font);
        logger.info("Loaded custom font: {}", name);
    }

    /**
     * Set the active font
     */
    public void setFont(String name) {
        Font font = fonts.get(name);
        if (font != null) {
            currentFont = font;
        } else {
            logger.warn("Font not found: {}, using current font", name);
        }
    }

    /**
     * Render text with default style (normal, white)
     */
    public void renderText(String text, float x, float y) {
        renderText(text, x, y, 1.0f, new Vector3f(1, 1, 1), TextStyle.NORMAL.getFlag());
    }

    /**
     * Render text with color
     */
    public void renderText(String text, float x, float y, Vector3f color) {
        renderText(text, x, y, 1.0f, color, TextStyle.NORMAL.getFlag());
    }

    /**
     * Render text with scale and color
     */
    public void renderText(String text, float x, float y, float scale, Vector3f color) {
        renderText(text, x, y, scale, color, TextStyle.NORMAL.getFlag());
    }

    /**
     * Render text with full styling options
     *
     * @param text Text to render
     * @param x X position
     * @param y Y position
     * @param scale Text scale
     * @param color Text color (RGB 0-1)
     * @param styleFlags Combination of TextStyle flags (use TextStyle.combine())
     */
    public void renderText(String text, float x, float y, float scale, Vector3f color, int styleFlags) {
        if (text == null || text.isEmpty()) return;

        // Disable depth test for 2D text rendering
        glDisable(GL_DEPTH_TEST);

        // Enable blending for text
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        textShader.bind();
        textShader.setUniform("projection", projectionMatrix);
        textShader.setUniform("text", 0); // Texture unit 0
        textShader.setUniform("textColor", color);

        // Configure style uniforms
        boolean hasOutline = TextStyle.has(styleFlags, TextStyle.OUTLINE);
        boolean hasShadow = TextStyle.has(styleFlags, TextStyle.SHADOW);
        boolean isBold = TextStyle.has(styleFlags, TextStyle.BOLD);

        textShader.setUniform("hasOutline", hasOutline ? 1 : 0);
        textShader.setUniform("hasShadow", hasShadow ? 1 : 0);

        if (hasOutline) {
            textShader.setUniform("outlineWidth", 0.002f);
            textShader.setUniform("outlineColor", new Vector3f(0, 0, 0));
        }

        if (hasShadow) {
            textShader.setUniform("shadowOffset", 0.002f);
            textShader.setUniform("shadowColor", new Vector3f(0, 0, 0));
        }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, currentFont.getTextureId());
        glBindVertexArray(vao);

        // Adjust scale for bold
        if (isBold) {
            scale *= 1.1f;
        }

        // Render each character
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(x);
            FloatBuffer yb = stack.floats(y);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c >= 128) continue; // Skip non-ASCII

                stbtt_GetBakedQuad(currentFont.getCharData(), currentFont.getAtlasWidth(),
                                   currentFont.getAtlasHeight(), c - 32, xb, yb, quad, false);

                float x0 = quad.x0() * scale;
                float y0 = quad.y0() * scale;
                float x1 = quad.x1() * scale;
                float y1 = quad.y1() * scale;

                float s0 = quad.s0();
                float t0 = quad.t0();
                float s1 = quad.s1();
                float t1 = quad.t1();

                // Update VBO with character quad
                float[] vertices = {
                    x0, y0, s0, t0,
                    x1, y0, s1, t0,
                    x1, y1, s1, t1,

                    x0, y0, s0, t0,
                    x1, y1, s1, t1,
                    x0, y1, s0, t1
                };

                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
                glBindBuffer(GL_ARRAY_BUFFER, 0);

                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
        }

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        textShader.unbind();
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST); // Re-enable depth test for 3D rendering
    }

    /**
     * Get text width in pixels
     */
    public float getTextWidth(String text, float scale) {
        if (text == null || text.isEmpty()) return 0;

        float width = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(0);
            FloatBuffer yb = stack.floats(0);
            STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c >= 128) continue;

                stbtt_GetBakedQuad(currentFont.getCharData(), currentFont.getAtlasWidth(),
                                   currentFont.getAtlasHeight(), c - 32, xb, yb, quad, false);
            }

            width = xb.get(0) * scale;
        }

        return width;
    }

    /**
     * Update projection matrix on screen resize
     */
    public void updateProjection(int width, int height) {
        projectionMatrix.identity().ortho(0, width, height, 0, -1, 1);
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        textShader.cleanup();
        for (Font font : fonts.values()) {
            font.cleanup();
        }
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}