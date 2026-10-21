package keystrokesmod.client.clickgui.apple.rebuild;

/** The one GUI frame clock. */
public final class AnimClock {
    private long previous;
    private float dt = 1F / 60F;

    public float begin() {
        long now = System.nanoTime();
        if (previous != 0L) dt = Math.min(.05F, Math.max(0F, (now - previous) / 1_000_000_000F));
        previous = now;
        return dt;
    }

    public void reset() { previous = 0L; dt = 1F / 60F; }
    public float dt() { return dt; }
}
