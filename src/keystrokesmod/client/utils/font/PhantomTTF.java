package keystrokesmod.client.utils.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.BufferUtils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.FloatBuffer;

/** GUI-scale-aware Poppins glyph atlas; usable by other GUI components. Render only on the client thread. */
public final class PhantomTTF {
    private static final int FIRST = 32, LAST = 126;
    private static final FloatBuffer PREVIOUS_COLOR = BufferUtils.createFloatBuffer(16);
    private static Font poppins;
    private final int style;
    private final float size;
    private final Glyph[] glyphs = new Glyph[LAST + 1];
    private DynamicTexture texture;
    private int scale = -1, displayWidth, displayHeight, guiSetting, atlasSize, textureId;
    private boolean mipmaps;

    private static final class Glyph {
        int x, y, w, h;
        float advance;
    }

    public PhantomTTF(float size, int style) { this.size = size; this.style = style; }

    private static Font font() {
        if (poppins == null) {
            try (InputStream in = PhantomTTF.class.getResourceAsStream("/assets/raindots/fonts/Poppins.ttf")) {
                if (in == null) throw new IllegalStateException("Bundled Poppins.ttf is missing");
                poppins = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (Exception ex) { throw new IllegalStateException("Could not load Poppins TTF", ex); }
        }
        return poppins;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void ensure() {
        Minecraft mc = Minecraft.getMinecraft();
        if (texture != null && guiSetting == mc.gameSettings.guiScale
                && displayWidth == mc.displayWidth && displayHeight == mc.displayHeight) return;
        PhantomDiagnostics.log("ATLAS rebuilding: size=" + size + ", style=" + style
                + ", guiSetting=" + mc.gameSettings.guiScale + ", display=" + mc.displayWidth + "x" + mc.displayHeight);
        int oldBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        if (texture != null) texture.deleteGlTexture();
        guiSetting = mc.gameSettings.guiScale;
        displayWidth = mc.displayWidth; displayHeight = mc.displayHeight;
        scale = new ScaledResolution(mc).getScaleFactor();
        final int raster = scale * 3, padding = Math.max(2, raster * 2);
        Font derived = font().deriveFont(style, size * raster);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = probe.createGraphics();
        hints(measure); measure.setFont(derived);
        FontMetrics metrics = measure.getFontMetrics();
        int line = metrics.getHeight() + 2 * padding;
        atlasSize = 512;
        while (true) {
            int x = 0, y = 0;
            boolean fits = true;
            for (int c = FIRST; c <= LAST; c++) {
                Glyph glyph = glyphs[c];
                if (glyph == null) glyphs[c] = glyph = new Glyph();
                glyph.advance = (float) derived.getStringBounds(String.valueOf((char) c),
                        measure.getFontRenderContext()).getWidth() / raster;
                glyph.w = Math.max(1, (int) Math.ceil(glyph.advance * raster)) + 2 * padding + 2;
                glyph.h = line;
                if (x + glyph.w > atlasSize) { x = 0; y += line; }
                if (y + line > atlasSize) { fits = false; break; }
                glyph.x = x; glyph.y = y; x += glyph.w;
            }
        if (fits) break;
            atlasSize *= 2;
            if (atlasSize > 4096) { measure.dispose(); throw new IllegalStateException("Poppins atlas too large"); }
        }
        BufferedImage image = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        hints(g); g.setFont(derived);
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, atlasSize, atlasSize);
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Color.WHITE);
        for (int c = FIRST; c <= LAST; c++) {
            Glyph glyph = glyphs[c];
            g.drawString(String.valueOf((char) c), glyph.x + padding,
                    glyph.y + padding + metrics.getAscent());
        }
        g.dispose(); measure.dispose();
        texture = new DynamicTexture(image);
        textureId = texture.getGlTextureId();
        // Keep OpenGL's real binding AND Minecraft's cached binding in sync. The old HUD's
        // custom font path did not do this, so a later GUI bind could silently use another atlas.
        GlStateManager.bindTexture(textureId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        mipmaps = GLContext.getCapabilities().OpenGL30 || GLContext.getCapabilities().GL_EXT_framebuffer_object;
        if (GLContext.getCapabilities().OpenGL30) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        else if (GLContext.getCapabilities().GL_EXT_framebuffer_object)
            EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D);
        filter();
        GlStateManager.bindTexture(oldBinding);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldBinding);
        PhantomDiagnostics.log("ATLAS ready: size=" + size + ", scale=" + scale + ", atlas=" + atlasSize
                + ", texture=" + textureId + ", mipmaps=" + mipmaps);
    }

    private void filter() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                mipmaps ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    public float width(String text) {
        ensure();
        float width = 0;
        if (text != null) for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= FIRST && c <= LAST) width += glyphs[c].advance;
        }
        return width;
    }

    /** colors may be null (solid color), or a cached ARGB color for each character. */
    public void draw(String text, float x, float y, int color, int[] colors) {
        if (text == null || text.isEmpty()) return;
        ensure();
        PhantomDiagnostics.once("ttf-draw-" + size + "-" + style,
                "FONT draw path active: size=" + size + ", style=" + style + ", texture=" + textureId);
        int oldBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean oldTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        PREVIOUS_COLOR.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, PREVIOUS_COLOR);
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();
            // The GUI projection flips Y; a retained world cull state can discard text quads.
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.bindTexture(textureId);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            filter();
            pass(text, x + .75F, y + .75F,
                    ((int) (((color >>> 24) & 255) * .40F) << 24), null);
            pass(text, x, y, color, colors);
        } finally {
            GlStateManager.bindTexture(oldBinding);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldBinding);
            GlStateManager.tryBlendFuncSeparate(src, dst, srcA, dstA);
            GlStateManager.depthMask(oldDepthMask);
            if (oldDepth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (oldCull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (oldBlend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (oldTexture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            GlStateManager.color(PREVIOUS_COLOR.get(0), PREVIOUS_COLOR.get(1),
                    PREVIOUS_COLOR.get(2), PREVIOUS_COLOR.get(3));
        }
    }

    private void pass(String text, float x, float y, int color, int[] colors) {
        final int raster = scale * 3;
        final float pad = Math.max(2, raster * 2) / (float) raster;
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST || c > LAST) continue;
            Glyph glyph = glyphs[c];
            int argb = colors == null ? color : colors[i];
            float alpha = ((argb >>> 24) & 255) / 255F;
            if (colors != null) alpha *= ((color >>> 24) & 255) / 255F;
            GL11.glColor4f(((argb >>> 16) & 255) / 255F, ((argb >>> 8) & 255) / 255F,
                    (argb & 255) / 255F, alpha);
            float x0 = x - pad, y0 = y - pad;
            float x1 = x0 + glyph.w / (float) raster, y1 = y0 + glyph.h / (float) raster;
            float u0 = glyph.x / (float) atlasSize, v0 = glyph.y / (float) atlasSize;
            float u1 = (glyph.x + glyph.w) / (float) atlasSize;
            float v1 = (glyph.y + glyph.h) / (float) atlasSize;
            GL11.glTexCoord2f(u0, v0); GL11.glVertex2f(x0, y0);
            GL11.glTexCoord2f(u1, v0); GL11.glVertex2f(x1, y0);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x1, y1);
            GL11.glTexCoord2f(u0, v1); GL11.glVertex2f(x0, y1);
            x += glyph.advance;
        }
        GL11.glEnd();
    }
}
