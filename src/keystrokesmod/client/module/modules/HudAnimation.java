package keystrokesmod.client.module.modules;

import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;

/** Appearance and motion of the installed HUD's enabled-module list. */
public final class HudAnimation {
    private static final Map<Module, Long> starts = new IdentityHashMap<>();
    private static final Map<Module, Float> opacity = new IdentityHashMap<>();
    private static final Map<Module, Float> rowPositions = new IdentityHashMap<>();
    private static long lastFrame;
    private static TickSetting rainbow, motion;
    private static SliderSetting rainbowSpeed;

    private HudAnimation() { }

    /** Called from the existing HUD constructor, without replacing its other settings. */
    public static void installSettings(HUD hud) {
        hud.registerSetting(rainbow = new TickSetting("Rainbow Modules", true));
        hud.registerSetting(rainbowSpeed = new SliderSetting("Rainbow Speed", 1.2, 0.3, 3.0, 0.1));
        hud.registerSetting(motion = new TickSetting("Animated Modules", true));
    }

    public static void beginFrame() {
        long now = System.nanoTime();
        if (lastFrame != 0 && now - lastFrame > 2_000_000_000L) {
            starts.clear();
            opacity.clear();
            rowPositions.clear();
        }
        lastFrame = now;
        for (Iterator<Module> it = starts.keySet().iterator(); it.hasNext();) {
            Module module = it.next();
            if (!module.isEnabled() || !module.showInHud()) {
                it.remove();
                opacity.remove(module);
                rowPositions.remove(module);
            }
        }
    }

    public static float x(Module module, float target, boolean rightAligned) {
        long now = System.nanoTime();
        Long start = starts.get(module);
        if (start == null) {
            start = now;
            starts.put(module, start);
        }
        boolean animate = motion == null || motion.isToggled();
        float progress = animate ? clamp((now - start) / 420_000_000F) : 1F;
        float eased = 1F - (float) Math.pow(1F - progress, 4);
        opacity.put(module, animate ? Math.max(0.04F, eased) : 1F);
        float previous = rowPositions.containsKey(module) ? rowPositions.get(module)
                : target + (rightAligned ? 40 : -40);
        float smoothed = animate ? previous + (target - previous) * 0.23F : target;
        if (Math.abs(target - smoothed) < 0.08F) smoothed = target;
        rowPositions.put(module, smoothed);
        float entrance = animate ? (rightAligned ? 31F : -31F) * (1F - eased) : 0F;
        float drift = animate && progress >= 1F ? (float) Math.sin(now / 850_000_000D
                + (module.getName().hashCode() & 7) * 0.55D) * 0.85F : 0F;
        return smoothed + entrance + drift;
    }

    public static int text(int original, Module module) {
        int color = original;
        if (rainbow != null && rainbow.isToggled()) {
            double speed = rainbowSpeed == null ? 1.2 : rainbowSpeed.getInput();
            float hue = (float) ((System.currentTimeMillis() * speed / 9500D
                    + (module.getName().hashCode() & 31) / 70D) % 1D);
            float pulse = 0.90F + 0.1F * (float) Math.sin(System.nanoTime() / 390_000_000D
                    + (module.getName().hashCode() & 7));
            color = Color.HSBtoRGB(hue, 0.68F, pulse);
        } else if (color == -1) {
            float hue = (float) ((System.currentTimeMillis() / 22000D
                    + (module.getName().hashCode() & 7) * 0.008D) % 1D);
            color = Color.HSBtoRGB(hue, 0.48F, 1F);
        }
        return multiplyAlpha(color, module);
    }

    public static int shadow(int original, Module module) {
        return multiplyAlpha(original, module);
    }

    public static int background(int ignored, Module module) {
        int color = 0xA1121915;
        if (rainbow != null && rainbow.isToggled()) {
            double speed = rainbowSpeed == null ? 1.2 : rainbowSpeed.getInput();
            float hue = (float) ((System.currentTimeMillis() * speed / 9500D
                    + (module.getName().hashCode() & 31) / 70D) % 1D);
            int tint = Color.HSBtoRGB(hue, 0.60F, 0.28F);
            color = 0xAE000000 | (tint & 0xFFFFFF);
        }
        return multiplyAlpha(color, module);
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    private static int multiplyAlpha(int color, Module module) {
        Float alpha = opacity.get(module);
        int value = Math.round(((color >>> 24) & 255) * (alpha == null ? 1F : alpha));
        return (Math.max(0, Math.min(255, value)) << 24) | (color & 0xFFFFFF);
    }
}
