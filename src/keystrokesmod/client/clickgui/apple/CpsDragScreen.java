package keystrokesmod.client.clickgui.apple;

import java.io.IOException;
import keystrokesmod.client.module.modules.render.AutoClickCps;
import keystrokesmod.client.module.setting.impl.CpsPositionSetting;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** Drag the CPS card over the actual screen area; persisted as normalized coordinates. */
public final class CpsDragScreen extends GuiScreen {
    private final GuiScreen parent;
    private final CpsPositionSetting position;
    private int left, top, dx, dy;
    private boolean dragging;

    public CpsDragScreen(GuiScreen parent, CpsPositionSetting position) {
        this.parent = parent;
        this.position = position;
    }

    @Override public void initGui() {
        left = position.hasPlaced() ? position.x(width, AutoClickCps.CARD_WIDTH)
                : (width - AutoClickCps.CARD_WIDTH) / 2;
        top = position.hasPlaced() ? position.y(height, AutoClickCps.CARD_HEIGHT)
                : (height - AutoClickCps.CARD_HEIGHT) / 2;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            left = Math.max(0, Math.min(width - AutoClickCps.CARD_WIDTH, mouseX - dx));
            top = Math.max(0, Math.min(height - AutoClickCps.CARD_HEIGHT, mouseY - dy));
            position.move(left, top, width, height, AutoClickCps.CARD_WIDTH, AutoClickCps.CARD_HEIGHT);
        }
        drawRect(0, 0, width, height, 0x9E0D160F);
        drawCenteredString(fontRendererObj, "DRAG THE GLOWING CPS COUNTER", width / 2, 16, 0xFFF1FFF1);
        drawCenteredString(fontRendererObj, "Release to place  |  Esc to finish", width / 2, 30, 0xFFB7D1BD);
        int buttonY = height - 30;
        drawRect(width / 2 - 42, buttonY, width / 2 + 42, buttonY + 21, 0xFF467B52);
        drawCenteredString(fontRendererObj, "Done", width / 2, buttonY + 7, 0xFFFFFFFF);
        drawRect(left - 5, top - 5, left + AutoClickCps.CARD_WIDTH + 5,
                top + AutoClickCps.CARD_HEIGHT + 5, 0x8079E99C);
        drawRect(left - 2, top - 2, left + AutoClickCps.CARD_WIDTH + 2,
                top + AutoClickCps.CARD_HEIGHT + 2, 0xFFACF1BC);
        AutoClickCps.drawPreview(left, top);
        drawCenteredString(fontRendererObj, "click + drag", left + AutoClickCps.CARD_WIDTH / 2,
                top + AutoClickCps.CARD_HEIGHT + 10, 0xFFC8F3CF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        if (mouseX >= width / 2 - 42 && mouseX <= width / 2 + 42
                && mouseY >= height - 30 && mouseY < height - 9) {
            mc.displayGuiScreen(parent);
        } else if (mouseX >= left && mouseX < left + AutoClickCps.CARD_WIDTH
                && mouseY >= top && mouseY < top + AutoClickCps.CARD_HEIGHT) {
            dragging = true;
            dx = mouseX - left;
            dy = mouseY - top;
        }
    }

    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (button == 0 && dragging) {
            left = Math.max(0, Math.min(width - AutoClickCps.CARD_WIDTH, x - dx));
            top = Math.max(0, Math.min(height - AutoClickCps.CARD_HEIGHT, y - dy));
            position.move(left, top, width, height, AutoClickCps.CARD_WIDTH, AutoClickCps.CARD_HEIGHT);
        }
    }

    @Override protected void mouseReleased(int x, int y, int button) {
        if (button == 0) dragging = false;
    }

    @Override protected void keyTyped(char ch, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
