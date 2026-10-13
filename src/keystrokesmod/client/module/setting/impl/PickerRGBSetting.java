package keystrokesmod.client.module.setting.impl;

import keystrokesmod.client.clickgui.raven.components.PickerRGBComponent;
import keystrokesmod.client.clickgui.raven.components.SettingComponent;

/** RGBSetting-compatible config, but opens a full color picker for this one module. */
public final class PickerRGBSetting extends RGBSetting {
    private final int red, green, blue;
    private final SliderSetting opacity;

    public PickerRGBSetting(String name, int red, int green, int blue, SliderSetting opacity) {
        super(name, red, green, blue);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.opacity = opacity;
    }

    public SliderSetting getOpacity() { return opacity; }

    @Override public void resetToDefaults() {
        // The original RGBSetting reset loop overruns its three color channels.
        setRed(red);
        setGreen(green);
        setBlue(blue);
    }

    @Override public Class<? extends SettingComponent> getRavenComponentType() {
        return PickerRGBComponent.class;
    }
}
