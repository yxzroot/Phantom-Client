package keystrokesmod.client.clickgui.apple;

import keystrokesmod.client.clickgui.apple.settings.ABind;
import keystrokesmod.client.clickgui.apple.settings.ACombo;
import keystrokesmod.client.clickgui.apple.settings.ADoubleSlider;
import keystrokesmod.client.clickgui.apple.settings.ASettingComponent;
import keystrokesmod.client.clickgui.apple.settings.ASlider;
import keystrokesmod.client.clickgui.apple.settings.AToggle;
import keystrokesmod.client.clickgui.apple.settings.AppleCpsMoveRow;
import keystrokesmod.client.clickgui.apple.settings.ApplePickerRGB;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.CoolDown;
import keystrokesmod.client.utils.RenderUtils;
import keystrokesmod.client.utils.Utils.Client;
import keystrokesmod.client.utils.font.FontUtil;
import net.minecraft.client.gui.Gui;

import java.util.ArrayList;
import java.util.List;

/** Apple module card with a deliberately disabled coming-soon section for Legit Scaffold. */
public class AppleModuleRow {
    private static final int ROW_H = 32;
    private static final int LEGIT_READY_SETTINGS = 3;
    final Module mod;
    private final List<ASettingComponent> settings = new ArrayList<>();
    private ABind bindRow;
    private boolean expanded;
    private int x, y, width;
    private final CoolDown toggleAnim = new CoolDown(300L);

    public AppleModuleRow(Module mod) {
        this.mod = mod;
        for (Setting setting : mod.getSettings()) {
            ASettingComponent component = buildComponent(setting);
            if (component != null) settings.add(component);
        }
        if (mod.isBindable()) bindRow = new ABind(mod);
    }

    private ASettingComponent buildComponent(Setting setting) {
        if (setting instanceof TickSetting) return new AToggle((TickSetting) setting, mod);
        if (setting instanceof SliderSetting) return new ASlider((SliderSetting) setting);
        if (setting instanceof ComboSetting) return new ACombo((ComboSetting) setting, mod);
        if (setting instanceof RGBSetting) return ApplePickerRGB.create((RGBSetting) setting);
        if (setting instanceof DoubleSliderSetting) return new ADoubleSlider((DoubleSliderSetting) setting);
        return AppleCpsMoveRow.create(setting);
    }

    public void setPosition(int x, int y, int width) { this.x = x; this.y = y; this.width = width; }

    public void draw(int mouseX, int mouseY) {
        int cardHeight = getTotalHeight() - 2;
        RenderUtils.drawRoundedRect(x, y, x + width, y + cardHeight, 6,
                mod.isEnabled() ? 0xFF1B2E1B : 0xFF222222);
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_H)
            RenderUtils.drawRoundedRect(x, y, x + width, y + Math.min(ROW_H, cardHeight), 6, 0x18FFFFFF);

        float nameY = y + 4;
        FontUtil.poppinsBold.drawSmoothString(mod.getName(), x + 12, nameY, 0xFFE0E0E0);
        String description = buildDescription();
        if (description != null && !description.isEmpty()) {
            float maxWidth = width - 90;
            String visible = description;
            while (FontUtil.poppinsRegular.getStringWidth(visible) > maxWidth && visible.length() > 0)
                visible = visible.substring(0, visible.length() - 1);
            if (!visible.equals(description)) visible += "...";
            FontUtil.poppinsRegular.drawSmoothString(visible, x + 12,
                    nameY + FontUtil.poppinsBold.getHeight() + 1, 0xFF808080);
        }
        drawStar(mouseX, mouseY);
        drawToggle();
        drawDotsButton(mouseX, mouseY);

        if (!expanded) return;
        Gui.drawRect(x + 8, y + ROW_H, x + width - 8, y + ROW_H + 1, 0xFF2A2A2A);
        int settingY = y + ROW_H + 6;
        int unavailableTop = -1;
        for (int index = 0; index < settings.size(); index++) {
            ASettingComponent component = settings.get(index);
            if (!component.visable) continue;
            if (isLegitScaffoldSoon() && index == LEGIT_READY_SETTINGS) unavailableTop = settingY;
            component.setPosition(x + 16, settingY, width - 32);
            component.draw(mouseX, mouseY);
            settingY += component.getHeight() + 4;
        }
        if (isLegitScaffoldSoon() && unavailableTop >= 0) drawSoonOverlay(unavailableTop, settingY - 4);
        if (bindRow != null) {
            bindRow.setPosition(x + 16, settingY, width - 32);
            bindRow.draw(mouseX, mouseY);
        }
    }

    private boolean isLegitScaffoldSoon() { return "Legit Scaffold".equals(mod.getName()); }

    /** A cheap frosted overlay: it visually mutes unfinished controls without a costly framebuffer blur. */
    private void drawSoonOverlay(int top, int bottom) {
        int left = x + 10, right = x + width - 10;
        RenderUtils.drawRoundedRect(left, top - 2, right, bottom + 2, 6, 0xB5141820);
        RenderUtils.drawRoundedRect(left + 2, top, right - 2, bottom, 5, 0x32191E28);
        for (int line = top + 8; line < bottom; line += 10)
            RenderUtils.drawRoundedRect(left + 8, line, right - 8, line + 1, .5F, 0x14FFFFFF);
        String label = "SOON...";
        float textWidth = (float) FontUtil.poppinsBold.getStringWidth(label);
        float textY = top + Math.max(4, (bottom - top - FontUtil.poppinsBold.getHeight()) / 2F);
        FontUtil.poppinsBold.drawSmoothString(label, left + (right - left - textWidth) / 2F, textY, 0xFFDDE5F0);
    }

    private String buildDescription() {
        String text = mod.getDescription();
        if (text != null && !text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        for (Setting setting : mod.getSettings()) {
            if (result.length() > 60) break;
            if (setting instanceof SliderSetting) append(result, setting.getName() + " " + ((SliderSetting) setting).getInput());
            else if (setting instanceof ComboSetting) append(result, ((ComboSetting) setting).getMode().toString());
            else if (setting instanceof TickSetting && ((TickSetting) setting).isToggled()) append(result, setting.getName());
            else if (setting instanceof DoubleSliderSetting) {
                DoubleSliderSetting range = (DoubleSliderSetting) setting;
                append(result, range.getInputMin() + "-" + range.getInputMax());
            }
        }
        return result.toString();
    }

    private static void append(StringBuilder builder, String value) {
        if (builder.length() > 0) builder.append(". ");
        builder.append(value);
    }

    private void drawStar(int mouseX, int mouseY) {
        boolean favorite = AppleClickGui.isFavorite(mod.getName());
        int starX = x + width - 68, starY = y + 11;
        FontUtil.poppinsMedium.drawSmoothString(favorite ? "★" : "☆", starX,
                starY + (10 - FontUtil.poppinsMedium.getHeight()) / 2F, favorite ? 0xFFFFD700 : 0xFF555555);
    }

    private void drawDotsButton(int mouseX, int mouseY) {
        int dotsX = x + width - 14, dotsY = y + 11;
        boolean hover = mouseX >= dotsX - 2 && mouseX <= dotsX + 12 && mouseY >= dotsY - 2 && mouseY <= dotsY + 12;
        for (int i = 0; i < 3; i++)
            RenderUtils.drawRoundedRect(dotsX + 4, dotsY + 1 + i * 4, dotsX + 6, dotsY + 3 + i * 4,
                    1, hover ? 0xFFE0E0E0 : 0xFF888888);
    }

    private void drawToggle() {
        int toggleX = x + width - 48, toggleY = y + 10;
        float progress = Client.smoothPercent((mod.isEnabled() ? toggleAnim.getElapsedTime() : toggleAnim.getTimeLeft())
                / (float) toggleAnim.getCooldownTime());
        int track = lerpColor(0xFF3A3A3A, 0xFF4CAF50, progress);
        RenderUtils.drawRoundedRect(toggleX, toggleY, toggleX + 24, toggleY + 12, 6, track);
        int knob = (int) (toggleX + 1 + progress * 12);
        RenderUtils.drawRoundedRect(knob, toggleY + 1, knob + 10, toggleY + 11, 5, 0xFFFFFFFF);
    }

    public int getTotalHeight() {
        if (!expanded) return 34;
        int height = 40;
        for (ASettingComponent component : settings) if (component.visable) height += component.getHeight() + 4;
        if (bindRow != null) height += bindRow.getHeight() + 4;
        return height;
    }

    public boolean mouseDown(int mouseX, int mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + getTotalHeight()) return false;
        int starX = x + width - 68, starY = y + 11;
        if (mouseX >= starX - 2 && mouseX <= starX + 12 && mouseY >= starY - 2 && mouseY <= starY + 12) {
            AppleClickGui.toggleFavorite(mod.getName());
            return true;
        }
        int toggleX = x + width - 48, toggleY = y + 10;
        if (mouseX >= toggleX && mouseX <= toggleX + 24 && mouseY >= toggleY - 2 && mouseY <= toggleY + 14) {
            toggleAnim.setCooldown(300L); toggleAnim.start(); mod.toggle(); return true;
        }
        int dotsX = x + width - 14, dotsY = y + 11;
        if (mouseX >= dotsX - 4 && mouseX <= dotsX + 14 && mouseY >= dotsY - 4 && mouseY <= dotsY + 14) {
            expanded = !expanded; return true;
        }
        if (mouseY <= y + ROW_H || !expanded) return true;
        int settingY = y + ROW_H + 6;
        for (int index = 0; index < settings.size(); index++) {
            ASettingComponent component = settings.get(index);
            if (!component.visable) continue;
            int height = component.getHeight() + 4;
            if (isLegitScaffoldSoon() && index >= LEGIT_READY_SETTINGS
                    && mouseY >= settingY && mouseY < settingY + height) return true;
            if (component.mouseDown(mouseX, mouseY, button)) return true;
            settingY += height;
        }
        if (bindRow != null && bindRow.mouseDown(mouseX, mouseY, button)) return true;
        return true;
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        for (int index = 0; index < settings.size(); index++)
            if (!(isLegitScaffoldSoon() && index >= LEGIT_READY_SETTINGS))
                settings.get(index).mouseReleased(mouseX, mouseY, button);
    }

    public void keyTyped(char character, int keyCode) {
        for (int index = 0; index < settings.size(); index++)
            if (!(isLegitScaffoldSoon() && index >= LEGIT_READY_SETTINGS))
                settings.get(index).keyTyped(character, keyCode);
        if (bindRow != null) bindRow.keyTyped(character, keyCode);
    }

    private static int lerpColor(int from, int to, float amount) {
        int r = (int) ((from >>> 16 & 255) + ((to >>> 16 & 255) - (from >>> 16 & 255)) * amount);
        int g = (int) ((from >>> 8 & 255) + ((to >>> 8 & 255) - (from >>> 8 & 255)) * amount);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
