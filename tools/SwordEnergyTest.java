import keystrokesmod.client.module.modules.render.swordglow.*;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import java.nio.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Production energy shader, alpha-cutout orange sword and stencil exclusion, not a mock renderer. */
public final class SwordEnergyTest {
    static final int SIZE=128;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static final class Model implements IBakedModel,NativeSwordGlint.ModelDraw {
        int calls;
        public void swordglow$drawNativeModel(IBakedModel model,int tint){calls++;geometry();}
        void geometry(){net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0,0);GL11.glVertex3f(0,0,0);GL11.glTexCoord2f(1,0);GL11.glVertex3f(1,0,0);
            GL11.glTexCoord2f(1,1);GL11.glVertex3f(1,1,0);GL11.glTexCoord2f(0,1);GL11.glVertex3f(0,1,0);GL11.glEnd();}
        public List<BakedQuad> getGeneralQuads(){return Collections.emptyList();}public List<BakedQuad> getFaceQuads(EnumFacing face){return Collections.emptyList();}
        public boolean isAmbientOcclusion(){return false;}public boolean isGui3d(){return true;}public boolean isBuiltInRenderer(){return false;}
        public TextureAtlasSprite getParticleTexture(){return null;}public ItemCameraTransforms getItemCameraTransforms(){return null;}
    }
    static boolean silhouette(int x,int y){float u=(x+.5F)/SIZE,v=(y+.5F)/SIZE;return u>.43&&u<.57&&v>.35&&v<.9||u>.30&&u<.70&&v>.30&&v<.38||u>.46&&u<.54&&v>.10&&v<.30;}
    static int texture(boolean atlas){
        int id=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);IntBuffer data=BufferUtils.createIntBuffer(SIZE*SIZE);
        if(atlas){for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++)data.put(silhouette(x,y)?0xFFFF6308:0x00FFFFFF);}
        else data.put(SwordEnergyTexture.pixels());data.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,SIZE,SIZE,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);return id;
    }
    static int[] pixels(){IntBuffer buffer=BufferUtils.createIntBuffer(SIZE*SIZE);GL11.glReadPixels(0,0,SIZE,SIZE,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,buffer);int[] out=new int[SIZE*SIZE];buffer.get(out);return out;}
    static int[] base(Model model,int atlas){
        GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,atlas);GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);
        GL11.glDisable(GL11.GL_BLEND);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(255);GL11.glClearStencil(1);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT|GL11.GL_STENCIL_BUFFER_BIT);
        model.geometry();model.calls=0;return pixels();
    }
    static SwordAnimator.Frame frame(SwordOptions options,float seconds){SwordAnimator anim=new SwordAnimator();anim.advance(0,0,true,options,0,false,false,false);return anim.advance(seconds,.25F,true,options,0,false,false,false);}
    static int[] render(SwordEnergyRenderer renderer,Model model,int texture,SwordOptions options,GlintPalette.Palette palette,float seconds){
        base(model,options.atlas);SwordModelBounds bounds=new SwordModelBounds(model);bounds.ax=0;bounds.ay=1;bounds.min=.1F;bounds.extent=.8F;bounds.tipX=.5F;bounds.tipY=.9F;bounds.tipZ=0;
        renderer.draw(frame(options,seconds),palette,options,model,bounds,texture,model,null,null,0,1,seconds);
        check(GL11.glGetError()==0,"Energy shader/GL error");
        check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_DEPTH_FUNC)==GL11.GL_LEQUAL,"Depth leak");
        check(GL11.glIsEnabled(GL11.GL_ALPHA_TEST)&&!GL11.glIsEnabled(GL11.GL_BLEND),"Alpha/blend leak");
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==options.atlas,"Atlas texture leaked");return pixels();
    }
    static long energy(int[] pixels){long total=0;for(int pixel:pixels)total+=(pixel>>16&255)+(pixel>>8&255)+(pixel&255);return total;}
    static void cpu(){
        SwordOptions opt=new SwordOptions();opt.style=AnimationStyle.Charge;SwordAnimator a=new SwordAnimator();
        for(int i=0;i<=480;i++)a.advance(i/240D,1F/240,true,opt,0,false,false,false);
        check(a.frame().charge>.8,"Idle charge did not build");a.advance(2.01,.01F,true,opt,.2F,true,false,false);check(a.frame().burst>.8&&a.frame().charge==0,"Swing did not release charge");
        a.advance(2.02,.01F,true,opt,.3F,true,true,true);check(a.frame().hitFlash==1&&a.frame().blocking>0,"Hit/block reaction missing");
        opt.style=AnimationStyle.Aurora;a.advance(2.12,.1F,true,opt,.4F,true,false,true);check(a.frame().transition>0&&a.frame().transition<1,"Style switch popped");
        SwordMotion m=new SwordMotion();for(int i=0;i<=480;i++)m.observe(i/480F,0,-1,i/240D,true);
        check(m.count==12&&2-m.stamp[(m.head+11)%12]<.02,"Trail not fixed-rate");
        check(2-m.stamp[m.head]>.16,"240FPS trail duration collapsed");
        SwordParticles particles=new SwordParticles();for(int i=0;i<200;i++)particles.spawn(0,m,0xFFAA55,1,true);check(particles.active()==25,"Particle cap exceeded");particles.update(2);check(particles.active()==0,"Particles do not age out");
        check(Arrays.equals(SwordEnergyTexture.pixels(),SwordEnergyTexture.pixels()),"Procedural texture not deterministic");
        check(SwordOptions.signature("Inferno")==AnimationStyle.Flame&&SwordOptions.signature("Aurora")==AnimationStyle.Aurora&&SwordOptions.signature("Amoled")==AnimationStyle.EnergyFlow,"Theme signatures incorrect");
        swingCpu();
        System.out.println("PASS: style fades, idle charge/swing release/hit/block, 12-sample fixed-rate trail, shared 25-particle cap, original cached procedural texture");
    }
    static void swingCpu(){
        SwordOptions opt=new SwordOptions();float energy=0;
        for(int fps:new int[]{60,240}){
            SwordAnimator anim=new SwordAnimator();int period=fps*3/10;
            for(int i=0;i<=fps*12/10;i++)anim.advance(i/(double)fps,1F/fps,true,opt,(i%period)/(float)period,true,false,false);
            check(anim.frame().swingId==5,"Continuous held attacks missed a progress wrap at "+fps);
            anim.advance(1.3,.1F,true,opt,1F/3,true,false,false);
            if(fps==60)energy=anim.frame().swingEnergy;
            else check(Math.abs(energy-anim.frame().swingEnergy)<.00001,"Swing envelope differs with frame rate");
            check(anim.frame().swingEnergy>.98&&anim.frame().swingSweep>.3,"Cut/release envelope missing");
            for(int i=1;i<=fps/2;i++)anim.advance(1.3+i/(double)fps,1F/fps,true,opt,0,false,false,false);
            check(anim.frame().swingEnergy<.001,"Swing glow hangs after recovery");
            anim.reset();check(anim.frame().swingId==0&&!anim.frame().swinging&&anim.frame().swingEnergy==0,"Equip/reset retains swing state");
        }
        SwordMotion motion=new SwordMotion();
        for(int i=0;i<=120;i++){float t=i/240F;motion.observe(t,.1F*(float)Math.sin(t*8),-.5F,t,true,1,1);}
        motion.trail.prepare(motion,.5,.2F,false);SwordTrail trail=motion.trail;
        check(trail.count>24&&trail.count<=SwordTrail.CAP,"Trail not subdivided/bounded");
        for(int i=0;i<trail.count;i++){
            check(Float.isFinite(trail.x[i])&&Float.isFinite(trail.nx[i])&&trail.alpha[i]>=0&&trail.alpha[i]<=1,"Trail invalid vertex");
            check(trail.nx[i]*trail.nx[i]+trail.ny[i]*trail.ny[i]<=.0361*.0361,"Trail width became a slab");
            check(trail.x[i]<=.50001&&trail.x[i]>=.29,"Curve overshot sampled path");
        }
        motion.observe(.51F,0,-.5F,.51,true,2,1);motion.observe(.53F,0,-.5F,.53,true,2,1);
        trail.prepare(motion,.53,.2F,false);int groups=0;for(int i=0;i<trail.count;i++)if(trail.start[i])groups++;
        check(groups==2,"Consecutive strokes connected into a sheet");
        trail.prepare(motion,1,.2F,false);check(trail.count==0,"Trail does not fade out");
        motion.observe(30,30,30,1.01,true,3,1);check(motion.count==1,"Teleport joins unrelated trail");
        motion.reset();for(int i=0;i<60;i++)motion.observe(0,0,-1,i/240D,true,1,1);
        trail.prepare(motion,.246,.2F,false);for(int i=0;i<trail.count;i++)check(trail.alpha[i]==0,"Stationary sword leaves solid trail");
        System.out.println("PASS: continuous swing wraps, analytical 60/240Hz cut envelope, soft recovery/reset, bounded smooth trails, stroke/teleport breaks and idle fade");
    }
    static void gl(String preview)throws Exception{
        Pbuffer context=new Pbuffer(SIZE,SIZE,new PixelFormat().withDepthBits(24).withStencilBits(8),null);context.makeCurrent();
        try{
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            GL11.glViewport(0,0,SIZE,SIZE);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,1,0,1,-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
            SwordOptions opt=new SwordOptions();opt.atlas=texture(true);int procedural=texture(false);opt.equip=false;opt.bloom=0;opt.particles=opt.trail=false;
            GlintPalette.Palette palette=new GlintPalette.Palette();palette.mode=GlintPalette.ColorMode.Gradient;palette.start=0x44CFFF;palette.end=0xC96BFF;
            Model model=new Model();SwordEnergyRenderer renderer=new SwordEnergyRenderer();int[] plain=base(model,opt.atlas),core=render(renderer,model,procedural,opt,palette,.8F);
            check(energy(core)>energy(plain)*1.10,"Energy not visible on saturated orange blade");
            for(int i=0;i<plain.length;i++)if((plain[i]&0xFFFFFF)==0)check((core[i]&0xFFFFFF)==0,"Core escaped cutout silhouette");
            opt.bloom=.9F;opt.stencilMask=128;int[] halo=render(renderer,model,procedural,opt,palette,.8F);int exterior=0;
            for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){int index=y*SIZE+x;if((plain[index]&0xFFFFFF)!=0)check(core[index]==halo[index],"Halo covered real blade pixels");
                else if(core[index]!=halo[index]){exterior++;boolean near=false;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)if(x+dx>=0&&x+dx<SIZE&&y+dy>=0&&y+dy<SIZE&&silhouette(x+dx,y+dy))near=true;check(near,"Halo became a slab");}}
            check(exterior>0,"Stencil halo absent");ByteBuffer stencil=BufferUtils.createByteBuffer(SIZE*SIZE);GL11.glReadPixels(0,0,SIZE,SIZE,GL11.GL_STENCIL_INDEX,GL11.GL_UNSIGNED_BYTE,stencil);
            for(int i=0;i<stencil.capacity();i++)check((stencil.get(i)&255)==1,"Foreign stencil bits changed");
            if(preview!=null){java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(SIZE,SIZE,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<SIZE;y++)image.setRGB(0,SIZE-y-1,SIZE,1,halo,y*SIZE,SIZE);javax.imageio.ImageIO.write(image,"png",new java.io.File(preview));}
            opt.bloom=0;Set<Integer> styles=new HashSet<>();
            for(AnimationStyle style:new AnimationStyle[]{AnimationStyle.EnergyFlow,AnimationStyle.Shimmer,AnimationStyle.Pulse,AnimationStyle.Flame,AnimationStyle.Lightning,AnimationStyle.Galaxy,AnimationStyle.Aurora,AnimationStyle.RainbowWave,AnimationStyle.Charge,AnimationStyle.Glitch,AnimationStyle.Void}){
                opt.style=style;int hash=1;for(float t:new float[]{.2F,.8F,1.7F,2.6F})hash=31*hash+Arrays.hashCode(render(renderer,model,procedural,opt,palette,t));check(styles.add(hash),"Identical animation: "+style);
            }
            opt.style=AnimationStyle.EnergyFlow;opt.intensity=.15F;int[] low=render(renderer,model,procedural,opt,palette,.8F);opt.intensity=1;int[] high=render(renderer,model,procedural,opt,palette,.8F);check(energy(high)>energy(low),"Intensity ineffective");
            opt.bandWidth=.08F;int[] narrow=render(renderer,model,procedural,opt,palette,.8F);opt.bandWidth=.6F;check(!Arrays.equals(narrow,render(renderer,model,procedural,opt,palette,.8F)),"Band width ineffective");opt.bandWidth=.3F;
            opt.brightness=.3F;int[] dim=render(renderer,model,procedural,opt,palette,.8F);opt.brightness=2;check(energy(render(renderer,model,procedural,opt,palette,.8F))>energy(dim),"Brightness ineffective");opt.brightness=1.2F;
            opt.scale=2;int[] coarse=render(renderer,model,procedural,opt,palette,.8F);opt.scale=24;check(!Arrays.equals(coarse,render(renderer,model,procedural,opt,palette,.8F)),"Energy scale ineffective");opt.scale=8;
            opt.angle=30;int[] rotated=render(renderer,model,procedural,opt,palette,.8F);opt.angle=-50;check(!Arrays.equals(rotated,render(renderer,model,procedural,opt,palette,.8F)),"Energy angle ineffective");
            opt.secondary=SwordOptions.Secondary.Pulse;check(!Arrays.equals(high,render(renderer,model,procedural,opt,palette,.8F)),"Secondary overlay ineffective");opt.secondary=SwordOptions.Secondary.None;
            opt.intensity=0;check(Arrays.equals(plain,render(renderer,model,procedural,opt,palette,.8F))&&model.calls==0,"Zero intensity draws");opt.intensity=.75F;
            opt.lowQuality=true;render(renderer,model,procedural,opt,palette,.8F);check(model.calls==1,"Low quality did not reduce model layers/remove halo");opt.lowQuality=false;
            for(int i=0;i<240;i++)render(renderer,model,procedural,opt,palette,i/240F);
            swingGl(renderer,model,procedural,opt,palette);
            SwordGLState.restoreDefaultState(opt.atlas);check(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)&&GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"HUD safety net failed");
            glyphAfter();
            System.out.println("PASS: production 11-style shader on orange alpha-cutout sword; core stays on pixels; 0.65/1.5px halo excludes blade and preserves foreign stencil; zero/low-quality controls; 240 simulated draws; GL/HUD safety");
        }finally{context.destroy();}
    }
    static void swingGl(SwordEnergyRenderer renderer,Model model,int texture,SwordOptions opt,GlintPalette.Palette palette){
        SwordModelBounds bounds=new SwordModelBounds(model);bounds.ax=0;bounds.ay=1;bounds.min=.1F;bounds.extent=.8F;bounds.tipX=.5F;bounds.tipY=.9F;
        SwordAnimator.Frame f=frame(opt,.8F);base(model,opt.atlas);renderer.draw(f,palette,opt,model,bounds,texture,model,null,null,0,1,.8);long idle=energy(pixels());
        base(model,opt.atlas);f.swingEnergy=1;f.swingSweep=.55F;renderer.draw(f,palette,opt,model,bounds,texture,model,null,null,0,2,.8);long cut=energy(pixels());
        check(cut>idle,"Swing energy uniform has no effect");
        base(model,opt.atlas);float saved=opt.swingEnergy;opt.swingEnergy=0;renderer.draw(f,palette,opt,model,bounds,texture,model,null,null,0,3,.8);
        check(energy(pixels())==idle,"Swing energy setting does not disable boost");opt.swingEnergy=saved;
        opt.trail=true;opt.particles=false;opt.bloom=0;
        SwordMotion motion=new SwordMotion();SwordParticles particles=new SwordParticles();
        for(int i=0;i<=48;i++){
            double now=i/240D;float angle=(float)(now*5),offset=(float)Math.sin(angle)*.28F;
            base(model,opt.atlas);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glTranslatef(offset,-.15F,0);model.geometry();f.swinging=true;f.swingId=1;f.swingEnergy=.9F;
            int[] before=pixels();renderer.draw(f,palette,opt,model,bounds,texture,model,motion,particles,0,i,now);
            check(GL11.glGetError()==0,"Motion GL error");
            check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_DEPTH_FUNC)==GL11.GL_LEQUAL,"Trail depth leak");
            check(GL11.glIsEnabled(GL11.GL_ALPHA_TEST)&&!GL11.glIsEnabled(GL11.GL_BLEND),"Trail alpha/blend leak");
            check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==opt.atlas&&GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)==0,"Trail texture/shader leak");
            FloatBuffer transform=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,transform);
            check(Math.abs(transform.get(12)-offset)<.00001&&Math.abs(transform.get(13)+.15F)<.00001,"Motion changed held sword transform");
            if(i==48){int changed=0;int[] after=pixels();
                for(int p=0;p<after.length;p++)if((before[p]&0xFFFFFF)==0&&(after[p]&0xFFFFFF)!=0){changed++;check((after[p]>>16&255)<100&&(after[p]>>8&255)<100&&(after[p]&255)<100,"Trail opaque/oversaturated");}
                check(changed>3&&changed<250,"Soft tip trail absent or became a slab: "+changed);
                check(motion.trail.count>24,"Production path did not use smooth trail");
            }
        }
        opt.trail=false;
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
        System.out.println("PASS: production swing shader responds to energy control; animated 240Hz tip trail is visible, soft and bounded; motion GL state restored");
    }
    static void glyphAfter(){
        int icons=GL11.glGenTextures();net.minecraft.client.renderer.GlStateManager.bindTexture(icons);
        IntBuffer glyph=BufferUtils.createIntBuffer(16);for(int y=0;y<4;y++)for(int x=0;x<4;x++)glyph.put(x==1||y==1?0xFFFFFFFF:0x00FFFFFF);glyph.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,4,4,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,glyph);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_BLEND);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);
        GL20.glUseProgram(0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);new Model().geometry();int[] glyphPixels=pixels();
        check((glyphPixels[8*SIZE+8]&0xFFFFFF)==0,"HUD transparency turned into a square");
        check((glyphPixels[8*SIZE+40]&0xFFFFFF)>0xEEEEEE,"HUD cross/font did not render white");GL11.glDeleteTextures(icons);
    }
    public static void main(String[] args)throws Exception{cpu();if(args.length>0&&args[0].equals("gl"))gl(args.length>1?args[1]:null);}
}
