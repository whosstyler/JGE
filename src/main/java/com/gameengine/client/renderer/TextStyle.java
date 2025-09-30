package com.gameengine.client.renderer;

/**
 * Text rendering styles with support for combining multiple styles
 */
public enum TextStyle {
    NORMAL(0),
    BOLD(1),
    ITALIC(2),
    OUTLINE(4),
    SHADOW(8),
    UNDERLINE(16);

    private final int flag;

    TextStyle(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }

    /**
     * Combine multiple text styles
     */
    public static int combine(TextStyle... styles) {
        int combined = 0;
        for (TextStyle style : styles) {
            combined |= style.flag;
        }
        return combined;
    }

    /**
     * Check if a combined style contains a specific style
     */
    public static boolean has(int combined, TextStyle style) {
        return (combined & style.flag) != 0;
    }
}