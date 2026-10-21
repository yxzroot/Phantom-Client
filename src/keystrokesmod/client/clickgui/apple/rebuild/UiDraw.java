package keystrokesmod.client.clickgui.apple.rebuild;

import java.awt.Font;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/** Draw helpers, a lightweight text batcher, and frame-local GUI profiling. */
public final class UiDraw {
    private static final int STACK = 12;
    private static final int CORNER_SEGMENTS = 8;
    private static final int ROUND_POINTS = CORNER_SEGMENTS + 1;
    private static final int[] SX = new int[STACK], SY = new int[STACK], SW = new int[STACK], SH = new int[STACK];
    private static final float[] LX = new float[STACK], LY = new float[STACK], LW = new float[STACK], LH = new float[STACK];
    private static final float[][] ROUND_COS = new float[4][ROUND_POINTS], ROUND_SIN = new float[4][ROUND_POINTS];
    private static final float[] STAR_COS = new float[10], STAR_SIN = new float[10];
    private static final GuiFont BODY = new GuiFont(15F, Font.PLAIN);
    private static final GuiFont SMALL = new GuiFont(13F, Font.PLAIN);
    private static final GuiFont MEDIUM = new GuiFont(16F, Font.PLAIN);
    private static final GuiFont BUTTON_FONT = new GuiFont(14F, Font.PLAIN);
    private static final GuiFont SEMIBOLD = new GuiFont(17F, Font.BOLD);
    private static final GuiFont SMALL_BOLD = new GuiFont(13F, Font.BOLD);
    private static final GuiFont LOGO = new GuiFont(24F, Font.BOLD);
    private static final GuiFont TITLE = new GuiFont(26F, Font.BOLD);
    private static final GuiFont DEBUG_30 = new GuiFont(30F, Font.PLAIN);

    private static int scissorDepth;
    private static float masterAlpha = 1F;
    private static float transformX, transformY, transformScale = 1F;
    private static boolean textSession, textBatch;
    private static GuiFont activeFont;
    private static int activeTexture;
    private static int glBegins, textDraws, lastGlBegins, lastTextDraws;
    private static long frameStarted, lastFrameNanos, textNanos;

    static {
        for (int corner = 0; corner < 4; corner++) for (int point = 0; point < ROUND_POINTS; point++) {
            double radians = Math.toRadians(-90D + corner * 90D + point * 90D / CORNER_SEGMENTS);
            ROUND_COS[corner][point] = (float) Math.cos(radians);
            ROUND_SIN[corner][point] = (float) Math.sin(radians);
        }
        for (int point = 0; point < 10; point++) {
            double radians = Math.toRadians(-90D + point * 36D);
            STAR_COS[point] = (float) Math.cos(radians);
            STAR_SIN[point] = (float) Math.sin(radians);
        }
    }

    private UiDraw() { }

    /** Starts one retained GUI frame. Text state is entered once and only flushed around shape work. */
    public static void beginFrame() {
        endText();
        GuiTokens.beginFrame();
        frameStarted = System.nanoTime();
        glBegins = textDraws = 0;
        textNanos = 0L;
        masterAlpha = 1F;
        clearRenderTransform();
        SdfRenderer.initialize();
        SdfRenderer.beginFrame();
        GuiFont.beginFrame();
    }

    public static void endFrame() {
        endText();
        SdfRenderer.endFrame();
        lastFrameNanos = Math.max(0L, System.nanoTime() - frameStarted);
        lastGlBegins = glBegins;
        lastTextDraws = textDraws;
        masterAlpha = 1F;
        clearRenderTransform();
        GuiProfiler.frame(lastFrameNanos);
        GuiTokens.endFrame();
    }

    /** Explicit baseline for states not protected by the compatibility attribute stacks. */
    public static void restoreGlAfterFrame(int activeUnit, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha,
                                           boolean red, boolean green, boolean blue, boolean alpha) {
        if (GLContext.getCapabilities().OpenGL20) {
            for (int index = 1; index <= 6; index++) GL20.glDisableVertexAttribArray(index);
            GL20.glUseProgram(0);
        }
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        if (GLContext.getCapabilities().OpenGL15) GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlStateManager.setActiveTexture(activeUnit);
        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        GlStateManager.colorMask(red, green, blue, alpha);
        GL11.glColorMask(red, green, blue, alpha);
        GlStateManager.resetColor();
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    /** Opens the text session once per GUI frame. Atlases are batched lazily as they are encountered. */
    public static void beginText() { textSession = true; }

    /** Flushes the current atlas and returns the GL state to the known GUI baseline. */
    public static void endText() {
        flushTextBatch();
        if (textSession) {
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            GlStateManager.resetColor();
        }
        textSession = false;
        activeFont = null;
        activeTexture = 0;
    }

    /** Ends only the currently-open GL_QUADS run; later text can continue the same frame session. */
    public static void flushText() { flushTextBatch(); }
    /** Flushes the streamed SDF layer before a matrix/scissor/state transition. */
    public static void flushShapes() { flushTextBatch(); SdfRenderer.flush(); }
    /** Prewarms the shader and all text atlases once per GUI opening. */
    public static void prepareResources() {
        SdfRenderer.initialize();
        BODY.prepare(); SMALL.prepare(); MEDIUM.prepare(); BUTTON_FONT.prepare(); SEMIBOLD.prepare(); SMALL_BOLD.prepare(); LOGO.prepare(); TITLE.prepare(); DEBUG_30.prepare();
    }

    public static float renderMillis() { return (System.nanoTime() - frameStarted) / 1_000_000F; }
    public static float lastRenderMillis() { return lastFrameNanos / 1_000_000F; }
    public static int glBeginCalls() { return glBegins; }
    public static int glDrawCalls() { return glBegins; }
    public static int textDrawCalls() { return textDraws; }
    public static int lastGlBeginCalls() { return lastGlBegins; }
    public static int lastTextDrawCalls() { return lastTextDraws; }
    public static float textMillis() { return textNanos / 1_000_000F; }
    public static int fontRebuilds() { return GuiFont.rebuildCount(); }
    public static boolean fontAtlasRebuiltThisFrame() { return GuiFont.rebuiltThisFrame(); }
    public static boolean sdfActive() { return SdfRenderer.active(); }
    public static String rendererStatus() { return SdfRenderer.status(); }
    public static void profileSections(long window, long header, long tabs, long grid, long settings, long tooltip) {
        GuiProfiler.sections(window, header, tabs, grid, textNanos, settings, tooltip);
    }

    public static void setMasterAlpha(float alpha) { masterAlpha = clamp(alpha); }
    public static float masterAlpha() { return masterAlpha; }

    /** Keeps scissor coordinates correct while GuiRoot scales the complete window transition. */
    public static void setRenderTransform(float centerX, float centerY, float scale) {
        transformX = centerX;
        transformY = centerY;
        transformScale = Math.max(.01F, scale);
    }

    public static void clearRenderTransform() { transformX = transformY = 0F; transformScale = 1F; }
    static float renderTransformScale() { return transformScale; }
    static void recordShapeDraw() { ++glBegins; }

    public static int alpha(int color, float value) {
        return Math.max(0, Math.min(255, Math.round((color >>> 24 & 255) * value))) << 24 | color & 0xFFFFFF;
    }

    public static int mix(int from, int to, float amount) {
        float t = clamp(amount);
        int a = Math.round((from >>> 24 & 255) + ((to >>> 24 & 255) - (from >>> 24 & 255)) * t);
        int r = Math.round((from >>> 16 & 255) + ((to >>> 16 & 255) - (from >>> 16 & 255)) * t);
        int g = Math.round((from >>> 8 & 255) + ((to >>> 8 & 255) - (from >>> 8 & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void rounded(float x, float y, float right, float bottom, float radius, int color) {
        color = master(color);
        if ((color >>> 24) == 0 || right <= x || bottom <= y || outside(x, y, right, bottom)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, radius, color, color, 0, 0F, 0, 0F, false); return; }
        float feather = feather();
        begin();
        roundedSolid(x, y, right, bottom, radius, color);
        ring(x - feather, y - feather, right + feather, bottom + feather, radius + feather, alpha(color, 0F),
                x, y, right, bottom, radius, color);
        end();
    }

    public static void outline(float x, float y, float right, float bottom, float radius, int color) {
        outline(x, y, right, bottom, radius, color, feather());
    }

    /** AA rounded stroke; the optional width is in GUI design pixels. */
    public static void outline(float x, float y, float right, float bottom, float radius, int color, float width) {
        color = master(color);
        if ((color >>> 24) == 0 || right <= x || bottom <= y || outside(x, y, right, bottom)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, radius, 0, 0, color, width, 0, 0F, false); return; }
        float feather = feather();
        float stroke = Math.max(feather, width);
        float innerX = x + stroke, innerY = y + stroke, innerRight = right - stroke, innerBottom = bottom - stroke;
        if (innerRight <= innerX || innerBottom <= innerY) { rounded(x, y, right, bottom, radius, color); return; }
        begin();
        ring(x - feather, y - feather, right + feather, bottom + feather, radius + feather, alpha(color, 0F),
                x, y, right, bottom, radius, color);
        ring(x, y, right, bottom, radius, color, innerX, innerY, innerRight, innerBottom, Math.max(0F, radius - stroke), color);
        ring(innerX, innerY, innerRight, innerBottom, Math.max(0F, radius - stroke), color,
                innerX + feather, innerY + feather, innerRight - feather, innerBottom - feather,
                Math.max(0F, radius - stroke - feather), alpha(color, 0F));
        end();
    }

    public static void rect(float x, float y, float right, float bottom, int color) {
        color = master(color);
        if ((color >>> 24) == 0 || right <= x || bottom <= y || outside(x, y, right, bottom)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, 0F, color, color, 0, 0F, 0, 0F, false); return; }
        begin();
        glBegin(GL11.GL_QUADS);
        vertex(x, y, color); vertex(right, y, color); vertex(right, bottom, color); vertex(x, bottom, color);
        GL11.glEnd();
        end();
    }

    public static void verticalGradient(float x, float y, float right, float bottom, int top, int bottomColor) {
        top = master(top); bottomColor = master(bottomColor);
        if (((top | bottomColor) >>> 24) == 0 || right <= x || bottom <= y || outside(x, y, right, bottom)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, 0F, top, bottomColor, 0, 0F, 0, 0F, false); return; }
        begin(); GL11.glShadeModel(GL11.GL_SMOOTH); glBegin(GL11.GL_QUADS);
        vertex(x, y, top); vertex(right, y, top); vertex(right, bottom, bottomColor); vertex(x, bottom, bottomColor);
        GL11.glEnd(); GL11.glShadeModel(GL11.GL_FLAT); end();
    }

    public static void horizontalGradient(float x, float y, float right, float bottom, int left, int rightColor) {
        left = master(left); rightColor = master(rightColor);
        if (((left | rightColor) >>> 24) == 0 || right <= x || bottom <= y || outside(x, y, right, bottom)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, 0F, left, rightColor, 0, 0F, 0, 0F, true); return; }
        begin(); GL11.glShadeModel(GL11.GL_SMOOTH); glBegin(GL11.GL_QUADS);
        vertex(x, y, left); vertex(right, y, rightColor); vertex(right, bottom, rightColor); vertex(x, bottom, left);
        GL11.glEnd(); GL11.glShadeModel(GL11.GL_FLAT); end();
    }

    public static void shadow(float x, float y, float right, float bottom, float radius, int color, int layers) {
        color = master(color);
        if (outside(x - layers, y - layers, right + layers, bottom + layers)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, radius, 0, 0, 0, 0F, color, Math.max(3F, layers * 4F), false); return; }
        for (int layer = layers; layer > 0; --layer)
            rounded(x - layer, y - layer, right + layer, bottom + layer, radius + layer, alpha(color, .16F / (layer + 1F)));
    }
    /** Window-only dark shadow: 40px falloff, 8px downward offset in the SDF path. */
    public static void windowShadow(float x, float y, float right, float bottom, float radius) {
        int color = master(0x59000000);
        if (SdfRenderer.active()) SdfRenderer.rounded(x, y, right, bottom, radius, 0, 0, 0, 0F, color, 40F, false);
        else shadow(x, y + 8F, right, bottom + 8F, radius, 0x59000000, 10);
    }

    public static void circle(float x, float y, float radius, int color) {
        color = master(color);
        if ((color >>> 24) == 0 || outside(x - radius, y - radius, x + radius, y + radius)) return;
        if (SdfRenderer.active()) { SdfRenderer.circle(x, y, radius, color, color, 0, 0F, 0, 0F); return; }
        rounded(x - radius, y - radius, x + radius, y + radius, radius, color);
    }

    public static void line(float x, float y, float right, float bottom, float width, int color) {
        color = master(color);
        if ((color >>> 24) == 0 || outside(Math.min(x, right) - width, Math.min(y, bottom) - width, Math.max(x, right) + width, Math.max(y, bottom) + width)) return;
        if (SdfRenderer.active()) { SdfRenderer.line(x, y, right, bottom, width, color); return; }
        begin(); GL11.glEnable(GL11.GL_LINE_SMOOTH); GL11.glLineWidth(Math.max(1F, width * GuiRenderContext.uiScale())); glBegin(GL11.GL_LINES);
        vertex(x, y, color); vertex(right, bottom, color); GL11.glEnd(); GL11.glDisable(GL11.GL_LINE_SMOOTH); end();
    }

    /** SDF capsule strokes used by every check/chevron icon. */
    public static void check(float x, float y, float size, float width, int color) {
        line(x - size * .5F, y, x - size * .12F, y + size * .34F, width, color);
        line(x - size * .12F, y + size * .34F, x + size * .5F, y - size * .42F, width, color);
    }
    public static void chevron(float x, float y, float size, boolean down, float width, int color) {
        float sign = down ? 1F : -1F;
        line(x - size * .5F, y - sign * size * .22F, x, y + sign * size * .28F, width, color);
        line(x, y + sign * size * .28F, x + size * .5F, y - sign * size * .22F, width, color);
    }

    public static void star(float x, float y, float outer, float inner, int color, boolean filled) {
        color = master(color);
        if ((color >>> 24) == 0 || outside(x - outer, y - outer, x + outer, y + outer)) return;
        if (SdfRenderer.active()) { SdfRenderer.star(x, y, outer, inner, color, filled); return; }
        begin();
        if (filled) {
            glBegin(GL11.GL_TRIANGLE_FAN); vertex(x, y, color);
            for (int i = 0; i <= 10; i++) starVertex(x, y, outer, inner, i % 10, color);
            GL11.glEnd();
        } else {
            GL11.glEnable(GL11.GL_LINE_SMOOTH); GL11.glLineWidth(Math.max(1F, 1.35F * GuiRenderContext.uiScale())); glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < 10; i++) starVertex(x, y, outer, inner, i, color);
            GL11.glEnd(); GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
        end();
    }

    /** One SDF quad: vertical glass fill, inner border and GPU soft shadow/glow. */
    public static void glassRounded(float x, float y, float right, float bottom, float radius, int topColor, int bottomColor,
                                    int borderColor, float borderWidth, int shadowColor, float shadowBlur) {
        topColor = master(topColor); bottomColor = master(bottomColor); borderColor = master(borderColor); shadowColor = master(shadowColor);
        if (((topColor | bottomColor | borderColor | shadowColor) >>> 24) == 0 || outside(x - shadowBlur, y - shadowBlur, right + shadowBlur, bottom + shadowBlur)) return;
        if (SdfRenderer.active()) { SdfRenderer.rounded(x, y, right, bottom, radius, topColor, bottomColor, borderColor, borderWidth, shadowColor, shadowBlur, false); return; }
        shadow(x, y, right, bottom, radius, shadowColor, Math.max(1, Math.round(shadowBlur * .25F)));
        verticalGradient(x, y, right, bottom, topColor, bottomColor);
        if ((borderColor >>> 24) != 0) outline(x, y, right, bottom, radius, borderColor, borderWidth);
    }

    public static void circleGradient(float x, float y, float radius, int topColor, int bottomColor, int borderColor, float borderWidth, int shadowColor, float shadowBlur) {
        topColor = master(topColor); bottomColor = master(bottomColor); borderColor = master(borderColor); shadowColor = master(shadowColor);
        if (SdfRenderer.active()) { SdfRenderer.circle(x, y, radius, topColor, bottomColor, borderColor, borderWidth, shadowColor, shadowBlur); return; }
        if ((shadowColor >>> 24) != 0) shadow(x - radius, y - radius, x + radius, y + radius, radius, shadowColor, Math.max(1, Math.round(shadowBlur * .25F)));
        circle(x, y, radius, topColor); if ((borderColor >>> 24) != 0) outline(x - radius, y - radius, x + radius, y + radius, radius, borderColor, borderWidth);
    }

    /** Soft radial light with no stacked translucent circles. */
    public static void glow(float x, float y, float radius, int color) {
        color = master(color);
        if ((color >>> 24) == 0 || outside(x - radius, y - radius, x + radius, y + radius)) return;
        if (SdfRenderer.active()) { SdfRenderer.glow(x, y, radius, color); return; }
        circle(x, y, radius, alpha(color, .18F));
    }

    public static void text(String text, float x, float y, int color) { draw(BODY, text, x, y, color, 0F, false); }
    public static void animatedText(String text, float x, float y, int color) { draw(BODY, text, x, y, color, 0F, true); }
    public static void small(String text, float x, float y, int color) { draw(SMALL, text, x, y, color, 0F, false); }
    public static void animatedSmall(String text, float x, float y, int color) { draw(SMALL, text, x, y, color, 0F, true); }
    public static void medium(String text, float x, float y, int color) { draw(MEDIUM, text, x, y, color, 0F, false); }
    public static void buttonText(String text, float x, float y, int color) { draw(BUTTON_FONT, text, x, y, color, 0F, false); }
    public static float buttonWidth(String text) { return BUTTON_FONT.width(text); }
    public static float buttonHeight() { return BUTTON_FONT.height(); }
    public static void bold(String text, float x, float y, int color) { draw(SEMIBOLD, text, x, y, color, 0F, false); }
    public static void smallBold(String text, float x, float y, int color) { draw(SMALL_BOLD, text, x, y, color, 0F, false); }
    public static void logo(String text, float x, float y, int color) { draw(LOGO, text, x, y, color, 0F, false); }
    public static void title(String text, float x, float y, int color) { draw(TITLE, text, x, y, color, 1F, false); }
    public static void debug30(String text, float x, float y, int color) { draw(DEBUG_30, text, x, y, color, 0F, false); }
    public static float textWidth(String text) { return BODY.width(text); }
    public static float smallWidth(String text) { return SMALL.width(text); }
    public static float mediumWidth(String text) { return MEDIUM.width(text); }
    public static float boldWidth(String text) { return SEMIBOLD.width(text); }
    public static float smallBoldWidth(String text) { return SMALL_BOLD.width(text); }
    public static float logoWidth(String text) { return LOGO.width(text); }
    public static float titleWidth(String text) { return TITLE.widthSpaced(text, 1F); }
    public static float textHeight() { return BODY.height(); }
    public static float smallHeight() { return SMALL.height(); }
    public static float mediumHeight() { return MEDIUM.height(); }
    public static float mediumBaseline() { return MEDIUM.baseline(); }
    public static float boldHeight() { return SEMIBOLD.height(); }
    public static float smallBoldHeight() { return SMALL_BOLD.height(); }
    public static float logoHeight() { return LOGO.height(); }

    private static void draw(GuiFont font, String text, float x, float y, int color, float spacing, boolean animated) {
        if (text == null || text.length() == 0 || (color >>> 24) == 0) return;
        // Card/container bounds are culled before their text. Avoid measuring every string every frame merely to cull it.
        if (outside(x, y, x + 1F, y + font.height())) return;
        font.draw(text, x, y, color, spacing, animated);
    }

    /** Called by GuiFont; it performs no GL state readbacks. */
    static void drawText(GuiFont font, String text, float x, float y, int color, float spacing) {
        if (!textSession) beginText();
        if (SdfRenderer.pending()) { flushTextBatch(); SdfRenderer.flush(); }
        if (!textBatch || activeFont != font || activeTexture != font.textureId()) {
            flushTextBatch();
            activeFont = font;
            activeTexture = font.textureId();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            font.bindAtlas();
            glBegin(GL11.GL_QUADS);
            textBatch = true;
        }
        ++textDraws;
        long started = System.nanoTime();
        font.emit(text, x, y, master(color), spacing);
        textNanos += System.nanoTime() - started;
    }

    public static boolean inside(float mx, float my, float x, float y, float right, float bottom) {
        return mx >= x && mx <= right && my >= y && my <= bottom;
    }

    /** True when the requested logical bounds are completely outside the active scissor. */
    public static boolean outside(float x, float y, float right, float bottom) {
        if (scissorDepth == 0) return false;
        int at = scissorDepth - 1;
        return right <= LX[at] || x >= LX[at] + LW[at] || bottom <= LY[at] || y >= LY[at] + LH[at];
    }

    public static void pushScissor(float x, float y, float width, float height) {
        if (scissorDepth >= STACK) return;
        flushTextBatch(); SdfRenderer.flush();
        LX[scissorDepth] = x; LY[scissorDepth] = y; LW[scissorDepth] = width; LH[scissorDepth] = height;
        float left = transformX + (x - transformX) * transformScale;
        float top = transformY + (y - transformY) * transformScale;
        float right = transformX + (x + width - transformX) * transformScale;
        float bottom = transformY + (y + height - transformY) * transformScale;
        float scale = GuiRenderContext.uiScale();
        int sx = Math.round(Math.min(left, right) * scale);
        int sy = Math.round(GuiRenderContext.framebufferHeight() - Math.max(top, bottom) * scale);
        int sw = Math.max(0, Math.round(Math.abs(right - left) * scale));
        int sh = Math.max(0, Math.round(Math.abs(bottom - top) * scale));
        if (scissorDepth > 0) {
            int ix = Math.max(sx, SX[scissorDepth - 1]), iy = Math.max(sy, SY[scissorDepth - 1]);
            int ir = Math.min(sx + sw, SX[scissorDepth - 1] + SW[scissorDepth - 1]);
            int ib = Math.min(sy + sh, SY[scissorDepth - 1] + SH[scissorDepth - 1]);
            sx = ix; sy = iy; sw = Math.max(0, ir - ix); sh = Math.max(0, ib - iy);
        }
        SX[scissorDepth] = sx; SY[scissorDepth] = sy; SW[scissorDepth] = sw; SH[scissorDepth] = sh;
        ++scissorDepth;
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(sx, sy, sw, sh);
    }

    public static void popScissor() {
        if (scissorDepth == 0) return;
        flushTextBatch(); SdfRenderer.flush();
        --scissorDepth;
        if (scissorDepth == 0) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        else GL11.glScissor(SX[scissorDepth - 1], SY[scissorDepth - 1], SW[scissorDepth - 1], SH[scissorDepth - 1]);
    }

    public static void clearScissor() { while (scissorDepth > 0) popScissor(); }

    private static void begin() {
        flushTextBatch();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableTexture2D(); GlStateManager.disableDepth(); GlStateManager.disableCull();
        GL11.glEnable(GL13.GL_MULTISAMPLE);
    }

    private static void end() { GlStateManager.enableTexture2D(); GlStateManager.enableDepth(); GlStateManager.enableCull(); GlStateManager.resetColor(); }

    private static void flushTextBatch() {
        if (!textBatch) return;
        GL11.glEnd();
        textBatch = false;
    }

    private static void glBegin(int mode) { ++glBegins; GL11.glBegin(mode); }
    private static float feather() { return GuiRenderContext.inverseUiScale(); }
    private static float clamp(float value) { return Math.max(0F, Math.min(1F, value)); }
    private static int master(int color) { return masterAlpha >= .9999F ? color : alpha(color, masterAlpha); }

    private static void roundedSolid(float x, float y, float right, float bottom, float radius, int color) {
        float r = radius(x, y, right, bottom, radius);
        glBegin(GL11.GL_TRIANGLE_FAN); vertex((x + right) * .5F, (y + bottom) * .5F, color);
        for (int corner = 0; corner < 4; corner++) for (int point = 0; point < ROUND_POINTS; point++)
            shapeVertex(x, y, right, bottom, r, corner, point, color);
        shapeVertex(x, y, right, bottom, r, 0, 0, color);
        GL11.glEnd();
    }

    private static void ring(float outerX, float outerY, float outerRight, float outerBottom, float outerRadius, int outerColor,
                             float innerX, float innerY, float innerRight, float innerBottom, float innerRadius, int innerColor) {
        float outerR = radius(outerX, outerY, outerRight, outerBottom, outerRadius);
        float innerR = radius(innerX, innerY, innerRight, innerBottom, innerRadius);
        glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int corner = 0; corner < 4; corner++) for (int point = 0; point < ROUND_POINTS; point++) {
            shapeVertex(outerX, outerY, outerRight, outerBottom, outerR, corner, point, outerColor);
            shapeVertex(innerX, innerY, innerRight, innerBottom, innerR, corner, point, innerColor);
        }
        shapeVertex(outerX, outerY, outerRight, outerBottom, outerR, 0, 0, outerColor);
        shapeVertex(innerX, innerY, innerRight, innerBottom, innerR, 0, 0, innerColor);
        GL11.glEnd();
    }

    private static float radius(float x, float y, float right, float bottom, float value) {
        return Math.max(0F, Math.min(value, Math.min((right - x) * .5F, (bottom - y) * .5F)));
    }

    private static void shapeVertex(float x, float y, float right, float bottom, float radius, int corner, int point, int color) {
        float cx = corner < 2 ? right - radius : x + radius;
        float cy = corner == 0 || corner == 3 ? y + radius : bottom - radius;
        vertex(cx + ROUND_COS[corner][point] * radius, cy + ROUND_SIN[corner][point] * radius, color);
    }

    private static void starVertex(float x, float y, float outer, float inner, int point, int color) {
        float radius = (point & 1) == 0 ? outer : inner;
        vertex(x + STAR_COS[point] * radius, y + STAR_SIN[point] * radius, color);
    }

    private static void vertex(float x, float y, int color) {
        GL11.glColor4f((color >>> 16 & 255) / 255F, (color >>> 8 & 255) / 255F, (color & 255) / 255F, (color >>> 24 & 255) / 255F);
        GL11.glVertex2f(x, y);
    }
}
