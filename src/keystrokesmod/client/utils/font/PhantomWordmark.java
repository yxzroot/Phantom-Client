package keystrokesmod.client.utils.font;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

/** Shared, lazily uploaded wordmark. A missing image warns once and falls back to text. */
public final class PhantomWordmark {
    private static final String RESOURCE = "/assets/raindots/phantom-wordmark.png";
    public static final float ASPECT = 2400F / 348F;
    private static int texture;
    private static boolean attempted;

    private PhantomWordmark() { }

    public static boolean available() {
        if (!attempted) load();
        return texture != 0;
    }

    public static float width(float height) { return height * ASPECT; }

    public static boolean draw(float x, float y, float height, float alpha) {
        if (alpha <= 0F) return available();
        if (!available()) return false;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glColor4f(1F, 1F, 1F, Math.max(0F, Math.min(1F, alpha)));
            float right = x + width(height), bottom = y + height;
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0F, 0F); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1F, 0F); GL11.glVertex2f(right, y);
            GL11.glTexCoord2f(1F, 1F); GL11.glVertex2f(right, bottom);
            GL11.glTexCoord2f(0F, 1F); GL11.glVertex2f(x, bottom);
            GL11.glEnd();
        } finally { GL11.glPopAttrib(); }
        return true;
    }

    private static void load() {
        attempted = true;
        int id = 0;
        try (InputStream input = PhantomWordmark.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("resource missing: " + RESOURCE);
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IllegalStateException("invalid PNG: " + RESOURCE);
            int w = image.getWidth(), h = image.getHeight();
            ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                pixels.put((byte) (argb >>> 16)).put((byte) (argb >>> 8)).put((byte) argb).put((byte) (argb >>> 24));
            }
            pixels.flip();
            GL11.glPushAttrib(GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT);
            try {
                id = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
                if (!GLContext.getCapabilities().OpenGL30) GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
                if (GLContext.getCapabilities().OpenGL30) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                texture = id;
            } finally { GL11.glPopAttrib(); }
        } catch (Throwable error) {
            if (id != 0) try { GL11.glDeleteTextures(id); } catch (Throwable ignored) { }
            System.err.println("Phantom wordmark unavailable; using PHANTOM text: " + error);
        }
    }
}
