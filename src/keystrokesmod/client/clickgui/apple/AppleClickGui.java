package keystrokesmod.client.clickgui.apple;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.nio.ByteBuffer;
import keystrokesmod.client.clickgui.apple.rebuild.GuiPreferences;
import keystrokesmod.client.clickgui.apple.rebuild.UiDraw;
import keystrokesmod.client.clickgui.apple.rebuild.GuiRoot;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.modules.client.GuiModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import keystrokesmod.client.clickgui.apple.rebuild.GuiRenderContext;
import keystrokesmod.client.clickgui.apple.rebuild.GuiGlDebug;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.BufferUtils;
import net.minecraft.client.renderer.GlStateManager;

/** Complete retained-state replacement for the legacy Apple sidebar GUI. */
public class AppleClickGui extends GuiScreen {
    // LWJGL 2 requires capacity for 16 GLboolean results even for a four-value query.
    private static final ByteBuffer COLOR_MASK = BufferUtils.createByteBuffer(16);
    /* Retained for binary compatibility with the legacy Apple setting widgets. */
    public static final int C_BG = 0xFF12151C, C_SIDEBAR = 0xFF101319, C_SEP = 0x14FFFFFF, C_HOVER = 0x12FFFFFF;
    public static final int C_SELECTED = 0xFF5B5FE0, C_CARD = 0xD91A1E27, C_CARD_ON = 0xE01D2230, C_TEXT = 0xFFFFFFFF;
    public static final int C_TEXT_SEC = 0xFF8A90A0, C_ACCENT = 0xFF5B5FE0, C_ON = 0xFF5B5FE0, C_OFF = 0xFF303641;
    public static final int C_THUMB = 0xFFFFFFFF, C_SLIDER_FILL = 0xFF5B5FE0, C_SLIDER_BG = 0xFF303641;
    public static final int C_FAVORITE = 0xFFF5D742, C_FAVORITE_OFF = 0xFF8A90A0, C_DOTS = 0xFF8A90A0;
    public static final int C_DIVIDER = 0x14FFFFFF, C_COUNT_BG = 0xFF5B5FE0, C_COUNT_TEXT = 0xFFFFFFFF;
    public static int mouseX, mouseY;
    public static final Set<String> favorites = new HashSet<String>();
    private final GuiRoot root = new GuiRoot(this);

    public AppleClickGui() { GuiPreferences.attach(favorites); }

    /** Kept for the existing Gui module open path. */
    public void open() { root.open(); }
    public static boolean isFavorite(String moduleName) { return favorites.contains(moduleName); }
    public static void toggleFavorite(String moduleName) {
        if (favorites.contains(moduleName)) favorites.remove(moduleName); else favorites.add(moduleName);
        GuiPreferences.save();
    }

    /* New class declarations are not remapped by the patch tool; retain SRG callback bridges for Lunar's runtime. */
    public void func_73866_w_() { initGui(); }
    public void func_73863_a(int x, int y, float partialTicks) { drawScreen(x, y, partialTicks); }
    protected void func_73864_a(int x, int y, int button) throws IOException { mouseClicked(x, y, button); }
    protected void func_146286_b(int x, int y, int button) { mouseReleased(x, y, button); }
    protected void func_146273_a(int x, int y, int button, long elapsed) { mouseClickMove(x, y, button, elapsed); }
    public void func_146274_d() throws IOException { handleMouseInput(); }
    protected void func_73869_a(char character, int keyCode) throws IOException { keyTyped(character, keyCode); }
    public void func_73876_c() { updateScreen(); }
    public void func_146281_b() { onGuiClosed(); }
    public boolean func_73868_f() { return doesGuiPauseGame(); }

    @Override public void initGui() {
        super.initGui();
        GuiRenderContext.update(Minecraft.getMinecraft());
        root.init(GuiRenderContext.designWidth(), GuiRenderContext.designHeight());
        root.open();
    }
    @Override public void drawScreen(int x, int y, float partialTicks) {
        GuiRenderContext.update(Minecraft.getMinecraft());
        float nativeX = GuiRenderContext.mouseX(x), nativeY = GuiRenderContext.mouseY(y);
        mouseX = Math.round(nativeX); mouseY = Math.round(nativeY);
        int activeUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        COLOR_MASK.clear(); GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, COLOR_MASK);
        boolean red = COLOR_MASK.get(0) != 0, green = COLOR_MASK.get(1) != 0;
        boolean blue = COLOR_MASK.get(2) != 0, alpha = COLOR_MASK.get(3) != 0;
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        GL11.glPushMatrix();
        GL11.glScalef(1F / GuiRenderContext.minecraftScale(), 1F / GuiRenderContext.minecraftScale(), 1F);
        GL11.glScalef(GuiRenderContext.uiScale(), GuiRenderContext.uiScale(), 1F);
        GL11.glEnable(GL13.GL_MULTISAMPLE);
        GuiGlDebug.begin(root.glDebugEnabled());
        UiDraw.beginFrame();
        UiDraw.beginText();
        try { root.draw(GuiRenderContext.designWidth(), GuiRenderContext.designHeight(), nativeX, nativeY); }
        finally {
            try { UiDraw.endText(); UiDraw.endFrame(); UiDraw.clearScissor(); }
            finally {
                GL11.glPopMatrix();
                GL11.glPopClientAttrib();
                GL11.glPopAttrib();
                UiDraw.restoreGlAfterFrame(activeUnit, srcRgb, dstRgb, srcAlpha, dstAlpha, red, green, blue, alpha);
                GuiGlDebug.finish();
            }
        }
    }
    @Override protected void mouseClicked(int x, int y, int button) throws IOException { root.mouseClicked(GuiRenderContext.mouseX(x), GuiRenderContext.mouseY(y), button); }
    @Override protected void mouseReleased(int x, int y, int button) { root.mouseReleased(GuiRenderContext.mouseX(x), GuiRenderContext.mouseY(y), button); }
    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) { root.mouseDragged(GuiRenderContext.mouseX(x), GuiRenderContext.mouseY(y), button); }
    @Override public void updateScreen() { super.updateScreen(); root.tick(); }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        Minecraft mc = Minecraft.getMinecraft(); GuiRenderContext.update(mc);
        mouseX = Math.round(GuiRenderContext.eventX(Mouse.getEventX()));
        mouseY = Math.round(GuiRenderContext.eventY(mc.displayHeight - Mouse.getEventY() - 1));
        root.wheel(wheel / 120F, GuiRenderContext.eventX(Mouse.getEventX()),
                GuiRenderContext.eventY(mc.displayHeight - Mouse.getEventY() - 1));
    }
    @Override protected void keyTyped(char character, int keyCode) throws IOException { if (!root.key(character, keyCode)) super.keyTyped(character, keyCode); }
    @Override public void onGuiClosed() {
        root.closed();
        Minecraft.getMinecraft().gameSettings.guiScale = GuiModule.guiScale;
        if (Raven.configManager != null) Raven.configManager.save();
        if (Raven.clientConfig != null) Raven.clientConfig.saveConfig();
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
