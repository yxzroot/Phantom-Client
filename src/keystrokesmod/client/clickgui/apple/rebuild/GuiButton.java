package keystrokesmod.client.clickgui.apple.rebuild;

/** Shared spring-driven button. Labels/icons are immutable so drawing is allocation-free. */
public final class GuiButton {
    public static final int PRIMARY = 0, SECONDARY = 1, DANGER = 2, GHOST_ICON = 3;
    private final String label;
    private final int type, icon;
    private final Anim hover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_HOVER), press = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_PRESS);
    private float labelWidth, measuredScale = -1F;
    private float x, y, width = 34F;
    public GuiButton(int type, String label, int icon) { this.type = type; this.label = label == null ? "" : label; this.icon = icon; }
    public void update(float dt) { hover.update(dt); press.update(dt); }
    public void draw(float left, float top, float w, float mouseX, float mouseY, boolean enabled) {
        x = left; y = top; width = type == GHOST_ICON ? 32F : w;
        if (measuredScale != GuiRenderContext.uiScale()) { measuredScale = GuiRenderContext.uiScale(); labelWidth = label.length() == 0 ? 0F : UiDraw.buttonWidth(label); }
        float buttonHeight = type == GHOST_ICON ? 32F : 34F;
        boolean over = enabled && UiDraw.inside(mouseX, mouseY, x, y, x + width, y + buttonHeight);
        hover.setTarget(over ? 1F : 0F);
        float scale = 1F - press.get() * .03F, sw = width * scale, sh = buttonHeight * scale, dx = x + (width - sw) * .5F, dy = y + (buttonHeight - sh) * .5F;
        float alpha = enabled ? 1F : .40F;
        if (type == PRIMARY) UiDraw.rounded(dx, dy, dx + sw, dy + sh, 10F, UiDraw.alpha(UiDraw.mix(GuiTokens.accent(), GuiTokens.accentHover(), hover.get()), alpha));
        else if (type == DANGER) UiDraw.glassRounded(dx, dy, dx + sw, dy + sh, 10F, UiDraw.alpha(0xFFF26A6A, alpha * hover.get() * .14F), UiDraw.alpha(0xFFF26A6A, alpha * hover.get() * .14F), UiDraw.alpha(0xFFF26A6A, alpha * .35F), GuiRenderContext.inverseUiScale(), 0, 0F);
        else if (type == GHOST_ICON) { if (hover.get() > .01F) UiDraw.circle(dx + sw * .5F, dy + sh * .5F, 16F, UiDraw.alpha(GuiTokens.ICON_HOVER_BG, hover.get() * alpha)); }
        else { int fill = UiDraw.mix(GuiTokens.BUTTON_BG, GuiTokens.BUTTON_HOVER_BG, hover.get());
            UiDraw.glassRounded(dx, dy, dx + sw, dy + sh, 10F, UiDraw.alpha(fill, alpha), UiDraw.alpha(fill, alpha), UiDraw.alpha(GuiTokens.CARD_BORDER, alpha), GuiRenderContext.inverseUiScale(), 0, 0F); }
        int color = type == DANGER ? 0xFFF26A6A : GuiTokens.TEXT_PRIMARY;
        boolean showIcon = icon >= 0 && (label.length() == 0 || sw >= labelWidth + 54F);
        float groupX = dx + (sw - (showIcon && label.length() > 0 ? labelWidth + 22F : labelWidth)) * .5F;
        if (showIcon) GuiIcons.draw(icon, label.length() == 0 ? dx + sw * .5F : groupX + 8F, dy + sh * .5F, 16F, UiDraw.alpha(color, alpha));
        if (label.length() > 0) {
            float textX = showIcon ? groupX + 22F : dx + (sw - labelWidth) * .5F;
            UiDraw.buttonText(label, textX, dy + (sh - UiDraw.buttonHeight()) * .5F, UiDraw.alpha(color, alpha));
        }
    }
    public boolean click(float mouseX, float mouseY, int button) { if (button != 0 || !UiDraw.inside(mouseX, mouseY, x, y, x + width, y + (type == GHOST_ICON ? 32F : 34F))) return false; press.setTarget(1F); return true; }
    public void release() { press.setTarget(0F); }
}
