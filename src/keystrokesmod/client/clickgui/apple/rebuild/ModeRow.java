package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import org.lwjgl.input.Keyboard;

public final class ModeRow extends SettingRow {
    private final ComboSetting<?> combo;
    private final Enum<?>[] options;
    private final String[] optionText;
    private final Anim expanded = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.INLINE_EXPAND);
    private boolean open;
    public ModeRow(Module module, ComboSetting<?> setting) {
        super(module, setting, ""); combo = setting; options = setting.getMode().getDeclaringClass().getEnumConstants(); optionText = new String[options.length];
        for (int i = 0; i < options.length; i++) optionText[i] = options[i].toString();
    }
    @Override protected boolean onClick(float mouseX, float mouseY, int button) {
        if (mouseY > rowY + baseHeight() && open) { select((int) ((mouseY - rowY - baseHeight()) / 24F)); return true; }
        if (mouseX >= rowX + rowW - 33F) { open = !open; expanded.setTarget(open ? 1F : 0F); return true; }
        if (button == 1) combo.prevMode();
        else if (button == 2 || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) { open = !open; expanded.setTarget(open ? 1F : 0F); }
        else combo.nextMode();
        return true;
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) private void select(int at) { if (at >= 0 && at < options.length) ((ComboSetting) combo).setMode(options[at]); open = false; expanded.setTarget(0F); }
    @Override public void update(float dt) { super.update(dt); expanded.update(dt); }
    @Override public boolean expanding() { return open; }
    @Override protected float expandedHeight() { return options.length * 24F * expanded.get(); }
    @Override protected float controlWidth() { return valueWidth + 42F; }
    @Override protected void drawValue(float right, float textY, float alpha) {
        float pillW = Math.min(rowW * .47F, valueWidth + 42F), pillX = right - 14F - pillW;
        UiDraw.glassRounded(pillX, rowY + 11F, right - 14F, rowY + 41F, 10F, UiDraw.alpha(GuiTokens.CARD_BG, alpha), UiDraw.alpha(0xFF10141C, alpha), UiDraw.alpha(GuiTokens.CARD_BORDER, alpha), GuiRenderContext.inverseUiScale(), 0, 0F);
        GuiIcons.draw(GuiIcons.CHEVRON, right - 28F, rowY + 26F, 13F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha));
        UiDraw.text(value, pillX + 11F, rowY + (52F - UiDraw.textHeight()) * .5F, UiDraw.alpha(GuiTokens.VALUE_TEXT, alpha));
    }
    @Override protected void drawExpanded(float right, float y, float alpha) {
        if (expanded.get() <= .01F) return;
        for (int i = 0; i < options.length; i++) {
            float ry = y + i * 24F * expanded.get();
            UiDraw.rounded(rowX + 36F, ry + 2F, right - 8F, ry + 22F * expanded.get(), 5F, UiDraw.alpha(GuiTokens.ROW_BG, alpha * expanded.get()));
            if (options[i] == combo.getMode()) GuiIcons.draw(GuiIcons.CHECK, rowX + 51F, ry + 12F, 14F, UiDraw.alpha(GuiTokens.accent(), alpha * expanded.get()));
            UiDraw.text(optionText[i], rowX + 64F, ry + (22F - UiDraw.textHeight()) * .5F, UiDraw.alpha(options[i] == combo.getMode() ? GuiTokens.accent() : GuiTokens.VALUE_TEXT, alpha * expanded.get()));
        }
    }
    @Override protected String readValue() { return combo.getMode().toString(); }
}
