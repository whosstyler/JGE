package com.gameengine.client.renderer;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

/**
 * 3D Mesh with VAO/VBO/EBO
 */
public class Mesh {

     final int vaoId;
    private final int vertexVboId;
    private final int indexVboId;
     final int vertexCount;

    public Mesh(float[] positions, float[] colors, int[] indices) {
        this(positions, colors, generateDefaultNormals(positions, indices), indices);
    }

    public Mesh(float[] positions, float[] colors, float[] normals, int[] indices) {
        vertexCount = indices.length;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Position VBO
        vertexVboId = glGenBuffers();
        FloatBuffer posBuffer = MemoryUtil.memAllocFloat(positions.length);
        posBuffer.put(positions).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vertexVboId);
        glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        MemoryUtil.memFree(posBuffer);

        // Color VBO
        int colorVboId = glGenBuffers();
        FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(colors.length);
        colorBuffer.put(colors).flip();
        glBindBuffer(GL_ARRAY_BUFFER, colorVboId);
        glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);
        MemoryUtil.memFree(colorBuffer);

        // Normal VBO
        int normalVboId = glGenBuffers();
        FloatBuffer normalBuffer = MemoryUtil.memAllocFloat(normals.length);
        normalBuffer.put(normals).flip();
        glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);
        MemoryUtil.memFree(normalBuffer);

        // Index VBO
        indexVboId = glGenBuffers();
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Generate default normals (all pointing up) if not provided
     */
    private static float[] generateDefaultNormals(float[] positions, int[] indices) {
        int vertexCount = positions.length / 3;
        float[] normals = new float[vertexCount * 3];

        // Simple default: all normals point up
        for (int i = 0; i < vertexCount; i++) {
            normals[i * 3] = 0;
            normals[i * 3 + 1] = 1;
            normals[i * 3 + 2] = 0;
        }

        return normals;
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vertexVboId);
        glDeleteBuffers(indexVboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    /**
     * Create a simple cube mesh
     */
    public static Mesh createCube(float r, float g, float b) {
        float[] positions = {
                // Front face
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                // Back face
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f
        };

        float[] colors = new float[24]; // 8 vertices * 3 components
        for (int i = 0; i < 8; i++) {
            colors[i * 3] = r;
            colors[i * 3 + 1] = g;
            colors[i * 3 + 2] = b;
        }

        int[] indices = {
                // Front face
                0, 1, 2, 2, 3, 0,
                // Back face
                4, 6, 5, 6, 4, 7,
                // Top face
                3, 2, 6, 6, 7, 3,
                // Bottom face
                4, 5, 1, 1, 0, 4,
                // Right face
                1, 5, 6, 6, 2, 1,
                // Left face
                4, 0, 3, 3, 7, 4
        };

        return new Mesh(positions, colors, indices);
    }

    /**
     * Create a line (for line of sight visualization)
     */
    public static Mesh createLine(float r, float g, float b) {
        float[] positions = {
                0, 0, 0,  // Start
                0, 0, -50 // End (50 units forward - much longer)
        };

        float[] colors = new float[6]; // 2 vertices * 3 components
        for (int i = 0; i < 2; i++) {
            colors[i * 3] = r;
            colors[i * 3 + 1] = g;
            colors[i * 3 + 2] = b;
        }

        int[] indices = {0, 1};

        return new Mesh(positions, colors, indices) {
            @Override
            public void render() {
                glBindVertexArray(vaoId);
                glDrawElements(GL_LINES, vertexCount, GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }
        };
    }

    /**
     * Create a flat ground plane with Unreal Engine-style grid
     */
    public static Mesh createGroundPlane(float size, int divisions) {
        int vertexCount = (divisions + 1) * (divisions + 1);
        float[] positions = new float[vertexCount * 3];
        float[] colors = new float[vertexCount * 3];

        float step = size / divisions;
        int index = 0;

        // Bright grid colors for visibility
        float baseGrey = 0.5f; // Bright base
        float lineGrey = 0.6f; // Even brighter grid lines

        for (int z = 0; z <= divisions; z++) {
            for (int x = 0; x <= divisions; x++) {
                float xPos = (x * step) - (size / 2);
                float zPos = (z * step) - (size / 2);

                positions[index * 3] = xPos;
                positions[index * 3 + 1] = 0;
                positions[index * 3 + 2] = zPos;

                // Unreal-style grid: lighter on edges of each grid square
                boolean isGridLine = (x % 1 == 0) || (z % 1 == 0);
                float brightness = isGridLine ? lineGrey : baseGrey;

                // Make every 10th line brighter (major grid lines)
                if ((x % 10 == 0) || (z % 10 == 0)) {
                    brightness = 0.8f;
                }

                colors[index * 3] = brightness;
                colors[index * 3 + 1] = brightness;
                colors[index * 3 + 2] = brightness;

                index++;
            }
        }

        // Generate indices for triangles
        int[] indices = new int[divisions * divisions * 6];
        int idx = 0;
        for (int z = 0; z < divisions; z++) {
            for (int x = 0; x < divisions; x++) {
                int topLeft = z * (divisions + 1) + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * (divisions + 1) + x;
                int bottomRight = bottomLeft + 1;

                // First triangle
                indices[idx++] = topLeft;
                indices[idx++] = bottomLeft;
                indices[idx++] = topRight;

                // Second triangle
                indices[idx++] = topRight;
                indices[idx++] = bottomLeft;
                indices[idx++] = bottomRight;
            }
        }

        return new Mesh(positions, colors, indices);
    }
}