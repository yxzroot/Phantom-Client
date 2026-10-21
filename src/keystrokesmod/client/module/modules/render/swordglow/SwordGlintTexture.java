package keystrokesmod.client.module.modules.render.swordglow;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/** Keeps the active pack's glint pattern but removes its baked purple so all tint modes work. */
public final class SwordGlintTexture implements IResourceManagerReloadListener {
    private static final ResourceLocation SOURCE = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    private DynamicTexture texture;
    private IResourceManager resources;
    private boolean registered, dirty = true, failed;

    public int id() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!registered) {
            registered = true; resources = mc.getResourceManager();
            if (resources instanceof IReloadableResourceManager)
                ((IReloadableResourceManager) resources).registerReloadListener(this);
        }
        if (failed) return 0;
        if (!dirty && texture != null) return texture.getGlTextureId();
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try (InputStream input = resources.getResource(SOURCE).getInputStream()) {
            BufferedImage source = ImageIO.read(input);
            if (source == null || source.getWidth() > 4096 || source.getHeight() > 4096)
                throw new IllegalStateException("Invalid glint image");
            BufferedImage gray = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++)
                gray.setRGB(x, y, neutralPixel(source.getRGB(x, y)));
            if (texture != null) texture.deleteGlTexture();
            texture = new DynamicTexture(gray);
            GlStateManager.bindTexture(texture.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            dirty = false;
            return texture.getGlTextureId();
        } catch (Exception | LinkageError error) {
            failed = true;
            java.util.logging.Logger.getLogger("Phantom/SwordGlow").log(java.util.logging.Level.WARNING,
                    "Sword glint texture unavailable until resources reload", error);
            return 0;
        } finally {
            GlStateManager.bindTexture(binding); GlStateManager.setActiveTexture(unit);
        }
    }

    public static int neutralPixel(int argb) {
        int value = Math.max(argb >> 16 & 255, Math.max(argb >> 8 & 255, argb & 255));
        value = value * (argb >>> 24) / 255;
        return 0xFF000000 | value << 16 | value << 8 | value;
    }

    @Override public void onResourceManagerReload(IResourceManager manager) {
        resources = manager; dirty = true; failed = false;
    }

    /** SRG interface bridge: this patch build remaps references but not arbitrary interface overrides. */
    public void func_110549_a(IResourceManager manager) { onResourceManagerReload(manager); }
}
