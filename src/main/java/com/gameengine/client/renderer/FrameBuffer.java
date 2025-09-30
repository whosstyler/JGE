package com.gameengine.client.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.GL11;

/**
 * OpenGL framebuffer wrapper for render-to-texture
 */
public class FrameBuffer {

    private static final Logger logger = LoggerFactory.getLogger(FrameBuffer.class);

    private final int width;
    private final int height;
    private int framebufferId;
    private int colorTexture;
    private int depthTexture;

    public FrameBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        createFrameBuffer();
    }

    private void createFrameBuffer() {
        // Create framebuffer
        framebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);

        // Create color texture (use RGBA8 for compatibility)
        colorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);

        // Create depth texture
        depthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);

        // Check if framebuffer is complete
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        logger.debug("Created framebuffer {}x{}", width, height);
    }

    /**
     * Bind this framebuffer for rendering
     */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glViewport(0, 0, width, height);
    }

    /**
     * Unbind framebuffer (return to default)
     */
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Bind the color texture for reading
     */
    public void bindColorTexture(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
    }

    /**
     * Bind the depth texture for reading
     */
    public void bindDepthTexture(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getColorTexture() { return colorTexture; }
    public int getDepthTexture() { return depthTexture; }

    /**
     * Cleanup OpenGL resources
     */
    public void cleanup() {
        if (framebufferId != 0) {
            glDeleteFramebuffers(framebufferId);
        }
        if (colorTexture != 0) {
            glDeleteTextures(colorTexture);
        }
        if (depthTexture != 0) {
            glDeleteTextures(depthTexture);
        }
    }
}
