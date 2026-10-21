package keystrokesmod.client.clickgui.apple.rebuild;

/** Text tabs with cached metrics and a label-only active underline. */
public final class TabBar {
    private final String[] labels;
    private final ColorAnim[] colors;
    private final float[] starts, widths, labelWidths;
    private final Anim underlineX = new Anim(0F, Anim.Mode.SPRING, MotionTokens.TAB_UNDERLINE);
    private final Anim underlineW = new Anim(0F, Anim.Mode.SPRING, MotionTokens.TAB_UNDERLINE);
    private final Anim gridHover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER);
    private int selected;
    private float x, y;
    private boolean metricsDirty = true;
    private float metricScale = -1F;

    public TabBar(String[] labels) {
        this.labels = labels;
        colors = new ColorAnim[labels.length]; starts = new float[labels.length]; widths = new float[labels.length]; labelWidths = new float[labels.length];
        for (int i = 0; i < labels.length; i++) colors[i] = new ColorAnim(GuiTokens.TEXT_SECONDARY, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.TAB_TEXT);
    }
    public int selected() { return selected; }
    public void select(int value) { if (value >= 0 && value < labels.length) selected = value; }
    public int count() { return labels.length; }
    public void invalidateMetrics() { metricsDirty = true; }
    public void resetInteraction() {
        for (int i = 0; i < colors.length; i++) colors[i].snapTo(i == selected ? GuiTokens.TEXT_PRIMARY : GuiTokens.TEXT_SECONDARY);
    }
    private boolean snapNext;
    public void snapSelectionOnNextDraw() { snapNext = true; }
    public void update(float dt) { for (int i = 0; i < colors.length; i++) colors[i].update(dt); underlineX.update(dt); underlineW.update(dt); gridHover.update(dt); }

    /** top is the literal text top in the native 1080p design coordinate system. */
    public void draw(float left, float top, float available, float mouseX, float mouseY, boolean gridIcon, boolean snapping, boolean interactive) {
        ensureMetrics(gridIcon);
        x = left; y = top;
        float cursor = x;
        for (int i = 0; i < labels.length; i++) {
            starts[i] = cursor;
            boolean hover = interactive && UiDraw.inside(mouseX, mouseY, cursor - 5F, y - 5F, cursor + widths[i] + 10F, y + UiDraw.mediumHeight() + 12F);
            colors[i].setTarget(i == selected ? GuiTokens.TEXT_PRIMARY : hover ? UiDraw.alpha(GuiTokens.TEXT_PRIMARY, .85F) : GuiTokens.TEXT_SECONDARY);
            float labelX = cursor + (gridIcon && i == 0 ? 24F : 0F);
            if (gridIcon && i == 0) {
                gridHover.setTarget(hover ? 1F : 0F);
                if (gridHover.get() > .01F) UiDraw.circle(cursor + 10F, y + UiDraw.mediumHeight() * .5F, 15F, UiDraw.alpha(GuiTokens.ICON_HOVER_BG, gridHover.get()));
                GuiIcons.draw(GuiIcons.GRID, cursor + 10F, y + UiDraw.mediumHeight() * .5F, 22F,
                        UiDraw.mix(GuiTokens.ICON_REST, GuiTokens.TEXT_PRIMARY, Math.max(gridHover.get(), i == selected ? 1F : 0F)));
            }
            UiDraw.medium(labels[i], labelX, y, colors[i].get());
            if (i == selected) {
                if (snapping || snapNext) { underlineX.snapTo(labelX); underlineW.snapTo(labelWidths[i]); }
                else { underlineX.setTarget(labelX); underlineW.setTarget(labelWidths[i]); }
            }
            cursor += widths[i] + 28F;
        }
        float underlineY = y + UiDraw.mediumBaseline() + 8F;
        UiDraw.rounded(underlineX.get(), underlineY, underlineX.get() + underlineW.get(), underlineY + 2F, 1F, GuiTokens.accent());
        snapNext = false;
    }

    public int click(float mouseX, float mouseY) {
        if (mouseY < y - 5F || mouseY > y + UiDraw.mediumHeight() + 12F) return -1;
        for (int i = 0; i < labels.length; i++) if (UiDraw.inside(mouseX, mouseY, starts[i] - 5F, y - 5F, starts[i] + widths[i] + 10F, y + UiDraw.mediumHeight() + 12F)) return i;
        return -1;
    }

    private void ensureMetrics(boolean gridIcon) {
        float scale = GuiRenderContext.uiScale();
        if (!metricsDirty && metricScale == scale) return;
        metricScale = scale; metricsDirty = false;
        for (int i = 0; i < labels.length; i++) {
            labelWidths[i] = UiDraw.mediumWidth(labels[i]);
            widths[i] = labelWidths[i] + (gridIcon && i == 0 ? 24F : 0F);
        }
    }

}
