package keystrokesmod.client.module.modules.combat.aim;

import keystrokesmod.client.module.setting.impl.SliderSetting;

/** Existing slider widget/config format, without legacy BigDecimal allocations on frame reads. */
public final class AssistSlider extends SliderSetting {
    private double cached;
    private final double initial,min,max;
    public AssistSlider(String name,double initial,double min,double max,double interval){super(name,initial,min,max,interval);this.initial=initial;this.min=min;this.max=max;cached=initial;}
    @Override public double getInput(){return cached;}
    @Override public void setValue(double value){super.setValue(PositionHistory.clamp(value,min,max));cached=super.getInput();}
    @Override public void resetToDefaults(){super.resetToDefaults();cached=initial;}
}
