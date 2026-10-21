package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.TickSetting;

    public final class BoolRow extends SettingRow {
    private final TickSetting toggle;
    private final Anim position = new Anim(0F, Anim.Mode.SPRING, MotionTokens.TOGGLE_KNOB);
    private final Anim track = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.TOGGLE_TRACK);
    public BoolRow(Module module, TickSetting setting) { super(module, setting, ""); toggle = setting; }
    @Override public void update(float dt) { super.update(dt); position.setTarget(toggle.isToggled() ? 1F : 0F); position.update(dt); track.setTarget(toggle.isToggled() ? 1F : 0F); track.update(dt); }
    @Override protected boolean onClick(float mouseX, float mouseY, int button) { if (button == 0) { toggle.toggle(); module.guiButtonToggled(toggle); } return true; }
    @Override protected String readValue() { return toggle.isToggled() ? "On" : "Off"; }
    @Override protected int valueColor() { return toggle.isToggled() ? GuiTokens.accent() : GuiTokens.TEXT_SECONDARY; }
    @Override protected float controlWidth() { return 58F; }
    @Override protected void drawValue(float right, float textY, float alpha) { GuiToggle.draw(right - 58F, rowY + (baseHeight() - 24F) * .5F, position.get(), track.get(), hover.get(), alpha, false); }
}
