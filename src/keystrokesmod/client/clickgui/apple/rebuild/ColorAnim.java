package keystrokesmod.client.clickgui.apple.rebuild;

/** Packed ARGB transition backed by four reusable scalar animations. */
public final class ColorAnim {
    private final Anim a, r, g, b;

    public ColorAnim(int initial, Anim.Mode mode, float duration) {
        a = new Anim(initial >>> 24 & 255, mode, duration);
        r = new Anim(initial >>> 16 & 255, mode, duration);
        g = new Anim(initial >>> 8 & 255, mode, duration);
        b = new Anim(initial & 255, mode, duration);
    }

    public void setTarget(int color) {
        a.setTarget(color >>> 24 & 255); r.setTarget(color >>> 16 & 255);
        g.setTarget(color >>> 8 & 255); b.setTarget(color & 255);
    }

    public void snapTo(int color) {
        a.snapTo(color >>> 24 & 255); r.snapTo(color >>> 16 & 255);
        g.snapTo(color >>> 8 & 255); b.snapTo(color & 255);
    }

    public void update(float dt) { a.update(dt); r.update(dt); g.update(dt); b.update(dt); }

    public int get() {
        return clamp(a.get()) << 24 | clamp(r.get()) << 16 | clamp(g.get()) << 8 | clamp(b.get());
    }

    private static int clamp(float value) { return Math.max(0, Math.min(255, Math.round(value))); }
}
