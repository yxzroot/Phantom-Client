package keystrokesmod.client.clickgui.apple.rebuild;

/** Fixed-capacity recent GUI event list. */
public final class NotificationCenter {
    private static final int CAPACITY = 12;
    private final String[] messages = new String[CAPACITY];
    private final long[] times = new long[CAPACITY];
    private final Anim open = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.DRAWER_OPEN);
    private int count;
    private boolean showing, unread;

    public void add(String message) {
        for (int i = Math.min(count, CAPACITY - 1); i > 0; --i) { messages[i] = messages[i - 1]; times[i] = times[i - 1]; }
        messages[0] = message; times[0] = System.nanoTime(); count = Math.min(CAPACITY, count + 1); if (!showing) unread = true;
    }
    public void toggle() { showing = !showing; if (showing) unread = false; open.setTarget(showing ? 1F : 0F); }
    public void close() { showing = false; open.setTarget(0F); }
    public void dismiss() { showing = false; unread = false; open.snapTo(0F); }
    public boolean showing() { return showing || open.get() > .01F; }
    public boolean unread() { return unread; }
    public void update(float dt) { open.update(dt); }
    public void draw(float right, float top) {
        if (open.get() <= .01F) return;
        float width = 220F, height = Math.max(36F, count * 24F + 16F), x = right - width, y = top + 22F - 8F * (1F - open.get());
        int alpha = UiDraw.alpha(0xF212151C, open.get());
        UiDraw.shadow(x, y, right, y + height, 7F, UiDraw.alpha(0xFF000000, open.get() * .45F), 7);
        UiDraw.rounded(x, y, right, y + height, 7F, alpha); UiDraw.outline(x, y, right, y + height, 7F, UiDraw.alpha(GuiTokens.WINDOW_BORDER, open.get()));
        if (count == 0) UiDraw.text("No recent notifications", x + 10F, y + 12F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, open.get()));
        for (int i = 0; i < count; i++) UiDraw.text(messages[i], x + 10F, y + 9F + i * 24F, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, open.get()));
    }
    public boolean contains(float mx, float my, float right, float top) {
        if (!showing()) return false;
        float height = Math.max(36F, count * 24F + 16F); return UiDraw.inside(mx, my, right - 220F, top + 14F, right, top + 14F + height);
    }
}
