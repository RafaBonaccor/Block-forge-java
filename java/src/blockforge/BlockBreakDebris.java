package blockforge;

final class BlockBreakDebris {
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

    BlockBreakDebris(
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

    void update(double delta, double gravity) {
        velocityY -= gravity * delta;
        x += velocityX * delta;
        y += velocityY * delta;
        z += velocityZ * delta;
        lifeSeconds = Math.max(0, lifeSeconds - delta);
    }

    boolean isAlive() {
        return lifeSeconds > 0;
    }

    BlockType blockType() {
        return blockType;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double z() {
        return z;
    }

    double size() {
        return size;
    }

    double lifeRatio() {
        return maxLifeSeconds <= 0 ? 0 : lifeSeconds / maxLifeSeconds;
    }
}
