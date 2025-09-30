package com.gameengine.client.renderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * TrueType font with bitmap texture atlas
 */
public class Font {

    private static final Logger logger = LoggerFactory.getLogger(Font.class);

    private final String name;
    private final int size;
    private final int textureId;
    private final STBTTBakedChar.Buffer charData;
    private final int atlasWidth;
    private final int atlasHeight;

    public Font(String fontPath, int size) throws IOException {
        this.name = fontPath;
        this.size = size;
        this.atlasWidth = 512;
        this.atlasHeight = 512;

        // Load font file
        byte[] fontBytes = Files.readAllBytes(Paths.get(fontPath));
        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
        fontBuffer.put(fontBytes);
        fontBuffer.flip();

        // Create bitmap for font atlas
        ByteBuffer bitmap = BufferUtils.createByteBuffer(atlasWidth * atlasHeight);

        // Bake font into bitmap (ASCII 32-126)
        charData = STBTTBakedChar.malloc(96);
        stbtt_BakeFontBitmap(fontBuffer, size, bitmap, atlasWidth, atlasHeight, 32, charData);

        // Create OpenGL texture (use GL_RED for OpenGL 3.3+ core profile)
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, atlasWidth, atlasHeight, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        logger.info("Loaded font: {} (size: {}, atlas: {}x{})", fontPath, size, atlasWidth, atlasHeight);
    }

    /**
     * Create default font (requires a default TTF file)
     */
    public static Font createDefault(int size) {
        try {
            // Try to load a system font
            String[] possibleFonts = {
                "C:\\Windows\\Fonts\\Arial.ttf",
                "C:\\Windows\\Fonts\\Consolas.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/System/Library/Fonts/Helvetica.ttc"
            };

            for (String fontPath : possibleFonts) {
                if (Files.exists(Paths.get(fontPath))) {
                    return new Font(fontPath, size);
                }
            }

            throw new RuntimeException("No default font found");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load default font", e);
        }
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public int getTextureId() {
        return textureId;
    }

    public STBTTBakedChar.Buffer getCharData() {
        return charData;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

    public void cleanup() {
        glDeleteTextures(textureId);
        charData.free();
    }
}