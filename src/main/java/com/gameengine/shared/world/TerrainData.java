package com.gameengine.shared.world;

/**
 * Stores procedurally generated terrain heightmap data
 */
public class TerrainData {

    private final int width;
    private final int depth;
    private final float[][] heightMap;
    private final long seed;

    public TerrainData(int width, int depth, long seed) {
        this.width = width;
        this.depth = depth;
        this.seed = seed;
        this.heightMap = new float[width][depth];
    }

    /**
     * Set height at specific grid position
     */
    public void setHeight(int x, int z, float height) {
        if (x >= 0 && x < width && z >= 0 && z < depth) {
            heightMap[x][z] = height;
        }
    }

    /**
     * Get height at specific grid position
     */
    public float getHeight(int x, int z) {
        if (x >= 0 && x < width && z >= 0 && z < depth) {
            return heightMap[x][z];
        }
        return 0;
    }

    /**
     * Get interpolated height at world position
     */
    public float getHeightAtWorldPos(float worldX, float worldZ) {
        // Convert world coordinates to grid coordinates
        float gridX = worldX + width / 2.0f;
        float gridZ = worldZ + depth / 2.0f;

        // Bilinear interpolation
        int x0 = (int) Math.floor(gridX);
        int z0 = (int) Math.floor(gridZ);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float fx = gridX - x0;
        float fz = gridZ - z0;

        float h00 = getHeight(x0, z0);
        float h10 = getHeight(x1, z0);
        float h01 = getHeight(x0, z1);
        float h11 = getHeight(x1, z1);

        float h0 = h00 * (1 - fx) + h10 * fx;
        float h1 = h01 * (1 - fx) + h11 * fx;

        return h0 * (1 - fz) + h1 * fz;
    }

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
    }

    public long getSeed() {
        return seed;
    }

    public float[][] getHeightMap() {
        return heightMap;
    }
}
