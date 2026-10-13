package keystrokesmod.client.clickgui.apple.settings;

import keystrokesmod.client.clickgui.raven.components.ColorPickerScreen;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.utils.RenderUtils;
import keystrokesmod.client.utils.font.FontUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** Color swatch for the Apple GUI; only used with Player Hitbox ESP. */
public final class ApplePickerRGB extends ARgb {
    private final PickerRGBSetting color;

    public ApplePickerRGB(PickerRGBSetting color) {
        super(color);
        this.color = color;
        this.height = 43;
    }

    public static ASettingComponent create(RGBSetting setting) {
        return setting instanceof PickerRGBSetting
                ? new ApplePickerRGB((PickerRGBSetting) setting) : new ARgb(setting);
    }

    @Override public void draw(int mouseX, int mouseY) {
        FontUtil.poppinsMedium.drawSmoothString(color.getName(), x, y + 1, 0xFFEDEFEA);
        RenderUtils.drawRoundedRect(x, y + 17, x + width, y + 41, 5.0F,
                mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
                        ? 0xFF345638 : 0xFF29482F);
        RenderUtils.drawRoundedRect(x + 4, y + 20, x + 51, y + 38, 3.0F, 0xFF111D14);
        RenderUtils.drawRoundedRect(x + 6, y + 22, x + 49, y + 36, 3.0F, color.getRGB());
        FontUtil.poppinsMedium.drawSmoothString(String.format("#%06X", color.getRGB() & 0xFFFFFF),
                x + 60, y + 24, 0xFFE8EDE6);
        FontUtil.poppinsMedium.drawSmoothString("EDIT COLOR", x + width - 89, y + 24, 0xFF9DE8B0);
    }

    @Override public void clicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen parent = mc.currentScreen;
        if (parent != null) mc.displayGuiScreen(new ColorPickerScreen(parent, color));
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int button) {
        // No channel slider to drag in this variant.
    }
}
