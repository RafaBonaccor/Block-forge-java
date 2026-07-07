package blockforge.world;

import java.util.ArrayDeque;
import java.util.Arrays;

final class WaterSimulator {
    private static final int MAX_SPREAD = 4;

    void simulate(World world) {
        int size = world.radius() * 2 + 1;
        int minY = world.minBlockY();
        int maxY = world.maxBlockY();
        int[][][] desiredLevels = new int[size][maxY + 1][size];
        ArrayDeque<WaterNode> pending = new ArrayDeque<>();

        for (int localX = 0; localX < size; localX += 1) {
            for (int y = minY; y <= maxY; y += 1) {
                Arrays.fill(desiredLevels[localX][y], -1);
                for (int localZ = 0; localZ < size; localZ += 1) {
                    int x = localX - world.radius();
                    int z = localZ - world.radius();
                    if (world.isWaterSource(x, y, z)) {
                        pending.addLast(new WaterNode(x, y, z, 0));
                    }
                }
            }
        }

        calculateDesiredFlow(world, desiredLevels, pending);
        applyNextFront(world, desiredLevels, size, minY, maxY);
    }

    private void calculateDesiredFlow(
        World world,
        int[][][] desiredLevels,
        ArrayDeque<WaterNode> pending
    ) {
        while (!pending.isEmpty()) {
            WaterNode node = pending.removeFirst();
            if (!world.containsBlockPosition(node.x(), node.y(), node.z())) {
                continue;
            }

            BlockType occupied = world.blockAt(node.x(), node.y(), node.z());
            if (occupied != null && occupied != BlockType.WATER) {
                continue;
            }

            int localX = node.x() + world.radius();
            int localZ = node.z() + world.radius();
            int existingLevel = desiredLevels[localX][node.y()][localZ];
            if (existingLevel >= 0 && existingLevel <= node.level()) {
                continue;
            }
            desiredLevels[localX][node.y()][localZ] = node.level();

            if (node.y() > world.minBlockY() && !world.hasSolidBlock(node.x(), node.y() - 1, node.z())) {
                pending.addLast(new WaterNode(node.x(), node.y() - 1, node.z(), node.level()));
                continue;
            }
            if (node.level() >= MAX_SPREAD) {
                continue;
            }

            int nextLevel = node.level() + 1;
            pending.addLast(new WaterNode(node.x() + 1, node.y(), node.z(), nextLevel));
            pending.addLast(new WaterNode(node.x() - 1, node.y(), node.z(), nextLevel));
            pending.addLast(new WaterNode(node.x(), node.y(), node.z() + 1, nextLevel));
            pending.addLast(new WaterNode(node.x(), node.y(), node.z() - 1, nextLevel));
        }
    }

    private void applyNextFront(World world, int[][][] desiredLevels, int size, int minY, int maxY) {
        boolean[][][] currentWater = new boolean[size][maxY + 1][size];
        for (int localX = 0; localX < size; localX += 1) {
            for (int y = minY; y <= maxY; y += 1) {
                for (int localZ = 0; localZ < size; localZ += 1) {
                    currentWater[localX][y][localZ] = world.blockAt(
                        localX - world.radius(), y, localZ - world.radius()
                    ) == BlockType.WATER;
                }
            }
        }

        boolean changed = false;
        for (int localX = 0; localX < size; localX += 1) {
            for (int y = minY; y <= maxY; y += 1) {
                for (int localZ = 0; localZ < size; localZ += 1) {
                    int x = localX - world.radius();
                    int z = localZ - world.radius();
                    boolean desired = desiredLevels[localX][y][localZ] >= 0;
                    boolean reached = world.isWaterSource(x, y, z) ||
                        hasAdjacentWater(currentWater, localX, y, localZ, size, minY, maxY);
                    changed |= world.setFlowingWater(x, y, z, desired && reached);
                    if (world.blockAt(x, y, z) == BlockType.WATER) {
                        world.setWaterLevel(x, y, z, desiredLevels[localX][y][localZ]);
                    }
                }
            }
        }
        if (changed) {
            world.markAllChunksDirty();
        }
    }

    private boolean hasAdjacentWater(
        boolean[][][] water,
        int x,
        int y,
        int z,
        int size,
        int minY,
        int maxY
    ) {
        return x > 0 && water[x - 1][y][z] ||
            x + 1 < size && water[x + 1][y][z] ||
            z > 0 && water[x][y][z - 1] ||
            z + 1 < size && water[x][y][z + 1] ||
            y > minY && water[x][y - 1][z] ||
            y < maxY && water[x][y + 1][z];
    }

    private record WaterNode(int x, int y, int z, int level) {
    }
}
