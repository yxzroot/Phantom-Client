package keystrokesmod.client.module.modules.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/** Render-frame camera controller. Placement remains owned by LegitScaffold's normal client-tick path. */
final class AutoAimController {
    enum State { IDLE, ACQUIRE, AIMING, SETTLED, RELEASING, OVERRIDDEN }
    enum AimMode {
        AUTO("Auto"), BRIDGE_ANGLE("Bridge angle");
        private final String label;
        AimMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    enum BridgeDirection {
        BACKWARD("Backward"), FORWARD("Forward"), AUTO("Auto");
        private final String label;
        BridgeDirection(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    enum AimPointMode {
        CENTER("Center"), RANDOM_INSIDE_FACE("Random inside face"), EDGE_BIASED("Edge-biased");
        private final String label;
        AimPointMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    static final class Config {
        AimMode aimMode;
        BridgeDirection bridgeDirection;
        AimPointMode aimPointMode;
        boolean snapYaw;
        boolean movementCorrection;
        boolean mouseOverride;
        boolean pauseAttacking;
        boolean onlyMoving;
        double pitchMin;
        double pitchMax;
        double aimSpeed;
        double randomization;
        double reactionMinTicks;
        double reactionMaxTicks;
        double placeTolerance;
        double maxTurnPerTick;
        double fovLimit;
        double overrideTimeoutMillis;
        float mouseSensitivity;
    }

    private final ScaffoldPlacementTarget target = new ScaffoldPlacementTarget();
    private final AimPointSelector pointSelector = new AimPointSelector();
    private final RotationSmoother smoother = new RotationSmoother();
    private final GcdFix gcd = new GcdFix();
    private State state = State.IDLE;
    private long reactionUntil;
    private long overrideUntil;
    private long pauseUntil;
    private long lastFrame;
    private float inputYaw = Float.NaN;
    private float movementYaw;
    private float targetSpeed;
    private float desiredYaw;
    private float desiredPitch;
    private float yawError;
    private float pitchError;
    private boolean autoDirectionBackward = true;
    private long noiseSeed = 0xD1B54A32D192ED03L;
    private boolean temporaryPitchRange;

    void reset() {
        target.clear();
        state = State.IDLE;
        reactionUntil = overrideUntil = pauseUntil = lastFrame = 0L;
        inputYaw = Float.NaN;
        autoDirectionBackward = true;
        yawError = pitchError = 0.0F;
        temporaryPitchRange = false;
        smoother.reset();
        gcd.reset();
    }

    void cacheTarget(EntityPlayerSP player, MovingObjectPosition hit, double moveX, double moveZ, Config config) {
        long now = System.nanoTime();
        if (hit == null || hit.getBlockPos() == null || hit.sideHit == null) {
            if (state != State.IDLE && state != State.RELEASING) state = State.RELEASING;
            return;
        }
        movementYaw = yawFromDirection(moveX, moveZ);
        boolean changed = !target.matches(hit);
        if (changed) {
            target.set(hit);
            pointSelector.select(target, player, config.aimPointMode);
        }
        if (changed || state == State.IDLE || state == State.RELEASING) {
            targetSpeed = randomizedSpeed((float) config.aimSpeed, (float) config.randomization);
            reactionUntil = now + reactionDelayNanos(config);
            state = State.ACQUIRE;
            smoother.reset();
            gcd.reset();
        }
        if (Float.isNaN(inputYaw)) inputYaw = player.rotationYaw;
    }

    void observeMouseTurn(float yawDelta, float pitchDelta, Config config) {
        if (!config.mouseOverride || state == State.IDLE || state == State.RELEASING) return;
        // Entity.turn receives count * (sensitivity^3 * 8), before vanilla's 0.15 rotation scale.
        float sensitivity = config.mouseSensitivity * 0.6F + 0.2F;
        float twoCounts = sensitivity * sensitivity * sensitivity * 16.0F;
        if (Math.abs(yawDelta) + Math.abs(pitchDelta) <= twoCounts) return;
        long now = System.nanoTime();
        overrideUntil = now + (long) (config.overrideTimeoutMillis * 1_000_000.0D);
        state = State.OVERRIDDEN;
        smoother.reset();
        gcd.reset();
    }

    /** Captured before the movement-correction transform so Auto direction follows the physical keys. */
    void captureInputIntent(net.minecraft.util.MovementInput input) {
        if (input.moveForward > 0.01F) autoDirectionBackward = true;
        else if (input.moveForward < -0.01F) autoDirectionBackward = false;
    }

    void render(EntityPlayerSP player, World world, float partialTicks, Config config, boolean attackHeld) {
        if (player == null || world == null || player.isDead || !target.valid) {
            release(player, config);
            return;
        }
        long now = System.nanoTime();
        float dt = frameDelta(now);
        if (attackHeld && config.pauseAttacking) pauseUntil = now + 300_000_000L;
        if (now < pauseUntil) return;
        if (config.onlyMoving && player.movementInput.moveForward == 0.0F && player.movementInput.moveStrafe == 0.0F)
            return;
        if (state == State.OVERRIDDEN) {
            if (now < overrideUntil) return;
            state = State.ACQUIRE;
            reactionUntil = now + reactionDelayNanos(config);
            return;
        }
        if (state == State.ACQUIRE) {
            if (now < reactionUntil) return;
            state = State.AIMING;
        }
        if (state == State.RELEASING) {
            applyRelease(player, dt, config);
            return;
        }
        if (state != State.AIMING && state != State.SETTLED) return;

        double eyeX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double eyeY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight();
        double eyeZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        double lead = Math.min(0.05D, dt);
        eyeX += player.motionX * lead;
        eyeY += player.motionY * lead;
        eyeZ += player.motionZ * lead;
        float rawYaw = angleYaw(target.aimX - eyeX, target.aimZ - eyeZ);
        float rawPitch = anglePitch(target.aimX - eyeX, target.aimY - eyeY, target.aimZ - eyeZ);
        if (Math.abs(MathHelper.wrapAngleTo180_float(rawYaw - player.rotationYaw)) > config.fovLimit) {
            state = State.RELEASING;
            return;
        }

        desiredYaw = rawYaw;
        desiredPitch = rawPitch;
        temporaryPitchRange = false;
        if (config.aimMode == AimMode.BRIDGE_ANGLE) {
            float bridge = bridgeYaw(config);
            float correction = clamp(MathHelper.wrapAngleTo180_float(rawYaw - bridge), -3.0F, 3.0F);
            desiredYaw = bridge + correction;
            if (!config.movementCorrection)
                desiredYaw = inputYaw + clamp(MathHelper.wrapAngleTo180_float(desiredYaw - inputYaw), -3.0F, 3.0F);
            if (desiredPitch < config.pitchMin || desiredPitch > config.pitchMax) temporaryPitchRange = true;
        }
        desiredPitch = clamp(desiredPitch, -90.0F, 90.0F);
        if (state == State.SETTLED) {
            float micro = (float) Math.sin(now * 0.000000007D + noiseSeed) * 0.20F;
            desiredYaw += micro;
            desiredPitch = clamp(desiredPitch + micro * 0.6F, -90.0F, 90.0F);
        }
        yawError = MathHelper.wrapAngleTo180_float(desiredYaw - player.rotationYaw);
        pitchError = desiredPitch - player.rotationPitch;
        smoother.step(yawError, pitchError, dt, targetSpeed, (float) config.maxTurnPerTick);
        apply(player, gcd.yaw(smoother.yawStep, config.mouseSensitivity), gcd.pitch(smoother.pitchStep, config.mouseSensitivity));
        yawError = MathHelper.wrapAngleTo180_float(desiredYaw - player.rotationYaw);
        pitchError = desiredPitch - player.rotationPitch;
        state = Math.max(Math.abs(yawError), Math.abs(pitchError)) <= config.placeTolerance
                ? State.SETTLED : State.AIMING;
    }

    MovingObjectPosition verifiedHit(EntityPlayerSP player, World world, Config config) {
        if (state != State.SETTLED || !target.valid
                || Math.max(Math.abs(yawError), Math.abs(pitchError)) > config.placeTolerance) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.playerController == null) return null;
        double reach = minecraft.playerController.getBlockReachDistance();
        Vec3 eyes = player.getPositionEyes(1.0F);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        MovingObjectPosition hit = world.rayTraceBlocks(eyes, end, false, true, false);
        return hit != null && target.support.equals(hit.getBlockPos()) && target.face == hit.sideHit
                && hit.hitVec != null && eyes.distanceTo(hit.hitVec) <= reach + 0.01D ? hit : null;
    }

    void correctMovementInput(net.minecraft.util.MovementInput input, EntityPlayerSP player, Config config) {
        if (!config.movementCorrection || !target.valid || state == State.IDLE || state == State.OVERRIDDEN) {
            inputYaw = player.rotationYaw;
            return;
        }
        if (Float.isNaN(inputYaw)) inputYaw = player.rotationYaw;
        if (Math.abs(MathHelper.wrapAngleTo180_float(player.rotationYaw - inputYaw)) < 0.001F) return;
        double oldRadians = Math.toRadians(inputYaw);
        double x = -Math.sin(oldRadians) * input.moveForward + Math.cos(oldRadians) * input.moveStrafe;
        double z = Math.cos(oldRadians) * input.moveForward + Math.sin(oldRadians) * input.moveStrafe;
        double newRadians = Math.toRadians(player.rotationYaw);
        input.moveForward = (float) (-Math.sin(newRadians) * x + Math.cos(newRadians) * z);
        input.moveStrafe = (float) (Math.cos(newRadians) * x + Math.sin(newRadians) * z);
        inputYaw = player.rotationYaw;
    }

    State state() { return state; }
    float yawError() { return yawError; }
    float pitchError() { return pitchError; }
    boolean temporaryPitchRange() { return temporaryPitchRange; }
    ScaffoldPlacementTarget target() { return target; }

    private void release(EntityPlayerSP player, Config config) {
        if (state != State.IDLE && state != State.RELEASING) state = State.RELEASING;
        if (player != null && state == State.RELEASING) applyRelease(player, frameDelta(System.nanoTime()), config);
    }

    private void applyRelease(EntityPlayerSP player, float dt, Config config) {
        smoother.release(dt);
        apply(player, gcd.yaw(smoother.yawStep, config.mouseSensitivity), gcd.pitch(smoother.pitchStep, config.mouseSensitivity));
        if (smoother.isStill()) {
            state = State.IDLE;
            target.clear();
            gcd.reset();
        }
    }

    private void apply(EntityPlayerSP player, float yaw, float pitch) {
        if (yaw != 0.0F) {
            player.rotationYaw += yaw;
            player.prevRotationYaw += yaw;
        }
        if (pitch != 0.0F) {
            float applied = clamp(player.rotationPitch + pitch, -90.0F, 90.0F) - player.rotationPitch;
            player.rotationPitch += applied;
            player.prevRotationPitch += applied;
        }
    }

    private float bridgeYaw(Config config) {
        float yaw = movementYaw;
        if (config.bridgeDirection == BridgeDirection.BACKWARD)
            yaw += 180.0F;
        else if (config.bridgeDirection == BridgeDirection.AUTO && autoDirectionBackward)
            yaw += 180.0F;
        if (config.snapYaw) yaw = Math.round(yaw / 45.0F) * 45.0F;
        return yaw;
    }

    private long reactionDelayNanos(Config config) {
        double min = Math.min(config.reactionMinTicks, config.reactionMaxTicks);
        double max = Math.max(config.reactionMinTicks, config.reactionMaxTicks);
        return (long) ((min + (max - min) * nextUnit()) * 50_000_000.0D);
    }

    private float randomizedSpeed(float speed, float percentage) {
        return Math.max(1.0F, speed * (1.0F + (float) ((nextUnit() * 2.0D - 1.0D) * percentage / 100.0D)));
    }

    private double nextUnit() {
        noiseSeed ^= noiseSeed << 13;
        noiseSeed ^= noiseSeed >>> 7;
        noiseSeed ^= noiseSeed << 17;
        return (double) (noiseSeed >>> 11) * 0x1.0p-53;
    }

    private float frameDelta(long now) {
        if (lastFrame == 0L) { lastFrame = now; return 1.0F / 60.0F; }
        float delta = Math.max(0.001F, Math.min(0.05F, (now - lastFrame) / 1_000_000_000.0F));
        lastFrame = now;
        return delta;
    }

    private static float angleYaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    }

    private static float anglePitch(double dx, double dy, double dz) {
        return (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    private static float yawFromDirection(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
