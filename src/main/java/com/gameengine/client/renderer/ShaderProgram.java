package com.gameengine.client.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL20.*;

/**
 * OpenGL shader program wrapper
 */
public class ShaderProgram {

    private static final Logger logger = LoggerFactory.getLogger(ShaderProgram.class);

    private final int programId;

    /**
     * Create shader from source code strings
     */
    public ShaderProgram(String vertexShaderCode, String fragmentShaderCode) throws Exception {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create shader program");
        }

        int vertexShaderId = createShader(vertexShaderCode, GL_VERTEX_SHADER);
        int fragmentShaderId = createShader(fragmentShaderCode, GL_FRAGMENT_SHADER);

        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking shader: " + glGetProgramInfoLog(programId, 1024));
        }

        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            logger.warn("Warning validating shader: " + glGetProgramInfoLog(programId, 1024));
        }
    }

    private int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader: " + glGetShaderInfoLog(shaderId, 1024));
        }

        return shaderId;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniform(String uniformName, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            int location = glGetUniformLocation(programId, uniformName);
            glUniformMatrix4fv(location, false, fb);
        }
    }

    public void setUniform(String uniformName, int value) {
        int location = glGetUniformLocation(programId, uniformName);
        glUniform1i(location, value);
    }

    public void setUniform(String uniformName, float value) {
        int location = glGetUniformLocation(programId, uniformName);
        glUniform1f(location, value);
    }

    public void setUniform(String uniformName, Vector3f value) {
        int location = glGetUniformLocation(programId, uniformName);
        glUniform3f(location, value.x, value.y, value.z);
    }

    // vector2f 
    public void setUniform(String uniformName, Vector2f value) {
        int location = glGetUniformLocation(programId, uniformName);
        glUniform2f(location, value.x, value.y);
    }

    public void setUniform(String uniformName, boolean value) {
        int location = glGetUniformLocation(programId, uniformName);
        glUniform1i(location, value ? 1 : 0);
    }



    /**
     * Create shader program from files
     */
    public static ShaderProgram fromFiles(String vertexPath, String fragmentPath) throws Exception {
        String vertexCode = readFile(vertexPath);
        String fragmentCode = readFile(fragmentPath);
        return new ShaderProgram(vertexCode, fragmentCode);
    }

    /**
     * Read shader file
     */
    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
}