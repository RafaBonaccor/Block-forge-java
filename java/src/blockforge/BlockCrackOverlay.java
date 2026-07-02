package blockforge;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

final class BlockCrackOverlay {
    private static final double[][] SEGMENTS = {
        {0.50, 0.10, 0.50, 0.88},
        {0.22, 0.28, 0.50, 0.45},
        {0.78, 0.24, 0.50, 0.45},
        {0.18, 0.72, 0.50, 0.60},
        {0.82, 0.68, 0.50, 0.60},
        {0.34, 0.16, 0.18, 0.06},
        {0.66, 0.14, 0.84, 0.05},
        {0.14, 0.54, 0.05, 0.70},
        {0.86, 0.56, 0.95, 0.72},
        {0.30, 0.82, 0.20, 0.96},
        {0.70, 0.82, 0.82, 0.97},
        {0.42, 0.42, 0.30, 0.30},
        {0.58, 0.42, 0.70, 0.30},
        {0.40, 0.58, 0.28, 0.72},
        {0.60, 0.58, 0.72, 0.74}
    };

    private BlockCrackOverlay() {
    }

    static void draw(Graphics2D g2, Polygon polygon, double progress) {
        if (polygon == null || progress <= 0) {
            return;
        }

        Rectangle bounds = polygon.getBounds();
        if (bounds.width <= 2 || bounds.height <= 2) {
            return;
        }

        int segmentCount = Math.max(1, (int) Math.ceil(clamp(progress, 0, 1) * SEGMENTS.length));
        Graphics2D overlay = (Graphics2D) g2.create();
        overlay.clip(polygon);
        overlay.setColor(new Color(24, 18, 18, (int) Math.round(18 + progress * 36)));
        overlay.fillPolygon(polygon);
        overlay.setStroke(new BasicStroke(
            (float) Math.max(1.2, Math.min(bounds.width, bounds.height) * 0.04),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));
        overlay.setColor(new Color(20, 12, 12, (int) Math.round(120 + progress * 110)));

        for (int index = 0; index < segmentCount; index += 1) {
            double[] segment = SEGMENTS[index];
            int x1 = bounds.x + (int) Math.round(bounds.width * segment[0]);
            int y1 = bounds.y + (int) Math.round(bounds.height * segment[1]);
            int x2 = bounds.x + (int) Math.round(bounds.width * segment[2]);
            int y2 = bounds.y + (int) Math.round(bounds.height * segment[3]);
            overlay.drawLine(x1, y1, x2, y2);
        }

        overlay.setColor(new Color(255, 255, 255, (int) Math.round(24 + progress * 32)));
        overlay.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        overlay.drawPolygon(polygon);
        overlay.dispose();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
