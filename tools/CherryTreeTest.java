import keystrokesmod.client.module.modules.render.aurora.*;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

/** Checks the actual tree panorama/projection, including the user's stretched 4:3 setup. */
public final class CherryTreeTest {
    static final int W=1280,H=720;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void cpu(){
        int[] high=CherryTreeArt.pixels(false),low=CherryTreeArt.pixels(true);
        check(high==CherryTreeArt.pixels(false)&&low==CherryTreeArt.pixels(true),"Tree art not cached");check(high.length==8192*256&&low.length==2048*64,"Panorama quality dimensions");
        double xPixelsPerDegree=CherryTreeArt.WIDTH/360D,yPixelsPerDegree=CherryTreeArt.HEIGHT/Math.toDegrees(CherryTreeArt.SPAN);check(Math.abs(xPixelsPerDegree/yPixelsPerDegree-1)<.2,"Mipmap texel density will horizontally blur trees");
        int transparent=0,soft=0,opaque=0;Set<Integer> colors=new HashSet<>();
        for(int color:high){int a=color>>>24;if(a==0)transparent++;else{colors.add(color&0xFFFFFF);if(a>8&&a<240)soft++;else opaque++;}}
        check(transparent>high.length*.4&&soft>10000,"Tree alpha/antialiasing missing");check(colors.size()>500,"Flat single-color canopy");
        for(int x=0;x<CherryTreeArt.WIDTH;x++)check((high[(CherryTreeArt.HEIGHT-1)*CherryTreeArt.WIDTH+x]>>>24)==0,"Tree clipped against top of panorama");
        System.out.println("PASS trees CPU: cached 8192x256/2048x64 panoramas, angular texel density, detailed colored soft crowns, top padding, transparent background");
    }
    static int[] pixels(){IntBuffer b=BufferUtils.createIntBuffer(W*H);GL11.glReadPixels(0,0,W,H,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,b);int[] p=new int[b.remaining()];b.get(p);return p;}
    static int[] draw(ArtSkyRenderer renderer,AuroraSimulation sim,AuroraOptions o,float aspect,float yaw){
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GLU.gluPerspective(70,aspect,.05F,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glRotatef(yaw,0,1,0);renderer.draw(sim,o);return pixels();
    }
    static void save(int[] pixels,String path)throws Exception{BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);for(int y=0;y<H;y++)image.setRGB(0,H-y-1,W,1,pixels,y*W,W);javax.imageio.ImageIO.write(image,"png",new java.io.File(path));}
    static void gl(String dir)throws Exception{
        Pbuffer p=new Pbuffer(W,H,new PixelFormat().withDepthBits(24),null);p.makeCurrent();ArtSkyRenderer renderer=new ArtSkyRenderer();
        try{
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;GL11.glViewport(0,0,W,H);
            AuroraSimulation sim=new AuroraSimulation();sim.fade=1;sim.time=100;AuroraOptions o=new AuroraOptions();o.art.cherryMix=1;o.art.palette(0,0,0);o.starField=false;
            for(float aspect:new float[]{16F/9,4F/3}){
                o.art.trees=false;int[] plain=draw(renderer,sim,o,aspect,-62);o.art.trees=true;int[] trees=draw(renderer,sim,o,aspect,-62);
                int changed=0,minY=H,maxY=0;for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=y*W+x;if(trees[i]!=plain[i]){changed++;minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}
                check(changed>1500,"Treeline not visible");check(minY>=H/2-1&&maxY<H*.66,"Trees repeat below horizon or spill overhead");
                save(trees,dir+(aspect<1.5?"/cherry-trees-stretched.png":"/cherry-trees-native.png"));
                o.lowQuality=true;draw(renderer,sim,o,aspect,-62);o.lowQuality=false;
                // The panorama wraps continuously, even when looking directly across its UV seam.
                int[] a=draw(renderer,sim,o,aspect,180),b=draw(renderer,sim,o,aspect,-180);long difference=0;for(int i=0;i<a.length;i++)for(int c=0;c<3;c++)difference+=Math.abs((a[i]>>(8*c)&255)-(b[i]>>(8*c)&255));check(difference<50000,"Wrapped tree panorama has seam");
            }
            java.lang.reflect.Field field=ArtSkyRenderer.class.getDeclaredField("trees");field.setAccessible(true);int texture=field.getInt(renderer);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            check(GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH)==8192&&GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT)==256,"Square/low-res tree upload");
            check(GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T)==GL12.GL_CLAMP_TO_EDGE,"Vertical profile repeats");check(GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER)==GL11.GL_LINEAR_MIPMAP_LINEAR,"Missing distant antialias filtering");check(GL11.glGetError()==0,"Tree GL error");
            System.out.println("PASS trees GL: native and stretched 4:3, angular-height bounds, seamless 360 wrap, rectangular high-res upload, mipmaps, clamped vertical UV and both quality modes");
        }finally{renderer.dispose();p.destroy();}
    }
    public static void main(String[] args)throws Exception{cpu();if(args.length>0)gl(args[0]);}
}
