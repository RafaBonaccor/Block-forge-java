package blockforge.world;

import java.awt.Color;

public enum BlockType {
    GRASS("Erba", new Color(108, 185, 91), 0.45, true),
    DIRT("Terra", new Color(122, 81, 52), 0.60, true),
    STONE("Pietra", new Color(141, 149, 160), 1.65, true),
    WOOD("Legno", new Color(156, 106, 54), 1.10, true),
    GLOW("Luce", new Color(246, 214, 110), 0.80, true),
    WATER("Acqua", new Color(48, 142, 218, 155), 0.25, false);

    private final String label;
    private final Color topColor;
    private final double breakDurationSeconds;
    private final boolean solid;

    BlockType(String label, Color topColor, double breakDurationSeconds, boolean solid) {
        this.label = label;
        this.topColor = topColor;
        this.breakDurationSeconds = breakDurationSeconds;
        this.solid = solid;
    }

    public String label() {
        return label;
    }

    public Color topColor() {
        return topColor;
    }

    public double breakDurationSeconds() {
        return breakDurationSeconds;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isTranslucent() {
        return topColor.getAlpha() < 255;
    }

    public Color leftColor() {
        return shade(topColor, 0.78);
    }

    public Color rightColor() {
        return shade(topColor, 0.62);
    }

    private static Color shade(Color color, double factor) {
        int red = (int) Math.round(color.getRed() * factor);
        int green = (int) Math.round(color.getGreen() * factor);
        int blue = (int) Math.round(color.getBlue() * factor);
        return new Color(
            Math.max(0, Math.min(255, red)),
            Math.max(0, Math.min(255, green)),
            Math.max(0, Math.min(255, blue)),
            color.getAlpha()
        );
    }
}
