package com.gameengine.client.assets;

import com.gameengine.client.renderer.Mesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Model Loader - Loads 3D models from various formats
 */
public class ModelLoader {

    private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

    /**
     * Load OBJ file format
     * Simple OBJ parser supporting vertices, normals, and faces
     */
    public static Mesh loadOBJ(String filePath) throws IOException {
        logger.info("Loading OBJ model: {}", filePath);

        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip comments and empty lines
                }

                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v": // Vertex position
                        vertices.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                        });
                        break;

                    case "vn": // Vertex normal
                        normals.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                        });
                        break;

                    case "f": // Face (triangle)
                        // Parse face format: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3
                        int[] faceIndices = new int[tokens.length - 1];
                        for (int i = 1; i < tokens.length; i++) {
                            String[] parts = tokens[i].split("/");
                            faceIndices[i - 1] = Integer.parseInt(parts[0]) - 1; // OBJ is 1-indexed
                        }
                        faces.add(faceIndices);
                        break;
                }
            }
        }

        // Convert to arrays for Mesh
        float[] positionArray = new float[faces.size() * 9]; // 3 vertices per face * 3 coords
        float[] normalArray = new float[faces.size() * 9];
        float[] colorArray = new float[faces.size() * 9]; // Default white color
        int[] indexArray = new int[faces.size() * 3];

        int vertexIndex = 0;
        int indexValue = 0;

        for (int[] face : faces) {
            for (int vertIdx : face) {
                if (vertIdx < vertices.size()) {
                    float[] vertex = vertices.get(vertIdx);
                    positionArray[vertexIndex * 3] = vertex[0];
                    positionArray[vertexIndex * 3 + 1] = vertex[1];
                    positionArray[vertexIndex * 3 + 2] = vertex[2];

                    // Default white color
                    colorArray[vertexIndex * 3] = 1.0f;
                    colorArray[vertexIndex * 3 + 1] = 1.0f;
                    colorArray[vertexIndex * 3 + 2] = 1.0f;

                    // Default normal (up)
                    normalArray[vertexIndex * 3] = 0.0f;
                    normalArray[vertexIndex * 3 + 1] = 1.0f;
                    normalArray[vertexIndex * 3 + 2] = 0.0f;

                    indexArray[indexValue++] = vertexIndex;
                    vertexIndex++;
                }
            }
        }

        logger.info("OBJ loaded: {} vertices, {} faces", vertices.size(), faces.size());
        return new Mesh(positionArray, colorArray, normalArray, indexArray);
    }
}
