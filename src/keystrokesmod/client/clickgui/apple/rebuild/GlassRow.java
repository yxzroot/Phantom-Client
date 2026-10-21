package keystrokesmod.client.clickgui.apple.rebuild;

/** Reusable retained glass surface for config, friend and drawer rows. */
public final class GlassRow {
    private final Anim hover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_HOVER);
    private final Anim press = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_PRESS);
    private float x, y, width, height;
    public void update(float dt) { hover.update(dt); press.update(dt); }
    public void draw(float left, float top, float w, float h, float mouseX, float mouseY, boolean active, float alpha) {
        x = left; y = top; width = w; height = h;
        boolean over = UiDraw.inside(mouseX, mouseY, left, top, left + w, top + h);
        hover.setTarget(over ? 1F : 0F);
        float scale = 1F - press.get() * (1F - MotionTokens.CARD_PRESS_SCALE), drawW = w * scale, drawH = h * scale;
        float dx = left + (w - drawW) * .5F, dy = top + (h - drawH) * .5F - hover.get() * MotionTokens.CARD_LIFT_PX;
        int fill = active ? UiDraw.mix(GuiTokens.CARD_BG, GuiTokens.accent(), .14F) : UiDraw.mix(GuiTokens.CARD_BG, GuiTokens.CARD_BG_HOVER, hover.get());
        int edge = active ? UiDraw.alpha(GuiTokens.accent(), .60F) : UiDraw.mix(GuiTokens.CARD_BORDER, GuiTokens.CARD_BORDER_HOVER, hover.get());
        UiDraw.glassRounded(dx, dy, dx + drawW, dy + drawH, 12F, UiDraw.alpha(fill, alpha), UiDraw.alpha(fill, alpha), UiDraw.alpha(edge, alpha),
                GuiRenderContext.inverseUiScale(), 0, 0F);
    }
    public boolean contains(float mouseX, float mouseY) { return UiDraw.inside(mouseX, mouseY, x, y, x + width, y + height); }
    public void press() { press.setTarget(1F); }
    public void release() { press.setTarget(0F); }
    public float x() { return x; } public float y() { return y - hover.get() * 3F; } public float width() { return width; } public float height() { return height; }
}
