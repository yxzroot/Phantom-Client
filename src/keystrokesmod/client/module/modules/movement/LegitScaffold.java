package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.GameLoopEvent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.player.AutoPlace;
import keystrokesmod.client.module.modules.player.RightClicker;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Edge guard and conservative hands-free bridging through vanilla item use. */
public final class LegitScaffold extends Module {
    private static LegitScaffold instance;
    private final SliderSetting edgeDistance = new SliderSetting("Edge Distance", 0.10, 0.07, 0.10, 0.01);
    private final SliderSetting unsneakDelay = new SliderSetting("Unsneak Delay (ms)", 30, 0, 300, 10);
    private final SliderSetting placementDelay = new SliderSetting("Placement Delay (ms)", 60, 50, 300, 10);
    private final TickSetting autoAim = new TickSetting("Auto Aim", true);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed (deg/tick)", 28, 8, 60, 1);
    private final SliderSetting aimCone = new SliderSetting("Aim Cone (degrees)", 60, 5, 180, 1);
    private final TickSetting autoWalk = new TickSetting("Auto Walk", true);
    private final TickSetting visibleFacePlace = new TickSetting("Visible Face Placement", false);

    private World world;
    private EntityPlayerSP player;
    private boolean ownsSneak;
    private int ownedSneakKey;
    private long safeSince;
    private long nextPlacement;
    private long userAimUntil;
    private boolean aimedLastTick;
    private float lastAimYaw, lastAimPitch;
    private boolean ownsForward;
    private int ownedForwardKey;
    private float travelYaw;
    private boolean restoringHeading;

    public LegitScaffold() {
        super("Legit Scaffold", ModuleCategory.movement);
        instance = this;
        withDescription("Guards the edge, visibly aims and places with normal item use; stops if no face is reachable.");
        registerSettings(new DescriptionSetting(getDescription()), placementDelay, edgeDistance, unsneakDelay,
                autoAim, aimSpeed, aimCone, autoWalk, visibleFacePlace);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        releaseSneak();
        releaseForward();
        nextPlacement = 0L;
        userAimUntil = 0L;
        aimedLastTick = false;
        restoringHeading = false;
        world = null;
        player = null;
    }

    @Subscribe
    public void onClientTick(GameLoopEvent event) {
        if (mc.theWorld != world || mc.thePlayer != player) {
            reset();
            world = mc.theWorld;
            player = mc.thePlayer;
            if (player != null) travelYaw = player.rotationYaw;
        }
        if (world == null || player == null || player.isDead || mc.playerController == null
                || mc.currentScreen != null || !mc.inGameHasFocus) {
            releaseSneak();
            releaseForward();
            nextPlacement = 0L;
            return;
        }

        boolean manualDirection = physicallyHeld(mc.gameSettings.keyBindForward.getKeyCode())
                || physicallyHeld(mc.gameSettings.keyBindBack.getKeyCode())
                || physicallyHeld(mc.gameSettings.keyBindLeft.getKeyCode())
                || physicallyHeld(mc.gameSettings.keyBindRight.getKeyCode());
        double[] movement = manualDirection ? movementDirection() : null;
        boolean walkingAutomatically = false;
        if (movement == null) {
            if (!autoWalk.isToggled()) {
                releaseSneak();
                releaseForward();
                return;
            }
            // The camera may move, but the auto-walk direction stays fixed from activation.
            movement = direction(travelYaw, 1, 0);
            walkingAutomatically = true;
        } else {
            releaseForward();
        }

        // If Bridge Helper is on, it owns sneak; never compete for the key.
        if (moduleEnabled(BridgeHelper.class))
            releaseSneak();
        else
            updateSneak(movement[0], movement[1]);

        ItemStack held = player.getHeldItem();
        boolean validBlocks = held != null && held.stackSize > 0 && held.getItem() instanceof ItemBlock;
        boolean canPlace = validBlocks && !moduleEnabled(AutoPlace.class)
                && !moduleEnabled(RightClicker.class)
                && !physicallyHeld(mc.gameSettings.keyBindUseItem.getKeyCode());
        if (walkingAutomatically)
            updateForward(canPlace && safeToWalk(movement[0], movement[1]));
        if (!canPlace)
            return;

        long now = System.nanoTime();
        if (aimedLastTick && (Math.abs(MathHelper.wrapAngleTo180_float(player.rotationYaw - lastAimYaw)) > 0.5F
                || Math.abs(player.rotationPitch - lastAimPitch) > 0.5F))
            userAimUntil = now + 200_000_000L;
        aimedLastTick = false;
        if (walkingAutomatically && restoringHeading) {
            updateForward(false);
            if (Math.abs(MathHelper.wrapAngleTo180_float(travelYaw - player.rotationYaw)) > 3.0F) {
                if (now >= userAimUntil) turnToward(travelYaw, player.rotationPitch);
                return;
            }
            restoringHeading = false;
        }
        if (now < nextPlacement)
            return;
        MovingObjectPosition sight = validTarget(held, (ItemBlock) held.getItem(), movement);
        if (sight == null) {
            if (nearEdge(movement[0], movement[1])) {
                MovingObjectPosition face = findVisibleFace(held, (ItemBlock) held.getItem(), movement);
                if (face != null && autoAim.isToggled()) {
                    if (walkingAutomatically) updateForward(false);
                    if (now >= userAimUntil) aimAtVisibleFace(face.hitVec, walkingAutomatically);
                    return; // Wait for the ordinary crosshair ray to reach the face.
                }
                if (face != null && visibleFacePlace.isToggled()) sight = face;
            }
            if (sight == null) return;
        }

        // The player controller does the item-use checks and sends its normal interaction.
        boolean placed = mc.playerController.onPlayerRightClick(player, mc.theWorld, held,
                sight.getBlockPos(), sight.sideHit, sight.hitVec);
        nextPlacement = now + (long) (placementDelay.getInput() * 1_000_000L);
        if (placed) {
            player.swingItem();
            if (walkingAutomatically) {
                restoringHeading = true;
                updateForward(false);
            }
        }
    }

    private void updateSneak(double dx, double dz) {
        int key = mc.gameSettings.keyBindSneak.getKeyCode();
        if (ownsSneak && key != ownedSneakKey)
            releaseSneak();
        if (physicallyHeld(key)) {
            // Track the handoff without clearing the key. On release, the edge
            // guard already owns it, including if a tick briefly reports airborne.
            if (nearEdge(dx, dz)) {
                ownedSneakKey = key;
                ownsSneak = true;
                safeSince = 0L;
            } else releaseSneak();
            return;
        }
        if (nearEdge(dx, dz)) {
            safeSince = 0L;
            KeyBinding.setKeyBindState(key, true);
            ownedSneakKey = key;
            ownsSneak = true;
        } else if (ownsSneak) {
            long now = System.nanoTime();
            if (unsneakDelay.getInput() == 0)
                releaseSneak();
            else if (safeSince == 0L)
                safeSince = now;
            else if (now - safeSince >= (long) (unsneakDelay.getInput() * 1_000_000L))
                releaseSneak();
        }
    }

    private boolean nearEdge(double dx, double dz) {
        if (!player.onGround)
            return ownsSneak;
        AxisAlignedBB body = player.getEntityBoundingBox();
        double ahead = 0.20 + edgeDistance.getInput();
        double x = player.posX + dx * ahead;
        double z = player.posZ + dz * ahead;
        // Same center-and-two-corners policy as the working Bridge Helper.
        if (!supported(x, z, body.minY))
            return true;
        return !supported(x + dz * 0.17, z - dx * 0.17, body.minY)
                && !supported(x - dz * 0.17, z + dx * 0.17, body.minY);
    }

    private boolean supported(double x, double z, double feet) {
        AxisAlignedBB probe = new AxisAlignedBB(x - 0.035, feet - 0.15, z - 0.035,
                x + 0.035, feet + 0.01, z + 0.035);
        return !world.getCollidingBoundingBoxes(player, probe).isEmpty();
    }

    /** Uses only ordinary block interaction on a face a real ray from the eyes can reach. */
    private MovingObjectPosition findVisibleFace(ItemStack held, ItemBlock item, double[] movement) {
        AxisAlignedBB body = player.getEntityBoundingBox();
        Vec3 eyes = player.getPositionEyes(1.0F);
        double reach = mc.playerController.getBlockReachDistance();
        int y = MathHelper.floor_double(body.minY - 0.05);
        java.util.HashSet<BlockPos> searched = new java.util.HashSet<>();
        MovingObjectPosition best = null;
        double bestScore = Double.MAX_VALUE;
        for (double ahead : new double[] {0.65, 1.0, 1.35}) {
            for (double side : new double[] {0, -0.42, 0.42}) {
                BlockPos destination = new BlockPos(player.posX + movement[0] * ahead - movement[1] * side,
                        y, player.posZ + movement[1] * ahead + movement[0] * side);
                if (!searched.add(destination) || !world.isBlockLoaded(destination)
                        || !world.isBlockModifiable(player, destination)
                        || !world.getBlockState(destination).getBlock().isReplaceable(world, destination)) continue;
                double dx = destination.getX() + 0.5 - player.posX;
                double dz = destination.getZ() + 0.5 - player.posZ;
                double forward = dx * movement[0] + dz * movement[1];
                if (forward < 0.12 || forward > 1.85 || dx * dx + dz * dz > 3.0) continue;
                AxisAlignedBB box = new AxisAlignedBB(destination, destination.add(1, 1, 1));
                if (box.intersectsWith(body) || !world.checkNoEntityCollision(box)) continue;
                for (EnumFacing face : EnumFacing.values()) {
                    BlockPos support = destination.offset(face.getOpposite());
                    if (!world.isBlockLoaded(support)
                            || !player.canPlayerEdit(destination, face, held)
                            || !item.canPlaceBlockOnSide(world, support, face, player, held)) continue;
                    for (double height : new double[] {0.3, 0.7}) {
                        Vec3 hit = new Vec3(support.getX() + 0.5 + face.getFrontOffsetX() * 0.5,
                                support.getY() + (face.getAxis() == EnumFacing.Axis.Y
                                        ? 0.5 + face.getFrontOffsetY() * 0.5 : height),
                                support.getZ() + 0.5 + face.getFrontOffsetZ() * 0.5);
                        if (eyes.distanceTo(hit) > reach - 0.01) continue;
                        MovingObjectPosition ray = world.rayTraceBlocks(eyes, hit, false, true, false);
                        if (ray == null || !support.equals(ray.getBlockPos()) || ray.sideHit != face
                                || ray.hitVec == null || eyes.distanceTo(ray.hitVec) > reach) continue;
                        double score = Math.abs(forward - 0.9) + Math.abs(dx * movement[1] - dz * movement[0]);
                        if (score < bestScore) {
                            bestScore = score;
                            best = ray;
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean safeToWalk(double dx, double dz) {
        if (!player.onGround) return false;
        AxisAlignedBB body = player.getEntityBoundingBox();
        // Forward is camera-relative. Never walk while aiming away from our
        // locked travel direction, even if the ground probe is supported.
        if (Math.abs(MathHelper.wrapAngleTo180_float(travelYaw - player.rotationYaw)) > 9.0F)
            return false;
        if (supported(player.posX + dx * 0.50, player.posZ + dz * 0.50, body.minY))
            return true;
        // Vanilla sneak prevents leaving the last supported sliver. Allow the
        // eyes to move beyond the lip so the outward face becomes ray-visible.
        int sneak = mc.gameSettings.keyBindSneak.getKeyCode();
        if (!(ownsSneak || player.movementInput.sneak || physicallyHeld(sneak)
                || mc.gameSettings.keyBindSneak.isKeyDown())) return false;
        return supported(player.posX - dx * 0.28, player.posZ - dz * 0.28, body.minY);
    }

    private void updateForward(boolean advance) {
        int key = mc.gameSettings.keyBindForward.getKeyCode();
        if (ownsForward && key != ownedForwardKey) releaseForward();
        if (physicallyHeld(key)) {
            releaseForward();
            return;
        }
        if (advance) {
            ownedForwardKey = key;
            ownsForward = true;
            KeyBinding.setKeyBindState(key, true);
        } else releaseForward();
    }

    private void releaseForward() {
        if (ownsForward) {
            KeyBinding.setKeyBindState(ownedForwardKey, physicallyHeld(ownedForwardKey));
            ownsForward = false;
        }
    }

    /** Runs after vanilla reads physical keys, including on the Shift-release tick. */
    public static void protectInput(MovementInput input) {
        LegitScaffold scaffold = instance;
        if (scaffold == null || !scaffold.isEnabled() || scaffold.player == null
                || scaffold.world == null || scaffold.player != mc.thePlayer
                || scaffold.world != mc.theWorld || input != scaffold.player.movementInput
                || mc.currentScreen != null || !mc.inGameHasFocus
                || scaffold.moduleEnabled(BridgeHelper.class)) return;
        double[] movement = scaffold.ownsForward
                ? direction(scaffold.travelYaw, 1, 0) : scaffold.movementDirection();
        boolean protect = movement != null && scaffold.nearEdge(movement[0], movement[1]);
        if (!protect && !scaffold.player.onGround) protect = scaffold.ownsSneak;
        if (!protect) return;
        if (!input.sneak) {
            input.sneak = true;
            input.moveForward *= 0.3F;
            input.moveStrafe *= 0.3F;
        }
        int key = mc.gameSettings.keyBindSneak.getKeyCode();
        if (!physicallyHeld(key)) KeyBinding.setKeyBindState(key, true);
        scaffold.ownedSneakKey = key;
        scaffold.ownsSneak = true;
        scaffold.safeSince = 0L;
    }

    /** A block is eligible only when the player's unchanged crosshair sees an exposed face. */
    private MovingObjectPosition validTarget(ItemStack held, ItemBlock item, double[] movement) {
        MovingObjectPosition target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || target.getBlockPos() == null || target.sideHit == null || target.hitVec == null)
            return null;

        double reach = mc.playerController.getBlockReachDistance();
        MovingObjectPosition ray = player.rayTrace(reach, 1.0F);
        if (ray == null || ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || !target.getBlockPos().equals(ray.getBlockPos()) || target.sideHit != ray.sideHit
                || ray.hitVec == null || player.getPositionEyes(1.0F).distanceTo(ray.hitVec) > reach + 0.01)
            return null;

        BlockPos support = ray.getBlockPos();
        BlockPos destination = support.offset(ray.sideHit);
        if (!world.isBlockLoaded(destination) || !world.isBlockModifiable(player, destination)
                || !player.canPlayerEdit(destination, ray.sideHit, held)
                || !item.canPlaceBlockOnSide(world, support, ray.sideHit, player, held))
            return null;

        Block existing = world.getBlockState(destination).getBlock();
        if (!existing.isReplaceable(world, destination))
            return null;

        AxisAlignedBB body = player.getEntityBoundingBox();
        if (destination.getY() != MathHelper.floor_double(body.minY - 0.05))
            return null;
        double x = destination.getX() + 0.5 - player.posX;
        double z = destination.getZ() + 0.5 - player.posZ;
        double aheadX = x - movement[0] * 0.7;
        double aheadZ = z - movement[1] * 0.7;
        if (x * movement[0] + z * movement[1] < 0.05
                || aheadX * aheadX + aheadZ * aheadZ > 2.0)
            return null;

        AxisAlignedBB blockSpace = new AxisAlignedBB(destination, destination.add(1, 1, 1));
        if (blockSpace.intersectsWith(body) || !world.checkNoEntityCollision(blockSpace))
            return null;
        return ray;
    }

    /** Turn the real camera toward the ray-visible placement face. */
    private void aimAtVisibleFace(Vec3 hit, boolean walkingAutomatically) {
        Vec3 eyes = player.getPositionEyes(1.0F);
        double deltaX = hit.xCoord - eyes.xCoord;
        double deltaY = hit.yCoord - eyes.yCoord;
        double deltaZ = hit.zCoord - eyes.zCoord;
        float yaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY,
                Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));
        float yawDiff = MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw);
        float pitchDiff = pitch - player.rotationPitch;
        if (!walkingAutomatically && Math.hypot(yawDiff, pitchDiff) > aimCone.getInput()) return;
        turnToward(yaw, pitch);
    }

    private void turnToward(float yaw, float pitch) {
        float yawDiff = MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw);
        float pitchDiff = MathHelper.clamp_float(pitch, -90, 90) - player.rotationPitch;
        float step = (float) aimSpeed.getInput();
        player.setAngles(Math.max(-step, Math.min(step, yawDiff)) / 0.15F,
                -Math.max(-step, Math.min(step, pitchDiff)) / 0.15F);
        lastAimYaw = player.rotationYaw;
        lastAimPitch = player.rotationPitch;
        aimedLastTick = true;
    }

    private double[] movementDirection() {
        float forward = player.movementInput.moveForward;
        float strafe = player.movementInput.moveStrafe;
        if (forward == 0 && strafe == 0)
            return null;
        return direction(player.rotationYaw, forward, strafe);
    }

    private static double[] direction(float heading, float forward, float strafe) {
        double yaw = Math.toRadians(heading);
        double x = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
        double z = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
        double magnitude = Math.sqrt(x * x + z * z);
        return magnitude < 0.001 ? null : new double[] {x / magnitude, z / magnitude};
    }

    private boolean moduleEnabled(Class<? extends Module> type) {
        Module module = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByClazz(type);
        return module != null && module.isEnabled();
    }

    private static boolean physicallyHeld(int key) {
        return (key > 0 && Keyboard.isCreated() && Keyboard.isKeyDown(key))
                || (key < 0 && Mouse.isCreated() && Mouse.isButtonDown(key + 100));
    }

    private void releaseSneak() {
        if (ownsSneak) {
            KeyBinding.setKeyBindState(ownedSneakKey, physicallyHeld(ownedSneakKey));
            ownsSneak = false;
        }
        safeSince = 0L;
    }
}
