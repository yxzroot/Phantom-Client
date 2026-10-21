import keystrokesmod.client.module.modules.render.aurora.*;
import java.util.*;
import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

/** Off-screen real-driver regressions, not a claim of live-game FPS. */
public final class ArtSkyTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void cpu(){
        check(ArtSkyAssets.noise(false)==ArtSkyAssets.noise(false),"Noise allocated twice");check(ArtSkyAssets.noise(true).length==256*256,"Low quality noise resolution");
        check(ArtSkyAssets.starMesh().length==2000*4*9,"Star cap");for(float v:ArtSkyAssets.starMesh())check(Float.isFinite(v),"Invalid star");
        for(int i=0;i<30;i++)for(boolean streak:new boolean[]{false,true}){double u=i*.073,v=i*.121;check(Math.abs(ArtSkyAssets.field(u,v,streak)-ArtSkyAssets.field(u+1,v,streak))<.0001,"Noise seam U");check(Math.abs(ArtSkyAssets.field(u,v,streak)-ArtSkyAssets.field(u,v+1,streak))<.0001,"Noise seam V");}
        for(int fps:new int[]{60,240})for(int s=0;s<6;s++)for(int t=0;t<6;t++){SkyStyleMix mix=new SkyStyleMix();mix.reset(s);for(int i=0;i<fps*3/10;i++){mix.advance(t,1D/fps,.3F);float sum=0;for(int j=0;j<6;j++){sum+=mix.amount(j);if(j!=s&&j!=t)check(mix.amount(j)==0,"Unrelated style flash");}check(Math.abs(sum-1)<.00001,"Non-convex fade");}check(mix.amount(t)>.9999,"Fade duration");}
        AuroraOptions o=new AuroraOptions();o.stars=1;check(ArtSkyRenderer.starCount(o)==2000,"Star density");o.lowQuality=true;check(ArtSkyRenderer.starCount(o)==1000,"Low quality star cap");
        Set<Integer> palettes=new HashSet<>();for(int i=0;i<=10;i++){o.art.palette(i,0xD17AFF,0x33BBDD);palettes.add(Arrays.hashCode(o.art.colors));}check(palettes.size()==11,"Palette setting ineffective");
        AuroraLightmap light=new AuroraLightmap();int[] p=new int[256];Arrays.fill(p,0xFFA0C090);int[] before=p.clone();check(light.apply(p,0,0,0,0,0,0,.5F,0x403080),"New tint missing");check(!Arrays.equals(p,before),"New tint inactive");light.restore(p);check(Arrays.equals(p,before),"Tint leaks after world");check(!light.apply(p,0,0,0,0,0,0,0,0),"OFF tint active");
        System.out.println("PASS art CPU: cached periodic noise, 2000-star cap, all 36 reversible 60/240Hz fades, presets, exact lightmap restore");
        PetalSimulation a=new PetalSimulation(),b=new PetalSimulation();ArtSkyOptions petals=new ArtSkyOptions();petals.petals=2;petals.cherryMix=1;
        for(int i=0;i<720;i++)a.advance(1D/240,i/240D,true,petals,false,0,70,0,(x,z)->0);for(int i=0;i<180;i++)b.advance(1D/60,i/60D,true,petals,false,0,70,0,(x,z)->0);
        check(a.count==250&&a.x.length==250&&Math.abs(a.fade-b.fade)<.0001,"Petal cap/frame-rate fade");
        for(int i=0;i<250;i++)check(Double.isFinite(a.x[i])&&a.alpha[i]>=0&&a.alpha[i]<=1,"Invalid petal");
        for(int i=0;i<240;i++)a.advance(1D/240,3+i/240D,true,petals,false,0,70,0,(x,z)->10000);
        check(a.fade<.0001,"Petals remain indoors");a.advance(.016,4,true,petals,true,1000,70,0,(x,z)->0);check(a.count<=125&&Math.abs(a.x[0]-1000)<16,"Petal teleport/quality reset");
        check(ArtSkyAssets.petalPixels()==ArtSkyAssets.petalPixels(),"Petal sprite not cached");
    }
    static int[] pixels(){IntBuffer b=BufferUtils.createIntBuffer(512*320);GL11.glReadPixels(0,0,512,320,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,b);int[] p=new int[b.remaining()];b.get(p);return p;}
    static int[] draw(ArtSkyRenderer r,AuroraSimulation sim,AuroraOptions o,float yaw){GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GLU.gluPerspective(95,1.6F,.05F,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glRotatef(-17,1,0,0);GL11.glRotatef(-yaw,0,1,0);r.draw(sim,o);return pixels();}
    static long delta(int[] a,int[] b){long d=0;for(int i=0;i<a.length;i++)for(int c=0;c<3;c++)d+=Math.abs((a[i]>>(c*8)&255)-(b[i]>>(c*8)&255));return d;}
    static void save(int[] p,String path)throws Exception{java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(512,320,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<320;y++)image.setRGB(0,319-y,512,1,p,y*512,512);javax.imageio.ImageIO.write(image,"png",new java.io.File(path));}
    static void gl(String dir,int stages)throws Exception{
        Pbuffer p=new Pbuffer(512,320,new PixelFormat().withDepthBits(24),null);p.makeCurrent();ArtSkyRenderer r=new ArtSkyRenderer();
        try{net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;GL11.glViewport(0,0,512,320);AuroraSimulation sim=new AuroraSimulation();sim.fade=1;AuroraOptions o=new AuroraOptions();o.art.palette(0,0xA647BF,0x2497C9);o.stars=1;sim.time=32;
            for(int s=0;s<stages;s++){
                o.art.nebulaMix=s==0?1:0;o.art.sunsetMix=s==1?1:0;o.art.cherryMix=s==2?1:0;
                int tex=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glDepthMask(true);
                int[] a=draw(r,sim,o,62);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==tex,"Texture leak");check(GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glIsEnabled(GL11.GL_ALPHA_TEST),"Enable state leak");check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),"Depth write leak");check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"Matrix mode leak");
                Set<Integer> unique=new HashSet<>();for(int value:a)unique.add(value);check(unique.size()>1000,"Flat/empty sky "+s);save(a,dir+"/art-sky-"+s+".png");
                sim.time+=30;int[] b=draw(r,sim,o,62);check(delta(a,b)>3000,"Sky does not animate "+s);o.art.palette(2,0,0);int[] c=draw(r,sim,o,62);check(delta(b,c)>100000,"Palette has no effect "+s);o.art.palette(0,0,0);
                o.art.brightness=.3F;check(delta(b,draw(r,sim,o,62))>100000,"Brightness inactive "+s);o.art.brightness=1;
                int[] normal=draw(r,sim,o,62);
                if(s==0){o.art.planet=false;check(delta(normal,draw(r,sim,o,62))>1000,"Planet toggle inactive");o.art.planet=true;}
                if(s==1){o.art.sunSize=.6F;check(delta(normal,draw(r,sim,o,62))>10000,"Sun size inactive");o.art.sunSize=1.25F;o.art.stripeSpeed=0;check(delta(normal,draw(r,sim,o,62))>5000,"Stripe speed inactive");o.art.stripeSpeed=1;o.art.skyline=false;check(delta(normal,draw(r,sim,o,62))>1000,"Skyline inactive");o.art.skyline=true;o.art.grid=true;check(delta(normal,draw(r,sim,o,62))>1000,"Grid inactive");o.art.grid=false;}
                if(s==2){o.art.body=1;check(delta(normal,draw(r,sim,o,62))>1000,"Moon selector inactive");o.art.body=0;o.art.trees=false;check(delta(normal,draw(r,sim,o,62))>1000,"Tree toggle inactive");o.art.trees=true;}
                o.lowQuality=true;for(int i=0;i<240;i++){sim.time+=1D/240;r.draw(sim,o);}o.lowQuality=false;
                check(GL11.glGetError()==GL11.GL_NO_ERROR,"GL error style "+s);GL11.glDeleteTextures(tex);
            }
            if(stages==3)petalGl();
            System.out.println("PASS art GL: "+stages+" styles rendered, moving pixels, palettes/brightness, cached low-quality 240 draws, texture/enables/depth/matrix restore");
        }finally{r.dispose();p.destroy();}
    }
    static void petalGl(){PetalRenderer r=new PetalRenderer();try{PetalSimulation sim=new PetalSimulation();sim.fade=1;sim.count=1;sim.x[0]=0;sim.y[0]=0;sim.z[0]=-5;sim.size[0]=.7F;sim.alpha[0]=1;
        ArtSkyOptions o=new ArtSkyOptions();GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_LIGHTING);GL20.glUseProgram(0);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_BLEND);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glClearColor(0,0,0,1);GL11.glClearDepth(1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GLU.gluPerspective(70,1.6F,.05F,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
        int[] before=pixels();r.draw(sim,o,1,0,0,0,1,0,0,1,0);check(delta(before,pixels())>1000,"Petal sprite not visible");GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glColor4f(.1F,.2F,.4F,1);GL11.glBegin(GL11.GL_QUADS);GL11.glVertex3f(-5,-5,-2);GL11.glVertex3f(5,-5,-2);GL11.glVertex3f(5,5,-2);GL11.glVertex3f(-5,5,-2);GL11.glEnd();before=pixels();r.draw(sim,o,1,0,0,0,1,0,0,1,0);check(Arrays.equals(before,pixels()),"Petals draw through blocks");check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST),"Petal depth restore");check(GL11.glGetError()==0,"Petal GL error");
        System.out.println("PASS petal GL: original notched sprite visible, depth occluded by foreground world, depth write/test restored");
    }finally{r.dispose();}}
    public static void main(String[] args)throws Exception{cpu();if(args.length>0)gl(args[0],args.length>1?Integer.parseInt(args[1]):1);}
}
