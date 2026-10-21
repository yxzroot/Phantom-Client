package keystrokesmod.client.clickgui.apple.rebuild;

import org.lwjgl.opengl.GL11;

/** F9-only section probes. The final drain prevents GUI errors reaching Minecraft's checker. */
public final class GuiGlDebug {
    private static boolean enabled;
    private static int firstError, lastError;
    private static String firstSection = "none", lastSection = "none";
    private GuiGlDebug() { }
    public static void begin(boolean debug) { enabled = debug; firstError = 0; firstSection = "none"; }
    public static void section(String name) {
        if (!enabled) return;
        UiDraw.flushShapes();
        record(GL11.glGetError(), name);
    }
    public static void finish() {
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) record(error, "cleanup");
        if (enabled) { lastError = firstError; lastSection = firstSection; }
    }
    private static void record(int error, String name) { if (error != 0 && firstError == 0) { firstError = error; firstSection = name; } }
    public static void capturedError(int error, String section) { if (enabled) record(error, section); }
    public static String status() { return lastError == 0 ? "none" : lastError + " (" + lastSection + ")"; }
}
