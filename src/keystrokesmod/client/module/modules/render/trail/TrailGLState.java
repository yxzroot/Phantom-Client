package keystrokesmod.client.module.modules.render.trail;

import org.lwjgl.opengl.*;
import net.minecraft.client.renderer.GlStateManager;

/** Raw GL isolation plus Minecraft-cache reconciliation; no GL calls at construction. */
public final class TrailGLState {
    private int unit,mode,program,array,texture0,texture1;
    public void begin(){
        unit=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);array=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);texture0=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);texture1=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);GL13.glActiveTexture(unit);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPushMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    private static void sync(int unit,int texture){
        GL13.glActiveTexture(unit);GlStateManager.setActiveTexture(unit);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GlStateManager.bindTexture(texture);
        if(GL11.glIsEnabled(GL11.GL_TEXTURE_2D))GlStateManager.enableTexture2D();else GlStateManager.disableTexture2D();
    }
    public void end(){
        GL20.glUseProgram(program);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPopMatrix();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
        GL11.glPopClientAttrib();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,array);GL11.glPopAttrib();
        sync(GL13.GL_TEXTURE0,texture0);sync(GL13.GL_TEXTURE1,texture1);
        if(GL11.glIsEnabled(GL11.GL_BLEND))GlStateManager.enableBlend();else GlStateManager.disableBlend();
        if(GL11.glIsEnabled(GL11.GL_LIGHTING))GlStateManager.enableLighting();else GlStateManager.disableLighting();
        if(GL11.glIsEnabled(GL11.GL_DEPTH_TEST))GlStateManager.enableDepth();else GlStateManager.disableDepth();
        if(GL11.glIsEnabled(GL11.GL_ALPHA_TEST))GlStateManager.enableAlpha();else GlStateManager.disableAlpha();
        if(GL11.glIsEnabled(GL11.GL_FOG))GlStateManager.enableFog();else GlStateManager.disableFog();
        if(GL11.glIsEnabled(GL11.GL_CULL_FACE))GlStateManager.enableCull();else GlStateManager.disableCull();
        GlStateManager.depthMask(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));GlStateManager.depthFunc(GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        GlStateManager.alphaFunc(GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC),GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF));
        GlStateManager.shadeModel(GL11.glGetInteger(GL11.GL_SHADE_MODEL));
        GlStateManager.tryBlendFuncSeparate(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA));
        GL13.glActiveTexture(unit);GlStateManager.setActiveTexture(unit);GL11.glMatrixMode(mode);
    }
    /** World-render boundary contract: keep original bindings/flags, but leave safe HUD defaults. */
    public void restoreDefaultState(){
        sync(GL13.GL_TEXTURE0,texture0);GL11.glEnable(GL11.GL_TEXTURE_2D);GlStateManager.enableTexture2D();
        GL11.glColor4f(1,1,1,1);GlStateManager.color(1,1,1,1);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
        GL13.glActiveTexture(unit);GlStateManager.setActiveTexture(unit);GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
