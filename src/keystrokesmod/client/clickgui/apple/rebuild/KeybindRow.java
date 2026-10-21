package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import org.lwjgl.input.Keyboard;

public final class KeybindRow extends SettingRow {
    private boolean binding;
    public KeybindRow(Module module) { super(module, null, "Keybind"); }
    @Override protected boolean onClick(float mouseX, float mouseY, int button) { if (button == 0) binding = true; else if (button == 1) module.setbind(0); return true; }
    @Override protected String readValue() { return binding ? "Press a key..." : module.getBindAsString(); }
    @Override public boolean key(char character, int keyCode) {
        if (!binding) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) { module.setbind(0); binding = false; }
        else if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) { module.setbind(0); binding = false; }
        else { module.setbind(keyCode); binding = false; }
        return true;
    }
    public void cancel() { binding = false; }
    public boolean capturing() { return binding; }
    @Override protected int valueColor() { return binding ? GuiTokens.accent() : GuiTokens.VALUE_TEXT; }
    @Override protected void drawValue(float right, float textY, float alpha) {
        float w = valueWidth + 22F, x = right - 14F - w;
        UiDraw.glassRounded(x, rowY + (baseHeight() - 28F) * .5F, right - 14F, rowY + (baseHeight() + 28F) * .5F, 8F, UiDraw.alpha(GuiTokens.CARD_BG, alpha), UiDraw.alpha(0xFF10141C, alpha), UiDraw.alpha(binding ? GuiTokens.accent() : GuiTokens.CARD_BORDER, alpha), GuiRenderContext.inverseUiScale(), 0, 0F);
        UiDraw.text(value, x + 11F, textY, UiDraw.alpha(valueColor(), alpha));
    }
}
