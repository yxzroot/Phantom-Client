package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ButtonSetting;

/** Existing button settings call their module's original GUI callback, never a parallel action. */
public final class ActionRow extends SettingRow {
    private final GuiButton button;
    public ActionRow(Module module, ButtonSetting setting) { super(module, setting, ""); button = new GuiButton(GuiButton.SECONDARY, "Run", GuiIcons.CHEVRON); }
    @Override public void update(float dt) { super.update(dt); button.update(dt); }
    @Override protected String readValue() { return ""; }
    @Override protected float controlWidth() { return 78F; }
    @Override protected void drawValue(float right, float textY, float alpha) { button.draw(right - 82F, rowY + 9F, 68F, -10000F, -10000F, true); }
    @Override protected boolean onClick(float mouseX, float mouseY, int mouseButton) { if (mouseButton == 0) module.guiButtonToggled(setting); return true; }
    @Override public void release() { super.release(); button.release(); }
}
