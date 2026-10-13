package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.GameLoopEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Keyboard;

/** Render-only perspective; the player's actual rotation and movement are never modified. */
public final class Freelook extends Module {
    public enum ActivationMode { Hold, Toggle }
    public enum Perspective { Back, Front }

    private static Freelook instance;
    private final ComboSetting<ActivationMode> activation = new ComboSetting<>("Activation Mode", ActivationMode.Hold);
    private final ComboSetting<Perspective> perspective = new ComboSetting<>("Perspective", Perspective.Back);
    private final SliderSetting sensitivity = new SliderSetting("Camera Sensitivity (%)", 100, 20, 200, 5);
    private final SliderSetting distance = new SliderSetting("Camera Distance", 4, 2, 6, 0.25);
    private final TickSetting smooth = new TickSetting("Smooth Camera", true);
    private EntityPlayerSP player;
    private WorldClient world;
    private int previousPerspective;
    private int selectedPerspective;
    private boolean active;
    private float targetYaw, targetPitch, cameraYaw, cameraPitch;
    private long lastFrame;

    public Freelook() {
        super("Freelook", ModuleCategory.render);
        withKeycode(Keyboard.KEY_LMENU);
        withDescription("Look around without changing your facing direction. Hold Left Alt by default.");
        registerSettings(new DescriptionSetting(getDescription()), activation, sensitivity, smooth, perspective, distance);
        instance = this;
    }

    @Override public void keybind() {
        if (activation.getMode() != ActivationMode.Hold) {
            super.keybind();
            return;
        }
        boolean down = getKeycode() > 0 && Keyboard.isCreated() && Keyboard.isKeyDown(getKeycode());
        if (down && !isEnabled()) enable();
        else if (!down && isEnabled()) disable();
    }

    @Override public void onEnable() {
        // Toggle mode opens immediately; hold mode opens only while its bind is held.
        if (mc.thePlayer != null && mc.theWorld != null && mc.currentScreen == null)
            start();
    }

    @Override public void onDisable() { stop(); }

    @Subscribe public void onGameLoop(GameLoopEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead
                || (active && (mc.thePlayer != player || mc.theWorld != world))) {
            disable();
            return;
        }
        if (active && (mc.currentScreen != null || !mc.inGameHasFocus
                || mc.gameSettings.thirdPersonView != selectedPerspective)) {
            stop();
        } else if (!active && mc.currentScreen == null && mc.inGameHasFocus
                && (activation.getMode() == ActivationMode.Toggle
                    || getKeycode() > 0 && Keyboard.isCreated() && Keyboard.isKeyDown(getKeycode()))) {
            start();
        }
    }

    private void start() {
        if (active || mc.thePlayer == null || mc.thePlayer.isDead || mc.theWorld == null) return;
        player = mc.thePlayer;
        world = mc.theWorld;
        previousPerspective = mc.gameSettings.thirdPersonView;
        selectedPerspective = perspective.getMode() == Perspective.Front ? 2 : 1;
        targetYaw = cameraYaw = player.rotationYaw;
        targetPitch = cameraPitch = player.rotationPitch;
        lastFrame = System.nanoTime();
        mc.gameSettings.thirdPersonView = selectedPerspective;
        active = true;
    }

    private void stop() {
        if (active && mc.gameSettings.thirdPersonView == selectedPerspective)
            mc.gameSettings.thirdPersonView = previousPerspective;
        active = false;
        player = null;
        world = null;
    }

    /** Called by the render hook even if Raven has stopped dispatching keybind events. */
    public static boolean rendering(Entity entity) {
        Freelook look = instance;
        if (look == null || !look.active) return false;
        // Other entities may receive turns too; only this player's camera is ours.
        if (entity != look.player) return false;
        if (!look.isEnabled() || mc.getRenderViewEntity() != look.player || mc.thePlayer != look.player
                || mc.theWorld != look.world || look.player.isDead || mc.currentScreen != null
                || !mc.inGameHasFocus || mc.gameSettings.thirdPersonView != look.selectedPerspective) {
            look.stop();
            return false;
        }
        return true;
    }

    public static boolean consumeMouse(Entity entity, float yaw, float pitch) {
        if (!rendering(entity)) return false;
        Freelook look = instance;
        float scale = (float) look.sensitivity.getInput() / 100.0F;
        look.targetYaw += yaw * 0.15F * scale;
        look.targetPitch = Math.max(-90, Math.min(90, look.targetPitch - pitch * 0.15F * scale));
        return true;
    }

    public static void frame(Entity entity) {
        if (!rendering(entity)) return;
        Freelook look = instance;
        long now = System.nanoTime();
        float delta = Math.max(0.0F, Math.min(0.1F, (now - look.lastFrame) / 1_000_000_000.0F));
        look.lastFrame = now;
        // Exponential easing is frame-rate independent; no delayed mouse events or rotation packets.
        float alpha = look.smooth.isToggled() ? 1.0F - (float) Math.exp(-delta * 24.0F) : 1.0F;
        look.cameraYaw += (look.targetYaw - look.cameraYaw) * alpha;
        look.cameraPitch += (look.targetPitch - look.cameraPitch) * alpha;
    }

    public static float angle(Entity entity, float original, int axis) {
        if (!rendering(entity)) return original;
        return axis == 0 ? instance.cameraYaw : instance.cameraPitch;
    }

    public static float cameraDistance(float original) {
        return instance != null && rendering(mc.getRenderViewEntity()) ? (float) instance.distance.getInput() : original;
    }

    public static boolean blockInteraction() {
        return instance != null && rendering(mc.thePlayer);
    }
}
