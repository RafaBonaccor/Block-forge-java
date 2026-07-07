package blockforge.render;

import java.awt.Polygon;

public record CellProjection(int x, int y, int z, double depth, Polygon topFace, boolean topVisible) {
}
