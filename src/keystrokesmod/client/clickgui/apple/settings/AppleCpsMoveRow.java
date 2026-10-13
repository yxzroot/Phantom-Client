package keystrokesmod.client.clickgui.apple.settings;

import keystrokesmod.client.clickgui.apple.CpsDragScreen;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.CpsPositionSetting;
import keystrokesmod.client.utils.RenderUtils;
import keystrokesmod.client.utils.font.FontUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/** The Apple GUI shows a position editor rather than X/Y sliders. */
public final class AppleCpsMoveRow extends ASettingComponent {
    private final CpsPositionSetting position;

    private AppleCpsMoveRow(CpsPositionSetting position) {
        this.position = position;
        height = 44;
    }

    public static ASettingComponent create(Setting setting) {
        return setting instanceof CpsPositionSetting ? new AppleCpsMoveRow((CpsPositionSetting) setting) : null;
    }

    @Override public void draw(int mouseX, int mouseY) {
        FontUtil.poppinsMedium.drawSmoothString("CPS position", x, y + 1, 0xFFEDEFEA);
        boolean hover = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        RenderUtils.drawRoundedRect(x, y + 17, x + width, y + 41, 5.0F,
                hover ? 0xFF345638 : 0xFF29482F);
        FontUtil.poppinsMedium.drawSmoothString("MOVE ON SCREEN", x + 10, y + 24, 0xFFDAF5DF);
        FontUtil.poppinsMedium.drawSmoothString(">", x + width - 17, y + 24, 0xFF9DE8B0);
    }

    @Override public void clicked(int mouseX, int mouseY, int button) {
        if (button != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen parent = mc.currentScreen;
        if (parent != null) mc.displayGuiScreen(new CpsDragScreen(parent, position));
    }
}
