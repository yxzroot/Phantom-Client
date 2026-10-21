import keystrokesmod.client.module.modules.render.worldparticles.*;
import keystrokesmod.client.module.setting.impl.*;
import java.nio.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Runs the actual simulation, setting codecs and production renderer in a real GL context. */
public final class WorldParticlesTest {
    static final int W=512,H=320;
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static final ParticleSimulation.Exposure OPEN=new ParticleSimulation.Exposure(){
        public int height(int x,int z){return 0;}
        public boolean open(double x,double y,double z){return true;}
    };
    static ParticleSimulation run(int fps,ParticleOptions o){
        ParticleSimulation s=new ParticleSimulation();s.advance(0,true,false,o,0,72,0,OPEN);
        // Stay clear of recycling boundaries while checking the exact motion integrals.
        for(int i=0;i<s.count;i++){s.x[i]*=.3;s.z[i]*=.3;s.y[i]=72+(i%7)*.1;}
        for(int f=0;f<fps*2;f++)s.advance(1D/fps,true,false,o,0,72,0,OPEN);
        return s;
    }
    static void cpu(){
        ParticleOptions o=new ParticleOptions();o.count=300;o.radius=24;
        for(ParticleOptions.Shape shape:ParticleOptions.Shape.values()){
            o.shape=shape;ParticleSimulation a=run(60,o),b=run(240,o);
            check(a.count==300&&b.count==300,"Particle cap/count incorrect");
            for(int i=0;i<a.count;i++){
                check(Math.abs(a.x[i]-b.x[i])<1e-7&&Math.abs(a.y[i]-b.y[i])<1e-7&&Math.abs(a.z[i]-b.z[i])<1e-7,"Motion differs at 60/240Hz: "+shape);
                check(Math.abs(a.angle[i]-b.angle[i])<.0001,"Rotation differs with FPS");
            }
        }
        o.motion=ParticleOptions.Motion.Still;o.count=20;
        ParticleSimulation s=new ParticleSimulation();s.advance(0,true,false,o,0,72,0,OPEN);
        double[] x=s.x.clone(),y=s.y.clone(),z=s.z.clone();
        for(int f=0;f<240;f++)s.advance(1D/240,true,false,o,.2,72,.1,OPEN);
        for(int i=0;i<s.count;i++)check(s.x[i]==x[i]&&s.y[i]==y[i]&&s.z[i]==z[i],"Still particles move/drag with the camera");
        double time=s.time;s.advance(.2,true,true,o,0,72,0,OPEN);check(s.time==time,"Pause advances world motion");
        s.advance(.2,false,true,o,0,72,0,OPEN);check(s.fade==0,"Toggle fade stops while paused");
        for(int fps:new int[]{60,240}){
            s=new ParticleSimulation();for(int f=0;f<fps/5;f++)s.advance(1D/fps,true,false,o,0,72,0,OPEN);
            check(s.fade>.9999,"Fade-in not 200ms");for(int f=0;f<fps/10;f++)s.advance(1D/fps,false,false,o,0,72,0,OPEN);
            check(Math.abs(s.fade-.5)<.0001,"Half fade differs with FPS");
            s.advance(.1,false,false,o,0,72,0,OPEN);check(s.fade<.0001,"Fade never reaches zero");
        }
        o.shape=ParticleOptions.Shape.Stars;s=new ParticleSimulation();s.advance(.1,true,false,o,0,72,0,OPEN);
        o.shape=ParticleOptions.Shape.ETH;s.advance(.09,true,false,o,0,72,0,OPEN);
        check(s.previousShape!=s.currentShape&&Math.abs(s.shapeMix-.5)<.001,"Shape switch not cross-faded");
        s.advance(.1,true,false,o,500,72,500,OPEN);
        for(int i=0;i<s.count;i++)check(Math.abs(s.x[i]-500)<=o.radius&&Math.abs(s.z[i]-500)<=o.radius,"Teleport retains old particle positions");
        check(s.fade<1,"Teleport doesn't reset fade");
        o.count=900;o.low=true;s.advance(.1,true,false,o,500,72,500,OPEN);check(s.count==120,"Low-quality cap ineffective");
        o.low=false;s.advance(.1,true,false,o,500,72,500,OPEN);check(s.count==300,"Hard cap ineffective");
        s.reset();check(s.count==0&&s.fade==0&&s.time==0,"World/death reset incomplete");
        ParticleSimulation.Exposure covered=new ParticleSimulation.Exposure(){
            public int height(int x,int z){return 1000;}
            public boolean open(double x,double y,double z){return true;}
        };
        for(int f=0;f<240;f++)s.advance(1D/240,true,false,o,0,72,0,covered);
        for(int i=0;i<s.count;i++)check(s.alpha[i]==0,"Particles spawn beneath a solid roof");
        o.outdoors=false;for(int f=0;f<240;f++)s.advance(1D/240,true,false,o,0,72,0,covered);
        check(s.alpha[0]>.9,"Outdoors-only toggle cannot be disabled");
        ParticleSimulation.Exposure solid=new ParticleSimulation.Exposure(){
            public int height(int x,int z){return 0;}
            public boolean open(double x,double y,double z){return false;}
        };
        for(int f=0;f<240;f++)s.advance(1D/240,true,false,o,0,72,0,solid);
        for(int i=0;i<s.count;i++)check(s.alpha[i]==0,"Particles spawn inside blocks");
        s.advance(.1,true,false,o,Double.NaN,72,0,OPEN);check(s.count==0,"Invalid camera position retained");
        o.mode=ParticleOptions.ColorMode.Static;o.color=0x123456;check(o.tint(.3F,4)==o.color,"Static picker ignored");
        o.mode=ParticleOptions.ColorMode.Rainbow;check(o.tint(.2F,0)!=o.tint(.2F,2),"Rainbow not moving");
        o.mode=ParticleOptions.ColorMode.Gradient;check(o.tint(.2F,0)!=o.tint(.8F,0),"Second color/gradient ignored");
        for(ParticleOptions.Shape shape:ParticleOptions.Shape.values()){
            ComboSetting<ParticleOptions.Shape> setting=new ComboSetting<>("Shape",shape);
            com.google.gson.JsonObject json=setting.getConfigAsJson();setting.setMode(ParticleOptions.Shape.Snow);setting.applyConfigFromJson(json);
            check(setting.getMode()==shape,"Shape config codec failed");
        }
        PickerRGBSetting c=new PickerRGBSetting("Color",10,20,30,null);c.setRed(205);c.setGreen(88);c.setBlue(45);
        com.google.gson.JsonObject json=c.getConfigAsJson();c.resetToDefaults();c.applyConfigFromJson(json);
        check(c.getRed()==205&&c.getGreen()==88&&c.getBlue()==45,"Shared color picker save/load failed");
        ByteBuffer pixels=ParticleAssets.pixels();check(pixels.remaining()==128*128*5*4,"Sprite atlas dimensions wrong");
        Set<Integer> hashes=new HashSet<>();
        for(int style=0;style<5;style++){
            int hash=1,coverage=0;
            for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++){
                int alpha=pixels.get(((yy*640)+style*128+xx)*4+3)&255;hash=31*hash+alpha;if(alpha>0)coverage++;
                if(xx<2||xx>125||yy<2||yy>125)check(alpha==0,"Atlas edge bleeds into neighboring shape");
            }
            check(coverage>400&&coverage<128*128*.8,"Sprite is blank or a slab");check(hashes.add(hash),"Shape silhouettes identical");
        }
        System.out.println("PASS: 60/240Hz exact drift, Still anchoring, pause, fades, cross-fade, roofs/solid blocks, teleport/reset, 300/120 caps, colors/config and four distinct antialiased silhouettes");
    }
    static int[] pixels(){IntBuffer data=BufferUtils.createIntBuffer(W*H);GL11.glReadPixels(0,0,W,H,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);int[] p=new int[W*H];data.get(p);return p;}
    static long energy(int[] p){long e=0;for(int c:p)e+=(c>>16&255)+(c>>8&255)+(c&255);return e;}
    static void clear(){GL11.glDepthMask(true);GL11.glClearDepth(1);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);}
    static void state(int t0,int t1,int vbo){
        check(GL11.glGetError()==0,"GL error");check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1,"Active texture leaked");
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==t1,"Lightmap binding leaked");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==t0,"Atlas binding leaked");GL13.glActiveTexture(GL13.GL_TEXTURE1);
        check(GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glIsEnabled(GL11.GL_CULL_FACE)&&GL11.glIsEnabled(GL11.GL_ALPHA_TEST),"Render capabilities leaked");
        check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),"Depth mask leaked");
        check(GL11.glGetInteger(GL11.GL_BLEND_SRC)==GL11.GL_SRC_ALPHA&&GL11.glGetInteger(GL11.GL_BLEND_DST)==GL11.GL_ONE_MINUS_SRC_ALPHA,"Blend function leaked");
        check(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)==0&&GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)==vbo,"Program/buffer leaked");
        check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"Matrix mode leaked");
        FloatBuffer c=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,c);for(int i=0;i<4;i++)check(c.get(i)==1,"Color tint leaked");
    }
    static void draw(ParticleRenderer r,ParticleSimulation s,ParticleOptions o){r.draw(s,o,0,0,0,1,0,0,1,0);}
    static void gl(String preview)throws Exception{
        Pbuffer context=new Pbuffer(W,H,new PixelFormat().withDepthBits(24),null);context.makeCurrent();ParticleRenderer r=new ParticleRenderer();
        try{
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            GL11.glViewport(0,0,W,H);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(-4,4,-2.5,2.5,.05,64);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
            int t0=GL11.glGenTextures(),t1=GL11.glGenTextures(),old=GL15.glGenBuffers();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,t0);GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,t1);GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,old);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,64,GL15.GL_STATIC_DRAW);
            GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glColor4f(1,1,1,1);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            ParticleOptions o=new ParticleOptions();o.twinkle=false;o.rotate=false;o.size=.24F;o.mode=ParticleOptions.ColorMode.Gradient;
            ParticleSimulation s=new ParticleSimulation();s.count=32;s.fade=1;s.shapeMix=1;
            for(int i=0;i<s.count;i++){s.x[i]=-2.8+(i%8)*.8;s.y[i]=-1.5+(i/8);s.z[i]=-6;s.size[i]=1;s.alpha[i]=1;s.phase[i]=i/(float)s.count;}
            java.awt.image.BufferedImage sheet=new java.awt.image.BufferedImage(W*2,H*2,java.awt.image.BufferedImage.TYPE_INT_RGB);
            Set<Integer> styles=new HashSet<>();
            for(int style=0;style<4;style++){
                s.currentShape=style;clear();draw(r,s,o);state(t0,t1,old);int[] p=pixels();check(energy(p)>5000,"Shape invisible: "+style);check(styles.add(Arrays.hashCode(p)),"Styles identical");
                for(int yy=0;yy<H;yy++)sheet.setRGB((style%2)*W,(style/2)*H+H-yy-1,W,1,p,yy*W,W);
            }
            javax.imageio.ImageIO.write(sheet,"png",new java.io.File(preview));
            o.mode=ParticleOptions.ColorMode.Static;o.color=0xFF1010;clear();draw(r,s,o);int[] red=pixels();
            o.color=0x1060FF;clear();draw(r,s,o);check(!Arrays.equals(red,pixels()),"Live color picker ineffective");
            o.glow=0;clear();draw(r,s,o);long normal=energy(pixels());o.glow=1;clear();draw(r,s,o);check(energy(pixels())>normal,"Glow ineffective");
            o.low=true;clear();draw(r,s,o);check(r.lastQuads==32&&energy(pixels())==normal,"Low quality still emits bloom");o.low=false;
            o.opacity=.1F;clear();draw(r,s,o);long dim=energy(pixels());o.opacity=1;clear();draw(r,s,o);check(energy(pixels())>dim,"Opacity ineffective");
            clear();GL11.glClearDepth(0);GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);draw(r,s,o);check(energy(pixels())==0,"Core/glow draw through blocks");
            clear();s.fade=0;draw(r,s,o);check(energy(pixels())==0,"Disabled module still visible");s.fade=1;
            for(int i=0;i<s.count;i++){s.x[i]=0;s.y[i]=0;s.z[i]=-.3;}
            clear();draw(r,s,o);check(energy(pixels())==0,"Particles obstruct camera at close range");
            // Worst-case pool + glow + shape cross-fade: <=900 quads in one batch.
            s.count=300;s.shapeMix=.5F;s.previousShape=0;
            for(int i=0;i<300;i++){s.x[i]=(i%20-10)*.25;s.y[i]=(i/20-7)*.2;s.z[i]=-6;s.alpha[i]=1;s.size[i]=1;}
            clear();draw(r,s,o);check(r.lastQuads==900,"Worst-case batching/cap wrong");state(t0,t1,old);
            for(int f=0;f<120;f++){clear();draw(r,s,o);}GL11.glFinish();
            long begin=System.nanoTime();for(int f=0;f<600;f++){clear();draw(r,s,o);}GL11.glFinish();
            System.out.printf(java.util.Locale.ROOT,"INFO: 300 particles + glow + cross-fade offscreen: %.3f ms/frame (not an in-game FPS guarantee)%n",(System.nanoTime()-begin)/6e8);
            r.dispose();clear();draw(r,s,o);check(energy(pixels())>0,"Renderer cannot recreate GPU resources");state(t0,t1,old);
            System.out.println("PASS: actual GL sprites, color/opacity/glow, camera fade, block occlusion, bounded single batch, disposal/reinit and texture/lightmap/depth/lighting/fog/cull/blend/color/matrix/program/buffer restoration");
        }finally{r.dispose();context.destroy();}
    }
    public static void main(String[] args)throws Exception{if(args.length>0&&args[0].equals("gl"))gl(args[1]);else cpu();}
}
