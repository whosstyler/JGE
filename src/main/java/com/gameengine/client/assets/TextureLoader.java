package com.gameengine.client.assets;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * Texture Loader - Loads textures from image files (PNG, JPG, etc.)
 */
public class TextureLoader {

    private static final Logger logger = LoggerFactory.getLogger(TextureLoader.class);

    /**
     * Load texture from file (supports PNG, JPG, BMP, TGA)
     * @param filePath Path to image file
     * @return OpenGL texture ID
     */
    public static int loadTexture(String filePath) throws Exception {
        logger.info("Loading texture: {}", filePath);

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Load image using STB
        ByteBuffer imageBuffer = STBImage.stbi_load(filePath, width, height, channels, 4); // Force RGBA

        if (imageBuffer == null) {
            throw new Exception("Failed to load texture: " + filePath + " - " + STBImage.stbi_failure_reason());
        }

        int w = width.get(0);
        int h = height.get(0);

        // Create OpenGL texture
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Upload texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        // Free image data
        STBImage.stbi_image_free(imageBuffer);

        logger.info("Texture loaded: {}x{} pixels, ID: {}", w, h, textureId);
        return textureId;
    }

    /**
     * Delete a texture
     */
    public static void deleteTexture(int textureId) {
        glDeleteTextures(textureId);
    }

    /**
     * Bind texture for rendering
     */
    public static void bindTexture(int textureId) {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    /**
     * Unbind texture
     */
    public static void unbindTexture() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
