package blockforge;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
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
        draw(g2, ScreenQuad.fromPolygon(polygon), progress);
    }

    static void draw(Graphics2D g2, ScreenQuad quad, double progress) {
        if (quad == null || progress <= 0) {
            return;
        }

        Polygon polygon = quad.asPolygon();
        Rectangle bounds = polygon.getBounds();
        if (bounds.width <= 2 || bounds.height <= 2) {
            return;
        }

        int segmentCount = Math.max(1, (int) Math.ceil(clamp(progress, 0, 1) * SEGMENTS.length));
        Graphics2D overlay = (Graphics2D) g2.create();
        overlay.clip(polygon);
        overlay.setStroke(new BasicStroke(
            (float) Math.max(1.2, Math.min(bounds.width, bounds.height) * 0.04),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));
        overlay.setColor(new Color(20, 12, 12, (int) Math.round(120 + progress * 110)));

        for (int index = 0; index < segmentCount; index += 1) {
            double[] segment = SEGMENTS[index];
            Point start = quad.sample(segment[0], segment[1]);
            Point end = quad.sample(segment[2], segment[3]);
            overlay.drawLine(start.x, start.y, end.x, end.y);
        }

        overlay.setColor(new Color(255, 255, 255, (int) Math.round(24 + progress * 32)));
        overlay.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        overlay.drawPolygon(polygon);
        overlay.dispose();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    record ScreenQuad(Point nw, Point ne, Point se, Point sw) {
        static ScreenQuad fromPolygon(Polygon polygon) {
            if (polygon == null || polygon.npoints != 4) {
                return null;
            }
            return new ScreenQuad(
                new Point(polygon.xpoints[0], polygon.ypoints[0]),
                new Point(polygon.xpoints[1], polygon.ypoints[1]),
                new Point(polygon.xpoints[2], polygon.ypoints[2]),
                new Point(polygon.xpoints[3], polygon.ypoints[3])
            );
        }

        Polygon asPolygon() {
            return new Polygon(
                new int[] {nw.x, ne.x, se.x, sw.x},
                new int[] {nw.y, ne.y, se.y, sw.y},
                4
            );
        }

        Point sample(double u, double v) {
            double topX = lerp(nw.x, ne.x, u);
            double topY = lerp(nw.y, ne.y, u);
            double bottomX = lerp(sw.x, se.x, u);
            double bottomY = lerp(sw.y, se.y, u);
            return new Point(
                (int) Math.round(lerp(topX, bottomX, v)),
                (int) Math.round(lerp(topY, bottomY, v))
            );
        }

        private static double lerp(double start, double end, double amount) {
            return start + (end - start) * amount;
        }
    }
}
