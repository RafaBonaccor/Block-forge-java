package blockforge;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

public final class World {
    private static final double EPSILON = 1e-6;
    private static final int CHUNK_SIZE = 8;
    private static final int MIN_BLOCK_Y = 0;
    private static final int MAX_BLOCK_Y = 23;
    private static final int MAX_WATER_SPREAD = 4;

    private final int radius;
    private final BlockType[][][] blocks;
    private final boolean[][][] waterSources;
    private final boolean[][] generatedVoidColumns;
    private final int[][] chunkRevisions;

    public World(int radius) {
        this.radius = radius;
        int size = radius * 2 + 1;
        blocks = new BlockType[size][MAX_BLOCK_Y + 1][size];
        waterSources = new boolean[size][MAX_BLOCK_Y + 1][size];
        generatedVoidColumns = new boolean[size][size];
        chunkRevisions = new int[chunkCountPerAxis()][chunkCountPerAxis()];
        generate();
        markAllChunksDirty();
    }

    public int radius() {
        return radius;
    }

    public int minBlockY() {
        return MIN_BLOCK_Y;
    }

    public int maxBlockY() {
        return MAX_BLOCK_Y;
    }

    public int chunkSize() {
        return CHUNK_SIZE;
    }

    public boolean containsCell(int x, int z) {
        return x >= -radius && x <= radius && z >= -radius && z <= radius;
    }

    public boolean containsBlockPosition(int x, int y, int z) {
        return containsCell(x, z) && y >= MIN_BLOCK_Y && y <= MAX_BLOCK_Y;
    }

    public int chunkCoordinateForBlock(int value) {
        return Math.floorDiv(value + radius, CHUNK_SIZE);
    }

    public int chunkCountPerAxis() {
        int worldWidth = radius * 2 + 1;
        return (worldWidth + CHUNK_SIZE - 1) / CHUNK_SIZE;
    }

    public Iterable<WorldChunk> chunksNear(double worldX, double worldZ, int viewRadius) {
        int minX = Math.max(-radius, (int) Math.floor(worldX) - viewRadius);
        int maxX = Math.min(radius, (int) Math.floor(worldX) + viewRadius);
        int minZ = Math.max(-radius, (int) Math.floor(worldZ) - viewRadius);
        int maxZ = Math.min(radius, (int) Math.floor(worldZ) + viewRadius);
        int minChunkX = chunkCoordinateForBlock(minX);
        int maxChunkX = chunkCoordinateForBlock(maxX);
        int minChunkZ = chunkCoordinateForBlock(minZ);
        int maxChunkZ = chunkCoordinateForBlock(maxZ);
        List<WorldChunk> chunks = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX += 1) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ += 1) {
                chunks.add(chunkAt(chunkX, chunkZ));
            }
        }
        return chunks;
    }

    public WorldChunk chunkAt(int chunkX, int chunkZ) {
        int minX = -radius + chunkX * CHUNK_SIZE;
        int minZ = -radius + chunkZ * CHUNK_SIZE;
        int maxX = Math.min(radius, minX + CHUNK_SIZE - 1);
        int maxZ = Math.min(radius, minZ + CHUNK_SIZE - 1);
        return new WorldChunk(chunkX, chunkZ, minX, maxX, minZ, maxZ);
    }

    public int chunkRevision(WorldChunk chunk) {
        return chunkRevisions[chunk.chunkX()][chunk.chunkZ()];
    }

    public boolean isVoidCell(int x, int z) {
        return !containsCell(x, z) || highestSolidBlockYAt(x, z) == Integer.MIN_VALUE;
    }

    public boolean wasGeneratedAsVoid(int x, int z) {
        return containsCell(x, z) && generatedVoidColumns[index(x)][index(z)];
    }

    public boolean hasBlock(int x, int y, int z) {
        return blockAt(x, y, z) != null;
    }

    public boolean hasSolidBlock(int x, int y, int z) {
        BlockType blockType = blockAt(x, y, z);
        return blockType != null && blockType.isSolid();
    }

    public boolean occludesFace(int x, int y, int z, BlockType sourceType) {
        BlockType neighbor = blockAt(x, y, z);
        if (neighbor == null) {
            return false;
        }
        if (sourceType.isTranslucent()) {
            return neighbor == sourceType;
        }
        return !neighbor.isTranslucent();
    }

    public BlockType blockAt(int x, int y, int z) {
        if (!containsBlockPosition(x, y, z)) {
            return null;
        }
        return blocks[index(x)][y][index(z)];
    }

    public boolean placeBlock(int x, int y, int z, BlockType blockType) {
        if (!containsBlockPosition(x, y, z) || blockType == null || hasBlock(x, y, z)) {
            return false;
        }
        blocks[index(x)][y][index(z)] = blockType;
        waterSources[index(x)][y][index(z)] = blockType == BlockType.WATER;
        markBlockAndNeighborChunksDirty(x, z);
        return true;
    }

    public void setBlock(int x, int y, int z, BlockType blockType) {
        if (!containsBlockPosition(x, y, z)) {
            return;
        }
        blocks[index(x)][y][index(z)] = blockType;
        waterSources[index(x)][y][index(z)] = blockType == BlockType.WATER;
        markBlockAndNeighborChunksDirty(x, z);
    }

    public void clearBlocks() {
        for (int x = -radius; x <= radius; x += 1) {
            for (int y = MIN_BLOCK_Y; y <= MAX_BLOCK_Y; y += 1) {
                for (int z = -radius; z <= radius; z += 1) {
                    blocks[index(x)][y][index(z)] = null;
                    waterSources[index(x)][y][index(z)] = false;
                }
            }
        }
        markAllChunksDirty();
    }

    public void forEachBlock(BlockVisitor visitor) {
        for (int x = -radius; x <= radius; x += 1) {
            for (int y = MIN_BLOCK_Y; y <= MAX_BLOCK_Y; y += 1) {
                for (int z = -radius; z <= radius; z += 1) {
                    BlockType blockType = blockAt(x, y, z);
                    if (blockType != null) {
                        visitor.visit(x, y, z, blockType);
                    }
                }
            }
        }
    }

    public boolean removeBlock(int x, int y, int z) {
        if (!hasBlock(x, y, z)) {
            return false;
        }
        blocks[index(x)][y][index(z)] = null;
        waterSources[index(x)][y][index(z)] = false;
        markBlockAndNeighborChunksDirty(x, z);
        return true;
    }

    public boolean isWaterSource(int x, int y, int z) {
        return containsBlockPosition(x, y, z) && waterSources[index(x)][y][index(z)];
    }

    public void simulateWater() {
        int size = radius * 2 + 1;
        int[][][] nextLevels = new int[size][MAX_BLOCK_Y + 1][size];
        ArrayDeque<WaterNode> pending = new ArrayDeque<>();

        for (int localX = 0; localX < size; localX += 1) {
            for (int y = MIN_BLOCK_Y; y <= MAX_BLOCK_Y; y += 1) {
                Arrays.fill(nextLevels[localX][y], -1);
                for (int localZ = 0; localZ < size; localZ += 1) {
                    if (waterSources[localX][y][localZ]) {
                        pending.addLast(new WaterNode(localX - radius, y, localZ - radius, 0));
                    }
                }
            }
        }

        while (!pending.isEmpty()) {
            WaterNode node = pending.removeFirst();
            if (!containsBlockPosition(node.x(), node.y(), node.z())) {
                continue;
            }

            int localX = index(node.x());
            int localZ = index(node.z());
            BlockType occupied = blocks[localX][node.y()][localZ];
            if (occupied != null && occupied != BlockType.WATER) {
                continue;
            }
            int existingLevel = nextLevels[localX][node.y()][localZ];
            if (existingLevel >= 0 && existingLevel <= node.level()) {
                continue;
            }
            nextLevels[localX][node.y()][localZ] = node.level();

            if (node.y() > MIN_BLOCK_Y && !hasSolidBlock(node.x(), node.y() - 1, node.z())) {
                pending.addLast(new WaterNode(node.x(), node.y() - 1, node.z(), node.level()));
                continue;
            }
            if (node.level() >= MAX_WATER_SPREAD) {
                continue;
            }

            int nextLevel = node.level() + 1;
            pending.addLast(new WaterNode(node.x() + 1, node.y(), node.z(), nextLevel));
            pending.addLast(new WaterNode(node.x() - 1, node.y(), node.z(), nextLevel));
            pending.addLast(new WaterNode(node.x(), node.y(), node.z() + 1, nextLevel));
            pending.addLast(new WaterNode(node.x(), node.y(), node.z() - 1, nextLevel));
        }

        boolean[][][] currentWater = new boolean[size][MAX_BLOCK_Y + 1][size];
        for (int localX = 0; localX < size; localX += 1) {
            for (int y = MIN_BLOCK_Y; y <= MAX_BLOCK_Y; y += 1) {
                for (int localZ = 0; localZ < size; localZ += 1) {
                    currentWater[localX][y][localZ] = blocks[localX][y][localZ] == BlockType.WATER;
                }
            }
        }

        boolean changed = false;
        for (int localX = 0; localX < size; localX += 1) {
            for (int y = MIN_BLOCK_Y; y <= MAX_BLOCK_Y; y += 1) {
                for (int localZ = 0; localZ < size; localZ += 1) {
                    BlockType current = blocks[localX][y][localZ];
                    boolean shouldContainWater = nextLevels[localX][y][localZ] >= 0;
                    boolean reachedByCurrentFlow = waterSources[localX][y][localZ] ||
                        hasAdjacentWater(currentWater, localX, y, localZ);
                    if (shouldContainWater && current == null && reachedByCurrentFlow) {
                        blocks[localX][y][localZ] = BlockType.WATER;
                        changed = true;
                    } else if (!shouldContainWater && current == BlockType.WATER) {
                        blocks[localX][y][localZ] = null;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            markAllChunksDirty();
        }
    }

    private boolean hasAdjacentWater(boolean[][][] water, int localX, int y, int localZ) {
        int size = radius * 2 + 1;
        return localX > 0 && water[localX - 1][y][localZ] ||
            localX + 1 < size && water[localX + 1][y][localZ] ||
            localZ > 0 && water[localX][y][localZ - 1] ||
            localZ + 1 < size && water[localX][y][localZ + 1] ||
            y > MIN_BLOCK_Y && water[localX][y - 1][localZ] ||
            y < MAX_BLOCK_Y && water[localX][y + 1][localZ];
    }

    public int highestSolidBlockYAt(int x, int z) {
        if (!containsCell(x, z)) {
            return Integer.MIN_VALUE;
        }

        for (int y = MAX_BLOCK_Y; y >= MIN_BLOCK_Y; y -= 1) {
            if (hasSolidBlock(x, y, z)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public OptionalDouble surfaceYAt(double worldX, double worldZ) {
        int cellX = (int) Math.floor(worldX);
        int cellZ = (int) Math.floor(worldZ);
        int blockY = highestSolidBlockYAt(cellX, cellZ);
        if (blockY == Integer.MIN_VALUE) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(blockY + 1.0);
    }

    public OptionalDouble supportYAtOrBelow(double worldX, double worldZ, double maxTopY) {
        int cellX = (int) Math.floor(worldX);
        int cellZ = (int) Math.floor(worldZ);
        if (!containsCell(cellX, cellZ)) {
            return OptionalDouble.empty();
        }

        int maxCandidateY = Math.min(MAX_BLOCK_Y, (int) Math.floor(maxTopY - 1.0 + EPSILON));
        for (int y = maxCandidateY; y >= MIN_BLOCK_Y; y -= 1) {
            if (hasSolidBlock(cellX, y, cellZ)) {
                return OptionalDouble.of(y + 1.0);
            }
        }
        return OptionalDouble.empty();
    }

    public int heightAt(int x, int z) {
        int highestBlockY = highestSolidBlockYAt(x, z);
        return highestBlockY == Integer.MIN_VALUE ? Integer.MIN_VALUE : highestBlockY + 1;
    }

    public BlockType topTypeAt(int x, int z) {
        int highestBlockY = highestSolidBlockYAt(x, z);
        return highestBlockY == Integer.MIN_VALUE ? null : blockAt(x, highestBlockY, z);
    }

    public SpawnPoint findSpawnPoint() {
        for (int ring = 0; ring <= radius; ring += 1) {
            for (int x = -ring; x <= ring; x += 1) {
                for (int z = -ring; z <= ring; z += 1) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != ring) {
                        continue;
                    }

                    int highestBlockY = highestSolidBlockYAt(x, z);
                    if (highestBlockY == Integer.MIN_VALUE) {
                        continue;
                    }
                    return new SpawnPoint(x + 0.5, highestBlockY + 1.0, z + 0.5);
                }
            }
        }
        return new SpawnPoint(0.5, 6.0, 0.5);
    }

    private int index(int value) {
        return value + radius;
    }

    private void markBlockAndNeighborChunksDirty(int x, int z) {
        markChunkDirty(chunkCoordinateForBlock(x), chunkCoordinateForBlock(z));
        if (Math.floorMod(x + radius, CHUNK_SIZE) == 0) {
            markChunkDirty(chunkCoordinateForBlock(x - 1), chunkCoordinateForBlock(z));
        }
        if (Math.floorMod(x + radius, CHUNK_SIZE) == CHUNK_SIZE - 1) {
            markChunkDirty(chunkCoordinateForBlock(x + 1), chunkCoordinateForBlock(z));
        }
        if (Math.floorMod(z + radius, CHUNK_SIZE) == 0) {
            markChunkDirty(chunkCoordinateForBlock(x), chunkCoordinateForBlock(z - 1));
        }
        if (Math.floorMod(z + radius, CHUNK_SIZE) == CHUNK_SIZE - 1) {
            markChunkDirty(chunkCoordinateForBlock(x), chunkCoordinateForBlock(z + 1));
        }
    }

    private void markChunkDirty(int chunkX, int chunkZ) {
        if (chunkX < 0 || chunkZ < 0 || chunkX >= chunkRevisions.length || chunkZ >= chunkRevisions[chunkX].length) {
            return;
        }
        chunkRevisions[chunkX][chunkZ] += 1;
    }

    private void markAllChunksDirty() {
        for (int chunkX = 0; chunkX < chunkRevisions.length; chunkX += 1) {
            for (int chunkZ = 0; chunkZ < chunkRevisions[chunkX].length; chunkZ += 1) {
                chunkRevisions[chunkX][chunkZ] += 1;
            }
        }
    }

    private void generate() {
        for (int x = -radius; x <= radius; x += 1) {
            for (int z = -radius; z <= radius; z += 1) {
                if (isGeneratedVoidPit(x, z)) {
                    generatedVoidColumns[index(x)][index(z)] = true;
                    continue;
                }

                int surfaceHeight = generatedSurfaceHeight(x, z);
                for (int y = MIN_BLOCK_Y; y < surfaceHeight; y += 1) {
                    blocks[index(x)][y][index(z)] = generatedBlockTypeForLevel(x, y, z, surfaceHeight);
                }
            }
        }
    }

    private int generatedSurfaceHeight(int x, int z) {
        double base =
            Math.sin(x * 0.27) * 1.8 +
            Math.cos(z * 0.23) * 1.6 +
            Math.sin((x + z) * 0.15) * 1.3;
        return Math.max(2, Math.min(MAX_BLOCK_Y, (int) Math.floor(base + 6)));
    }

    private BlockType generatedBlockTypeForLevel(int x, int y, int z, int surfaceHeight) {
        if (y == surfaceHeight - 1) {
            if ((Math.abs(x) + Math.abs(z)) % 17 == 0 && surfaceHeight >= 5) {
                return BlockType.GLOW;
            }
            if ((x * x + z * z) % 19 == 0 && surfaceHeight >= 4) {
                return BlockType.WOOD;
            }
            return BlockType.GRASS;
        }

        if (y >= surfaceHeight - 3) {
            return BlockType.DIRT;
        }

        return BlockType.STONE;
    }

    private boolean isGeneratedVoidPit(int x, int z) {
        return insideCircle(x, z, 5, 2, 2)
            || insideCircle(x, z, -6, -4, 2)
            || insideCircle(x, z, 1, 9, 3)
            || (x >= 9 && x <= 11 && z >= -10 && z <= -7);
    }

    private boolean insideCircle(int x, int z, int centerX, int centerZ, int radius) {
        int dx = x - centerX;
        int dz = z - centerZ;
        return dx * dx + dz * dz <= radius * radius;
    }

    public record SpawnPoint(double x, double y, double z) {
    }

    public record WorldChunk(int chunkX, int chunkZ, int minX, int maxX, int minZ, int maxZ) {
        public double centerX() {
            return (minX + maxX + 1) * 0.5;
        }

        public double centerZ() {
            return (minZ + maxZ + 1) * 0.5;
        }
    }

    private record WaterNode(int x, int y, int z, int level) {
    }

    public interface BlockVisitor {
        void visit(int x, int y, int z, BlockType blockType);
    }
}
