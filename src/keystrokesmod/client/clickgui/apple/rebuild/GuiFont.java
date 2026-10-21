package keystrokesmod.client.clickgui.apple.rebuild;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;
import keystrokesmod.client.utils.font.PhantomDiagnostics;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

/** Native-pixel Poppins atlas for the rebuilt GUI. One cached atlas per token size and weight. */
public final class GuiFont {
    private static final int FIRST = 32, LAST = 126, SUPERSAMPLE = 2;
    private static Font poppins;
    private static int rebuildCount;
    private static boolean rebuiltThisFrame;
    private final float tokenPixels;
    private final int style;
    private final Glyph[] glyphs = new Glyph[LAST + 1];
    private DynamicTexture texture;
    private int atlasSize, textureId, builtWidth, builtHeight;
    private float builtScale = -1F, lineHeight, baseline;
    private boolean mipmaps;

    private static final class Glyph {
        int x, y, w, h;
        float advance;
    }

    public GuiFont(float tokenPixels, int style) { this.tokenPixels = tokenPixels; this.style = style; }

    public float width(String text) {
        ensure();
        float width = 0F;
        if (text != null) for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= FIRST && c <= LAST) width += glyphs[c].advance;
        }
        return width;
    }

    public float height() { ensure(); return lineHeight; }
    public float baseline() { ensure(); return baseline; }

    public void draw(String text, float x, float y, int color) { draw(text, x, y, color, 0F, false); }
    public void drawAnimated(String text, float x, float y, int color) { draw(text, x, y, color, 0F, true); }
    public void drawSpaced(String text, float x, float y, int color, float letterSpacing) { draw(text, x, y, color, letterSpacing, false); }

    public float widthSpaced(String text, float letterSpacing) {
        float result = width(text);
        return text == null || text.length() < 2 ? result : result + letterSpacing * (text.length() - 1);
    }

    void draw(String text, float x, float y, int color, float letterSpacing, boolean animated) {
        if (text == null || text.length() == 0 || (color >>> 24) == 0) return;
        ensure();
        if (!animated) {
            float scale = GuiRenderContext.uiScale();
            x = Math.round(x * scale) / scale;
            y = Math.round(y * scale) / scale;
        }
        UiDraw.drawText(this, text, x, y, color, letterSpacing);
    }

    int textureId() { return textureId; }
    void prepare() { ensure(); }
    static void beginFrame() { rebuiltThisFrame = false; }
    static int rebuildCount() { return rebuildCount; }
    static boolean rebuiltThisFrame() { return rebuiltThisFrame; }

    /** Called only when UiDraw switches to this atlas; filters are set when the atlas is rebuilt. */
    void bindAtlas() {
        GlStateManager.bindTexture(textureId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    /** Emits glyph quads into UiDraw's currently-open atlas batch. */
    void emit(String text, float x, float y, int color, float letterSpacing) {
        float alpha = (color >>> 24 & 255) / 255F;
        // The atlas stores premultiplied coverage; keep the source premultiplied through blending.
        GL11.glColor4f((color >>> 16 & 255) / 255F * alpha, (color >>> 8 & 255) / 255F * alpha, (color & 255) / 255F * alpha, alpha);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST || c > LAST) continue;
            Glyph glyph = glyphs[c];
            float x0 = x, y0 = y, x1 = x + glyph.w / (float) SUPERSAMPLE / builtScale, y1 = y + glyph.h / (float) SUPERSAMPLE / builtScale;
            float u0 = glyph.x / (float) atlasSize, v0 = glyph.y / (float) atlasSize;
            float u1 = (glyph.x + glyph.w) / (float) atlasSize, v1 = (glyph.y + glyph.h) / (float) atlasSize;
            GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(x0, y0);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(x1, y0);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x1, y1);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(x0, y1);
            x += glyph.advance + (i + 1 < text.length() ? letterSpacing : 0F);
        }
    }

    private void ensure() {
        float scale = GuiRenderContext.uiScale();
        int width = GuiRenderContext.framebufferWidth(), height = GuiRenderContext.framebufferHeight();
        if (texture != null && builtScale == scale && builtWidth == width && builtHeight == height) return;
        if (texture != null) texture.deleteGlTexture();
        builtScale = scale; builtWidth = width; builtHeight = height;
        ++rebuildCount; rebuiltThisFrame = true;
        PhantomDiagnostics.log("GUI font atlas rebuild #" + rebuildCount + " (" + tokenPixels + "px, scale=" + scale + ", framebuffer=" + width + "x" + height + ")");
        float pixels = tokenPixels * scale;
        Font derived = font().deriveFont(style, pixels * SUPERSAMPLE);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = probe.createGraphics(); hints(measure); measure.setFont(derived);
        FontMetrics metrics = measure.getFontMetrics();
        int padding = 2 * SUPERSAMPLE;
        int line = metrics.getHeight() + padding * 2;
        atlasSize = 512;
        while (true) {
            int cursorX = 0, cursorY = 0; boolean fits = true;
            for (int c = FIRST; c <= LAST; c++) {
                Glyph glyph = glyphs[c]; if (glyph == null) glyphs[c] = glyph = new Glyph();
                glyph.advance = metrics.charWidth((char) c) / (float) SUPERSAMPLE / scale;
                glyph.w = Math.max(1, metrics.charWidth((char) c)) + padding * 2;
                glyph.h = line;
                if (cursorX + glyph.w > atlasSize) { cursorX = 0; cursorY += line; }
                if (cursorY + line > atlasSize) { fits = false; break; }
                glyph.x = cursorX; glyph.y = cursorY; cursorX += glyph.w;
            }
            if (fits) break;
            atlasSize *= 2;
            if (atlasSize > 4096) { measure.dispose(); throw new IllegalStateException("GUI font atlas exceeds 4096px"); }
        }
        lineHeight = metrics.getHeight() / (float) SUPERSAMPLE / scale;
        baseline = (metrics.getAscent() + padding) / (float) SUPERSAMPLE / scale;
        BufferedImage image = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics(); hints(graphics); graphics.setFont(derived);
        graphics.setComposite(AlphaComposite.Src); graphics.setColor(new Color(0, 0, 0, 0)); graphics.fillRect(0, 0, atlasSize, atlasSize);
        graphics.setComposite(AlphaComposite.SrcOver); graphics.setColor(Color.WHITE);
        for (int c = FIRST; c <= LAST; c++) {
            Glyph glyph = glyphs[c]; graphics.drawString(String.valueOf((char) c), glyph.x + padding, glyph.y + padding + metrics.getAscent());
        }
        graphics.dispose(); measure.dispose();
        // DynamicTexture uploads these raw ARGB values. Premultiplying once here keeps antialiased
        // glyph edges crisp with GL_ONE/GL_ONE_MINUS_SRC_ALPHA at render time.
        int[] pixelsData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixelsData.length; i++) {
            int value = pixelsData[i], alpha = value >>> 24;
            int red = ((value >>> 16 & 255) * alpha + 127) / 255;
            int green = ((value >>> 8 & 255) * alpha + 127) / 255;
            int blue = ((value & 255) * alpha + 127) / 255;
            pixelsData[i] = alpha << 24 | red << 16 | green << 8 | blue;
        }
        texture = new DynamicTexture(image); textureId = texture.getGlTextureId();
        GlStateManager.bindTexture(textureId); GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        // Text is native-pixel and linearly filtered. Avoid an expensive mipmap upload on resize.
        mipmaps = false;
        filter();
    }

    private static Font font() {
        if (poppins != null) return poppins;
        try {
            InputStream stream = GuiFont.class.getResourceAsStream("/assets/raindots/fonts/Poppins.ttf");
            if (stream == null) throw new IllegalStateException("Bundled Poppins.ttf is missing");
            try { poppins = Font.createFont(Font.TRUETYPE_FONT, stream); } finally { stream.close(); }
            return poppins;
        } catch (Exception exception) { throw new IllegalStateException("Could not load Poppins GUI font", exception); }
    }

    private static void hints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void filter() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmaps ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }
}
