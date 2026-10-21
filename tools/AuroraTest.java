import keystrokesmod.client.module.modules.render.aurora.*;
import java.util.Arrays;
import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

public final class AuroraTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final AuroraSimulation.Roof OPEN=(x,z)->0, ROOF=(x,z)->10000;
    private static AuroraSimulation run(int fps,int snow,AuroraSimulation.Roof roof){
        AuroraSimulation sim=new AuroraSimulation();
        for(int i=0;i<fps*3;i++)sim.advance(1.0/fps,true,snow,1,.3F,.6F,0,70,0,roof);
        return sim;
    }
    private static void cpu(){
        AuroraSimulation a=run(60,2,OPEN),b=run(240,2,OPEN);
        check(Math.abs(a.phase-b.phase)<1e-8&&a.fade==1&&b.fade==1,"Framerate-dependent clock/fade");
        check(a.x.length==300&&a.density==300,"Particle cap/density");
        for(int i=0;i<300;i++)check(Double.isFinite(a.x[i])&&a.size[i]>0&&a.alpha[i]>=0&&a.alpha[i]<=1,"Invalid particle");
        AuroraSimulation indoor=run(240,2,ROOF);
        for(int i=0;i<300;i++)check(!indoor.exposed[i]&&indoor.alpha[i]==0,"Indoor snow visible");
        for(int i=0;i<72;i++)a.advance(1.0/240,false,2,1,.3F,.6F,0,70,0,OPEN);
        check(a.fade<.0001&&a.snowFade<.0001,"Disable fade did not finish in 300ms");
        a=run(240,1,OPEN);for(int i=0;i<72;i++)a.advance(1.0/240,true,0,1,.3F,.6F,0,70,0,OPEN);
        check(a.fade==1&&a.snowFade<.0001,"Snow Off affected sky or did not fade");
        a=new AuroraSimulation();a.advance(Double.NaN,true,0,Float.NaN,.3F,1,0,70,0,OPEN);
        check(Double.isFinite(a.phase),"NaN clock");
        for(int count=0;count<6;count++){
            float[] mesh=AuroraSkyGeometry.curtain(count);
            check(mesh.length==AuroraSkyGeometry.COLUMNS*AuroraSkyGeometry.BANDS*4*7,"Mesh size");for(float value:mesh)check(Float.isFinite(value),"NaN mesh");
            check(Math.abs(mesh[1]-mesh[(AuroraSkyGeometry.COLUMNS/2)*AuroraSkyGeometry.BANDS*4*7+1])>.1,"Lower edge is flat");
        }
        check(Arrays.equals(AuroraSkyGeometry.stars(),AuroraSkyGeometry.stars()),"Star field regenerates nondeterministically");
        check(Arrays.equals(AuroraSkyGeometry.milkyPixels(),AuroraSkyGeometry.milkyPixels()),"Galaxy texture is not cached/deterministic");
        AuroraOptions options=new AuroraOptions();options.curtains=6;options.lowQuality=true;
        check(AuroraRenderer.effectiveCurtains(options)==3,"Low quality does not halve curtains");
        AuroraLightmap light=new AuroraLightmap();int[] pixels=new int[256];Arrays.fill(pixels,0xFFA0B0C0);int[] original=pixels.clone();
        check(light.apply(pixels,.6F,.03F,0x44FFAA)&&!Arrays.equals(original,pixels),"Cold/reflection inactive");
        check(light.restore(pixels)&&Arrays.equals(original,pixels),"Lightmap restore not exact");
        check(!light.apply(pixels,0,0,0)&&Arrays.equals(original,pixels),"OFF changes lightmap");
        float angle=AuroraSimulation.lockedAngle(18000,0);check(angle>=0&&angle<=1,"Locked time range");
        clouds();
        shootingStars();
        constellations();
        overcast();
        System.out.println("PASS: 60/240Hz timing; 300ms style/disable/snow fades; 300-particle cap; indoor roofs; cached deterministic meshes; cold lightmap exact restoration");
    }
    private static void overcast(){
        for(int i=0;i<20;i++){double u=i*.073,v=i*.117;check(Math.abs(OvercastNoise.value(u,v)-OvercastNoise.value(u+1,v))<.00001&&Math.abs(OvercastNoise.value(u,v)-OvercastNoise.value(u,v+1))<.00001,"Cloud texture not seamless");}
        check(Arrays.equals(OvercastNoise.pixels(),OvercastNoise.pixels()),"Overcast noise changes/regenerates nondeterministically");
        for(int fps:new int[]{60,240}){
            SkyStyleMix mix=new SkyStyleMix();mix.reset(1);
            for(int i=0;i<fps*3/10;i++){mix.advance(2,1D/fps,.3F);check(mix.amount(0)==0&&Math.abs(mix.amount(1)+mix.amount(2)-1)<.00001,"Unrelated aurora flashes during constellation/dusk fade");}
            check(mix.amount(2)>.9999,"Dusk crossfade duration wrong");
        }
        AuroraOptions o=new AuroraOptions();AuroraLightmap light=new AuroraLightmap();int[] p=new int[256];Arrays.fill(p,0xFFC0C0C0);int[] original=p.clone();
        check(light.apply(p,0,0,0,.5F,o.duskColor,o.duskDarkness)&&!Arrays.equals(p,original),"Dusk world tint ineffective");
        check(light.restore(p)&&Arrays.equals(p,original),"Dusk leaks into next hand/GUI pass");
        check(!light.apply(p,0,0,0,0,o.duskColor,o.duskDarkness)&&Arrays.equals(p,original),"Dusk tint affects other styles");
        Arrays.fill(p,0xFF40DC50);int greenBefore=(p[0]>>8&255)-(p[0]&255);light.apply(p,0,0,0,.5F,o.duskColor,o.duskDarkness);
        check((p[0]>>8&255)-(p[0]&255)<greenBefore*.65F,"Dusk lighting still has excessive green saturation");light.restore(p);
        System.out.println("PASS: seamless deterministic overcast texture, 60/240Hz three-way fades with no unrelated-style flashes, world tint and exact hand/world lightmap restoration");
    }
    private static void constellations(){
        ConstellationGeometry a=new ConstellationGeometry(),b=new ConstellationGeometry();
        check(Arrays.equals(a.points,b.points)&&Arrays.equals(a.lines(),b.lines()),"Constellation topology not deterministic/cached");
        for(float v:a.lines())check(Float.isFinite(v),"Invalid constellation vertex");
        for(int nodes=2;nodes<=ConstellationGeometry.NODES;nodes++){
            int edges=ConstellationGeometry.edgeCount(nodes);boolean[] linked=new boolean[nodes];linked[0]=true;
            for(int i=0;i<edges;i++){
                check(a.from[i]<nodes&&a.to[i]<a.from[i],"Density prefix loses connected endpoints");
                linked[a.from[i]]|=linked[a.to[i]];
            }
            for(boolean connected:linked)check(connected,"Disconnected constellation point");
        }
        for(int fps:new int[]{60,240}){
            SkyStyleBlend blend=new SkyStyleBlend();blend.reset(false);
            for(int i=0;i<fps*3/10;i++)blend.advance(true,1D/fps,.3F);
            check(blend.amount()>.9999,"Constellation crossfade not 300ms");
            for(int i=0;i<fps*3/20;i++)blend.advance(false,1D/fps,.3F);
            check(Math.abs(blend.amount()-.5)<.00001,"Sky crossfade depends on FPS");
            blend.advance(false,1,.3F);check(blend.amount()==0,"Aurora return fade stuck");
        }
        keystrokesmod.client.module.setting.impl.ComboSetting<keystrokesmod.client.module.modules.render.SkyChanger.Style> style=new keystrokesmod.client.module.setting.impl.ComboSetting<>("Style",keystrokesmod.client.module.modules.render.SkyChanger.Style.AntarcticAurora);
        style.setMode(keystrokesmod.client.module.modules.render.SkyChanger.Style.Constellation);com.google.gson.JsonObject saved=style.getConfigAsJson();style.resetToDefaults();style.applyConfigFromJson(saved);
        check(style.getMode()==keystrokesmod.client.module.modules.render.SkyChanger.Style.Constellation,"Constellation style config failed");
        check(keystrokesmod.client.module.modules.render.SkyChanger.Style.AntarcticAurora.toString().equals("Antarctic Aurora"),"Old sky config label changed");
        System.out.println("PASS: deterministic connected constellation graph at every density, 60/240Hz 300ms style fades, new/legacy style config labels");
    }
    private static void shootingStars(){
        int hits=0;
        for(long slot=0;slot<5000;slot++){
            check(!AuroraShootingStar.occurs(slot,0),"0% shooting chance still spawns");
            check(AuroraShootingStar.occurs(slot,1),"100% shooting chance missed a slot");
            if(AuroraShootingStar.occurs(slot,.35F))hits++;
            check(!AuroraShootingStar.occurs(slot,.2F)||AuroraShootingStar.occurs(slot,.8F),"Chance is not monotonic");
        }
        check(hits>1550&&hits<1950,"Shooting chance distribution incorrect: "+hits);
        java.util.Set<Long> first=null;
        for(int fps:new int[]{60,240}){
            AuroraShootingStar star=new AuroraShootingStar();java.util.Set<Long> events=new java.util.HashSet<>();
            for(int i=0;i<fps*240;i++){
                double time=i/(double)fps;
                if(star.sample(time,.35F,8)){
                    events.add((long)(time/8));check(star.alpha>0&&star.alpha<=1&&star.progress>0&&star.progress<1,"Invalid meteor fade");
                }
            }
            check(events.size()>3,"No shooting stars appear");
            if(first==null)first=events;else check(first.equals(events),"Shooting chance changes with FPS");
        }
        AuroraShootingStar star=new AuroraShootingStar();
        check(!star.sample(Double.NaN,1,8)&&!star.sample(-1,1,8),"Invalid clock spawns meteor");
        for(float interval:new float[]{3,8,60})for(long slot=0;slot<15;slot++){
            double start=AuroraShootingStar.startsAt(slot,interval);
            check(star.sample(start+.3,1,interval),"Interval setting lost a meteor");
            check(!star.sample(start+1.1,1,interval),"Meteor never fades");
            check(!star.sample(start+.3,0,interval),"Chance toggle ignored");
        }
        System.out.println("PASS: shooting stars 0/35/100% chance, per-interval distribution, identical 60/240Hz events, adjustable interval, finite clocks and soft lifetime");
    }
    private static final class TestProvider extends net.minecraft.world.WorldProvider {
        public String getDimensionName(){return "test";}
        public String getInternalNameSuffix(){return "test";}
    }
    private static void clouds(){
        AuroraClouds clouds=new AuroraClouds();TestProvider first=new TestProvider(), second=new TestProvider();
        net.minecraftforge.client.IRenderHandler original=new net.minecraftforge.client.IRenderHandler(){
            public void render(float partial,net.minecraft.client.multiplayer.WorldClient world,net.minecraft.client.Minecraft mc){}
        };
        first.setCloudRenderer(original);
        clouds.update(first,true);check(first.getCloudRenderer()!=original,"Cloud hiding not installed");
        first.getCloudRenderer().render(0,null,null); // No draw, no GL context or Minecraft required.
        clouds.update(first,false);check(first.getCloudRenderer()==original,"Toggle did not restore custom clouds");
        clouds.update(first,true);clouds.update(second,true);
        check(first.getCloudRenderer()==original&&second.getCloudRenderer()!=null,"World change left cloud override behind");
        clouds.restore();check(second.getCloudRenderer()==null,"Vanilla clouds were not restored");
        clouds.update(first,true);first.setCloudRenderer(null);clouds.restore();
        check(first.getCloudRenderer()==null,"Clobbered a later renderer from another mod");
        System.out.println("PASS: Hide clouds draws nothing; toggle/world unload restore vanilla or custom clouds; other mods respected");
    }
    private static int[] pixels(){IntBuffer data=BufferUtils.createIntBuffer(512*320);GL11.glReadPixels(0,0,512,320,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);int[] result=new int[data.remaining()];data.get(result);return result;}
    private static void clear(){GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);}
    private static void clean(){check(GL11.glGetError()==GL11.GL_NO_ERROR,"GL error");}
    private static void hudAfterPasses(){
        int light=GL11.glGenTextures();int[] lightPixels=new int[256];Arrays.fill(lightPixels,0xFF7799AA);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,light);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,16,16,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,(IntBuffer)null);
        int icons=GL11.glGenTextures(),font=GL11.glGenTextures();
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        net.minecraft.client.renderer.GlStateManager.bindTexture(icons);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
        net.minecraft.client.renderer.GlStateManager.bindTexture(font);
        new AuroraLightmap().uploadPixels(light,lightPixels);
        check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1&&GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==font,"Upload lost lightmap unit/binding");
        net.minecraft.client.renderer.GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==icons,"Upload leaked onto icons binding");
        check(GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER)==GL11.GL_LINEAR,"Upload changed caller sampler");
        // Simulate the desync reported with patched texture upload paths. A cache-only bind skips this.
        GL11.glBindTexture(GL11.GL_TEXTURE_2D,font);
        AuroraGLState.restoreDefaultState(icons);
        net.minecraft.client.renderer.GlStateManager.bindTexture(icons);
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==icons,"Cache and actual binding disagree before crosshair");
        check(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)&&GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"HUD default state unsafe");
        net.minecraft.client.renderer.GlStateManager.bindTexture(font);
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==font,"Next font bind skipped");
        IntBuffer glyph=BufferUtils.createIntBuffer(16);
        for(int y=0;y<4;y++)for(int x=0;x<4;x++)glyph.put(x==1||y==1?0xFFFFFFFF:0x00FFFFFF);glyph.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,4,4,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,glyph);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL20.glUseProgram(0);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,512,0,320,-1,1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();clear();
        GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2f(0,0);GL11.glVertex2f(128,96);GL11.glTexCoord2f(1,0);GL11.glVertex2f(384,96);
        GL11.glTexCoord2f(1,1);GL11.glVertex2f(384,224);GL11.glTexCoord2f(0,1);GL11.glVertex2f(128,224);GL11.glEnd();
        int[] rendered=pixels();check((rendered[112*512+144]&0xFFFFFF)==0,"Transparent glyph became a white rectangle");
        check((rendered[112*512+208]&0xFFFFFF)>0xE0E0E0,"Textured cross/glyph did not draw");
        GL11.glDeleteTextures(light);GL11.glDeleteTextures(icons);GL11.glDeleteTextures(font);clean();
        System.out.println("PASS: real lightmap upload preserves both units/samplers; desynchronized texture cache repaired; transparent HUD glyph/cross pixels remain correct");
    }
    private static void restored(int unit0,int unit1,int buffer){
        check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1,"Active texture leaked");
        check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==unit1,"Lightmap binding leaked");
        GL13.glActiveTexture(GL13.GL_TEXTURE0);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==unit0,"Atlas binding leaked");GL13.glActiveTexture(GL13.GL_TEXTURE1);
        check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),"Depth mask leaked");
        check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_FOG),"World state leaked");
        check(GL11.glGetInteger(GL11.GL_BLEND_SRC)==GL11.GL_SRC_ALPHA&&GL11.glGetInteger(GL11.GL_BLEND_DST)==GL11.GL_ONE_MINUS_SRC_ALPHA,"Blend leaked");
        check(GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)==buffer,"VBO leaked");
        check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_PROJECTION,"Matrix mode leaked");
        FloatBuffer color=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);
        check(Math.abs(color.get(0)-.2F)<.001&&Math.abs(color.get(3)-.8F)<.001,"Color leaked");clean();
    }
    private static void gl(String output)throws Exception{
        Pbuffer pbuffer=new Pbuffer(512,320,new PixelFormat().withDepthBits(24),null);pbuffer.makeCurrent();
        AuroraRenderer renderer=new AuroraRenderer();
        try{
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;
            net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            GL11.glViewport(0,0,512,320);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GLU.gluPerspective(85,512F/320,.05F,128);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glRotatef(-18,1,0,0);
            int tex0=GL11.glGenTextures(),tex1=GL11.glGenTextures(),buffer=GL15.glGenBuffers();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex1);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,buffer);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,64,GL15.GL_STATIC_DRAW);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glDepthMask(true);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(.2F,.3F,.4F,.8F);GL11.glMatrixMode(GL11.GL_PROJECTION);
            AuroraSimulation sim=run(240,2,OPEN);clear();
            renderer.sky(sim,1,4,0,0x66EEAA,.7F,false,.06F,.10F,.15F);restored(tex0,tex1,buffer);
            int[] first=pixels();long energy=0;for(int pixel:first)energy+=(pixel>>8&255)+(pixel&255);check(energy>100000,"Sky is blank");
            if(output!=null){java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(512,320,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<320;y++)image.setRGB(0,319-y,512,1,first,y*512,512);javax.imageio.ImageIO.write(image,"png",new java.io.File(output));}
            sim.phase+=30;clear();renderer.sky(sim,1,4,0,0x66EEAA,.7F,false,.06F,.10F,.15F);check(!Arrays.equals(first,pixels()),"Curtains and stars not animated");
            clear();renderer.sky(sim,1,4,1,0x66EEAA,.7F,false,.06F,.10F,.15F);int[] pink=pixels();
            clear();renderer.sky(sim,1,4,0,0x66EEAA,.7F,false,.06F,.10F,.15F);check(!Arrays.equals(pink,pixels()),"Palette setting ignored");
            clear();renderer.sky(sim,0,4,0,0x66EEAA,.7F,false,.06F,.10F,.15F);int[] zero=pixels();
            clear();renderer.sky(sim,1,4,0,0x66EEAA,.7F,false,.06F,.10F,.15F);check(!Arrays.equals(zero,pixels()),"Intensity setting ignored");
            GL11.glClearDepth(0);clear();int[] beforeSnow=pixels();
            renderer.snow(sim,0,70,0,1,0,0,1,0);check(Arrays.equals(beforeSnow,pixels()),"Snow ignores depth/roofs");restored(tex0,tex1,buffer);
            GL11.glClearDepth(1);clear();renderer.snow(sim,0,70,0,1,0,0,1,0);check(!Arrays.equals(beforeSnow,pixels()),"Snow not rendered");restored(tex0,tex1,buffer);
            renderer.vignette(1,512,320);restored(tex0,tex1,buffer);
            AuroraOptions opt=new AuroraOptions();opt.curtains=6;
            clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] full=pixels();
            opt.starField=false;clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] withoutStars=pixels();
            check(!Arrays.equals(full,withoutStars),"Stars toggle ineffective");opt.starField=true;opt.stars=0;
            clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(Arrays.equals(withoutStars,pixels()),"Star density zero differs from Stars off");opt.stars=.7F;
            opt.folds=0;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(full,pixels()),"Fold strength ignored");opt.folds=1;
            opt.milkyWay=false;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(full,pixels()),"Milky Way toggle ignored");opt.milkyWay=true;
            opt.moon=false;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(full,pixels()),"Moon toggle ignored");opt.moon=true;
            opt.bloom=false;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(full,pixels()),"Bloom toggle ignored");opt.bloom=true;
            opt.cloudMode=2;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(full,pixels()),"Wispy clouds toggle ignored");opt.cloudMode=0;
            opt.lowQuality=true;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(full,pixels()),"Low quality ignored");
            restored(tex0,tex1,buffer);
            // Aim at a known accepted meteor so the real production pass is exercised.
            AuroraShootingStar meteor=new AuroraShootingStar();sim.time=AuroraShootingStar.startsAt(0,8)+.35;
            check(meteor.sample(sim.time,1,8),"Meteor fixture missing");
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
            GL11.glRotatef(-35,1,0,0);GL11.glRotatef(-meteor.yaw,0,1,0);GL11.glMatrixMode(GL11.GL_PROJECTION);
            opt.shooting=true;opt.shootingChance=0;clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] noMeteor=pixels();
            opt.shootingChance=1;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(noMeteor,pixels()),"Shooting star chance did not change production pixels");
            restored(tex0,tex1,buffer);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);
            System.out.println("PASS: Stars/density switches and 0/100% shooting-star chance change production pixels; meteor matrix/GL state restored");
            System.out.println("PASS: folds, bloom, Milky Way, moon, wispy clouds and low-quality settings change production pixels");
            for(int i=0;i<240;i++){sim.phase+=1.0/240;renderer.sky(sim,1,5,i%4,0x66AAFF,1,true,.06F,.10F,.15F);}
            restored(tex0,tex1,buffer);constellationGl(renderer,sim,tex0,tex1,buffer,output);overcastGl(renderer,sim,tex0,tex1,buffer,output);renderer.dispose();clean();hudAfterPasses();
            System.out.println("PASS: production shaders/VBOs at 240 simulated frames; palette/intensity/motion; snow depth occlusion; GL blend/color/depth/lightmap/atlas/VBO/matrix restoration");
            System.out.println("Preview: "+output);
        }finally{renderer.dispose();pbuffer.destroy();}
    }
    private static void overcastGl(AuroraRenderer renderer,AuroraSimulation sim,int tex0,int tex1,int buffer,String preview)throws Exception{
        AuroraOptions opt=new AuroraOptions();opt.overcast=true;opt.duskMix=1;opt.shooting=false;
        clear();renderer.sky(sim,opt,0,0,0);int[] first=pixels();check(energy(first)>100000,"Overcast dome blank");restored(tex0,tex1,buffer);
        int highlights=0,min=255,max=0;long blue=0;
        for(int i=first.length/2;i<first.length;i++){int b=first[i]&255;blue+=b;min=Math.min(min,b);max=Math.max(max,b);if(b>70)highlights++;}
        check(highlights>1000&&max-min>30&&blue/(double)(first.length/2)>35,"Cloud bands too dark or flat");
        for(int c=0;c<3;c++){int expected=Math.round(OvercastNoise.horizon(opt,c)*255);for(int x=0;x<512;x++)check(Math.abs((first[x]>>((2-c)*8)&255)-expected)<=1,"Low mist does not match fog horizon");}
        if(preview!=null){java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(512,320,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<320;y++)image.setRGB(0,319-y,512,1,first,y*512,512);javax.imageio.ImageIO.write(image,"png",new java.io.File(preview.replace("aurora-preview","overcast-preview")));}
        opt.duskDarkness=1;clear();renderer.sky(sim,opt,0,0,0);check(energy(pixels())<energy(first),"Dusk darkness ineffective");opt.duskDarkness=.55F;
        opt.duskColor=0x803020;clear();renderer.sky(sim,opt,0,0,0);check(!Arrays.equals(first,pixels()),"Dusk color ineffective");opt.duskColor=0x2D2156;
        opt.duskScale=2.8F;clear();renderer.sky(sim,opt,0,0,0);check(!Arrays.equals(first,pixels()),"Dusk scale ineffective");opt.duskScale=1.3F;
        opt.duskContrast=2.4F;clear();renderer.sky(sim,opt,0,0,0);check(!Arrays.equals(first,pixels()),"Cloud contrast ineffective");opt.duskContrast=1.1F;
        opt.duskGlow=2;clear();renderer.sky(sim,opt,0,0,0);check(!Arrays.equals(first,pixels()),"Horizon glow ineffective");opt.duskGlow=.5F;
        sim.time+=20;clear();renderer.sky(sim,opt,0,0,0);check(!Arrays.equals(first,pixels()),"Overcast cloud layers stationary");
        for(int i=0;i<240;i++){sim.time+=1D/240;renderer.sky(sim,opt,0,0,0);}restored(tex0,tex1,buffer);
        clear();renderer.sky(sim,opt,0,0,0);int[] noStars=pixels();opt.duskStars=true;
        clear();renderer.sky(sim,opt,0,0,0);check(!Arrays.equals(noStars,pixels()),"Dusk stars toggle ineffective");opt.duskStars=false;
        sim.time+=13.01;boolean flashed=false;
        for(int i=0;i<40&&!flashed;i++){
            sim.time+=17;opt.duskLightning=false;clear();renderer.sky(sim,opt,0,0,0);int[] quiet=pixels();
            opt.duskLightning=true;clear();renderer.sky(sim,opt,0,0,0);flashed=!Arrays.equals(quiet,pixels());
        }
        check(flashed,"Optional distant lightning never flickers");restored(tex0,tex1,buffer);
        System.out.println("PASS: production Overcast Dusk dome/shader/texture; tint, darkness, contrast, scale and horizon glow change pixels; moving cloud layers; 240 simulated frames and exact GL restoration");
    }
    private static long energy(int[] p){long sum=0;for(int v:p)sum+=(v>>16&255)+(v>>8&255)+(v&255);return sum;}
    private static void constellationGl(AuroraRenderer renderer,AuroraSimulation sim,int tex0,int tex1,int buffer,String preview)throws Exception{
        AuroraOptions opt=new AuroraOptions();opt.constellation=true;opt.constellationMix=1;opt.shooting=false;opt.constellationDensity=.8F;
        clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] first=pixels();check(energy(first)>20000,"Constellation sky blank");
        for(int v:first)check(Math.abs((v>>16&255)-(v>>8&255))<=1&&Math.abs((v>>8&255)-(v&255))<=1,"Constellation sky not black/white");
        restored(tex0,tex1,buffer);
        if(preview!=null){java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(512,320,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int y=0;y<320;y++)image.setRGB(0,319-y,512,1,first,y*512,512);javax.imageio.ImageIO.write(image,"png",new java.io.File(preview.replace("aurora-preview","constellation-preview")));}
        opt.constellationDensity=0;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(energy(pixels())==0,"Black sky retains aurora/blue gradient/moon");opt.constellationDensity=.8F;
        opt.constellationBrightness=0;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(energy(pixels())==0,"Constellation brightness setting ineffective");opt.constellationBrightness=1;
        opt.constellationLines=false;clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] nodes=pixels();check(energy(nodes)>0&&!Arrays.equals(first,nodes),"White point/line toggles ineffective");
        opt.starField=false;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(energy(pixels())==0,"Hidden points/lines still render");opt.constellationLines=true;
        clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(energy(pixels())>0,"Constellation lines missing");opt.starField=true;
        sim.time+=10;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(first,pixels()),"Constellation network not moving");
        opt.constellationMix=.5F;clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] middle=pixels();check(!Arrays.equals(first,middle),"Sky style crossfade ineffective");opt.constellationMix=1;
        opt.constellationSpeed=.1F;sim.time+=.5;clear();renderer.sky(sim,opt,.06F,.10F,.15F);int[] slow=pixels();
        opt.constellationSpeed=3;sim.time+=.5;clear();renderer.sky(sim,opt,.06F,.10F,.15F);check(!Arrays.equals(slow,pixels()),"Constellation speed not moving");
        opt.lowQuality=true;for(int i=0;i<240;i++){sim.time+=1D/240;renderer.sky(sim,opt,0,0,0);}restored(tex0,tex1,buffer);
        System.out.println("PASS: production constellation shader on fully black sky, white-only connected lines/points, motion/brightness/density/toggles/style blend, 240 simulated frames, exact GL state restoration");
    }
    public static void main(String[]args)throws Exception{if(args.length>0&&args[0].equals("gl"))gl(args.length>1?args[1]:null);else cpu();}
}
