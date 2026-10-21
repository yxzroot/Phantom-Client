package keystrokesmod.client.module.modules.render.aurora;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL12;
import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;

/** World-pass-only lightmap multiplier. Original pixels are restored BEFORE the held hand. */
public final class AuroraLightmap {
    private final int[] original=new int[256];
    private boolean applied;
    private final IntBuffer uploadPixels=BufferUtils.createIntBuffer(256);
    public boolean restore(int[] pixels) {
        if(!applied) return false;
        System.arraycopy(original,0,pixels,0,Math.min(256,pixels.length)); applied=false; return true;
    }
    public boolean apply(int[] pixels,float cold,float reflection,int color) {
        restore(pixels);
        cold=AuroraSimulation.clamp(cold,0,1); reflection=AuroraSimulation.clamp(reflection,0,.2F);
        if(cold<=0 && reflection<=0) return false;
        int length=Math.min(256,pixels.length); System.arraycopy(pixels,0,original,0,length);
        for(int i=0;i<length;i++) {
            int pixel=pixels[i], r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
            float luminance=r*.2126F+g*.7152F+b*.0722F,desaturate=cold*.45F;
            r=Math.round((r*(1-desaturate)+luminance*desaturate)*(1-cold*.18F)+reflection*(color>>16&255)*.18F);
            g=Math.round((g*(1-desaturate)+luminance*desaturate)*(1-cold*.32F)+reflection*(color>>8&255)*.18F);
            b=Math.round((b*(1-desaturate)+luminance*desaturate)*(1-cold*.06F)+cold*6+reflection*(color&255)*.18F);
            pixels[i]=(pixel&0xFF000000)|Math.min(255,r)<<16|Math.min(255,g)<<8|Math.min(255,b);
        }
        applied=true;return true;
    }
    public boolean apply(int[] pixels,float cold,float reflection,int color,float dusk,int tint,float darkness){
        apply(pixels,cold,reflection,color);dusk=AuroraSimulation.clamp(dusk,0,1);
        if(dusk<=0)return applied;
        int length=Math.min(256,pixels.length);if(!applied)System.arraycopy(pixels,0,original,0,length);
        float dim=1-AuroraSimulation.clamp(darkness,0,1)*.28F;
        for(int i=0;i<length;i++){
            int p=pixels[i],r=p>>16&255,g=p>>8&255,b=p&255;
            float gray=r*.2126F+g*.7152F+b*.0722F,desaturate=dusk*.65F;
            r=Math.round((r*(1-desaturate)+gray*desaturate)*((1-dusk)+dusk*(.36F+.24F*(tint>>16&255)/255F)*dim));
            g=Math.round((g*(1-desaturate)+gray*desaturate)*((1-dusk)+dusk*(.29F+.24F*(tint>>8&255)/255F)*dim));
            b=Math.round((b*(1-desaturate)+gray*desaturate)*((1-dusk)+dusk*(.72F+.20F*(tint&255)/255F)*dim));
            pixels[i]=(p&0xFF000000)|r<<16|g<<8|b;
        }
        applied=true;return true;
    }
    public boolean apply(int[] pixels,float cold,float reflection,int color,float dusk,int tint,float darkness,float art,int artColor){
        apply(pixels,cold,reflection,color,dusk,tint,darkness);art=AuroraSimulation.clamp(art,0,1);if(art<=0)return applied;
        int count=Math.min(256,pixels.length);if(!applied)System.arraycopy(pixels,0,original,0,count);
        for(int i=0;i<count;i++){int p=pixels[i];float r=p>>16&255,g=p>>8&255,b=p&255,gray=r*.2126F+g*.7152F+b*.0722F,desat=art*.5F;
            int rr=Math.round((r*(1-desat)+gray*desat)*(1-art+art*(.55F+.45F*(artColor>>16&255)/255F)));
            int gg=Math.round((g*(1-desat)+gray*desat)*(1-art+art*(.55F+.45F*(artColor>>8&255)/255F)));
            int bb=Math.round((b*(1-desat)+gray*desat)*(1-art+art*(.55F+.45F*(artColor&255)/255F)));
            pixels[i]=(p&0xFF000000)|rr<<16|gg<<8|bb;}
        applied=true;return true;
    }
    /** Upload only existing pixels: do not run TextureUtil/OptiFine's sampler/state-changing path. */
    public void upload(DynamicTexture texture) {
        uploadPixels(texture.getGlTextureId(),texture.getTextureData());
    }
    public void uploadPixels(int textureId,int[] pixels) {
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        int binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,textureId);
            GlStateManager.bindTexture(textureId);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);
            uploadPixels.clear();uploadPixels.put(pixels,0,256).flip();
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,16,16,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,uploadPixels);
        } finally {
            GL11.glPopClientAttrib();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D,binding);GlStateManager.bindTexture(binding);
            GL13.glActiveTexture(active);GlStateManager.setActiveTexture(active);
        }
    }
}
