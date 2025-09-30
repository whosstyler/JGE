package com.gameengine.client.world;

import com.gameengine.client.renderer.Mesh;
import com.gameengine.client.renderer.Renderer;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents static world geometry (walls, floors, props)
 */
public class StaticGeometry {

    private static final Logger logger = LoggerFactory.getLogger(StaticGeometry.class);

    private final String type;
    private final Vector3f position;
    private final Vector3f dimensions; // width, height, depth
    private final Vector3f color;
    private Mesh mesh;

    public StaticGeometry(String type, Vector3f position, Vector3f dimensions, Vector3f color) {
        this.type = type;
        this.position = position;
        this.dimensions = dimensions;
        this.color = color;
        createMesh();
    }

    /**
     * Create mesh based on geometry type
     */
    private void createMesh() {
        switch (type.toLowerCase()) {
            case "floor":
            case "ground":
                // Create ground plane
                int size = (int) dimensions.x;
                int divisions = Math.max(10, size / 2);
                mesh = Mesh.createGroundPlane(size, divisions);
                break;

            case "wall":
            case "cube":
            case "box":
                // Create colored cube
                mesh = Mesh.createCube(color.x, color.y, color.z);
                break;

            default:
                logger.warn("Unknown geometry type: {}, defaulting to cube", type);
                mesh = Mesh.createCube(color.x, color.y, color.z);
                break;
        }
    }

    /**
     * Render this static geometry
     */
    public void render(Renderer renderer) {
        if (mesh == null) return;

        if (type.equalsIgnoreCase("wall")) {
            // For walls, render individual cubes to fill the dimensions
            for (float x = 0; x < dimensions.x; x += 1) {
                for (float y = 0; y < dimensions.y; y += 1) {
                    for (float z = 0; z < dimensions.z; z += 1) {
                        renderer.render(mesh,
                            position.x + x,
                            position.y + y + 0.5f,
                            position.z + z);
                    }
                }
            }
        } else {
            // For other types, render at position
            renderer.render(mesh, position.x, position.y, position.z);
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
        }
    }

    public String getType() {
        return type;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getDimensions() {
        return dimensions;
    }

    public Vector3f getColor() {
        return color;
    }
}
