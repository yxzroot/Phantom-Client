import keystrokesmod.client.module.modules.render.killeffect.*;
import keystrokesmod.client.module.setting.impl.*;
import java.nio.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Production kill attribution/pool/config and real offscreen renderer/overlay regressions. */
public final class KillEffectTest {
    static final int W=400,H=300;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void cpu(){
        KillCredit ledger=new KillCredit();Object victim=new Object(),other=new Object(),stale=new Object();
        ledger.watch(victim,true,0);check(ledger.observe(victim,true,0,1,false)==0,"Uninvolved death counted");
        ledger.clear();ledger.attack(victim,1);check(ledger.observe(victim,false,0,1.2,false)==0,"Alive entity counted");
        check(ledger.observe(victim,true,0,3.99,false)==1,"Recent local hit not credited");
        for(int i=0;i<60;i++)check(ledger.observe(victim,true,1,4+i*.01,false)==0,"Duplicate kill fired");
        ledger.watch(victim,true,5);ledger.attack(victim,5);check(ledger.observe(victim,true,0,5.5,false)==1,"Reused entity cannot be credited after a genuine revival");
        ledger.clear();ledger.attack(stale,0);check(ledger.observe(stale,true,0,3.01,false)==0,"Expired hit counted");
        ledger.clear();ledger.attack(victim,1);ledger.attack(other,1.5);check(ledger.observe(victim,true,0,2,false)==0,"Old target still treated as last attacked");
        ledger.credit(other,2);check(ledger.observe(other,true,0,2,false)==0,"Known other attacker was ignored");
        ledger.clear();ledger.watch(victim,true,0);check(ledger.observe(victim,true,2,1,true)==2,"Others' kills toggle ineffective");
        ledger.clear();ledger.watch(victim,true,0);ledger.damage(victim,.5);check(ledger.observe(victim,true,0,1,true)==2,"Observed hurt/death fallback unavailable");
        ledger.clear();ledger.watch(victim,true,0);ledger.damage(victim,.5);check(ledger.observe(victim,true,0,1,false)==0,"Observer fallback leaks into local attribution");
        ledger.clear();ledger.watch(victim,false,0);check(ledger.observe(victim,true,2,1,true)==0,"Already-dead entity generated effect");
        ledger.clear();ledger.attack(victim,0);check(ledger.observe(victim,true,3,1,false)==0,"Known mob attacker incorrectly credited locally");
        ledger.clear();check(!ledger.contains(victim),"World change retains identity");
        KillOptions o=new KillOptions();KillPool pool=new KillPool();o.effect=KillOptions.Effect.Random;
        Set<Integer> seen=new HashSet<>();
        for(int i=0;i<10;i++){int slot=pool.spawn(o,i,0,0,i*.1,true);check(seen.add(pool.style[slot]),"Random repeats before cycling all effects");}
        check(pool.particles(false)==300&&pool.any(),"Pool does not cap at five effects / 300 particles");
        for(int i=0;i<5;i++)check(pool.x[i]>=5,"Oldest effect not dropped");
        check(pool.streak==10&&pool.size[4]<=1.45,"Streak scaling unbounded");
        int saved=pool.streak;pool.spawn(o,0,0,0,1.1,false);check(pool.streak==saved,"Other kills advance local streak");
        pool.expire(20);check(!pool.any()&&pool.particles(false)==0&&pool.streak==0,"Effects/streak do not expire");
        o.low=true;for(int i=0;i<20;i++)pool.spawn(o,0,0,0,20+i*.01,true);check(pool.particles(true)==120,"Low-quality pool exceeds cap");
        pool.clear();check(pool.particles(false)==0&&pool.streak==0&&!pool.any(),"World/death reset incomplete");
        check(pool.spawn(o,Double.NaN,0,0,0,true)==-1,"NaN effect origin accepted");
        for(int fps:new int[]{60,240}){
            double now=0;pool.clear();o.low=false;pool.spawn(o,0,0,0,0,true);
            for(int f=0;f<fps;f++){now+=1D/fps;pool.expire(now);}check(pool.any(),"Frame-dependent lifetime early expiry");
            for(int f=0;f<fps;f++){now+=1D/fps;pool.expire(now);}check(!pool.any(),"Frame-dependent lifetime late expiry");
        }
        check(KillPool.pulse(0)==0&&KillPool.pulse(.075)>.999F&&KillPool.pulse(.15)==0,"Camera pulse duration/shape wrong");
        check(KillPool.flash(.01)>KillPool.flash(.1)&&KillPool.flash(.25)==0,"Edge flash not brief/fading");
        check(KillOptions.tint(0,0x123456,0xFFEEDD,.2F,.4F,1)==0x123456,"Static picker ignored");
        check(KillOptions.tint(1,0,0,.2F,0,1)!=KillOptions.tint(1,0,0,.2F,0,2),"Rainbow frozen");
        check(KillOptions.tint(2,0xFF0000,0x0000FF,0,0,0)==0xFF0000,"Gradient first color wrong");
        check(KillOptions.tint(2,0xFF0000,0x0000FF,1,1,0)==0x0000FF,"Gradient second color wrong");
        for(KillOptions.Effect effect:KillOptions.Effect.values()){
            ComboSetting<KillOptions.Effect> setting=new ComboSetting<>("Effect",effect);com.google.gson.JsonObject json=setting.getConfigAsJson();setting.setMode(KillOptions.Effect.Shatter);setting.applyConfigFromJson(json);check(setting.getMode()==effect,"Effect codec failed");
        }
        for(KillOptions.Preset preset:KillOptions.Preset.values())if(preset!=KillOptions.Preset.Custom)check(preset.a!=preset.b,"Invalid existing color preset");
        KillCounter counter=new KillCounter();counter.increment();counter.increment();counter.enable();com.google.gson.JsonObject json=counter.getConfigAsJson();counter.resetToDefaults();counter.applyConfigFromJson(json);check(counter.total()==2&&counter.isToggled()&&counter.label().equals("Kills: 2"),"Counter config roundtrip failed");
        json.addProperty("totalKills",-42);counter.applyConfigFromJson(json);check(counter.total()==0,"Negative counter accepted");
        PickerRGBSetting picker=new PickerRGBSetting("Color",5,15,25,null);picker.setBlue(250);json=picker.getConfigAsJson();picker.resetToDefaults();picker.applyConfigFromJson(json);check(picker.getBlue()==250,"Shared color picker config failed");
        System.out.println("PASS: recent-hit/last-target/other-credit attribution, dedup, expiry, pre-existing deaths, 5/300 and low-quality caps, Random shuffle, streak/reset, 60/240Hz lifetime, flash/FOV pulse, colors/presets/settings and persistent counter");
    }
    static int[] pixels(){IntBuffer data=BufferUtils.createIntBuffer(W*H);GL11.glReadPixels(0,0,W,H,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);int[] out=new int[W*H];data.get(out);return out;}
    static long energy(int[] pixels){long sum=0;for(int p:pixels)sum+=(p>>16&255)+(p>>8&255)+(p&255);return sum;}
    static void clear(){GL11.glDepthMask(true);GL11.glClearDepth(1);GL11.glClearColor(.012F,.015F,.024F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);}
    static void camera(){GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();org.lwjgl.util.glu.GLU.gluPerspective(52,W/(float)H,.05F,128);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();org.lwjgl.util.glu.GLU.gluLookAt(6,4,9,0,1.6F,0,0,1,0);}
    static void draw(KillRenderer r,KillPool p,KillOptions o,double time){r.draw(p,o,time,0,0,0,.83205F,-.5547F,-.1185F,.9769F,-.1778F);}
    static void state(int t0,int t1,int buffer){
        check(GL11.glGetError()==0,"GL error");check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1,"Active texture leaked");check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==t1,"Lightmap binding leaked");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==t0&&GL11.glIsEnabled(GL11.GL_TEXTURE_2D),"Texture binding/enable leaked");GL13.glActiveTexture(GL13.GL_TEXTURE1);
        check(GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glIsEnabled(GL11.GL_CULL_FACE)&&GL11.glIsEnabled(GL11.GL_ALPHA_TEST),"Capabilities leaked");
        check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_DEPTH_FUNC)==GL11.GL_LEQUAL,"Depth state leaked");
        check(GL11.glGetInteger(GL11.GL_BLEND_SRC)==GL11.GL_SRC_ALPHA&&GL11.glGetInteger(GL11.GL_BLEND_DST)==GL11.GL_ONE_MINUS_SRC_ALPHA,"Blend leaked");
        check(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)==0&&GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)==buffer&&GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"Program/buffer/matrix leaked");
        FloatBuffer color=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);for(int i=0;i<4;i++)check(color.get(i)==1,"Tint leaked");
    }
    static void lightningRegression(KillRenderer renderer,String baseline)throws Exception{
        if(baseline==null)return;
        final String name="keystrokesmod.client.module.modules.render.killeffect.KillRenderer";
        final byte[] bytes;
        try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(baseline)){
            java.util.zip.ZipEntry entry=zip.getEntry(name.replace('.','/')+".class");if(entry==null)return;
            try(java.io.InputStream in=zip.getInputStream(entry)){bytes=org.apache.commons.io.IOUtils.toByteArray(in);}
        }
        ClassLoader loader=new ClassLoader(KillEffectTest.class.getClassLoader()){
            @Override protected Class<?> loadClass(String key,boolean resolve)throws ClassNotFoundException{
                if(!key.equals(name))return super.loadClass(key,resolve);
                Class<?> result=findLoadedClass(key);if(result==null)result=defineClass(key,bytes,0,bytes.length);if(resolve)resolveClass(result);return result;
            }
        };
        Class<?> type=loader.loadClass(name);Object previous=type.getConstructor().newInstance();
        boolean legacy;try{type.getField("LIGHTNING_HEIGHT");legacy=false;}catch(NoSuchFieldException ignored){legacy=true;}
        java.lang.reflect.Method draw=type.getMethod("draw",KillPool.class,KillOptions.class,double.class,double.class,double.class,double.class,float.class,float.class,float.class,float.class,float.class);
        KillPool pool=new KillPool();KillOptions options=new KillOptions();
        try{
            for(int style=0;style<10;style++){
                pool.clear();options.effect=KillOptions.Effect.values()[style];pool.spawn(options,0,0,0,0,true);
                for(double age:new double[]{.08,.23,.4}){
                    clear();long empty=energy(pixels());draw.invoke(previous,pool,options,age,0D,0D,0D,.83205F,-.5547F,-.1185F,.9769F,-.1778F);int[] old=pixels();
                    clear();draw(renderer,pool,options,age);int[] current=pixels();
                    if(style!=0)check(Arrays.equals(old,current),"Non-Lightning style changed: "+style);
                    else if(legacy)check(energy(current)-empty>(energy(old)-empty)*1.15,"Lightning brightness increase too small at "+age);
                }
            }
            check(KillRenderer.LIGHTNING_HEIGHT==7.5F&&KillRenderer.LIGHTNING_WIDTH>.014F&&KillRenderer.LIGHTNING_STEP_SECONDS<.105,"Lightning proportions/speed not increased");
            System.out.println("PASS: brighter Lightning versus installed baseline at three ages; all nine other effects pixel-identical");
        }finally{type.getMethod("dispose").invoke(previous);}
    }
    static void gl(String preview,String baseline)throws Exception{
        Pbuffer context=new Pbuffer(W,H,new PixelFormat().withDepthBits(24),null);context.makeCurrent();KillRenderer renderer=new KillRenderer();KillOverlay overlay=new KillOverlay();
        try{
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            GL11.glViewport(0,0,W,H);camera();int t0=GL11.glGenTextures(),t1=GL11.glGenTextures(),buffer=GL15.glGenBuffers();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,t0);GL11.glEnable(GL11.GL_TEXTURE_2D);GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,t1);GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,buffer);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,64,GL15.GL_STATIC_DRAW);
            GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glColor4f(1,1,1,1);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            KillOptions o=new KillOptions();KillPool pool=new KillPool();Set<Integer> hashes=new HashSet<>();
            java.awt.image.BufferedImage sheet=new java.awt.image.BufferedImage(W*2,H*5,java.awt.image.BufferedImage.TYPE_INT_RGB);
            clear();int[] blank=pixels();
            for(int effect=0;effect<10;effect++){
                pool.clear();o.effect=KillOptions.Effect.values()[effect];pool.spawn(o,0,0,0,0,true);clear();draw(renderer,pool,o,.23);state(t0,t1,buffer);int[] p=pixels();
                check(!Arrays.equals(blank,p)&&energy(p)>energy(blank)+500,"Effect invisible: "+effect);check(hashes.add(Arrays.hashCode(p)),"Effect identical: "+effect);
                check(renderer.mesh.vertices<KillMesh.MAX_VERTICES,"Geometry cap exceeded");
                for(int y=0;y<H;y++)sheet.setRGB((effect%2)*W,(effect/2)*H+H-y-1,W,1,p,y*W,W);
                java.awt.Graphics2D g=sheet.createGraphics();g.setColor(java.awt.Color.WHITE);g.drawString(o.effect.toString(),(effect%2)*W+10,(effect/2)*H+18);g.dispose();
                clear();GL11.glClearDepth(0);GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);draw(renderer,pool,o,.23);check(Arrays.equals(blank,pixels()),"Effect draws through blocks: "+effect);
                clear();draw(renderer,pool,o,3);check(Arrays.equals(blank,pixels()),"Expired effect still drawn");
            }
            javax.imageio.ImageIO.write(sheet,"png",new java.io.File(preview));
            lightningRegression(renderer,baseline);
            o.effect=KillOptions.Effect.Firework;o.byEffect=false;o.mode=KillOptions.ColorMode.Static;o.color=0xFF1010;
            pool.clear();pool.spawn(o,0,0,0,0,true);clear();draw(renderer,pool,o,.4);int[] red=pixels();o.color=0x1050FF;pool.clear();pool.spawn(o,0,0,0,0,true);clear();draw(renderer,pool,o,.4);check(!Arrays.equals(red,pixels()),"Manual color ineffective");
            o.intensity=0;clear();draw(renderer,pool,o,.4);check(Arrays.equals(blank,pixels()),"Zero intensity still draws");o.intensity=.8F;
            clear();overlay.draw(W,H,0xB0CFFF,.2F,"+1 Kill  Тест",1,"Kills: 12");state(t0,t1,buffer);check(energy(pixels())>energy(blank)+5000,"HUD fallback/edge flash missing");
            clear();overlay.draw(W,H,0xFFFFFF,.2F,null,0,null);int[] flash=pixels();check(flash[H/2*W+W/2]==blank[H/2*W+W/2],"Edge flash covers center of screen");
            // Stress all styles at the complete five-slot particle budget.
            for(KillOptions.Effect effect:KillOptions.Effect.values())if(effect!=KillOptions.Effect.Random){
                o.effect=effect;pool.clear();for(int i=0;i<5;i++)pool.spawn(o,(i-2)*.3,0,0,0,true);
                clear();draw(renderer,pool,o,.25);check(pool.particles(false)==300&&renderer.mesh.vertices<KillMesh.MAX_VERTICES,"Rapid kill geometry overflow");state(t0,t1,buffer);
            }
            o.effect=KillOptions.Effect.Shatter;pool.clear();for(int i=0;i<5;i++)pool.spawn(o,(i-2)*.3,0,0,0,true);
            for(int f=0;f<80;f++){clear();draw(renderer,pool,o,.3);}GL11.glFinish();long begin=System.nanoTime();
            for(int f=0;f<300;f++){clear();draw(renderer,pool,o,.3);}GL11.glFinish();System.out.printf(java.util.Locale.ROOT,"INFO: five effects / 300 cube fragments offscreen: %.3f ms/draw (not full-game FPS)%n",(System.nanoTime()-begin)/3e8);
            renderer.dispose();clear();draw(renderer,pool,o,.3);check(!Arrays.equals(blank,pixels()),"GPU recreation failed");state(t0,t1,buffer);
            System.out.println("PASS: all ten real GL effects, distinct silhouettes, depth occlusion, expiry, colors/intensity, Unicode popup/edge-only flash, worst-case batching and full GL-state/resource restoration");
        }finally{renderer.dispose();overlay.dispose();context.destroy();}
    }
    public static void main(String[] args)throws Exception{if(args.length>0&&args[0].equals("gl"))gl(args[1],args.length>2?args[2]:null);else cpu();}
}
