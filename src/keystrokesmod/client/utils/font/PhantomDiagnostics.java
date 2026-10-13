package keystrokesmod.client.utils.font;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/** Milestone-only diagnostics for Phantom HUD. Writes both latest.log and a dedicated file. */
public final class PhantomDiagnostics {
    private static final Set<String> ONCE = new HashSet<>();
    private static boolean started;
    private static File file;

    private PhantomDiagnostics() { }

    public static void begin(String details) {
        if (started) return;
        started = true;
        try {
            file = new File(Minecraft.getMinecraft().mcDataDir, "logs/phantom-hud-debug.log");
            write("=== Phantom HUD diagnostic session ===", false, null);
        } catch (Throwable ignored) { file = null; }
        log("BOOT " + details);
    }

    public static void once(String key, String details) {
        if (ONCE.add(key)) log(details);
    }

    public static void log(String details) {
        String line = "[Phantom HUD] " + details;
        System.err.println(line);
        write(line, true, null);
    }

    public static void failure(String stage, Throwable error) {
        String line = "[Phantom HUD] FAILURE at " + stage + ": "
                + error.getClass().getName() + ": " + error.getMessage();
        System.err.println(line);
        error.printStackTrace();
        write(line, true, error);
    }

    private static void write(String line, boolean append, Throwable error) {
        if (file == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, append))) {
                writer.println(System.currentTimeMillis() + " " + line);
                if (error != null) error.printStackTrace(writer);
            }
        } catch (Throwable ignored) {
            // The standard Minecraft log remains available if the dedicated file cannot be written.
        }
    }
}
