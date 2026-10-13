package keystrokesmod.client.clickgui.raven.components;

import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

/** A small swatch in Raven's module settings that opens the standalone picker. */
public final class PickerRGBComponent extends SettingComponent {
    private final PickerRGBSetting color;

    public PickerRGBComponent(Setting setting, ModuleComponent module) {
        super(setting, module);
        color = (PickerRGBSetting) setting;
    }

    @Override public void draw(int mouseX, int mouseY) {
        setDimensions(moduleComponent.getWidth() - 10, 19);
        int x1 = x + 5, x2 = x + width;
        Gui.drawRect(x1, y + 2, x2, y + 17, 0x5530253C);
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRendererObj.drawStringWithShadow(color.getName(), x1 + 4, y + 6, 0xFFECE5FA);
        Gui.drawRect(x2 - 30, y + 4, x2 - 4, y + 15, color.getRGB());
    }

    @Override public void clicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen parent = mc.currentScreen;
        if (parent != null) mc.displayGuiScreen(new ColorPickerScreen(parent, color));
    }
}
