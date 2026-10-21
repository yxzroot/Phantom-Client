package keystrokesmod.client.clickgui.apple.rebuild;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import keystrokesmod.client.utils.font.PhantomDiagnostics;

/** Fixed-storage GUI diagnostics. It never allocates or logs during a normal frame. */
public final class GuiProfiler {
    private static final int HISTORY = 120;
    private static final float[] FRAME_MS = new float[HISTORY];
    private static final List<GarbageCollectorMXBean> GC = ManagementFactory.getGarbageCollectorMXBeans();
    private static final java.lang.management.ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final com.sun.management.ThreadMXBean ALLOCATION = THREADS instanceof com.sun.management.ThreadMXBean
            ? (com.sun.management.ThreadMXBean) THREADS : null;

    private static int frames, cursor;
    private static long sessionGcCount, sessionGcMillis, sessionAllocated;
    private static boolean allocationAvailable;
    private static float lastWindow, lastHeader, lastTabs, lastGrid, lastText, lastSettings, lastTooltip;

    private GuiProfiler() { }

    /** Called once after the OpenGL GUI context is ready. */
    public static void openSession() {
        frames = cursor = 0;
        for (int i = 0; i < HISTORY; i++) FRAME_MS[i] = 0F;
        sessionGcCount = gcCountRaw();
        sessionGcMillis = gcMillisRaw();
        allocationAvailable = false;
        sessionAllocated = -1L;
        if (ALLOCATION != null) {
            try {
                if (ALLOCATION.isThreadAllocatedMemorySupported()) {
                    if (!ALLOCATION.isThreadAllocatedMemoryEnabled()) ALLOCATION.setThreadAllocatedMemoryEnabled(true);
                    sessionAllocated = ALLOCATION.getThreadAllocatedBytes(Thread.currentThread().getId());
                    allocationAvailable = sessionAllocated >= 0L;
                }
            } catch (Throwable ignored) {
                allocationAvailable = false;
                sessionAllocated = -1L;
            }
        }
    }

    public static void sections(long window, long header, long tabs, long grid, long text, long settings, long tooltip) {
        lastWindow = millis(window); lastHeader = millis(header); lastTabs = millis(tabs); lastGrid = millis(grid);
        lastText = millis(text); lastSettings = millis(settings); lastTooltip = millis(tooltip);
    }

    public static void frame(long totalNanos) {
        float frame = millis(totalNanos);
        FRAME_MS[cursor] = frame;
        cursor = (cursor + 1) % HISTORY;
        if (frames < HISTORY) ++frames;
        if (frame > 8F) {
            // Diagnostics deliberately write only through the asynchronous, opt-in diagnostic sink.
            PhantomDiagnostics.log("GUI slow " + tenths(frame) + "ms [window=" + tenths(lastWindow)
                    + ", header=" + tenths(lastHeader) + ", tabs=" + tenths(lastTabs)
                    + ", grid=" + tenths(lastGrid) + ", text=" + tenths(lastText)
                    + ", settings=" + tenths(lastSettings) + ", tooltip=" + tenths(lastTooltip) + "]");
        }
    }

    public static int samples() { return frames; }
    public static float sample(int oldestIndex) {
        if (oldestIndex < 0 || oldestIndex >= frames) return 0F;
        int base = frames == HISTORY ? cursor : 0;
        return FRAME_MS[(base + oldestIndex) % HISTORY];
    }
    public static float averageMillis() {
        if (frames == 0) return 0F;
        float sum = 0F;
        for (int i = 0; i < frames; i++) sum += sample(i);
        return sum / frames;
    }
    public static float worstMillis() {
        float worst = 0F;
        for (int i = 0; i < frames; i++) worst = Math.max(worst, sample(i));
        return worst;
    }
    public static long gcCountSinceOpen() { return Math.max(0L, gcCountRaw() - sessionGcCount); }
    public static long gcMillisSinceOpen() { return Math.max(0L, gcMillisRaw() - sessionGcMillis); }
    public static long allocationBytes() {
        if (!allocationAvailable) return -1L;
        try { return Math.max(0L, ALLOCATION.getThreadAllocatedBytes(Thread.currentThread().getId()) - sessionAllocated); }
        catch (Throwable ignored) { allocationAvailable = false; return -1L; }
    }
    public static float windowMillis() { return lastWindow; }
    public static float headerMillis() { return lastHeader; }
    public static float tabsMillis() { return lastTabs; }
    public static float gridMillis() { return lastGrid; }
    public static float textMillis() { return lastText; }
    public static float settingsMillis() { return lastSettings; }
    public static float tooltipMillis() { return lastTooltip; }

    private static long gcCountRaw() {
        long total = 0L;
        for (int i = 0; i < GC.size(); i++) {
            long value = GC.get(i).getCollectionCount();
            if (value > 0L) total += value;
        }
        return total;
    }
    private static long gcMillisRaw() {
        long total = 0L;
        for (int i = 0; i < GC.size(); i++) {
            long value = GC.get(i).getCollectionTime();
            if (value > 0L) total += value;
        }
        return total;
    }
    private static float millis(long nanos) { return nanos / 1_000_000F; }
    private static String tenths(float value) { return String.valueOf(Math.round(value * 10F) / 10F); }
}
