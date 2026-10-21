package keystrokesmod.client.utils.font;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Opt-in diagnostics for the Phantom HUD, plus an always-on stall watchdog.
 *
 * <p>Diagnostics used to print to {@code System.err} and open/write/close a file on every call,
 * from the client render thread. {@code System.err} is a synchronized {@link java.io.PrintStream}
 * shared with the launcher's IPC pipe, so a single blocked write there parks the client thread
 * while it waits for the print stream monitor. This class therefore:
 *
 * <ul>
 *   <li>writes nothing unless explicitly enabled ({@code -Dphantom.diagnostics=true} or a marker
 *       file at {@code config/phantom-diagnostics.enabled});</li>
 *   <li>never touches {@code System.out}/{@code System.err};</li>
 *   <li>only appends to an in-memory buffer on the calling thread and flushes from the watchdog
 *       thread, so the client thread performs no file I/O at all.</li>
 * </ul>
 *
 * <p>The watchdog only starts once {@link #heartbeat()} has been observed, so it can never emit a
 * false dump. When the client thread stops pulsing it writes every thread's stack, with lock
 * owner information, to {@code logs/phantom-stall-*.txt}. That is what makes a future freeze
 * diagnosable from a file instead of an unresponsive window.
 */
public final class PhantomDiagnostics {
    private static final Set<String> ONCE = new HashSet<>();
    private static final Object LOCK = new Object();
    private static final long STALL_NANOS = 10_000_000_000L;
    private static final long FLUSH_NANOS = 500_000_000L;

    private static boolean started;
    private static boolean enabled;
    private static boolean watchdogStarted;
    private static volatile long heartbeatNanos;
    private static File file;
    private static PrintWriter writer;
    private static long lastFlush;

    private PhantomDiagnostics() { }

    public static void begin(String details) {
        if (started) return;
        started = true;
        enabled = Boolean.getBoolean("phantom.diagnostics") || markerFile().isFile();
        if (enabled) {
            try {
                file = new File(bootDir(), "logs/phantom-hud-debug.log");
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                writer = new PrintWriter(new FileWriter(file, false));
                writer.println(System.currentTimeMillis() + " === Phantom HUD diagnostic session ===");
                startWatchdog();
            } catch (Throwable ignored) {
                file = null;
                writer = null;
            }
        }
        log("BOOT " + details);
    }

    public static void once(String key, String details) {
        if (ONCE.add(key)) log(details);
    }

    public static void log(String details) {
        if (writer == null) return;
        synchronized (LOCK) {
            writer.println(System.currentTimeMillis() + " [Phantom HUD] " + details);
        }
    }

    public static void failure(String stage, Throwable error) {
        if (writer == null) return;
        synchronized (LOCK) {
            writer.println(System.currentTimeMillis() + " [Phantom HUD] FAILURE at " + stage + ": "
                    + error.getClass().getName() + ": " + error.getMessage());
            error.printStackTrace(writer);
            writer.flush();
        }
    }

    /**
     * Cheap per-frame pulse from the client thread. Starts the watchdog on the first call and
     * performs a single volatile store afterwards.
     */
    public static void heartbeat() {
        heartbeatNanos = System.nanoTime();
        if (!watchdogStarted) startWatchdog();
    }

    private static void startWatchdog() {
        synchronized (PhantomDiagnostics.class) {
            if (watchdogStarted) return;
            watchdogStarted = true;
            Thread thread = new Thread(PhantomDiagnostics::watch, "Phantom stall watchdog");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    private static void watch() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long lastDump = 0L;
        boolean seen = false;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException interrupted) {
                return;
            }
            flush();
            long pulse = heartbeatNanos;
            if (pulse != 0L) seen = true;
            if (!seen) continue;
            long stall = System.nanoTime() - pulse;
            if (stall < STALL_NANOS) {
                lastDump = 0L;
                continue;
            }
            if (lastDump != 0L && System.nanoTime() - lastDump < STALL_NANOS) continue;
            lastDump = System.nanoTime();
            dump(bean, stall);
        }
    }

    private static void flush() {
        if (writer == null) return;
        long now = System.nanoTime();
        if (now - lastFlush < FLUSH_NANOS) return;
        lastFlush = now;
        synchronized (LOCK) {
            writer.flush();
        }
    }

    private static void dump(ThreadMXBean bean, long stall) {
        File output = new File(bootDir(), "logs/phantom-stall-" + System.currentTimeMillis() + ".txt");
        try {
            File parent = output.getParentFile();
            if (parent != null) parent.mkdirs();
            try (PrintWriter out = new PrintWriter(new FileWriter(output, false))) {
                out.println("Phantom stall watchdog");
                out.println("client thread has not ticked for " + (stall / 1_000_000_000L) + "s");
                out.println("time: " + new Date());
                long[] deadlocked = bean.findDeadlockedThreads();
                if (deadlocked != null && deadlocked.length > 0) {
                    out.println();
                    out.println("=== DEADLOCKED THREADS ===");
                    for (ThreadInfo info : bean.getThreadInfo(deadlocked, true, true)) out.println(info);
                }
                out.println();
                out.println("=== ALL THREADS (with lock owners) ===");
                for (ThreadInfo info : bean.dumpAllThreads(true, true)) out.println(info);
            }
        } catch (Throwable ignored) {
            // Never let the watchdog itself become a problem.
        }
    }

    private static File markerFile() {
        return new File(bootDir(), "config/phantom-diagnostics.enabled");
    }

    private static File bootDir() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.mcDataDir != null) return mc.mcDataDir;
        } catch (Throwable ignored) {
            // Fall through to the working directory while the game is still booting.
        }
        return new File(System.getProperty("user.dir", "."));
    }
}
