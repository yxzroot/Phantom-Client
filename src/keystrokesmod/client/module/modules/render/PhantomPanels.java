package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;

/** Independently configurable Panels arraylist; Classic Phantom HUD remains unchanged. */
public final class PhantomPanels extends Module {
    private static PhantomPanels instance;
    private final PanelsHud renderer = new PanelsHud();
    private final ComboSetting<PhantomHUD.SortMode> sort = new ComboSetting<>("Sort", PhantomHUD.SortMode.Length);
    private final TickSetting values = new TickSetting("Show values", true);
    private final ComboSetting<PhantomHUD.ValueColor> valueColor = new ComboSetting<>("Value color", PhantomHUD.ValueColor.SoftBlue);
    private final PickerRGBSetting customColor = new PickerRGBSetting("Custom value color", 143, 163, 199, null);
    private final TickSetting bar = new TickSetting("Bar", true);
    private final ComboSetting<PhantomHUD.BarColor> barColor = new ComboSetting<>("Bar color", PhantomHUD.BarColor.White);
    private final SliderSetting opacity = new SliderSetting("Background opacity (%)", 82, 0, 100, 1);
    private final SliderSetting radius = new SliderSetting("Corner radius", 4, 0, 8, .5);
    private final TickSetting logo = new TickSetting("Show logo", true);
    private final TickSetting animations = new TickSetting("Animations", true);
    private final SliderSetting scale = new SliderSetting("Scale", 1, .5, 2, .05);
    private final ComboSetting<PhantomHUD.Position> position = new ComboSetting<>("Position", PhantomHUD.Position.TopRight);
    private final SliderSetting xOffset = new SliderSetting("X Offset", 6, 0, 150, 1);
    private final SliderSetting yOffset = new SliderSetting("Y Offset", 6, 0, 150, 1);
    private final PickerRGBSetting gradientStart = new PickerRGBSetting("Gradient Start", 123, 92, 255, null);
    private final PickerRGBSetting gradientEnd = new PickerRGBSetting("Gradient End", 79, 195, 247, null);
    private final ComboSetting<PhantomHUD.Palette> palette = new ComboSetting<>("Accent Theme", PhantomHUD.Palette.Phantom);
    private boolean hostWasEnabled;

    public PhantomPanels() {
        super("Phantom Panels", ModuleCategory.client);
        instance = this;
        withDescription("Clean, right-aligned Phantom arraylist panels.");
        registerSettings(sort, values, valueColor, customColor, bar, barColor, opacity, radius,
                logo, animations, scale, position, xOffset, yOffset, gradientStart, gradientEnd, palette);
        showInHud = false;
    }

    @Override public void onEnable() {
        Module host = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByName("HUD");
        hostWasEnabled = host != null && host.isEnabled();
        if (host != null && !host.isEnabled()) host.enable();
        renderer.reset();
    }

    @Override public void onDisable() {
        Module host = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByName("HUD");
        if (host != null && host.isEnabled() && !hostWasEnabled && !PhantomHUD.classicActive()) host.disable();
        renderer.reset();
    }

    static boolean isActive() { return instance != null && instance.isEnabled(); }

    static boolean renderHosted(Render2DEvent event) {
        if (!isActive()) return false;
        instance.renderGate();
        return true;
    }

    @Subscribe public void onRender2D(Render2DEvent event) {
        Module host = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByName("HUD");
        if (host == null || !host.isEnabled()) renderGate();
    }

    private void renderGate() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null
                || mc.gameSettings.showDebugInfo || mc.gameSettings.hideGUI || Raven.moduleManager == null) return;
        renderer.render(this);
    }

    public boolean settingVisible(Setting setting) {
        return (setting != customColor || valueColor.getMode() == PhantomHUD.ValueColor.Custom)
                && (setting != barColor || bar.isToggled())
                && (setting != gradientStart && setting != gradientEnd || barColor.getMode() == PhantomHUD.BarColor.Gradient);
    }

    java.util.List<Module> panelRoster() { return Raven.moduleManager.getModules(); }
    float panelScale() { return (float) scale.getInput(); }
    PhantomHUD.Position panelPosition() { return position.getMode(); }
    float panelXOffset() { return (float) xOffset.getInput(); }
    float panelYOffset() { return (float) yOffset.getInput(); }
    PhantomHUD.SortMode panelSort() { return sort.getMode(); }
    boolean panelShowValues() { return values.isToggled(); }
    boolean panelBar() { return bar.isToggled(); }
    PhantomHUD.BarColor panelBarColor() { return barColor.getMode(); }
    PhantomHUD.ValueColor panelValueColor() { return valueColor.getMode(); }
    int panelCustomColor() { return rgb(customColor); }
    int panelAccent() { return palette.getMode() == PhantomHUD.Palette.Client
            ? keystrokesmod.client.module.modules.client.GuiModule.getEnabledTopRGB() : rgb(gradientStart); }
    int panelGradientEnd() { return palette.getMode() == PhantomHUD.Palette.Client
            ? keystrokesmod.client.module.modules.client.GuiModule.getEnabledBottomRGB() : rgb(gradientEnd); }
    float panelOpacity() { return (float) opacity.getInput() / 100F; }
    float panelRadius() { return (float) radius.getInput(); }
    boolean panelLogo() { return logo.isToggled(); }
    boolean panelAnimations() { return animations.isToggled(); }
    void panelBounds(float x, float y, float width, float height, boolean right) {
        PhantomHUD.panelBounds(x, y, width, height, right);
    }
    private static int rgb(PickerRGBSetting color) {
        return 0xFF000000 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
    }
}
