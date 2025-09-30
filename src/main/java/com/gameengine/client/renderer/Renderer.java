package com.gameengine.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * OpenGL rendering engine with 3D and 2D (text) rendering capabilities
 */
public class Renderer {

    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

    private long window;
    private int width;
    private int height;
    private String title;
    private ShaderProgram shaderProgram;
    private TextRenderer textRenderer;
    private Camera camera;
    private Matrix4f modelMatrix;
    private FrameBuffer sceneFramebuffer;
    private PostProcessing postProcessing;
    private boolean postProcessingEnabled = false; // DISABLED - for better visibility

    public Renderer(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.modelMatrix = new Matrix4f();
    }

    public void init() throws Exception {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        // Create window
        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Setup resize callback
        glfwSetFramebufferSizeCallback(window, (window, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
            if (textRenderer != null) {
                textRenderer.updateProjection(w, h);
            }
        });

        // Center window
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);

        // Make OpenGL context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable vsync

        // Show window
        glfwShowWindow(window);

        // Initialize OpenGL bindings
        GL.createCapabilities();

        // Enable depth test
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f); // Sky blue background

        // Set line width for line of sight visualization
        glLineWidth(5.0f);

        // Load 3D shader program from files
        shaderProgram = ShaderProgram.fromFiles(
            "src/main/java/com/gameengine/client/renderer/shaders/vertex.glsl",
            "src/main/java/com/gameengine/client/renderer/shaders/fragment.glsl"
        );

        // Create text renderer
        textRenderer = new TextRenderer(width, height);

        // Create camera
        camera = new Camera();

        // Create framebuffer for rendering scene
        sceneFramebuffer = new FrameBuffer(width, height);

        // Initialize post-processing with beautiful effects
        postProcessing = new PostProcessing(width, height);
        setupPostProcessingEffects();

        logger.info("Renderer initialized: {}x{} with post-processing", width, height);
    }

    /**
     * Setup post-processing effects - Disabled for better visibility
     */
    private void setupPostProcessingEffects() {
        // DISABLED ALL EFFECTS - for maximum visibility
        postProcessing.setVignette(false, 0.25f, 0.75f);
        postProcessing.setChromaticAberration(false, 0.0015f);
        postProcessing.setColorGrading(false, 1.15f, 0.05f, 1.1f,
                                      new org.joml.Vector3f(1.0f, 0.98f, 0.95f));
        postProcessing.setEdgeDetection(false, 0.3f);
        postProcessing.setBloom(false, 1.5f, 0.8f);
        postProcessing.setBlur(false, 1.0f);
        postProcessing.setDistortion(false, 0.0f, new org.joml.Vector2f(0.5f, 0.5f));

        logger.info("Post-processing: All effects disabled for maximum visibility - F6 to toggle");
    }

    /**
     * Begin rendering frame - binds scene framebuffer
     */
    public void beginFrame() {
        if (postProcessingEnabled) {
            sceneFramebuffer.bind();
            // Set clear color for framebuffer
            glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
        } else {
            // Make sure clear color is set for direct rendering
            glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * End rendering frame - applies post-processing
     */
    public void endFrame() {
        if (postProcessingEnabled) {
            sceneFramebuffer.unbind();
            postProcessing.process(sceneFramebuffer.getColorTexture());
        }
    }

    /**
     * Legacy clear method - use beginFrame() instead
     */
    @Deprecated
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Mesh mesh, float x, float y, float z) {
        shaderProgram.bind();

        // Set transformation matrices
        modelMatrix.identity().translate(x, y, z);
        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = camera.getProjectionMatrix(
                (float) Math.toRadians(60.0f),
                (float) width / height,
                0.1f,
                1000.0f
        );

        shaderProgram.setUniform("model", modelMatrix);
        shaderProgram.setUniform("view", viewMatrix);
        shaderProgram.setUniform("projection", projectionMatrix);

        mesh.render();

        shaderProgram.unbind();
    }

    /**
     * Render mesh with rotation (for line of sight visualization)
     */
    public void render(Mesh mesh, float x, float y, float z, float pitch, float yaw) {
        shaderProgram.bind();

        // Set transformation matrices with rotation
        modelMatrix.identity()
                .translate(x, y, z)
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(pitch));

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f projectionMatrix = camera.getProjectionMatrix(
                (float) Math.toRadians(60.0f),
                (float) width / height,
                0.1f,
                1000.0f
        );

        shaderProgram.setUniform("model", modelMatrix);
        shaderProgram.setUniform("view", viewMatrix);
        shaderProgram.setUniform("projection", projectionMatrix);

        mesh.render();

        shaderProgram.unbind();
    }

    public void swapBuffers() {
        glfwSwapBuffers(window);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public Camera getCamera() {
        return camera;
    }

    public long getWindow() {
        return window;
    }

    /**
     * Render text with default style (white, normal)
     */
    public void renderText(String text, float x, float y) {
        textRenderer.renderText(text, x, y);
    }

    /**
     * Render text with color
     */
    public void renderText(String text, float x, float y, Vector3f color) {
        textRenderer.renderText(text, x, y, color);
    }

    /**
     * Render text with scale and color
     */
    public void renderText(String text, float x, float y, float scale, Vector3f color) {
        textRenderer.renderText(text, x, y, scale, color);
    }

    /**
     * Render text with full styling
     * Usage: renderText("Hello", 10, 10, 1.0f, new Vector3f(1,1,1), TextStyle.combine(TextStyle.BOLD, TextStyle.OUTLINE))
     */
    public void renderText(String text, float x, float y, float scale, Vector3f color, int styleFlags) {
        textRenderer.renderText(text, x, y, scale, color, styleFlags);
    }

    /**
     * Load a custom font
     * @param name Identifier for the font
     * @param fontPath Path to TTF file
     * @param size Font size in pixels
     */
    public void loadFont(String name, String fontPath, int size) throws Exception {
        textRenderer.loadFont(name, fontPath, size);
    }

    /**
     * Set active font for text rendering
     */
    public void setFont(String name) {
        textRenderer.setFont(name);
    }

    /**
     * Get width of text in pixels
     */
    public float getTextWidth(String text, float scale) {
        return textRenderer.getTextWidth(text, scale);
    }

    public void cleanup() {
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        if (sceneFramebuffer != null) {
            sceneFramebuffer.cleanup();
        }
        if (postProcessing != null) {
            postProcessing.cleanup();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    /**
     * Toggle post-processing effects on/off
     */
    public void setPostProcessingEnabled(boolean enabled) {
        this.postProcessingEnabled = enabled;
        logger.info("Post-processing {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Get post-processing instance for custom effect control
     */
    public PostProcessing getPostProcessing() {
        return postProcessing;
    }
}