package keystrokesmod.client.clickgui.raven.components;

import java.awt.Color;
import java.io.IOException;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

/** Compact, texture-backed color picker for Player Hitbox ESP. */
public final class ColorPickerScreen extends GuiScreen {
    private static final int PALETTE_SIZE = 128;
    private final GuiScreen parent;
    private final PickerRGBSetting setting;
    private final int original, originalOpacity;
    private float hue, saturation, brightness;
    private int tab, dragging;
    private boolean accepted;
    private GuiTextField[] fields;
    private int px, py, pw, ph, squareX, squareY, squareW, squareH;
    private DynamicTexture palette, hueTexture, opacityTexture;
    private ResourceLocation paletteLocation, hueLocation, opacityLocation;
    private int paletteHue = -1, opacityRGB = -1;

    public ColorPickerScreen(GuiScreen parent, PickerRGBSetting setting) {
        this.parent = parent;
        this.setting = setting;
        original = currentRGB();
        originalOpacity = (int) setting.getOpacity().getInput();
        fromRGB(original);
    }

    private int currentRGB() {
        return (setting.getRed() << 16) | (setting.getGreen() << 8) | setting.getBlue();
    }

    private void fromRGB(int rgb) {
        float[] hsv = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    private void applyRGB(int rgb) {
        setting.setRed((rgb >> 16) & 255);
        setting.setGreen((rgb >> 8) & 255);
        setting.setBlue(rgb & 255);
        fromRGB(rgb);
        syncFields();
    }

    private void applyHSV() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        setting.setRed((rgb >> 16) & 255);
        setting.setGreen((rgb >> 8) & 255);
        setting.setBlue(rgb & 255);
        syncFields();
    }

    @Override public void initGui() {
        pw = Math.min(278, width - 24);
        ph = Math.min(264, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        squareX = px + 31;
        squareY = py + 106;
        squareW = pw - 62;
        squareH = Math.max(24, ph - 150);
        fields = new GuiTextField[3];
        int fieldWidth = Math.min(67, (pw - 42) / 3);
        for (int i = 0; i < 3; i++) {
            int x = px + 17 + i * (pw - 22) / 3;
            fields[i] = new GuiTextField(i, fontRendererObj, x, py + 77, fieldWidth, 18);
            fields[i].setMaxStringLength(7);
            fields[i].setTextColor(0xFFE9F5EA);
            fields[i].setDisabledTextColour(0xFFE9F5EA);
        }
        syncFields();
        palette = new DynamicTexture(PALETTE_SIZE, PALETTE_SIZE);
        hueTexture = new DynamicTexture(1, PALETTE_SIZE);
        opacityTexture = new DynamicTexture(12, PALETTE_SIZE);
        paletteLocation = mc.getTextureManager().getDynamicTextureLocation("hitbox-color-palette", palette);
        hueLocation = mc.getTextureManager().getDynamicTextureLocation("hitbox-color-hue", hueTexture);
        opacityLocation = mc.getTextureManager().getDynamicTextureLocation("hitbox-color-opacity", opacityTexture);
        int[] hues = hueTexture.getTextureData();
        for (int y = 0; y < PALETTE_SIZE; y++) hues[y] = Color.HSBtoRGB(y / (float) (PALETTE_SIZE - 1), 1, 1);
        hueTexture.updateDynamicTexture();
        Keyboard.enableRepeatEvents(true);
    }

    private void syncFields() {
        if (fields == null) return;
        if (tab == 0) {
            fields[0].setText(Integer.toString(Math.round(hue * 360)));
            fields[1].setText(Integer.toString(Math.round(saturation * 100)));
            fields[2].setText(Integer.toString(Math.round(brightness * 100)));
        } else if (tab == 1) {
            fields[0].setText(Integer.toString(setting.getRed()));
            fields[1].setText(Integer.toString(setting.getGreen()));
            fields[2].setText(Integer.toString(setting.getBlue()));
        } else fields[0].setText(String.format("#%06X", currentRGB()));
    }

    private void updateTextures() {
        int h = Math.round(hue * 1024);
        if (h != paletteHue) {
            paletteHue = h;
            int[] pixels = palette.getTextureData();
            for (int y = 0; y < PALETTE_SIZE; y++)
                for (int x = 0; x < PALETTE_SIZE; x++)
                    pixels[y * PALETTE_SIZE + x] = Color.HSBtoRGB(hue,
                            x / (float) (PALETTE_SIZE - 1), 1 - y / (float) (PALETTE_SIZE - 1));
            palette.updateDynamicTexture();
        }
        int rgb = currentRGB();
        if (rgb != opacityRGB) {
            opacityRGB = rgb;
            int[] pixels = opacityTexture.getTextureData();
            for (int y = 0; y < PALETTE_SIZE; y++) {
                int alpha = Math.round(255 * (1 - y / (float) (PALETTE_SIZE - 1)));
                for (int x = 0; x < 12; x++) {
                    int checker = ((x / 6 + y / 8) & 1) == 0 ? 0xE8 : 0x8C;
                    int red = (((rgb >> 16) & 255) * alpha + checker * (255 - alpha)) / 255;
                    int green = (((rgb >> 8) & 255) * alpha + checker * (255 - alpha)) / 255;
                    int blue = ((rgb & 255) * alpha + checker * (255 - alpha)) / 255;
                    pixels[y * 12 + x] = 0xFF000000 | red << 16 | green << 8 | blue;
                }
            }
            opacityTexture.updateDynamicTexture();
        }
    }

    private void texture(ResourceLocation location, int x, int y, int w, int h, int sourceW, int sourceH) {
        mc.getTextureManager().bindTexture(location);
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, sourceW, sourceH, w, h, sourceW, sourceH);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xB8000000);
        drawRect(px - 4, py - 4, px + pw + 4, py + ph + 4, 0x76000000);
        drawRect(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF497450);
        drawRect(px, py, px + pw, py + ph, 0xFF1B231E);
        drawRect(px, py, px + pw, py + 33, 0xFF26332A);
        fontRendererObj.drawStringWithShadow("Outline color", px + 12, py + 12, 0xFFF2F7F1);
        drawRect(px + pw - 39, py + 8, px + pw - 12, py + 26, 0xFF0D150F);
        drawRect(px + pw - 37, py + 10, px + pw - 14, py + 24, 0xFF000000 | currentRGB());

        int tabY = py + 38, tabWidth = (pw - 24) / 3;
        for (int i = 0; i < 3; i++) {
            int x = px + 12 + i * tabWidth;
            drawRect(x, tabY, i == 2 ? px + pw - 12 : x + tabWidth - 3, tabY + 21,
                    tab == i ? 0xFF41684B : 0xFF29372E);
            drawCenteredString(fontRendererObj, new String[] {"HSV", "RGB", "HEX"}[i],
                    x + tabWidth / 2 - (i == 2 ? 1 : 2), tabY + 6,
                    tab == i ? 0xFFFFFFFF : 0xFFABC0AF);
        }

        if (tab == 2) {
            fontRendererObj.drawString("HEX", px + 17, py + 66, 0xFFAFC4B4);
            fields[0].drawTextBox();
        } else {
            String[] labels = tab == 0 ? new String[] {"HUE", "SATURATION", "BRIGHTNESS"}
                    : new String[] {"RED", "GREEN", "BLUE"};
            for (int i = 0; i < 3; i++) {
                int labelX = px + 17 + i * (pw - 22) / 3;
                fontRendererObj.drawString(labels[i], labelX, py + 66, 0xFFAFC4B4);
                fields[i].drawTextBox();
            }
        }

        updateTextures();
        drawRect(squareX - 1, squareY - 1, squareX + squareW + 1, squareY + squareH + 1, 0xFF718577);
        texture(paletteLocation, squareX, squareY, squareW, squareH, PALETTE_SIZE, PALETTE_SIZE);
        texture(hueLocation, squareX + squareW + 9, squareY, 12, squareH, 1, PALETTE_SIZE);
        texture(opacityLocation, squareX - 22, squareY, 12, squareH, 12, PALETTE_SIZE);
        int cx = squareX + Math.round((squareW - 1) * saturation);
        int cy = squareY + Math.round((squareH - 1) * (1 - brightness));
        drawRect(cx - 3, cy - 3, cx + 4, cy + 4, 0xFF121912);
        drawRect(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
        drawRect(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF000000 | currentRGB());
        int hy = squareY + Math.round((squareH - 1) * hue);
        drawRect(squareX + squareW + 7, hy - 1, squareX + squareW + 23, hy + 2, 0xFFFFFFFF);
        int ay = squareY + (int) Math.round((squareH - 1) * (1 - setting.getOpacity().getInput() / 100.0));
        drawRect(squareX - 24, ay - 1, squareX - 8, ay + 2, 0xFFFFFFFF);

        int footer = py + ph - 32;
        drawRect(px + 10, footer, px + pw / 2 - 4, py + ph - 8, 0xFF477F54);
        drawRect(px + pw / 2 + 4, footer, px + pw - 10, py + ph - 8, 0xFF344038);
        drawCenteredString(fontRendererObj, "Apply", px + pw / 4, footer + 8, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "Cancel", px + pw * 3 / 4, footer + 8, 0xFFE0E9DF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (mouseY >= py + 38 && mouseY < py + 59 && mouseX >= px + 12 && mouseX < px + pw - 12) {
            commitFields();
            tab = Math.min(2, (mouseX - px - 12) * 3 / (pw - 24));
            for (GuiTextField field : fields) field.setFocused(false);
            syncFields();
            return;
        }
        if (mouseY >= py + ph - 32 && mouseY < py + ph - 8 && mouseX >= px + 10 && mouseX < px + pw - 10) {
            if (mouseX < px + pw / 2) {
                commitFields();
                accepted = true;
            }
            mc.displayGuiScreen(parent);
            return;
        }
        if (mouseY >= squareY && mouseY < squareY + squareH) {
            if (mouseX >= squareX && mouseX < squareX + squareW) dragging = 1;
            else if (mouseX >= squareX + squareW + 8 && mouseX < squareX + squareW + 23) dragging = 2;
            else if (mouseX >= squareX - 24 && mouseX < squareX - 8) dragging = 3;
            if (dragging != 0) {
                updateDrag(mouseX, mouseY);
                return;
            }
        }
        for (int i = 0; i < (tab == 2 ? 1 : 3); i++) fields[i].mouseClicked(mouseX, mouseY, button);
    }

    private void updateDrag(int x, int y) {
        float value = Math.max(0, Math.min(1, (y - squareY) / (float) (squareH - 1)));
        if (dragging == 1) {
            saturation = Math.max(0, Math.min(1, (x - squareX) / (float) (squareW - 1)));
            brightness = 1 - value;
            applyHSV();
        } else if (dragging == 2) {
            hue = Math.min(0.9999F, value);
            applyHSV();
        } else if (dragging == 3) setting.getOpacity().setValue(Math.round((1 - value) * 100));
    }

    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (button == 0 && dragging != 0) updateDrag(x, y);
    }

    @Override protected void mouseReleased(int x, int y, int button) {
        if (button == 0) dragging = 0;
    }

    private void commitFields() {
        try {
            if (tab == 2) {
                String hex = fields[0].getText().replace("#", "").trim();
                if (hex.length() != 6) throw new NumberFormatException();
                applyRGB(Integer.parseInt(hex, 16));
            } else {
                int a = Integer.parseInt(fields[0].getText().trim());
                int b = Integer.parseInt(fields[1].getText().trim());
                int c = Integer.parseInt(fields[2].getText().trim());
                if (tab == 1) {
                    if (a < 0 || a > 255 || b < 0 || b > 255 || c < 0 || c > 255)
                        throw new NumberFormatException();
                    applyRGB(a << 16 | b << 8 | c);
                } else {
                    if (a < 0 || a > 360 || b < 0 || b > 100 || c < 0 || c > 100)
                        throw new NumberFormatException();
                    hue = (a % 360) / 360.0F;
                    saturation = b / 100.0F;
                    brightness = c / 100.0F;
                    applyHSV();
                }
            }
        } catch (NumberFormatException ignored) {
            syncFields();
        }
    }

    @Override protected void keyTyped(char character, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commitFields();
            for (GuiTextField field : fields) field.setFocused(false);
            return;
        }
        for (int i = 0; i < (tab == 2 ? 1 : 3); i++) fields[i].textboxKeyTyped(character, keyCode);
    }

    @Override public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (!accepted) {
            applyRGB(original);
            setting.getOpacity().setValue(originalOpacity);
        }
        if (paletteLocation != null) mc.getTextureManager().deleteTexture(paletteLocation);
        if (hueLocation != null) mc.getTextureManager().deleteTexture(hueLocation);
        if (opacityLocation != null) mc.getTextureManager().deleteTexture(opacityLocation);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
