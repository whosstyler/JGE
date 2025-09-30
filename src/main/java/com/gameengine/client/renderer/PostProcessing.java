package com.gameengine.client.renderer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Post-processing effects system
 */
public class PostProcessing {

    private static final Logger logger = LoggerFactory.getLogger(PostProcessing.class);

    private final int screenWidth;
    private final int screenHeight;
    
    // Screen quad for full-screen effects
    private int quadVAO;
    private int quadVBO;
    
    // Framebuffers for ping-pong rendering
    private FrameBuffer pingBuffer;
    private FrameBuffer pongBuffer;
    
    // Shader programs for different effects
    private ShaderProgram passthroughShader; // Simple texture display
    private ShaderProgram blurShader;
    private ShaderProgram bloomExtractShader;
    private ShaderProgram bloomCompositeShader;
    private ShaderProgram distortionShader;
    private ShaderProgram chromaticAberrationShader;
    private ShaderProgram vignetteShader;
    private ShaderProgram colorGradingShader;
    private ShaderProgram edgeDetectionShader;
    
    // Effect parameters - SAFE effects enabled by default
    private boolean bloomEnabled = false;
    private float bloomThreshold = 1.5f;  // Higher threshold = less bloom
    private float bloomExposure = 0.8f;   // Lower exposure = darker bloom

    private boolean blurEnabled = false;
    private float blurRadius = 1.0f;

    private boolean distortionEnabled = false;
    private float distortionIntensity = 0.1f;
    private Vector2f distortionCenter = new Vector2f(0.5f, 0.5f);

    private boolean chromaticAberrationEnabled = true;  // ENABLED - very subtle
    private float aberrationStrength = 0.0015f;         // Very subtle RGB split

    private boolean vignetteEnabled = true;             // ENABLED - cinematic look
    private float vignetteStrength = 0.25f;             // Subtle vignette
    private float vignetteRadius = 0.75f;

    private boolean colorGradingEnabled = true;         // ENABLED - subtle color enhancement
    private float contrast = 1.15f;                     // Slight contrast boost
    private float brightness = 0.05f;                   // Slight brightness boost
    private float saturation = 1.1f;                    // Slight saturation boost
    private Vector3f colorFilter = new Vector3f(1.0f, 0.98f, 0.95f); // Slight warm tint

    private boolean edgeDetectionEnabled = true;        // ENABLED - subtle edge highlight
    private float edgeThreshold = 0.3f;                 // Higher threshold = less visible edges

    public PostProcessing(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        try {
            init();
        } catch (Exception e) {
            logger.error("Failed to initialize post-processing", e);
            throw new RuntimeException(e);
        }
    }

    private void init() throws Exception {
        // Create screen quad
        createScreenQuad();
        
        // Create framebuffers
        pingBuffer = new FrameBuffer(screenWidth, screenHeight);
        pongBuffer = new FrameBuffer(screenWidth, screenHeight);
        
        // Load shaders
        loadShaders();
        
        logger.info("Post-processing initialized");
    }

    private void createScreenQuad() {
        // Full-screen quad vertices
        float[] vertices = {
            // positions   // texCoords
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,

            -1.0f,  1.0f,  0.0f, 1.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f
        };

        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();

        glBindVertexArray(quadVAO);
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Position attribute
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);

        // Texture coordinate attribute
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        glBindVertexArray(0);
    }

    private void loadShaders() throws Exception {
        String shaderPath = "src/main/java/com/gameengine/client/renderer/shaders/";

        // Simple passthrough shader for displaying texture
        passthroughShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "passthrough_fragment.glsl"
        );

        blurShader = ShaderProgram.fromFiles(
            shaderPath + "blur_vertex.glsl",
            shaderPath + "blur_fragment.glsl"
        );
        
        bloomExtractShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "bloom_extract_fragment.glsl"
        );
        
        bloomCompositeShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "bloom_fragment.glsl"
        );
        
        distortionShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "distortion_fragment.glsl"
        );
        
        chromaticAberrationShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "chromatic_aberration_fragment.glsl"
        );
        
        vignetteShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "vignette_fragment.glsl"
        );
        
        colorGradingShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "color_grading_fragment.glsl"
        );
        
        edgeDetectionShader = ShaderProgram.fromFiles(
            shaderPath + "screen_vertex.glsl",
            shaderPath + "edge_detection_fragment.glsl"
        );
    }

    /**
     * Apply all enabled post-processing effects to the input texture
     */
    public void process(int inputTexture) {
        // Disable depth testing for post-processing
        glDisable(GL_DEPTH_TEST);
        
        FrameBuffer currentInput = null;
        FrameBuffer currentOutput = pingBuffer;
        
        // Start with input texture
        boolean firstPass = true;
        
        // Bloom effect
        if (bloomEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processBloom(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Blur effect
        if (blurEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processBlur(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Distortion effect
        if (distortionEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processDistortion(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Chromatic aberration
        if (chromaticAberrationEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processChromaticAberration(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Vignette effect
        if (vignetteEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processVignette(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Color grading
        if (colorGradingEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processColorGrading(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Edge detection
        if (edgeDetectionEnabled) {
            currentOutput = firstPass ? pingBuffer : (currentInput == pingBuffer ? pongBuffer : pingBuffer);
            processEdgeDetection(firstPass ? inputTexture : currentInput.getColorTexture(), currentOutput);
            currentInput = currentOutput;
            firstPass = false;
        }
        
        // Final pass to screen using passthrough shader
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT);

        // Use passthrough shader to display the texture
        passthroughShader.bind();
        passthroughShader.setUniform("screenTexture", 0);

        glActiveTexture(GL_TEXTURE0);
        if (firstPass) {
            glBindTexture(GL_TEXTURE_2D, inputTexture);
        } else {
            glBindTexture(GL_TEXTURE_2D, currentInput.getColorTexture());
        }

        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        passthroughShader.unbind();

        // Re-enable depth testing
        glEnable(GL_DEPTH_TEST);
    }

    private void processBloom(int inputTexture, FrameBuffer output) {
        // Extract bright areas
        FrameBuffer brightBuffer = pingBuffer == output ? pongBuffer : pingBuffer;
        brightBuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        bloomExtractShader.bind();
        bloomExtractShader.setUniform("image", 0);
        bloomExtractShader.setUniform("threshold", bloomThreshold);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        bloomExtractShader.unbind();
        
        // Blur the bright areas (simplified - you'd normally do multiple passes)
        processBlur(brightBuffer.getColorTexture(), output);
        
        // Composite with original
        output.bind();
        bloomCompositeShader.bind();
        bloomCompositeShader.setUniform("scene", 0);
        bloomCompositeShader.setUniform("bloomBlur", 1);
        bloomCompositeShader.setUniform("bloom", true);
        bloomCompositeShader.setUniform("exposure", bloomExposure);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, output.getColorTexture());
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        bloomCompositeShader.unbind();
    }

    private void processBlur(int inputTexture, FrameBuffer output) {
        // Two-pass Gaussian blur
        FrameBuffer tempBuffer = pingBuffer == output ? pongBuffer : pingBuffer;
        
        // Horizontal pass
        tempBuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        blurShader.bind();
        blurShader.setUniform("screenTexture", 0);
        blurShader.setUniform("horizontal", true);
        blurShader.setUniform("blurRadius", blurRadius);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        // Vertical pass
        output.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        blurShader.setUniform("horizontal", false);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tempBuffer.getColorTexture());
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        blurShader.unbind();
    }

    private void processDistortion(int inputTexture, FrameBuffer output) {
        output.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        distortionShader.bind();
        distortionShader.setUniform("screenTexture", 0);
        distortionShader.setUniform("time", System.currentTimeMillis() / 1000.0f);
        distortionShader.setUniform("intensity", distortionIntensity);
        distortionShader.setUniform("center", distortionCenter);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        distortionShader.unbind();
    }

    private void processChromaticAberration(int inputTexture, FrameBuffer output) {
        output.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        chromaticAberrationShader.bind();
        chromaticAberrationShader.setUniform("screenTexture", 0);
        chromaticAberrationShader.setUniform("aberrationStrength", aberrationStrength);
        chromaticAberrationShader.setUniform("center", new Vector2f(0.5f, 0.5f));
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        chromaticAberrationShader.unbind();
    }

    private void processVignette(int inputTexture, FrameBuffer output) {
        output.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        vignetteShader.bind();
        vignetteShader.setUniform("screenTexture", 0);
        vignetteShader.setUniform("vignetteStrength", vignetteStrength);
        vignetteShader.setUniform("vignetteRadius", vignetteRadius);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        vignetteShader.unbind();
    }

    private void processColorGrading(int inputTexture, FrameBuffer output) {
        output.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        colorGradingShader.bind();
        colorGradingShader.setUniform("screenTexture", 0);
        colorGradingShader.setUniform("contrast", contrast);
        colorGradingShader.setUniform("brightness", brightness);
        colorGradingShader.setUniform("saturation", saturation);
        colorGradingShader.setUniform("colorFilter", colorFilter);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        colorGradingShader.unbind();
    }

    private void processEdgeDetection(int inputTexture, FrameBuffer output) {
        output.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        
        edgeDetectionShader.bind();
        edgeDetectionShader.setUniform("screenTexture", 0);
        edgeDetectionShader.setUniform("edgeThreshold", edgeThreshold);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        
        edgeDetectionShader.unbind();
    }

    // Effect control methods
    public void setBloom(boolean enabled, float threshold, float exposure) {
        this.bloomEnabled = enabled;
        this.bloomThreshold = threshold;
        this.bloomExposure = exposure;
    }

    public void setBlur(boolean enabled, float radius) {
        this.blurEnabled = enabled;
        this.blurRadius = radius;
    }

    public void setDistortion(boolean enabled, float intensity, Vector2f center) {
        this.distortionEnabled = enabled;
        this.distortionIntensity = intensity;
        this.distortionCenter.set(center);
    }

    public void setChromaticAberration(boolean enabled, float strength) {
        this.chromaticAberrationEnabled = enabled;
        this.aberrationStrength = strength;
    }

    public void setVignette(boolean enabled, float strength, float radius) {
        this.vignetteEnabled = enabled;
        this.vignetteStrength = strength;
        this.vignetteRadius = radius;
    }

    public void setColorGrading(boolean enabled, float contrast, float brightness, float saturation, Vector3f colorFilter) {
        this.colorGradingEnabled = enabled;
        this.contrast = contrast;
        this.brightness = brightness;
        this.saturation = saturation;
        this.colorFilter.set(colorFilter);
    }

    public void setEdgeDetection(boolean enabled, float threshold) {
        this.edgeDetectionEnabled = enabled;
        this.edgeThreshold = threshold;
    }

    public void cleanup() {
        if (quadVAO != 0) glDeleteVertexArrays(quadVAO);
        if (quadVBO != 0) glDeleteBuffers(quadVBO);

        if (pingBuffer != null) pingBuffer.cleanup();
        if (pongBuffer != null) pongBuffer.cleanup();

        if (passthroughShader != null) passthroughShader.cleanup();
        if (blurShader != null) blurShader.cleanup();
        if (bloomExtractShader != null) bloomExtractShader.cleanup();
        if (bloomCompositeShader != null) bloomCompositeShader.cleanup();
        if (distortionShader != null) distortionShader.cleanup();
        if (chromaticAberrationShader != null) chromaticAberrationShader.cleanup();
        if (vignetteShader != null) vignetteShader.cleanup();
        if (colorGradingShader != null) colorGradingShader.cleanup();
        if (edgeDetectionShader != null) edgeDetectionShader.cleanup();
    }
}
