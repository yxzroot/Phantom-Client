package keystrokesmod.client.clickgui.apple.rebuild;

/** Stateless shared layout primitives used by the secondary views and drawer. */
public final class GuiWidgets {
    private GuiWidgets() { }
    public static void badge(String text, float x, float y, boolean active) {
        float w = UiDraw.smallWidth(text) + 16F; int color = active ? UiDraw.alpha(GuiTokens.accent(), .22F) : 0x332A303B;
        UiDraw.rounded(x, y, x + w, y + 22F, 11F, color); UiDraw.small(text, x + 8F, y + (22F - UiDraw.smallHeight()) * .5F, active ? GuiTokens.accent() : GuiTokens.TEXT_SECONDARY);
    }
    public static void section(String text, float x, float y, float width) {
        UiDraw.small(text, x, y, GuiTokens.TEXT_SECONDARY);
        UiDraw.rect(x, y + UiDraw.smallHeight() + 7F, x + width, y + UiDraw.smallHeight() + 7F + GuiRenderContext.inverseUiScale(), 0x18FFFFFF);
    }
    public static void empty(int icon, String title, String subtitle, float x, float y, float width, float height, float alpha) {
        float cx = x + width * .5F, cy = y + height * .5F - 22F;
        GuiIcons.draw(icon, cx, cy, 44F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha * .60F));
        UiDraw.bold(title, cx - UiDraw.boldWidth(title) * .5F, cy + 32F, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, alpha));
        UiDraw.small(subtitle, cx - UiDraw.smallWidth(subtitle) * .5F, cy + 52F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha));
    }
    public static void toggle(float x, float y, boolean on, float alpha, boolean disabled) {
        GuiToggle.draw(x, y, on ? 1F : 0F, on ? 1F : 0F, 0F, alpha, disabled);
    }
}
