package keystrokesmod.client.clickgui.apple.rebuild;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Native-framebuffer GUI transform, independent of Minecraft's GUI scale. */
public final class GuiRenderContext {
    private static int framebufferWidth, framebufferHeight, minecraftScale = 1, lastGuiScale = Integer.MIN_VALUE;
    private static float uiScale = 1F, inverseUiScale = 1F, designWidth, designHeight;
    private static boolean initialized;

    private GuiRenderContext() { }

    /** Recomputes only on open, a framebuffer resize, or the persisted Phantom UI-scale change. */
    public static boolean update(Minecraft mc) {
        int width = Math.max(1, mc.displayWidth), height = Math.max(1, mc.displayHeight);
        int requestedScale = Float.floatToIntBits(GuiPreferences.guiScale());
        boolean resized = !initialized || width != framebufferWidth || height != framebufferHeight || requestedScale != lastGuiScale;
        try { resized |= Display.wasResized(); } catch (Throwable ignored) { }
        if (!resized) return false;
        framebufferWidth = width;
        framebufferHeight = height;
        minecraftScale = scaleFactor(mc);
        float screenScale = Math.max(.75F, Math.min(1.5F, framebufferHeight / 1080F));
        uiScale = screenScale * GuiPreferences.guiScale();
        inverseUiScale = 1F / uiScale;
        designWidth = framebufferWidth * inverseUiScale;
        designHeight = framebufferHeight * inverseUiScale;
        lastGuiScale = requestedScale;
        initialized = true;
        return true;
    }

    public static int designWidth() { return Math.round(designWidth); }
    public static int designHeight() { return Math.round(designHeight); }
    public static float mouseX(int scaledX) { return scaledX * minecraftScale * inverseUiScale; }
    public static float mouseY(int scaledY) { return scaledY * minecraftScale * inverseUiScale; }
    public static float eventX(int nativeX) { return nativeX * inverseUiScale; }
    public static float eventY(int nativeY) { return nativeY * inverseUiScale; }
    public static int framebufferWidth() { return framebufferWidth; }
    public static int framebufferHeight() { return framebufferHeight; }
    public static int minecraftScale() { return minecraftScale; }
    public static float uiScale() { return uiScale; }
    public static float inverseUiScale() { return inverseUiScale; }

    private static int scaleFactor(Minecraft mc) {
        int scale = 1, requested = mc.gameSettings.guiScale;
        if (requested == 0) requested = 1000;
        while (scale < requested && mc.displayWidth / (scale + 1) >= 320 && mc.displayHeight / (scale + 1) >= 240) ++scale;
        return scale;
    }
}
