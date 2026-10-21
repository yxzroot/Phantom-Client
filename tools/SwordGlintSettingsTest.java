import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import keystrokesmod.client.module.modules.render.swordglow.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Exercises the production pass, including settings changes, depth clipping and GL isolation. */
public final class SwordGlintSettingsTest {
    private static final int SIZE = 96;
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static SwordAnimator.Frame frame(double seconds, float speed, float intensity, float scale, float angle) {
        SwordAnimator animator = new SwordAnimator();
        animator.update(0, 0, true, AnimationStyle.Classic, intensity, scale, angle);
        return animator.update(seconds * speed, .25F, true, AnimationStyle.Classic, intensity, scale, angle);
    }
    private static byte[] pixels() {
        ByteBuffer data = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        GL11.glReadPixels(0,0,SIZE,SIZE,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);
        byte[] out = new byte[data.capacity()]; data.get(out); return out;
    }
    private static float[] floats(int property) {
        FloatBuffer data = BufferUtils.createFloatBuffer(16); GL11.glGetFloat(property,data);
        float[] result = new float[16]; data.get(result); return result;
    }
    private static final class Fixture implements IBakedModel, NativeSwordGlint.ModelDraw {
        int calls;
        final int[] colors = new int[2];
        public void swordglow$drawNativeModel(IBakedModel model, int tint) {
            check(model == this, "Renderer changed the original model");
            colors[calls++ % 2] = tint;
            geometry(tint);
        }
        void geometry(int tint) {
            GL11.glColor4f((tint >> 16 & 255)/255F,(tint >> 8 & 255)/255F,(tint & 255)/255F,1);
            GL11.glBegin(GL11.GL_QUADS);
            try { box(.46F,.1F,.54F,.3F);box(.3F,.3F,.7F,.38F);box(.43F,.38F,.57F,.9F); }
            finally { GL11.glEnd(); }
        }
        void box(float x,float y,float x2,float y2) {
            vertex(x,y);vertex(x2,y);vertex(x2,y2);vertex(x,y2);
        }
        void vertex(float x,float y) { GL11.glTexCoord2f(x,y);GL11.glVertex3f(x,y,0); }
        public List<BakedQuad> getGeneralQuads(){return Collections.emptyList();}
        public List<BakedQuad> getFaceQuads(EnumFacing face){return Collections.emptyList();}
        public boolean isAmbientOcclusion(){return false;}
        public boolean isGui3d(){return true;}
        public boolean isBuiltInRenderer(){return false;}
        public TextureAtlasSprite getParticleTexture(){return null;}
        public ItemCameraTransforms getItemCameraTransforms(){return null;}
    }
    private static int texture() {
        int id=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
        ByteBuffer data=BufferUtils.createByteBuffer(32*32*4);
        for(int y=0;y<32;y++)for(int x=0;x<32;x++) {
            int value=(x+y)%16<4?150:8;
            data.put((byte)value).put((byte)value).put((byte)value).put((byte)255);
        }
        data.flip();GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,32,32,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        return id;
    }
    private static byte[] draw(NativeSwordGlint renderer,Fixture model,SwordAnimator.Frame frame,GlintPalette.Palette palette,int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        model.geometry(0xFF151515);byte[] base=pixels();
        GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glColor4f(.2F,.3F,.4F,.5F);GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE,GL11.GL_ZERO);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,37);int atlas=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glTranslatef(.2F,.3F,0);
        float[] matrix=floats(GL11.GL_TEXTURE_MATRIX);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glBindTexture(GL11.GL_TEXTURE_2D,38);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        model.calls=0;renderer.draw(frame,palette,model,texture,model);
        check(GL11.glGetError()==0,"GL error");
        check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_PROJECTION,"Matrix mode leaked");
        check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1,"Texture unit leaked");
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==38,"Lightmap binding leaked");
        check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_DEPTH_FUNC)==GL11.GL_LEQUAL,"Depth state leaked");
        check(GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_ALPHA_TEST)&&GL11.glIsEnabled(GL11.GL_CULL_FACE),"Enable flags leaked");
        check(!GL11.glIsEnabled(GL11.GL_BLEND)&&GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)==GL11.GL_ONE,"Blend state leaked");
        float[] color=floats(GL11.GL_CURRENT_COLOR);check(color[0]==.2F&&color[3]==.5F,"Color leaked");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==atlas,"Atlas binding leaked");
        check(Arrays.equals(matrix,floats(GL11.GL_TEXTURE_MATRIX)),"Texture matrix leaked");
        byte[] after=pixels();
        for(int i=0;i<base.length;i+=4)if(base[i]==0&&base[i+1]==0&&base[i+2]==0)
            check(after[i]==0&&after[i+1]==0&&after[i+2]==0,"Shimmer escaped sword silhouette");
        check(frame.brightness==0?model.calls==0:model.calls==2,"Incorrect native model pass count");
        return after;
    }
    private static long energy(byte[] pixels) {
        long sum=0;for(int i=0;i<pixels.length;i+=4)sum+=(pixels[i]&255)+(pixels[i+1]&255)+(pixels[i+2]&255);return sum;
    }
    public static void main(String[] args)throws Exception {
        GlintPalette.Palette palette=new GlintPalette.Palette();palette.mode=GlintPalette.ColorMode.Rainbow;
        SwordAnimator.Frame red=frame(.5,1,1,8,-50),green=frame(2,1,1,8,-50);
        check(GlintPalette.tint(red,palette,0)!=GlintPalette.tint(green,palette,0),"Rainbow stayed one color");
        palette.mode=GlintPalette.ColorMode.Static;palette.start=0xFF00FF00;
        check((GlintPalette.tint(red,palette,0)&0xFFFFFF)==0x00FF00,"Static color was ignored");
        check(SwordGlintTexture.neutralPixel(0xFF8040CC)==0xFFCCCCCC,"Purple pack texture was not neutralized");
        check(SwordGlintTexture.neutralPixel(0x008040CC)==0xFF000000,"Transparent texture created a flat fill");
        check(Math.abs(frame(.8F,.1F,1,8,-50).scroll[0]-frame(.8F,3F,1,8,-50).scroll[0])>.05,"Speed slider did not move the band");
        check(frame(.8F,1,1,16,20).textureScale==16&&frame(.8F,1,1,16,20).angle[0]==20,"Scale/angle ignored");
        System.out.println("PASS: rainbow hue changes; static RGB exact; colored-pack neutralization; speed/scale/angle respond");
        if(args.length==0)return;
        Pbuffer context=new Pbuffer(SIZE,SIZE,new PixelFormat().withDepthBits(24),null);
        try {
            context.makeCurrent();GL11.glViewport(0,0,SIZE,SIZE);
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,1,0,1,-1,1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
            int pattern=texture();Fixture model=new Fixture();NativeSwordGlint renderer=new NativeSwordGlint();
            byte[] dim=draw(renderer,model,frame(.8,1,.2F,8,-50),palette,pattern);
            byte[] bright=draw(renderer,model,frame(.8,1,1,8,-50),palette,pattern);
            check(energy(bright)>energy(dim)*1.25,"Intensity change was not visibly brighter");
            byte[] slow=draw(renderer,model,frame(.8,.1F,1,8,-50),palette,pattern);
            byte[] fast=draw(renderer,model,frame(.8,3F,1,8,-50),palette,pattern);
            check(!Arrays.equals(slow,fast),"Speed slider produced identical rendered bands");
            for(int i=0;i<240;i++) {
                palette.mode=GlintPalette.ColorMode.Rainbow;
                draw(renderer,model,frame(i/240D,1,1,8,-50),palette,pattern);
            }
            draw(renderer,model,frame(.8,1,0,8,-50),palette,pattern);
            System.out.println("PASS: production two-pass draw at 240Hz; intensity and speed change pixels; exact silhouette; blend/color/matrices/lightmap/atlas/depth restored");
        }finally{context.destroy();}
    }
}
