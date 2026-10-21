package keystrokesmod.client.clickgui.apple.settings;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.RGBSetting;
/** Legacy factory retained for existing module rows. All colors use the same popup. */
public final class ApplePickerRGB extends ARgb {
    public ApplePickerRGB(PickerRGBSetting setting){super(setting);}
    public static ASettingComponent create(RGBSetting setting){return new ARgb(setting);}
}
