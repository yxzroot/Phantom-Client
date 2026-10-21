package keystrokesmod.client.clickgui.apple.rebuild;

import org.lwjgl.input.Keyboard;
import net.minecraft.client.gui.GuiScreen;

/** Expanding search input; strings are materialized only after input events. */
public final class SearchField {
    private final StringBuilder input = new StringBuilder(48);
    private final Anim width = new Anim(28F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.SEARCH_EXPAND);
    private final Anim alpha = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.SEARCH_EXPAND);
    private final Anim caretX = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER);
    private final Anim hoverAlpha = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER);
    private boolean focused;
    private String query = "";
    private String beforeCaret = "";
    private int caret;
    private boolean selectAll;
    private float x, y, h;
    private float beforeCaretWidth;
    private float caretScale = -1F;

    public String query() { return query; }
    public boolean focused() { return focused; }
    public void focus() { focused = true; width.setTarget(220F); alpha.setTarget(1F); Keyboard.enableRepeatEvents(true); }
    public void blur() { focused = false; if (input.length() == 0) { width.setTarget(28F); alpha.setTarget(0F); } }
    public void clear() { input.setLength(0); query = beforeCaret = ""; beforeCaretWidth = 0F; caret = 0; selectAll = false; if (!focused) blur(); }
    public void cancelFocus() { focused = false; selectAll = false; if (input.length() == 0) { width.snapTo(28F); alpha.snapTo(0F); } }
    public boolean expanded() { return focused || input.length() > 0 || width.get() > 29F; }

    public void update(float dt) { width.update(dt); alpha.update(dt); caretX.update(dt); hoverAlpha.update(dt); }
    public void draw(float right, float top, float mouseX, float mouseY, long now) {
        layout(right, top);
        boolean hover = UiDraw.inside(mouseX, mouseY, x, y, right, y + h);
        hoverAlpha.setTarget(hover ? 1F : 0F);
        if (expanded()) {
            UiDraw.glassRounded(x, y, right, y + h, 13F, GuiTokens.FIELD_BG, GuiTokens.FIELD_BG,
                    focused ? GuiTokens.accent() : GuiTokens.FIELD_BORDER, GuiRenderContext.inverseUiScale(), 0, 0F);
        }
        if (hoverAlpha.get() > .01F && !expanded()) UiDraw.circle(right - 11F, y + h * .5F, 13F, UiDraw.alpha(GuiTokens.ICON_HOVER_BG, hoverAlpha.get()));
        GuiIcons.draw(GuiIcons.SEARCH, right - 11F, y + h * .5F, 22F, UiDraw.mix(GuiTokens.ICON_REST, GuiTokens.TEXT_PRIMARY, hoverAlpha.get()));
        if (alpha.get() <= .01F) return;
        String text = input.length() == 0 ? "Search modules..." : query;
        int color = input.length() == 0 ? GuiTokens.TEXT_SECONDARY : GuiTokens.TEXT_PRIMARY;
        float textY = y + (h - UiDraw.textHeight()) * .5F;
        UiDraw.text(text, x + 10F, textY, UiDraw.alpha(color, alpha.get()));
        if (caretScale != GuiRenderContext.uiScale()) { caretScale = GuiRenderContext.uiScale(); beforeCaretWidth = UiDraw.textWidth(beforeCaret); }
        caretX.setTarget(x + 10F + beforeCaretWidth);
        if (focused && ((now / 500_000_000L) & 1L) == 0L)
            UiDraw.rect(caretX.get(), y + 5F, caretX.get() + 1F, y + h - 5F, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, alpha.get()));
    }

    public boolean click(float mouseX, float mouseY, float right, float top) {
        layout(right, top);
        float left = x;
        if (!UiDraw.inside(mouseX, mouseY, left, y, right, y + h)) { blur(); return false; }
        focus();
        int at = 0;
        while (at < input.length()) {
            float next = UiDraw.textWidth(input.substring(0, at + 1));
            if (mouseX < left + 10F + next) break;
            ++at;
        }
        caret = at; selectAll = false; sync(); return true;
    }

    private void layout(float right, float top) {
        h = 26F;
        x = right - width.get();
        y = top + (UiDraw.mediumHeight() - h) * .5F;
    }

    public boolean key(char character, int keyCode) {
        if (!focused) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) { if (input.length() > 0) clear(); else blur(); return true; }
        boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (control && keyCode == Keyboard.KEY_A) { caret = input.length(); beforeCaret = query; selectAll = true; return true; }
        if (control && keyCode == Keyboard.KEY_V) { paste(GuiScreen.getClipboardString()); return true; }
        if (keyCode == Keyboard.KEY_LEFT) { caret = Math.max(0, caret - 1); selectAll = false; sync(); return true; }
        if (keyCode == Keyboard.KEY_RIGHT) { caret = Math.min(input.length(), caret + 1); selectAll = false; sync(); return true; }
        if (keyCode == Keyboard.KEY_HOME) { caret = 0; selectAll = false; sync(); return true; }
        if (keyCode == Keyboard.KEY_END) { caret = input.length(); selectAll = false; sync(); return true; }
        if (keyCode == Keyboard.KEY_BACK) { backspace(control); return true; }
        if (keyCode == Keyboard.KEY_DELETE) { if (selectAll) clear(); else if (caret < input.length()) { input.deleteCharAt(caret); sync(); } return true; }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) { blur(); return true; }
        if (character >= 32 && character != 127 && input.length() < 48) { clearSelection(); input.insert(caret++, character); sync(); return true; }
        return true;
    }

    private void backspace(boolean word) {
        if (selectAll) { clear(); return; }
        if (caret <= 0) return;
        int start = caret - 1;
        if (word) { while (start > 0 && Character.isWhitespace(input.charAt(start))) --start; while (start > 0 && !Character.isWhitespace(input.charAt(start - 1))) --start; }
        input.delete(start, caret); caret = start; sync();
    }
    private void paste(String clipboard) {
        if (clipboard == null || clipboard.length() == 0) return;
        clearSelection();
        for (int i = 0; i < clipboard.length() && input.length() < 48; i++) { char c = clipboard.charAt(i); if (c >= 32 && c != 127) { input.insert(caret++, c); } }
        sync();
    }
    private void clearSelection() { if (selectAll) { input.setLength(0); caret = 0; selectAll = false; } }
    private void sync() { query = input.toString(); caret = Math.max(0, Math.min(caret, input.length())); beforeCaret = input.substring(0, caret); beforeCaretWidth = UiDraw.textWidth(beforeCaret); caretScale = GuiRenderContext.uiScale(); if (input.length() > 0) width.setTarget(220F); }
}
