package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.GameLoopEvent;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.player.AutoPlace;
import keystrokesmod.client.module.modules.player.RightClicker;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.font.FontUtil;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
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
    private final TickSetting autoAim = new TickSetting("Auto Aim", false);
    private final ComboSetting<AutoAimController.AimMode> aimMode = new ComboSetting<>("Aim Mode", AutoAimController.AimMode.BRIDGE_ANGLE);
    private final ComboSetting<AutoAimController.BridgeDirection> bridgeDirection = new ComboSetting<>("Bridge Direction", AutoAimController.BridgeDirection.BACKWARD);
    private final TickSetting snapYaw = new TickSetting("Snap Yaw to 45°", true);
    private final DoubleSliderSetting pitchRange = new DoubleSliderSetting("Pitch Range", 74, 82, 45, 89, 1);
    private final TickSetting movementCorrection = new TickSetting("Movement Correction", true);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed (deg/s)", 360, 60, 720, 5);
    private final SliderSetting randomization = new SliderSetting("Randomization (%)", 15, 0, 35, 1);
    private final DoubleSliderSetting reactionDelay = new DoubleSliderSetting("Reaction Delay (ticks)", 0, 2, 0, 10, 1);
    private final ComboSetting<AutoAimController.AimPointMode> aimPoint = new ComboSetting<>("Aim Point", AutoAimController.AimPointMode.CENTER);
    private final SliderSetting placeTolerance = new SliderSetting("Place Tolerance (degrees)", 2, .25, 6, .25);
    private final SliderSetting maxTurnPerTick = new SliderSetting("Max Turn Per Tick", 30, 3, 45, 1);
    private final SliderSetting fovLimit = new SliderSetting("FOV Limit (degrees)", 120, 15, 180, 1);
    private final TickSetting mouseOverride = new TickSetting("Mouse Override", true);
    private final SliderSetting overrideTimeout = new SliderSetting("Override Timeout (ms)", 400, 100, 1500, 25);
    private final TickSetting pauseAttacking = new TickSetting("Pause While Attacking", true);
    private final TickSetting onlyWhenMoving = new TickSetting("Only When Moving", false);
    private final TickSetting debugOverlay = new TickSetting("Debug Overlay", false);
    private final TickSetting autoWalk = new TickSetting("Auto Walk", true);
    private final TickSetting visibleFacePlace = new TickSetting("Visible Face Placement", false);
    private final AutoAimController autoAimController = new AutoAimController();
    private final AutoAimController.Config aimConfig = new AutoAimController.Config();

    private World world;
    private EntityPlayerSP player;
    private boolean ownsSneak;
    private int ownedSneakKey;
    private long safeSince;
    private long nextPlacement;
    private boolean ownsForward;
    private int ownedForwardKey;
    private float travelYaw;
    private boolean restoringHeading;

    public LegitScaffold() {
        super("Legit Scaffold", ModuleCategory.movement);
        instance = this;
        withDescription("Guards the edge, visibly aims and places with normal item use; stops if no face is reachable.");
        registerSettings(new DescriptionSetting(getDescription()), placementDelay, edgeDistance, unsneakDelay,
                autoAim, aimMode, bridgeDirection, snapYaw, pitchRange, movementCorrection, aimSpeed,
                randomization, reactionDelay, aimPoint, placeTolerance, maxTurnPerTick, fovLimit,
                mouseOverride, overrideTimeout, pauseAttacking, onlyWhenMoving, debugOverlay,
                autoWalk, visibleFacePlace);
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
        restoringHeading = false;
        autoAimController.reset();
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
            autoAimController.reset();
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
        if (!canPlace) {
            autoAimController.reset();
            return;
        }

        long now = System.nanoTime();
        if (!autoAim.isToggled() && walkingAutomatically && restoringHeading) {
            updateForward(false);
            if (Math.abs(MathHelper.wrapAngleTo180_float(travelYaw - player.rotationYaw)) > 3.0F) {
                return;
            }
            restoringHeading = false;
        }
        ItemBlock item = (ItemBlock) held.getItem();
        MovingObjectPosition sight;
        if (autoAim.isToggled()) {
            // Cache a candidate once on the client tick. Camera work happens from the render hook.
            MovingObjectPosition candidate = validTarget(held, item, movement);
            if (candidate == null)
                candidate = findVisibleFace(held, item, movement,
                        aimMode.getMode() == AutoAimController.AimMode.AUTO);
            autoAimController.cacheTarget(player, candidate, movement[0], movement[1], autoAimConfig());
            if (now < nextPlacement) return;
            sight = autoAimController.verifiedHit(player, world, aimConfig);
            if (sight == null || !validVerifiedTarget(held, item, movement, sight)) return;
        } else {
            // Keep the pre-Auto-Aim placement path intact when the new layer is off.
            autoAimController.reset();
            if (now < nextPlacement) return;
            sight = validTarget(held, item, movement);
            if (sight == null && nearEdge(movement[0], movement[1])) {
                MovingObjectPosition face = findVisibleFace(held, item, movement, false);
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
            if (walkingAutomatically && !autoAim.isToggled()) {
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
    private MovingObjectPosition findVisibleFace(ItemStack held, ItemBlock item, double[] movement,
                                                 boolean prioritizeRotation) {
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
                        if (prioritizeRotation) {
                            double hitX = ray.hitVec.xCoord - eyes.xCoord;
                            double hitY = ray.hitVec.yCoord - eyes.yCoord;
                            double hitZ = ray.hitVec.zCoord - eyes.zCoord;
                            float yaw = (float) Math.toDegrees(Math.atan2(hitZ, hitX)) - 90.0F;
                            float pitch = (float) -Math.toDegrees(Math.atan2(hitY, Math.sqrt(hitX * hitX + hitZ * hitZ)));
                            score = Math.abs(MathHelper.wrapAngleTo180_float(yaw - player.rotationYaw))
                                    + Math.abs(pitch - player.rotationPitch);
                        }
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
        if (!autoAim.isToggled()
                && Math.abs(MathHelper.wrapAngleTo180_float(travelYaw - player.rotationYaw)) > 9.0F)
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
        if (scaffold.autoAim.isToggled()) {
            scaffold.autoAimController.captureInputIntent(input);
            scaffold.autoAimController.correctMovementInput(input, scaffold.player, scaffold.autoAimConfig());
        }
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

    /** Rechecks the controller's real camera ray against the unchanged placement geometry rules. */
    private boolean validVerifiedTarget(ItemStack held, ItemBlock item, double[] movement, MovingObjectPosition ray) {
        if (ray == null || ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || ray.getBlockPos() == null || ray.sideHit == null || ray.hitVec == null) return false;
        double reach = mc.playerController.getBlockReachDistance();
        if (player.getPositionEyes(1.0F).distanceTo(ray.hitVec) > reach + 0.01D) return false;
        BlockPos support = ray.getBlockPos();
        BlockPos destination = support.offset(ray.sideHit);
        if (!world.isBlockLoaded(destination) || !world.isBlockModifiable(player, destination)
                || !player.canPlayerEdit(destination, ray.sideHit, held)
                || !item.canPlaceBlockOnSide(world, support, ray.sideHit, player, held)) return false;
        Block existing = world.getBlockState(destination).getBlock();
        if (!existing.isReplaceable(world, destination)) return false;
        AxisAlignedBB body = player.getEntityBoundingBox();
        if (destination.getY() != MathHelper.floor_double(body.minY - 0.05D)) return false;
        double x = destination.getX() + 0.5D - player.posX;
        double z = destination.getZ() + 0.5D - player.posZ;
        double aheadX = x - movement[0] * 0.7D;
        double aheadZ = z - movement[1] * 0.7D;
        if (x * movement[0] + z * movement[1] < 0.05D
                || aheadX * aheadX + aheadZ * aheadZ > 2.0D) return false;
        AxisAlignedBB blockSpace = new AxisAlignedBB(destination, destination.add(1, 1, 1));
        return !blockSpace.intersectsWith(body) && world.checkNoEntityCollision(blockSpace);
    }

    /** Called by the existing EntityRenderer mixin at render-frame cadence. */
    public static void renderFrame(float partialTicks) {
        LegitScaffold scaffold = instance;
        if (scaffold == null || !scaffold.isEnabled() || !scaffold.autoAim.isToggled()) return;
        if (scaffold.player == null || scaffold.world == null || scaffold.player != mc.thePlayer
                || scaffold.world != mc.theWorld || scaffold.player.isDead || mc.currentScreen != null
                || !mc.inGameHasFocus) {
            scaffold.autoAimController.reset();
            return;
        }
        int attack = mc.gameSettings.keyBindAttack.getKeyCode();
        scaffold.autoAimController.render(scaffold.player, scaffold.world, partialTicks,
                scaffold.autoAimConfig(), physicallyHeld(attack));
    }

    /** Observes vanilla's real Entity.turn mouse deltas; it never cancels the user's input. */
    public static void observeMouseTurn(Entity entity, float yaw, float pitch) {
        LegitScaffold scaffold = instance;
        if (scaffold == null || !scaffold.isEnabled() || !scaffold.autoAim.isToggled()
                || entity != mc.thePlayer) return;
        scaffold.autoAimController.observeMouseTurn(yaw, pitch, scaffold.autoAimConfig());
    }

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!debugOverlay.isToggled() || !autoAim.isToggled() || player == null || world == null) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        ScaffoldPlacementTarget target = autoAimController.target();
        String targetText = target.valid ? target.support + " " + target.face : "none";
        drawDebug("Scaffold Aim: " + autoAimController.state(), 5, 5, 0xFFFFFFFF);
        drawDebug(String.format(java.util.Locale.ROOT, "error %.2f / %.2f", autoAimController.yawError(), autoAimController.pitchError()), 5, 16, 0xFFB9D7FF);
        drawDebug("target: " + targetText + (autoAimController.temporaryPitchRange() ? " (pitch widened)" : ""), 5, 27, 0xFFB9D7FF);
        int centerX = resolution.getScaledWidth() / 2;
        int centerY = resolution.getScaledHeight() / 2;
        int endX = centerX + (int) Math.max(-90, Math.min(90, autoAimController.yawError() * 4.0F));
        int endY = centerY + (int) Math.max(-70, Math.min(70, autoAimController.pitchError() * 4.0F));
        drawDebugLine(centerX, centerY, endX, endY, 0xCC76D7FF);
    }

    private AutoAimController.Config autoAimConfig() {
        aimConfig.aimMode = aimMode.getMode();
        aimConfig.bridgeDirection = bridgeDirection.getMode();
        aimConfig.aimPointMode = aimPoint.getMode();
        aimConfig.snapYaw = snapYaw.isToggled();
        aimConfig.movementCorrection = movementCorrection.isToggled();
        aimConfig.mouseOverride = mouseOverride.isToggled();
        aimConfig.pauseAttacking = pauseAttacking.isToggled();
        aimConfig.onlyMoving = onlyWhenMoving.isToggled();
        aimConfig.pitchMin = pitchRange.getInputMin();
        aimConfig.pitchMax = pitchRange.getInputMax();
        aimConfig.aimSpeed = aimSpeed.getInput();
        aimConfig.randomization = randomization.getInput();
        aimConfig.reactionMinTicks = reactionDelay.getInputMin();
        aimConfig.reactionMaxTicks = reactionDelay.getInputMax();
        aimConfig.placeTolerance = placeTolerance.getInput();
        aimConfig.maxTurnPerTick = maxTurnPerTick.getInput();
        aimConfig.fovLimit = fovLimit.getInput();
        aimConfig.overrideTimeoutMillis = overrideTimeout.getInput();
        aimConfig.mouseSensitivity = mc.gameSettings.mouseSensitivity;
        return aimConfig;
    }

    private void drawDebug(String text, float x, float y, int color) {
        if (FontUtil.poppinsMedium != null) FontUtil.poppinsMedium.drawStringWithShadow(text, x, y, color);
        else mc.fontRendererObj.drawStringWithShadow(text, (int) x, (int) y, color);
    }

    private static void drawDebugLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            Gui.drawRect(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
            int twice = error * 2;
            if (twice >= dy) { error += dy; x0 += sx; }
            if (twice <= dx) { error += dx; y0 += sy; }
        }
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
