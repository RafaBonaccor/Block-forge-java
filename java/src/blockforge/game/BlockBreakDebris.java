package blockforge.game;

import blockforge.world.BlockType;

public final class BlockBreakDebris {
    private final BlockType blockType;
    private final double size;
    private final double maxLifeSeconds;
    private double x;
    private double y;
    private double z;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double lifeSeconds;

    public BlockBreakDebris(
        BlockType blockType,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        double size,
        double maxLifeSeconds
    ) {
        this.blockType = blockType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.size = size;
        this.maxLifeSeconds = maxLifeSeconds;
        this.lifeSeconds = maxLifeSeconds;
    }

    public void update(double delta, double gravity) {
        velocityY -= gravity * delta;
        x += velocityX * delta;
        y += velocityY * delta;
        z += velocityZ * delta;
        lifeSeconds = Math.max(0, lifeSeconds - delta);
    }

    public boolean isAlive() {
        return lifeSeconds > 0;
    }

    public BlockType blockType() {
        return blockType;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double size() {
        return size;
    }

    public double lifeRatio() {
        return maxLifeSeconds <= 0 ? 0 : lifeSeconds / maxLifeSeconds;
    }
}
