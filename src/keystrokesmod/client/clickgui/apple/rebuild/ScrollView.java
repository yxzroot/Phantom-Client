package keystrokesmod.client.clickgui.apple.rebuild;

/** Reusable bounded inertia scroll state. */
public final class ScrollView {
    private final Anim position = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_HOVER);
    private final Anim barAlpha = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.INLINE_EXPAND);
    private float target, max, viewport, content, velocity, visualOverscroll;
    private long lastInput;
    private boolean draggingBar;
    private float grabOffset;

    public void setMetrics(float contentHeight, float viewportHeight) {
        float previousMax = max;
        content = Math.max(0F, contentHeight); viewport = Math.max(0F, viewportHeight); max = Math.max(0F, content - viewport);
        if (max < previousMax) {
            visualOverscroll = 0F;
            target = clamp(target, 0F, max);
            if (position.get() < 0F || position.get() > max) position.snapTo(clamp(position.get(), 0F, max));
            if (max == 0F || velocity > 0F && target >= max || velocity < 0F && target <= 0F) velocity = 0F;
        }
    }
    public void wheel(float amount) {
        if (max <= 0F || amount == 0F || draggingBar) return;
        if (amount > 0F && target <= 0F || amount < 0F && target >= max) {
            visualOverscroll = clamp(visualOverscroll - amount * 6F, -MotionTokens.SCROLL_OVERSCROLL_PX, MotionTokens.SCROLL_OVERSCROLL_PX);
            velocity = 0F; lastInput = System.nanoTime(); barAlpha.setTarget(1F); return;
        }
        velocity -= amount * MotionTokens.SCROLL_WHEEL_IMPULSE; target = clamp(target + velocity * .12F, 0F, max);
        lastInput = System.nanoTime(); barAlpha.setTarget(1F);
    }
    public void page(float amount) { velocity = visualOverscroll = 0F; target = clamp(target + amount, 0F, max); lastInput = System.nanoTime(); barAlpha.setTarget(1F); }
    public void home() { velocity = visualOverscroll = 0F; target = 0F; }
    public void end() { velocity = visualOverscroll = 0F; target = max; }
    public void reset() { draggingBar = false; grabOffset = 0F; velocity = visualOverscroll = 0F; target = 0F; position.snapTo(0F); barAlpha.snapTo(0F); lastInput = 0L; }
    public void ensureVisible(float bottom) { if (bottom > target + viewport) page(bottom - viewport - target); }
    public void update(float dt) {
        velocity *= (float) Math.exp(-MotionTokens.SCROLL_VELOCITY_DECAY * dt);
        if (Math.abs(velocity) > .05F && !draggingBar) {
            float proposed = target + velocity * dt;
            target = clamp(proposed, 0F, max);
            if (proposed != target) {
                visualOverscroll = clamp(visualOverscroll + (proposed - target) * .35F, -MotionTokens.SCROLL_OVERSCROLL_PX, MotionTokens.SCROLL_OVERSCROLL_PX);
                velocity = 0F;
            }
        }
        visualOverscroll *= (float) Math.exp(-6F * dt / MotionTokens.SCROLL_RETURN);
        if (Math.abs(visualOverscroll) < .01F) visualOverscroll = 0F;
        position.setTarget(target); position.update(dt);
        if (!draggingBar && System.nanoTime() - lastInput > 800_000_000L) barAlpha.setTarget(0F);
        barAlpha.update(dt);
    }
    public float position() { return position.get(); }
    /** Visual-only rubber band; logical scroll position and target always remain in range. */
    public float visualPosition() { return position.get() + visualOverscroll; }
    public float target() { return target; }
    /** Restores a saved logical position after content metrics are known, without scroll animation. */
    public void restore(float value) {
        velocity = visualOverscroll = 0F;
        target = clamp(value, 0F, max);
        position.snapTo(target);
    }
    public float content() { return content; }
    public float viewport() { return viewport; }
    public float max() { return max; }
    public boolean dragging() { return draggingBar; }
    public boolean canScroll() { return max > .5F; }
    public boolean beginBar(float mx, float my, float x, float y, float width, float height) {
        if (!canScroll() || !UiDraw.inside(mx, my, x - 5F, y, x + width + 5F, y + height)) return false;
        float thumb = thumbHeight(height), thumbY = y + (height - thumb) * clamp(position.get() / max, 0F, 1F);
        if (my < thumbY || my > thumbY + thumb) {
            page((my < thumbY ? -1F : 1F) * viewport);
        } else {
            draggingBar = true; grabOffset = my - thumbY;
            velocity = 0F; lastInput = System.nanoTime(); barAlpha.setTarget(1F);
        }
        return true;
    }
    public void dragBar(float my, float y, float height) {
        if (!draggingBar || max <= 0F) return;
        float thumb = thumbHeight(height);
        float progress = clamp((my - y - grabOffset) / Math.max(1F, height - thumb), 0F, 1F);
        target = progress * max; position.snapTo(target); velocity = visualOverscroll = 0F; lastInput = System.nanoTime(); barAlpha.setTarget(1F);
    }
    public void release() { draggingBar = false; }
    public void renderBar(float x, float y, float height, boolean hover) {
        if (!canScroll() || barAlpha.get() <= .01F) return;
        float thumb = thumbHeight(height);
        float at = max <= 0F ? 0F : clamp(position.get() / max, 0F, 1F);
        float width = hover || draggingBar ? 5F : GuiTokens.SCROLLBAR_WIDTH;
        UiDraw.rounded(x - width, y + (height - thumb) * at, x, y + (height - thumb) * at + thumb, width * .5F,
                UiDraw.alpha(GuiTokens.TEXT_SECONDARY, barAlpha.get()));
    }
    private float thumbHeight(float height) { return Math.min(height, Math.max(20F, height * height / Math.max(height, content))); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
