package keystrokesmod.client.module.modules.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;

/** Selects a stable interior face point once per support-block/face target. */
final class AimPointSelector {
    private static final double MARGIN = 0.08D;
    private long randomState = 0x9E3779B97F4A7C15L;

    void select(ScaffoldPlacementTarget target, EntityPlayerSP player, AutoAimController.AimPointMode mode) {
        randomState ^= ((long) target.support.getX() * 73428767L)
                ^ ((long) target.support.getY() * 912271L)
                ^ ((long) target.support.getZ() * 19349663L)
                ^ target.face.ordinal() * 0xC2B2AE3D27D4EB4FL;
        double minX = target.support.getX() + MARGIN;
        double minY = target.support.getY() + MARGIN;
        double minZ = target.support.getZ() + MARGIN;
        double maxX = target.support.getX() + 1.0D - MARGIN;
        double maxY = target.support.getY() + 1.0D - MARGIN;
        double maxZ = target.support.getZ() + 1.0D - MARGIN;

        double x = choose(minX, maxX, player.posX, mode);
        double y = choose(minY, maxY, player.posY + player.getEyeHeight(), mode);
        double z = choose(minZ, maxZ, player.posZ, mode);
        EnumFacing face = target.face;
        if (face == EnumFacing.WEST) x = target.support.getX();
        else if (face == EnumFacing.EAST) x = target.support.getX() + 1.0D;
        else if (face == EnumFacing.DOWN) y = target.support.getY();
        else if (face == EnumFacing.UP) y = target.support.getY() + 1.0D;
        else if (face == EnumFacing.NORTH) z = target.support.getZ();
        else if (face == EnumFacing.SOUTH) z = target.support.getZ() + 1.0D;
        target.aimX = x;
        target.aimY = y;
        target.aimZ = z;
    }

    private double choose(double min, double max, double eye, AutoAimController.AimPointMode mode) {
        if (mode == AutoAimController.AimPointMode.CENTER) return (min + max) * 0.5D;
        if (mode == AutoAimController.AimPointMode.EDGE_BIASED)
            return Math.max(min, Math.min(max, eye));
        return min + (max - min) * nextUnit();
    }

    private double nextUnit() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 7;
        randomState ^= randomState << 17;
        return (double) (randomState >>> 11) * 0x1.0p-53;
    }
}
