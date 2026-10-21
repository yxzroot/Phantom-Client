package keystrokesmod.client.module.modules.render.swordglow;

import net.minecraft.client.resources.model.IBakedModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/** Vanilla's two glint texture passes, using RenderItem's actual model draw rather than copied vertices. */
public final class NativeSwordGlint {
    public interface ModelDraw { void swordglow$drawNativeModel(IBakedModel model, int tint); }

    public void draw(SwordAnimator.Frame frame, GlintPalette.Palette palette,
                     IBakedModel model, int texture, ModelDraw renderer) {
        if (texture == 0 || frame.brightness <= .0001F || model == null || model.isBuiltInRenderer()) return;
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        try {
            GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            // Increase band energy without increasing geometry or filling the sword with a flat tint.
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_MODULATE);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL11.GL_TEXTURE);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_RGB, GL13.GL_PRIMARY_COLOR);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_RGB, GL11.GL_SRC_COLOR);
            GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL13.GL_RGB_SCALE, 2);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_EQUAL); GL11.glDepthMask(false);
            for (int pass = 0; pass < 2; pass++) {
                GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPushMatrix();
                try {
                    GL11.glLoadIdentity();
                    GL11.glScalef(frame.textureScale, frame.textureScale, frame.textureScale);
                    GL11.glTranslatef(frame.scroll[pass], 0, 0);
                    GL11.glRotatef(frame.angle[pass], 0, 0, 1);
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    renderer.swordglow$drawNativeModel(model, GlintPalette.tint(frame, palette, pass));
                } finally { GL11.glMatrixMode(GL11.GL_TEXTURE); GL11.glPopMatrix(); }
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
            GL11.glPopClientAttrib(); GL11.glPopAttrib();
            GL13.glActiveTexture(unit); GL11.glMatrixMode(mode);
        }
    }
}
