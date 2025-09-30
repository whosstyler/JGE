package com.gameengine.client.assets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates simple test textures programmatically
 */
public class TextureGenerator {

    /**
     * Generate a checkerboard texture for testing
     */
    public static void generateCheckerboard(String outputPath, int size, int checkSize) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Draw checkerboard pattern
        for (int y = 0; y < size; y += checkSize) {
            for (int x = 0; x < size; x += checkSize) {
                boolean isWhite = ((x / checkSize) + (y / checkSize)) % 2 == 0;
                g.setColor(isWhite ? Color.WHITE : Color.DARK_GRAY);
                g.fillRect(x, y, checkSize, checkSize);
            }
        }

        g.dispose();

        // Save to file
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, "PNG", outputFile);
    }

    /**
     * Generate a brick texture
     */
    public static void generateBrickTexture(String outputPath, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Brick colors
        Color brickColor = new Color(139, 69, 19);
        Color mortarColor = new Color(200, 200, 200);
        Color brickVariation = new Color(160, 82, 45);

        // Background (mortar)
        g.setColor(mortarColor);
        g.fillRect(0, 0, width, height);

        int brickWidth = width / 4;
        int brickHeight = height / 8;
        int mortarSize = 4;

        // Draw bricks
        for (int row = 0; row < 8; row++) {
            int offsetX = (row % 2 == 0) ? 0 : brickWidth / 2;
            for (int col = -1; col < 5; col++) {
                int x = col * (brickWidth + mortarSize) + offsetX;
                int y = row * (brickHeight + mortarSize);

                // Alternate brick color for variation
                g.setColor((row + col) % 2 == 0 ? brickColor : brickVariation);
                g.fillRect(x, y, brickWidth, brickHeight);

                // Add some detail lines
                g.setColor(brickColor.darker());
                g.drawLine(x + 5, y + brickHeight / 2, x + brickWidth - 5, y + brickHeight / 2);
            }
        }

        g.dispose();

        // Save to file
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, "PNG", outputFile);
    }

    /**
     * Generate a stone texture
     */
    public static void generateStoneTexture(String outputPath, int size) throws IOException {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Base stone color
        Color baseColor = new Color(120, 120, 130);
        g.setColor(baseColor);
        g.fillRect(0, 0, size, size);

        // Add noise/variation
        java.util.Random rand = new java.util.Random(12345);
        for (int i = 0; i < 1000; i++) {
            int x = rand.nextInt(size);
            int y = rand.nextInt(size);
            int brightness = 100 + rand.nextInt(60);
            g.setColor(new Color(brightness, brightness, brightness + 10));
            g.fillOval(x, y, rand.nextInt(8) + 2, rand.nextInt(8) + 2);
        }

        g.dispose();

        // Save to file
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ImageIO.write(image, "PNG", outputFile);
    }

    public static void main(String[] args) {
        try {
            String baseDir = "textures/";

            System.out.println("Generating test textures...");

            generateCheckerboard(baseDir + "checkerboard.png", 512, 64);
            System.out.println("Generated: " + baseDir + "checkerboard.png");

            generateBrickTexture(baseDir + "brick.png", 512, 512);
            System.out.println("Generated: " + baseDir + "brick.png");

            generateStoneTexture(baseDir + "stone.png", 512);
            System.out.println("Generated: " + baseDir + "stone.png");

            System.out.println("All textures generated successfully!");
        } catch (IOException e) {
            System.err.println("Failed to generate textures: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
