package keystrokesmod.client.clickgui.apple.rebuild;

import org.lwjgl.input.Keyboard;
import net.minecraft.client.gui.GuiScreen;

/** Reusable input with retained caret/focus state; strings are materialized only after key input. */
public final class GuiTextField {
    private final StringBuilder input = new StringBuilder(48);
    private final String placeholder;
    private final int icon, maxLength;
    private final Anim focus = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_HOVER), caretX = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER);
    private String value = "", before = "";
    private float beforeWidth, x, y, width;
    private boolean focused, selectAll, submitted, invalid;
    public GuiTextField(String placeholder, int icon, int maxLength) { this.placeholder = placeholder; this.icon = icon; this.maxLength = maxLength; }
    public void update(float dt) { focus.update(dt); caretX.update(dt); }
    public void draw(float left, float top, float w, float mouseX, float mouseY) {
        x = left; y = top; width = w;
        boolean over = UiDraw.inside(mouseX, mouseY, x, y, x + width, y + 40F); focus.setTarget(focused || over ? 1F : 0F);
        int border = invalid ? 0xFFF26A6A : focused ? GuiTokens.accent() : GuiTokens.FIELD_BORDER;
        UiDraw.glassRounded(x, y, x + width, y + 40F, 10F, GuiTokens.FIELD_BG, GuiTokens.FIELD_BG, border, GuiRenderContext.inverseUiScale(), 0, 0F);
        float textX = x + 14F;
        if (icon >= 0) { GuiIcons.draw(icon, x + 17F, y + 20F, 16F, GuiTokens.TEXT_SECONDARY); textX += 22F; }
        String display = input.length() == 0 ? placeholder : value;
        UiDraw.text(display, textX, y + (40F - UiDraw.textHeight()) * .5F, input.length() == 0 ? GuiTokens.TEXT_SECONDARY : GuiTokens.TEXT_PRIMARY);
        caretX.setTarget(textX + beforeWidth);
        if (focused && ((System.nanoTime() / 500_000_000L) & 1L) == 0L) UiDraw.rect(caretX.get(), y + 10F, caretX.get() + GuiRenderContext.inverseUiScale(), y + 30F, GuiTokens.TEXT_PRIMARY);
    }
    public boolean click(float mouseX, float mouseY, int button) { if (button != 0 || !UiDraw.inside(mouseX, mouseY, x, y, x + width, y + 40F)) return false; focused = true; invalid = false; Keyboard.enableRepeatEvents(true); return true; }
    public boolean key(char c, int key) {
        if (!focused) return false;
        submitted = false;
        if (key == Keyboard.KEY_ESCAPE) { cancel(); return true; }
        boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (control && key == Keyboard.KEY_A) { selectAll = true; before = value; beforeWidth = UiDraw.textWidth(before); return true; }
        if (control && key == Keyboard.KEY_V) { paste(GuiScreen.getClipboardString()); return true; }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { submitted = true; return true; }
        if (key == Keyboard.KEY_BACK) { if (selectAll) input.setLength(0); else if (input.length() > 0) input.setLength(input.length() - 1); selectAll = false; sync(); return true; }
        if (key == Keyboard.KEY_DELETE) { if (selectAll) { input.setLength(0); selectAll = false; sync(); } return true; }
        if (c >= 32 && c != 127 && input.length() < maxLength) { if (selectAll) { input.setLength(0); selectAll = false; } input.append(c); sync(); }
        return true;
    }
    public boolean consumeSubmit() { boolean result = submitted; submitted = false; return result; }
    public String value() { return value; }
    public boolean empty() { return input.length() == 0; }
    public boolean focused() { return focused; }
    public void clear() { input.setLength(0); sync(); focused = false; invalid = false; }
    public void cancel() { focused = false; selectAll = false; }
    public void blurIfOutside(float mouseX, float mouseY) { if (!UiDraw.inside(mouseX, mouseY, x, y, x + width, y + 40F)) cancel(); }
    public void invalid() { invalid = true; }
    private void paste(String text) { if (text == null) return; if (selectAll) { input.setLength(0); selectAll = false; } for (int i = 0; i < text.length() && input.length() < maxLength; i++) { char c = text.charAt(i); if (c >= 32 && c != 127) input.append(c); } sync(); }
    private void sync() { value = input.toString(); before = value; beforeWidth = UiDraw.textWidth(before); }
}
