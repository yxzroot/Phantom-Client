package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;

public final class SliderRow extends SettingRow {
    private final SliderSetting slider;
    private final DoubleSliderSetting range;
    private boolean dragging, high;
    private final Anim knobPress = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.SLIDER_PRESS);
    private float fill, lowFill;
    private long known = Long.MIN_VALUE;
    public SliderRow(Module module, SliderSetting setting) { super(module, setting, ""); slider = setting; range = null; prime(); }
    public SliderRow(Module module, DoubleSliderSetting setting) { super(module, setting, ""); slider = null; range = setting; prime(); }
    @Override public void update(float dt) { super.update(dt); fill = slider != null ? percent(slider.getInput(), slider.getMin(), slider.getMax()) : percent(range.getInputMax(), range.getMin(), range.getMax()); if (range != null) lowFill = percent(range.getInputMin(), range.getMin(), range.getMax()); knobPress.setTarget(dragging ? 1F : 0F); knobPress.update(dt); }
    @Override protected boolean onClick(float mouseX, float mouseY, int button) {
        if (button != 0) return true;
        dragging = true;
        if (range != null) high = Math.abs(mouseX - (rowX + 14F + (rowW - 28F) * fill)) < Math.abs(mouseX - (rowX + 14F + (rowW - 28F) * lowFill));
        drag(mouseX, false); return true;
    }
    @Override public void release() { super.release(); dragging = false; }
    @Override public boolean dragging() { return dragging; }
    @Override public void drag(float mouseX, boolean slow) {
        if (!dragging) return;
        float at = Math.max(0F, Math.min(1F, (mouseX - rowX - 14F) / Math.max(1F, rowW - 28F)));
        if (slider != null) {
            double target = slider.getMin() + at * (slider.getMax() - slider.getMin());
            if (slow) target = slider.getInput() + (target - slider.getInput()) * .15D;
            slider.setValue(target);
        } else {
            double target = range.getMin() + at * (range.getMax() - range.getMin());
            if (slow) target = (high ? range.getInputMax() : range.getInputMin()) + (target - (high ? range.getInputMax() : range.getInputMin())) * .15D;
            if (high) range.setValueMax(Math.max(target, range.getInputMin())); else range.setValueMin(Math.min(target, range.getInputMax()));
        }
        fill = slider != null ? percent(slider.getInput(), slider.getMin(), slider.getMax()) : percent(range.getInputMax(), range.getMin(), range.getMax());
        if (range != null) lowFill = percent(range.getInputMin(), range.getMin(), range.getMax());
    }
    @Override public void wheel(float amount) {
        if (slider != null) slider.setValue(slider.getInput() + amount * (slider.getMax() - slider.getMin()) / 100D);
        else if (high) range.setValueMax(Math.max(range.getInputMin(), range.getInputMax() + amount * (range.getMax() - range.getMin()) / 100D));
        else range.setValueMin(Math.min(range.getInputMax(), range.getInputMin() + amount * (range.getMax() - range.getMin()) / 100D));
    }
    @Override protected float baseHeight() { return 64F; }
    @Override protected void drawValue(float right, float textY, float alpha) {
        float barY = rowY + baseHeight() - 15F, left = rowX + 14F, barRight = right - 14F;
        UiDraw.rounded(left, barY, barRight, barY + 6F, 3F, UiDraw.alpha(0xFF272C37, alpha));
        UiDraw.rounded(left + (range == null ? 0F : (barRight - left) * lowFill), barY, left + Math.max(3F, (barRight - left) * fill), barY + 6F, 3F, UiDraw.alpha(GuiTokens.accent(), alpha));
        if (range != null) UiDraw.circle(left + (barRight - left) * lowFill, barY + 3F, 6F + (high ? 0F : MotionTokens.SLIDER_PRESS_GROW_PX * knobPress.get()), UiDraw.alpha(GuiTokens.accent(), alpha));
        UiDraw.circle(left + (barRight - left) * fill, barY + 3F, 6F + (high || range == null ? MotionTokens.SLIDER_PRESS_GROW_PX * knobPress.get() : 0F), UiDraw.alpha(GuiTokens.accent(), alpha));
        super.drawValue(right, textY, alpha);
    }
    @Override protected String readValue() {
        long hash;
        if (slider != null) hash = Double.doubleToLongBits(slider.getInput());
        else hash = Double.doubleToLongBits(range.getInputMin()) * 31L + Double.doubleToLongBits(range.getInputMax());
        if (hash == known) return value;
        known = hash;
        return slider != null ? format(slider.getInput()) : format(range.getInputMin()) + " - " + format(range.getInputMax());
    }
    private static float percent(double value, double min, double max) { return max <= min ? 0F : (float) Math.max(0D, Math.min(1D, (value - min) / (max - min))); }
    private static String format(double value) { long whole = (long) value; return value == whole ? Long.toString(whole) : String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private void prime() { value = readValue(); oldValue = value; valueWidth = UiDraw.textWidth(value); }
}
