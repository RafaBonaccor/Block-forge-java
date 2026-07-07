package blockforge.game;

import java.awt.Polygon;

public record SelectionTarget(
    int blockX,
    int blockY,
    int blockZ,
    int placeX,
    int placeY,
    int placeZ,
    boolean inReach,
    Polygon topFace,
    double distance
) {
}
