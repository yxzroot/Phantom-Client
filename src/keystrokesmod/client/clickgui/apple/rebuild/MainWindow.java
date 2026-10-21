package keystrokesmod.client.clickgui.apple.rebuild;

/** Draggable glass frame and its resolved layout. */
public final class MainWindow {
    private final Anim x = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.GRID_REFLOW), y = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.GRID_REFLOW);
    private final Anim animatedHeight = new Anim(560F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.GRID_REFLOW);
    private float width, height;
    private float progress;
    private float blurAlpha;
    private boolean positioned, dragging;
    private float dragX, dragY;

    public void setProgress(float value) { progress = Math.max(0F, Math.min(1F, value)); }
    public void setBlurAlpha(float value) { blurAlpha = Math.max(0F, Math.min(1F, value)); }
    public void update(float dt) { animatedHeight.update(dt); height = animatedHeight.get(); if (!dragging) { x.update(dt); y.update(dt); } }
    public void layout(int screenW, int screenH) { layout(screenW, screenH, 0F); }
    public void layout(int screenW, int screenH, float contentHeight) {
        width = Math.min(1200F, screenW * .66F);
        float maxHeight = screenH * .78F;
        float desired = Math.max(Math.min(560F, maxHeight), Math.min(maxHeight, contentHeight + 176F + GuiTokens.WINDOW_PADDING));
        if (!positioned) animatedHeight.snapTo(desired); else animatedHeight.setTarget(desired);
        height = animatedHeight.get();
        if (!positioned) {
            float px = GuiPreferences.windowX(), py = GuiPreferences.windowY();
            if (Float.isNaN(px) || Float.isNaN(py)) { px = (screenW - width) * .5F; py = (screenH - height) * .5F; }
            x.snapTo(clampX(px, screenW)); y.snapTo(clampY(py, screenH)); positioned = true;
        } else { x.setTarget(clampX(x.target(), screenW)); y.setTarget(clampY(y.target(), screenH)); }
    }
    public float x() { return x.get(); } public float y() { return y.get(); } public float width() { return width; } public float height() { return height; }
    public float contentX() { return x.get() + GuiTokens.WINDOW_PADDING; }
    public float contentY() { return y() + 176F; }
    public float contentW() { return width - GuiTokens.WINDOW_PADDING * 2F; }
    public float contentH() { return Math.max(1F, height - 176F - GuiTokens.WINDOW_PADDING); }
    public float alpha() { return progress; } public float scale() { return MotionTokens.WINDOW_START_SCALE + progress * (1F - MotionTokens.WINDOW_START_SCALE); }
    public void draw() {
        float left = x.get(), top = y();
        BackdropBlur blur = BackdropBlur.shared();
        if (!dragging) UiDraw.windowShadow(left, top, left + width, top + height, GuiTokens.WINDOW_RADIUS);
        float inset = GuiRenderContext.inverseUiScale() * .5F;
        blur.draw(left + inset, top + inset, width - 2F * inset, height - 2F * inset, GuiTokens.WINDOW_RADIUS - inset, blurAlpha);
        if (!SdfRenderer.active()) {
            UiDraw.rounded(left, top, left + width, top + height, GuiTokens.WINDOW_RADIUS,
                    blur.active() ? GuiTokens.WINDOW_BG : 0xF50D1016);
            UiDraw.outline(left, top, left + width, top + height, GuiTokens.WINDOW_RADIUS, GuiTokens.WINDOW_BORDER);
            return;
        }
        int topTint = blur.active() ? 0xE012151D : 0xF512151D;
        int bottomTint = blur.active() ? GuiTokens.WINDOW_BG : 0xF50D1016;
        UiDraw.glassRounded(left + inset, top + inset, left + width - inset, top + height - inset, GuiTokens.WINDOW_RADIUS - inset,
                topTint, bottomTint, GuiTokens.WINDOW_BORDER, GuiRenderContext.inverseUiScale(),
                0, 0F);
    }
    public boolean beginDrag(float mouseX, float mouseY) {
        if (!UiDraw.inside(mouseX, mouseY, x.get(), y.get(), x.get() + width - 126F, y.get() + 76F)) return false;
        dragging = true; dragX = mouseX - x.get(); dragY = mouseY - y.get(); return true;
    }
    public void drag(float mouseX, float mouseY, int screenW, int screenH) {
        if (!dragging) return; x.snapTo(clampX(mouseX - dragX, screenW)); y.snapTo(clampY(mouseY - dragY, screenH));
        GuiPreferences.setWindow(x.get(), y.get());
    }
    public void release() { if (dragging) { dragging = false; GuiPreferences.setWindow(x.get(), y.get()); } }
    public void cancelDrag() { dragging = false; }
    public boolean dragging() { return dragging; }
    private float clampX(float value, int screenW) { return Math.max(4F - width + 80F, Math.min(screenW - 80F, value)); }
    private float clampY(float value, int screenH) { return Math.max(4F, Math.min(screenH - 28F, value)); }
}
