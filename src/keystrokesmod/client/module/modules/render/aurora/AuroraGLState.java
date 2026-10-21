package keystrokesmod.client.module.modules.render.aurora;

import org.lwjgl.opengl.*;
import net.minecraft.client.renderer.GlStateManager;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

/** Exact raw-GL isolation plus explicit reconciliation of Minecraft's state cache. */
public final class AuroraGLState {
    private int matrix, active, program, array, texture0, texture1;
    private final FloatBuffer color=BufferUtils.createFloatBuffer(16);
    public void begin() {
        matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        array = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);texture0=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);texture1=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(active);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPushMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE1); GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0); GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND); GL11.glDepthMask(false);
    }
    public void end() {
        GL20.glUseProgram(program);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPopMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
        GL11.glPopClientAttrib(); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, array);
        GL11.glPopAttrib();
        // glPopAttrib does not update GlStateManager's cached texture/enable/color values.
        // Force the real value first, then mirror it through the cache, even if the cache
        // would otherwise skip its GL call. This also repairs a desync from a driver/mod upload.
        syncUnit(GL13.GL_TEXTURE0,texture0);syncUnit(GL13.GL_TEXTURE1,texture1);
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND), lighting=GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean fog=GL11.glIsEnabled(GL11.GL_FOG),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if(blend)GlStateManager.enableBlend();else GlStateManager.disableBlend();
        if(lighting)GlStateManager.enableLighting();else GlStateManager.disableLighting();
        if(depth)GlStateManager.enableDepth();else GlStateManager.disableDepth();
        if(alpha)GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
        if(fog)GlStateManager.enableFog();else GlStateManager.disableFog();
        if(cull)GlStateManager.enableCull();else GlStateManager.disableCull();
        GlStateManager.depthMask(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        GlStateManager.depthFunc(GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        GlStateManager.shadeModel(GL11.glGetInteger(GL11.GL_SHADE_MODEL));
        GlStateManager.alphaFunc(GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF));
        if(GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL))GlStateManager.enablePolygonOffset();else GlStateManager.disablePolygonOffset();
        GlStateManager.doPolygonOffset(GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR),GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS));
        GlStateManager.tryBlendFuncSeparate(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA));
        color.clear();GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);
        GlStateManager.color(color.get(0),color.get(1),color.get(2),color.get(3));
        GL13.glActiveTexture(active);GlStateManager.setActiveTexture(active);GL11.glMatrixMode(matrix);
    }
    private static void syncUnit(int unit,int texture) {
        GL13.glActiveTexture(unit);GlStateManager.setActiveTexture(unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GlStateManager.bindTexture(texture);
        if(GL11.glIsEnabled(GL11.GL_TEXTURE_2D))GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
    }
    public static int boundTexture0() {
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);int texture=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(active);return texture;
    }
    /** Handler boundary safety net; never assumes the atlas/font/icons texture. */
    public static void restoreDefaultState(int texture) {
        syncUnit(GL13.GL_TEXTURE0,texture);
        GL11.glEnable(GL11.GL_TEXTURE_2D);GlStateManager.enableTexture2D();
        GL11.glColor4f(1,1,1,1);GlStateManager.color(1,1,1,1);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
