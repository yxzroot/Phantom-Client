package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.GameLoopEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Holds the ordinary sneak key near an unsupported walking edge. Never places or clicks. */
public final class BridgeHelper extends Module {
    private static BridgeHelper instance;
    private final SliderSetting edgeDistance = new SliderSetting("Edge Distance", 0.10, 0.07, 0.10, 0.01);
    private final SliderSetting releaseDelay = new SliderSetting("Unsneak Delay (ms)", 30, 0, 300, 10);
    private final TickSetting requireBlocks = new TickSetting("Only While Holding Blocks", false);

    private boolean ownsSneak;
    private int ownedKey;
    private long safeSince;
    private World world;
    private EntityPlayerSP player;

    public BridgeHelper() {
        super("Bridge Helper", ModuleCategory.movement);
        instance = this;
        withDescription("Holds sneak at edges; place blocks yourself. Bind a key in the module menu.");
        registerSettings(new DescriptionSetting(getDescription()), edgeDistance, releaseDelay, requireBlocks);
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
        world = null;
        player = null;
    }

    @Subscribe
    public void onClientTick(GameLoopEvent event) {
        if (world != mc.theWorld || player != mc.thePlayer) {
            releaseSneak();
            world = mc.theWorld;
            player = mc.thePlayer;
        }
        if (world == null || player == null || player.isDead || mc.currentScreen != null || !mc.inGameHasFocus) {
            releaseSneak();
            return;
        }

        ItemStack held = player.getHeldItem();
        if (requireBlocks.isToggled() && (held == null || held.stackSize <= 0 || !(held.getItem() instanceof ItemBlock))) {
            releaseSneak();
            return;
        }

        int key = mc.gameSettings.keyBindSneak.getKeyCode();
        if (ownsSneak && key != ownedKey)
            releaseSneak();
        if (physicallyHeld(key)) {
            // Hand off a held Shift to the edge guard before the physical key
            // is released; do not clear the real key while the user holds it.
            float forward = player.movementInput.moveForward;
            float strafe = player.movementInput.moveStrafe;
            if (forward != 0 || strafe != 0) {
                double yaw = Math.toRadians(player.rotationYaw);
                double dx = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
                double dz = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
                double size = Math.sqrt(dx * dx + dz * dz);
                if (size > 0.001 && approachingEdge(dx / size, dz / size)) {
                    ownedKey = key;
                    ownsSneak = true;
                    safeSince = 0L;
                    return;
                }
            }
            releaseSneak();
            return;
        }

        float forward = player.movementInput.moveForward;
        float strafe = player.movementInput.moveStrafe;
        if (forward == 0 && strafe == 0) {
            releaseSneak();
            return;
        }
        double yaw = Math.toRadians(player.rotationYaw);
        double dx = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
        double dz = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
        double size = Math.sqrt(dx * dx + dz * dz);
        if (size < 0.001) {
            releaseSneak();
            return;
        }
        dx /= size;
        dz /= size;

        if (approachingEdge(dx, dz)) {
            safeSince = 0L;
            KeyBinding.setKeyBindState(key, true);
            ownedKey = key;
            ownsSneak = true;
        } else if (ownsSneak) {
            long now = System.nanoTime();
            if (releaseDelay.getInput() == 0)
                releaseSneak();
            else if (safeSince == 0L)
                safeSince = now;
            else if (now - safeSince >= (long) (releaseDelay.getInput() * 1_000_000L))
                releaseSneak();
        }
    }

    private boolean approachingEdge(double dx, double dz) {
        if (!player.onGround)
            return ownsSneak; // keep holding until a landing is known to be safe

        AxisAlignedBB body = player.getEntityBoundingBox();
        // The old 0.30 + distance probe anticipated the edge too far in
        // advance. At 0.10 this one checks roughly at the leading toe.
        double lead = 0.20 + edgeDistance.getInput();
        double centerX = player.posX + dx * lead;
        double centerZ = player.posZ + dz * lead;
        double sideX = -dz * 0.17;
        double sideZ = dx * 0.17;

        // A single corner over air is normal while diagonal bridging. Sneak
        // only if the center loses support or both front corners are exposed.
        if (!supported(centerX, centerZ, body.minY))
            return true;
        return !supported(centerX - sideX, centerZ - sideZ, body.minY)
                && !supported(centerX + sideX, centerZ + sideZ, body.minY);
    }

    private boolean supported(double x, double z, double footY) {
        AxisAlignedBB foot = new AxisAlignedBB(x - 0.035, footY - 0.15, z - 0.035,
                x + 0.035, footY + 0.01, z + 0.035);
        return !world.getCollidingBoundingBoxes(player, foot).isEmpty();
    }

    /** Sneak on the exact input update where the physical key is released. */
    public static void protectInput(MovementInput input) {
        BridgeHelper helper = instance;
        if (helper == null || !helper.isEnabled() || helper.player == null
                || helper.world == null || helper.player != mc.thePlayer
                || helper.world != mc.theWorld || input != helper.player.movementInput
                || mc.currentScreen != null || !mc.inGameHasFocus) return;
        ItemStack held = helper.player.getHeldItem();
        if (helper.requireBlocks.isToggled()
                && (held == null || held.stackSize <= 0 || !(held.getItem() instanceof ItemBlock))) return;
        double yaw = Math.toRadians(helper.player.rotationYaw);
        double dx = -Math.sin(yaw) * input.moveForward + Math.cos(yaw) * input.moveStrafe;
        double dz = Math.cos(yaw) * input.moveForward + Math.sin(yaw) * input.moveStrafe;
        double size = Math.sqrt(dx * dx + dz * dz);
        boolean protect = size > 0.001 && helper.approachingEdge(dx / size, dz / size);
        if (!protect && !helper.player.onGround) protect = helper.ownsSneak;
        if (!protect) return;
        if (!input.sneak) {
            input.sneak = true;
            input.moveForward *= 0.3F;
            input.moveStrafe *= 0.3F;
        }
        int key = mc.gameSettings.keyBindSneak.getKeyCode();
        if (!physicallyHeld(key)) KeyBinding.setKeyBindState(key, true);
        helper.ownedKey = key;
        helper.ownsSneak = true;
        helper.safeSince = 0L;
    }

    private static boolean physicallyHeld(int key) {
        return (key > 0 && Keyboard.isCreated() && Keyboard.isKeyDown(key))
                || (key < 0 && Mouse.isCreated() && Mouse.isButtonDown(key + 100));
    }

    private void releaseSneak() {
        if (ownsSneak) {
            KeyBinding.setKeyBindState(ownedKey, physicallyHeld(ownedKey));
            ownsSneak = false;
        }
        safeSince = 0L;
    }
}
