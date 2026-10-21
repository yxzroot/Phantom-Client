package keystrokesmod.client.module.modules.movement;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

/** Mutable target cache; it deliberately keeps scalar aim coordinates to avoid render-frame Vec3 allocation. */
final class ScaffoldPlacementTarget {
    BlockPos support;
    EnumFacing face;
    double aimX;
    double aimY;
    double aimZ;
    boolean valid;

    boolean matches(MovingObjectPosition hit) {
        return valid && hit != null && hit.getBlockPos() != null && hit.sideHit != null
                && support.equals(hit.getBlockPos()) && face == hit.sideHit;
    }

    void set(MovingObjectPosition hit) {
        support = hit.getBlockPos();
        face = hit.sideHit;
        valid = support != null && face != null;
    }

    void clear() {
        support = null;
        face = null;
        valid = false;
    }
}
