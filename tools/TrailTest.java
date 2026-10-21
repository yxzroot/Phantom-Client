import keystrokesmod.client.module.modules.render.trail.*;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.*;
import java.nio.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Production simulation/curve/settings codecs and offscreen renderer, not a fake effect. */
public final class TrailTest {
    static final int W=384,H=240;
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static TrailBuffer path(int fps,TrailOptions opt,double origin){
        TrailBuffer b=new TrailBuffer();
        for(int i=0;i<=fps*2;i++){
            double t=i/(double)fps,arc=Math.sin(t*Math.PI)*.5;
            b.record(origin+t*2,1+arc,Math.sin(t*2)*.7,t,arc<=0,false,opt);
        }
        b.visible=true;return b;
    }
    static void cpu(){
        TrailOptions opt=new TrailOptions();TrailBuffer a=path(60,opt,0),b=path(240,opt,0);
        check(b.count>a.count*3&&b.count<=TrailBuffer.CAP,"Not recording interpolated frame positions");
        TrailCurve ca=new TrailCurve(),cb=new TrailCurve();ca.prepare(a,opt,2,0,0,0,3,3,8,false);cb.prepare(b,opt,2,0,0,0,3,3,8,false);
        check(Math.abs(ca.count-cb.count)<=2&&ca.count>20,"Distance resampling changes with FPS");
        for(int i=0;i<Math.min(ca.count,cb.count);i++){
            check(Math.abs(ca.x[i]-cb.x[i])<.025&&Math.abs(ca.y[i]-cb.y[i])<.025&&Math.abs(ca.z[i]-cb.z[i])<.025,"60/240Hz curve drift");
            check(Float.isFinite(cb.nx[i])&&Math.abs(cb.nx[i]*cb.nx[i]+cb.ny[i]*cb.ny[i]+cb.nz[i]*cb.nz[i]-1)<.002,"Billboard normal invalid");
        }
        check(cb.alpha[cb.count-1]==0&&cb.width[cb.count-1]==0,"Tail not tapered");
        // Standing still never refreshes timestamps or adds points.
        int before=b.count;double time=b.stamp[b.index(0)];
        for(int i=1;i<=240*3;i++)b.record(4,1,Math.sin(4)*.7,2+i/240D,true,false,opt);
        check(b.count==0&&b.stamp[b.index(0)]==time,"Stationary trail piles up or never expires");
        b=path(240,opt,0);b.record(100,3,100,2.01,true,false,opt);check(b.count==1&&!b.landed,"Teleport leaves streak/landing burst");
        b.record(Double.NaN,1,1,2.02,true,false,opt);check(b.count==0&&!b.initialized,"NaN position not cleared");
        b=path(240,opt,0);b.clear();check(b.count==0&&b.landingAt<0,"World/death clear retains state");
        b.record(0,1,0,0,true,false,opt);b.record(.1,2,0,.1,false,false,opt);b.record(.2,1,0,.2,true,false,opt);
        check(b.landed&&b.landingAt==.2,"Landing not detected");
        b.record(.3,1,0,.3,true,false,opt);check(!b.landed,"Landing repeats while grounded");
        TrailBuffer sprint=new TrailBuffer();sprint.record(0,0,0,0,true,true,opt);sprint.record(1,0,0,.1,true,true,opt);
        check(sprint.life[sprint.index(0)]>1&&sprint.boost[sprint.index(0)]>1,"Sprint not longer/brighter");
        TrailBuffer airborne=new TrailBuffer();airborne.record(0,0,0,0,false,false,opt);check(airborne.boost[0]>1,"Airborne not wider/brighter");
        opt.low=true;TrailBuffer low=path(240,opt,0);check(low.count<a.count+2,"Low quality does not reduce frame samples");
        cb.prepare(low,opt,2,0,0,0,0,3,8,false);check(cb.count<=256,"Low-quality curve cap");opt.low=false;
        TrailBuffer far=path(240,opt,30000000);cb.prepare(far,opt,2,30000000,0,0,3,3,8,false);
        ca.prepare(path(240,opt,0),opt,2,0,0,0,3,3,8,false);
        for(int i=0;i<Math.min(ca.count,cb.count);i++)check(Math.abs(ca.x[i]-cb.x[i])<.00001,"Camera-relative precision drift near world edge");
        TrailBuffer vertical=new TrailBuffer();for(int i=0;i<180;i++)vertical.record(0,i*.015,0,i/240D,false,false,opt);
        ca.prepare(vertical,opt,.75,0,0,0,0,10,0,false);for(int i=0;i<ca.count;i++)check(Float.isFinite(ca.nx[i]),"Vertical/end-on path normal failed");
        TrailEffects effects=new TrailEffects();for(int i=0;i<200;i++)effects.spawn(i%8,0,0,0,0xFF00FF,0);
        check(effects.active()==20,"Spark cap exceeded");effects.expire(2);check(effects.active()==0,"Spark lifetime failed");
        opt.mode=TrailOptions.ColorMode.Static;opt.color=0x123456;check(opt.tint(.5F,12,6)==0x123456,"Static color not exact");
        opt.mode=TrailOptions.ColorMode.Gradient;opt.second=0xABCDEF;check(opt.tint(0,0,0)==opt.color&&opt.tint(1,0,0)==opt.second,"Gradient endpoints wrong");
        opt.mode=TrailOptions.ColorMode.Rainbow;check(opt.tint(.5F,0,0)!=opt.tint(.5F,1,0)&&opt.tint(.5F,0,0)!=opt.tint(.5F,0,1),"Rainbow not animated/spatial");
        settings();
        opt.style=TrailOptions.Style.SolidBand;opt.matchLength=false;
        TrailBuffer bandPath=path(240,opt,0);cb.prepare(bandPath,opt,2,0,0,0,3,3,8,false);
        check(Math.abs(cb.distance[0]-cb.distance[cb.count-1]-1.6)<1e-5,"Band length is not a physical block limit");
        check(Math.abs(cb.topY[0]-cb.hemY[0]-.35)<1e-5,"Band not skinny by default");
        check(Math.abs((cb.topY[0]+cb.hemY[0])*.5-1.05)<1e-5,"Band height offset ignored");
        check(cb.width[0]==0&&cb.width[cb.count-1]==0,"Rounded nose/tail not closed");
        opt.bandHeight=.2F;opt.heightOffset=.6F;cb.prepare(bandPath,opt,2,0,0,0,3,3,8,false);
        check(Math.abs(cb.topY[0]-cb.hemY[0]-.2)<1e-5&&Math.abs((cb.topY[0]+cb.hemY[0])*.5-.6)<1e-5,"Live band geometry controls ignored");
        opt.matchLength=true;cb.prepare(bandPath,opt,2,0,0,0,3,3,8,false);check(cb.distance[0]-cb.distance[cb.count-1]>1.6,"Band still limited by physical cap when matching normal trail length");
        opt.heightOffset=3;cb.prepare(bandPath,opt,2,0,0,0,3,3,8,false);check(Math.abs((cb.topY[0]+cb.hemY[0])*.5-3)<1e-5,"Band cannot be raised above the player");
        opt.bandHeight=.35F;opt.heightOffset=1.05F;opt.style=TrailOptions.Style.Ribbon;
        TrailBuffer upright=new TrailBuffer();upright.pose(0,false);upright.record(0,0,0,0,true,false,opt);upright.record(.2,0,0,.1,true,true,opt);
        cb.prepare(upright,opt,.1,0,0,0,0,2,4,false);
        check(cb.topY[0]==1.5F&&cb.hemY[0]==0,"Trail not neck-to-feet by default");
        check(cb.topY[0]-cb.hemY[0]==1.5F,"Sprint stretches trail height");
        upright.clear();upright.pose(0,true);upright.record(0,0,0,0,true,false,opt);upright.record(.2,0,0,.1,true,false,opt);
        cb.prepare(upright,opt,.1,0,0,0,0,2,4,false);check(cb.topY[0]<1.5F&&cb.topY[0]>1.1F&&cb.hemY[0]==0,"Crouch does not follow neck/body lean");
        opt.hem=.5F;cb.prepare(upright,opt,.1,0,0,0,0,2,4,false);check(cb.hemY[0]==.5F,"Hem control ineffective");opt.hem=0;
        for(int fps:new int[]{60,240}){
            TrailFade fade=new TrailFade();for(int i=0;i<fps/5;i++)fade.advance(true,1D/fps);
            check(Math.abs(fade.gain()-1)<1e-6,"Enable fade not 200ms");
            for(int i=0;i<fps/10;i++)fade.advance(false,1D/fps);check(Math.abs(fade.gain()-.5)<1e-6,"Half fade differs with FPS");
            fade.advance(true,.02);check(fade.gain()>.5&&fade.gain()<1,"Re-enabling pops instead of reversing fade");
            fade.advance(false,.3);check(fade.gain()==0,"Disable fade never reaches zero");
        }
        System.out.println("PASS: 60/240Hz frame recording and spatial resampling, stationary expiry, bounded 3D curves, camera/world-edge precision, vertical normals, teleport/death/world clear, sprint/air/landing, low-quality caps and 20-spark pool");
    }
    static void settings(){
        ComboSetting<TrailOptions.Style> styles=new ComboSetting<>("Style",TrailOptions.Style.Ribbon);
        for(TrailOptions.Style style:TrailOptions.Style.values()){
            styles.setMode(style);com.google.gson.JsonObject json=styles.getConfigAsJson();styles.resetToDefaults();styles.applyConfigFromJson(json);check(styles.getMode()==style,"Style config roundtrip failed");
        }
        ComboSetting<TrailOptions.Preset> presets=new ComboSetting<>("Preset",TrailOptions.Preset.Custom);
        for(TrailOptions.Preset p:TrailOptions.Preset.values()){presets.setMode(p);com.google.gson.JsonObject json=presets.getConfigAsJson();presets.resetToDefaults();presets.applyConfigFromJson(json);check(presets.getMode()==p,"Preset codec failed");}
        PickerRGBSetting color=new PickerRGBSetting("Color",1,2,3,null);color.setRed(251);color.setGreen(87);color.setBlue(35);
        com.google.gson.JsonObject saved=color.getConfigAsJson();color.resetToDefaults();color.applyConfigFromJson(saved);check((color.getRGB()&0xFFFFFF)==0xFB5723,"Picker color config failed");
        Setting[] settings={new SliderSetting("Length (s)",1.2,.25,3,.05),new TickSetting("Show on other players",false),new SliderSetting("Band height",.35,.1,1.5,.01),new SliderSetting("Band length",1.6,.4,6,.05),new SliderSetting("Height offset",1.05,0,1.6,.025),new SliderSetting("Edge width",1,.5,3,.25),new SliderSetting("Tail taper amount",65,0,100,1)};
        for(Setting s:settings){com.google.gson.JsonObject json=s.getConfigAsJson();s.resetToDefaults();s.applyConfigFromJson(json);check(json.equals(s.getConfigAsJson()),"Existing setting codec changed");}
        System.out.println("PASS: actual Phantom styles/presets, band sliders, toggles and picker config codecs roundtrip");
    }
    static int[] pixels(){IntBuffer data=BufferUtils.createIntBuffer(W*H);GL11.glReadPixels(0,0,W,H,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);int[] p=new int[W*H];data.get(p);return p;}
    static void clear(){GL11.glClearColor(0,0,0,1);GL11.glClearDepth(1);GL11.glDepthMask(true);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);}
    static long energy(int[] p){long sum=0;for(int v:p)sum+=(v>>16&255)+(v>>8&255)+(v&255);return sum;}
    static void state(int t0,int t1,int vbo){
        check(GL11.glGetError()==0,"Trail GL error");check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1,"Active texture leaked");
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==t1,"Lightmap binding leaked");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==t0&&GL11.glIsEnabled(GL11.GL_TEXTURE_2D),"Atlas/texture enable leaked");GL13.glActiveTexture(GL13.GL_TEXTURE1);
        check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),"Depth state leaked");
        check(GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_CULL_FACE)&&GL11.glIsEnabled(GL11.GL_ALPHA_TEST),"Fixed-function state leaked");
        check(GL11.glGetInteger(GL11.GL_BLEND_SRC)==GL11.GL_SRC_ALPHA&&GL11.glGetInteger(GL11.GL_BLEND_DST)==GL11.GL_ONE_MINUS_SRC_ALPHA,"Blend func leaked");
        check(GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)==vbo&&GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"VBO/matrix leaked");
        FloatBuffer color=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);for(int i=0;i<4;i++)check(color.get(i)==1,"Color tinted after render");
    }
    static void gl(String preview)throws Exception{
        Pbuffer context=new Pbuffer(W,H,new PixelFormat().withDepthBits(24),null);context.makeCurrent();
        TrailRenderer renderer=new TrailRenderer();
        try{
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            GL11.glViewport(0,0,W,H);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();org.lwjgl.util.glu.GLU.gluPerspective(45,W/(float)H,.05F,64);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();org.lwjgl.util.glu.GLU.gluLookAt(2,2.3F,6,2,1,0,0,1,0);
            int t0=GL11.glGenTextures(),t1=GL11.glGenTextures(),oldVbo=GL15.glGenBuffers();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,t0);GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,t1);GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,oldVbo);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,64,GL15.GL_STATIC_DRAW);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            TrailOptions opt=new TrailOptions();opt.length=3;opt.mode=TrailOptions.ColorMode.Gradient;opt.width=.3F;
            TrailBuffer b=path(240,opt,0);TrailBuffer[] paths={b};TrailEffects effects=new TrailEffects();
            java.awt.image.BufferedImage sheet=new java.awt.image.BufferedImage(W*2,H*4,java.awt.image.BufferedImage.TYPE_INT_RGB);Set<Integer> styles=new HashSet<>();
            int tile=0;for(TrailOptions.Style style:TrailOptions.Style.values()){
                opt.style=style;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);state(t0,t1,oldVbo);int[] p=pixels();
                check(energy(p)>200,"Style blank: "+style);check(styles.add(Arrays.hashCode(p)),"Style identical: "+style);
                for(int y=0;y<H;y++)sheet.setRGB((tile%2)*W,(tile/2)*H+H-1-y,W,1,p,y*W,W);tile++;
            }
            if(preview!=null)javax.imageio.ImageIO.write(sheet,"png",new java.io.File(preview));
            opt.style=TrailOptions.Style.Ribbon;opt.bloom=0;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);int[] base=pixels();
            opt.bloom=1;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())>energy(base),"Bloom ineffective");
            opt.intensity=.1F;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);long dim=energy(pixels());opt.intensity=1;
            clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())>dim,"Intensity ineffective");
            opt.width=.05F;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);int[] thin=pixels();opt.width=.6F;
            clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())>energy(thin),"Width ineffective");opt.width=.3F;
            Set<Integer> colorModes=new HashSet<>();for(TrailOptions.ColorMode mode:TrailOptions.ColorMode.values()){opt.mode=mode;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);int[] p=pixels();check(energy(p)>0,"Color mode blank");if(mode!=TrailOptions.ColorMode.Color&&mode!=TrailOptions.ColorMode.Theme)check(colorModes.add(Arrays.hashCode(p)),"Color mode identical");}
            opt.style=TrailOptions.Style.SolidBand;opt.mode=TrailOptions.ColorMode.Static;opt.color=0xFF1010;
            clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);int[] redBand=pixels();
            opt.color=0x1040FF;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(!Arrays.equals(redBand,pixels()),"Band manual color changes ignored");
            opt.bandHeight=.15F;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);int[] shortBand=pixels();opt.bandHeight=.7F;
            clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())>energy(shortBand),"Band height has no visible effect");
            opt.heightOffset=.2F;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);int[] lowBand=pixels();opt.heightOffset=1.2F;
            clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(!Arrays.equals(lowBand,pixels()),"Band height offset has no visible effect");
            opt.bandHeight=.35F;opt.heightOffset=1.05F;opt.style=TrailOptions.Style.Ribbon;
            opt.mode=TrailOptions.ColorMode.Gradient;Set<Integer> presetColors=new HashSet<>();
            for(TrailOptions.Preset preset:TrailOptions.Preset.values())if(preset!=TrailOptions.Preset.Custom){opt.color=preset.start;opt.second=preset.end;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(presetColors.add(Arrays.hashCode(pixels())),"Preset colors identical: "+preset);}
            opt.intensity=0;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())==0,"Zero intensity still draws");opt.intensity=.75F;
            // Fully occluded depth plane must hide ALL trails, bloom and sparkles.
            clear();GL11.glClearDepth(0);GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())==0,"Trail visible through blocks");GL11.glClearDepth(1);
            clear();renderer.render(paths,effects,opt,2,0,0,0,0,false);check(energy(pixels())==0,"Disabled fade still draws");
            opt.low=true;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())>0,"Low quality blank");opt.low=false;
            for(int angle=0;angle<8;angle++){
                double az=angle*Math.PI/4;GL11.glLoadIdentity();org.lwjgl.util.glu.GLU.gluLookAt(2+(float)Math.sin(az)*5,2.3F,(float)Math.cos(az)*5,2,1,0,0,1,0);
                FloatBuffer before=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,before);
                clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())>100,"Ribbon edge-on/absent at camera angle "+angle);
                FloatBuffer after=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,after);
                for(int i=0;i<16;i++)check(before.get(i)==after.get(i),"Trail changed caller's camera matrix");
            }
            b.visible=false;clear();renderer.render(paths,effects,opt,2,1,0,0,0,false);check(energy(pixels())==0,"Visibility toggle still draws");b.visible=true;
            TrailCurve clipped=new TrailCurve();clipped.prepare(b,opt,2,4,1,Math.sin(4)*.7,0,1.6,0,true);
            check(clipped.count>1&&clipped.alpha[0]==0,"First-person head blocks local view");
            b.landingAt=1.8;b.landingX=2;b.landingY=1;b.landingZ=0;effects.spawn(0,2,1,0,0xFFFFAA,1.8);
            for(int i=0;i<240;i++){clear();opt.style=TrailOptions.Style.values()[i%TrailOptions.Style.values().length];renderer.render(paths,effects,opt,2+i/240D*.1,1,0,0,0,false);state(t0,t1,oldVbo);}
            glyphAfter();
            GL11.glDeleteTextures(t0);GL11.glDeleteTextures(t1);GL15.glDeleteBuffers(oldVbo);
            System.out.println("PASS: seven production styles including filled skinny Solid Band; manual colors, band height/offset controls; palettes/bloom/width/intensity; eight camera angles and first-person head clipping; depth occlusion; 240 simulated renders; GL/bindings/matrices and transparent HUD glyph restored");
        }finally{renderer.dispose();context.destroy();}
    }
    static void glyphAfter(){
        GL13.glActiveTexture(GL13.GL_TEXTURE0);net.minecraft.client.renderer.GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        int tex=GL11.glGenTextures();net.minecraft.client.renderer.GlStateManager.bindTexture(tex);
        IntBuffer data=BufferUtils.createIntBuffer(16);for(int y=0;y<4;y++)for(int x=0;x<4;x++)data.put(x==1||y==1?0xFFFFFFFF:0x00FFFFFF);data.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,4,4,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,0);GL20.glUseProgram(0);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,1,0,1,-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();clear();
        GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2f(0,0);GL11.glVertex2f(0,0);GL11.glTexCoord2f(1,0);GL11.glVertex2f(1,0);GL11.glTexCoord2f(1,1);GL11.glVertex2f(1,1);GL11.glTexCoord2f(0,1);GL11.glVertex2f(0,1);GL11.glEnd();
        int[] p=pixels();check((p[H/8*W+W/8]&0xFFFFFF)==0,"Transparent HUD became white rectangle");check((p[H/8*W+W*3/8]&0xFFFFFF)>0xEEEEEE,"HUD crosshair/glyph tinted or missing");GL11.glDeleteTextures(tex);
    }
    public static void main(String[] args)throws Exception{if(args.length>0&&args[0].equals("gl"))gl(args.length>1?args[1]:null);else cpu();}
}
