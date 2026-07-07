package blockforge.game;

import blockforge.world.BlockType;
import blockforge.world.World;

public final class MiningController {
    private MiningState state;

    public boolean select(World world, SelectionTarget target) {
        if (target == null || !target.inReach()) {
            reset();
            return false;
        }

        BlockType blockType = world.blockAt(target.blockX(), target.blockY(), target.blockZ());
        if (blockType == null) {
            reset();
            return false;
        }

        if (state == null || !state.matches(target, blockType)) {
            state = new MiningState(target.blockX(), target.blockY(), target.blockZ(), blockType, 0);
        }
        return true;
    }

    public BrokenBlock update(World world, SelectionTarget target, boolean active, double delta) {
        if (!active || !select(world, target)) {
            reset();
            return null;
        }

        MiningState current = state;
        double nextProgress = Math.min(
            current.progressSeconds() + delta,
            current.blockType().breakDurationSeconds()
        );
        state = current.withProgress(nextProgress);
        if (nextProgress < current.blockType().breakDurationSeconds()) {
            return null;
        }

        reset();
        if (!world.removeBlock(current.blockX(), current.blockY(), current.blockZ())) {
            return null;
        }
        return new BrokenBlock(current.blockX(), current.blockY(), current.blockZ(), current.blockType());
    }

    public void reset() {
        state = null;
    }

    public boolean isActive() {
        return state != null;
    }

    public double progressRatio() {
        return state == null ? 0 : state.progressRatio();
    }

    public BlockType blockType() {
        return state == null ? null : state.blockType();
    }

    public SelectionTarget target(SelectionTarget selectedTarget) {
        if (state == null) {
            return null;
        }
        if (state.matches(selectedTarget)) {
            return selectedTarget;
        }
        return new SelectionTarget(
            state.blockX(), state.blockY(), state.blockZ(),
            state.blockX(), state.blockY() + 1, state.blockZ(),
            true, null, 0
        );
    }

    public record BrokenBlock(int x, int y, int z, BlockType blockType) {
    }

    private record MiningState(int blockX, int blockY, int blockZ, BlockType blockType, double progressSeconds) {
        private boolean matches(SelectionTarget target, BlockType type) {
            return matches(target) && blockType == type;
        }

        private boolean matches(SelectionTarget target) {
            return target != null && blockX == target.blockX() && blockY == target.blockY() && blockZ == target.blockZ();
        }

        private double progressRatio() {
            return Math.min(1.0, progressSeconds / blockType.breakDurationSeconds());
        }

        private MiningState withProgress(double nextProgress) {
            return new MiningState(blockX, blockY, blockZ, blockType, nextProgress);
        }
    }
}
