package keystrokesmod.client.module.modules.render.aurora;

import keystrokesmod.client.module.modules.render.SkyChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.logging.*;

/** Owns provider attachment, event lifetime, fades and bounded roof cache. */
public final class AuroraController extends IRenderHandler implements AuroraSimulation.Roof {
    private final SkyChanger module;
    public final AuroraSimulation simulation = new AuroraSimulation();
    private final AuroraRenderer renderer = new AuroraRenderer();
    private final AuroraClouds clouds = new AuroraClouds();
    private final SkyStyleMix styleBlend=new SkyStyleMix();
    private final PetalSimulation petals=new PetalSimulation();
    private final PetalRenderer petalRenderer=new PetalRenderer();
    private final boolean[] failed = new boolean[6];
    private final BlockPos.MutableBlockPos roofPos = new BlockPos.MutableBlockPos();
    private final int[] heights = new int[33*33];
    private WorldClient world;
    private IRenderHandler original;
    private boolean registered, attached;
    private long lastNanos;
    private double roofsAt;
    private int roofX=Integer.MIN_VALUE, roofZ;
    private float fogR=.06F,fogG=.10F,fogB=.15F;
    private AuroraWind wind;
    public AuroraController(SkyChanger module) { this.module=module; }
    public void start() {
        if(registered) return;
        registered=true; lastNanos=0;
        java.util.Arrays.fill(failed,false);
        MinecraftForge.EVENT_BUS.register(this); FMLCommonHandler.instance().bus().register(this);
    }
    public void beginFadeOut() { /* Keep listeners until both world and snow fades finish. */ }
    private void fail(int element, Throwable error) {
        if(failed[element]) return;
        failed[element]=true;
        Logger.getLogger("Phantom/SkyChanger").log(Level.WARNING,"Aurora element "+element+" disabled after failure",error);
    }
    private boolean surface() { return world!=null && world.provider.getDimensionId()==0; }
    private void detach(Minecraft mc) {
        clouds.restore();
        if(attached && world!=null && world.provider.getSkyRenderer()==this) world.provider.setSkyRenderer(original);
        attached=false;original=null;
        if(wind!=null) { wind.finish(); mc.getSoundHandler().stopSound(wind); wind=null; }
    }
    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.START) return;
        Minecraft mc=Minecraft.getMinecraft();
        try {
            module.snapshot();
            if(world!=mc.theWorld) {
                detach(mc); world=mc.theWorld; simulation.reset();petals.reset(); lastNanos=0; roofX=Integer.MIN_VALUE;styleBlend.reset(styleIndex(module.options));
            }
            if(world==null || mc.getRenderViewEntity()==null) {
                if(!module.isEnabled()) stop(mc);
                return;
            }
            long now=System.nanoTime(); double dt=lastNanos==0?0:(now-lastNanos)/1e9; lastNanos=now;
            AuroraOptions opt=module.options;
            if(opt.aurora)styleBlend.advance(styleIndex(opt),dt,opt.art.style>0||opt.art.mix()>0?.3F:opt.fadeSeconds);
            opt.constellationMix=styleBlend.amount(1);opt.duskMix=styleBlend.amount(2);
            opt.art.nebulaMix=styleBlend.amount(3);opt.art.sunsetMix=styleBlend.amount(4);opt.art.cherryMix=styleBlend.amount(5);
            boolean allowed=surface() || opt.otherSkies;
            boolean active=module.isEnabled() && opt.aurora && allowed && !failed[0];
            Entity camera=mc.getRenderViewEntity();
            double cx=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*event.renderTickTime;
            double cy=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*event.renderTickTime+camera.getEyeHeight();
            double cz=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*event.renderTickTime;
            if(surface() && ((!opt.overcast&&!opt.constellation&&opt.art.style==0&&opt.snow>0) || simulation.snowFade>0||opt.art.cherryMix>0||petals.fade>.001F)) cacheRoofs(cx,cz);
            simulation.advance(dt,active,surface()&&!failed[1]&&!opt.constellation&&!opt.overcast&&opt.art.style==0?opt.snow:0,opt.speed,opt.art.style>0||opt.art.mix()>0?.3F:opt.fadeSeconds,opt.wind,cx,cy,cz,this);
            if(surface()&&(opt.art.cherryMix>0||petals.fade>.001F))petals.advance(dt,simulation.time,active&&!failed[5],opt.art,opt.lowQuality,cx,cy,cz,this);
            if((active || simulation.fade>0) && !attached) {
                original=world.provider.getSkyRenderer(); world.provider.setSkyRenderer(this); attached=true;
            }
            if(!active && simulation.fade<=0 && attached) detach(mc);
            // Aurora supplies its own optional high wisps; never allow vanilla block clouds in front.
            clouds.update(surface()?world.provider:null,(module.isEnabled()&&opt.hideClouds)||simulation.fade>0);
            if(surface() && opt.sound && simulation.fade>0 && opt.constellationMix+opt.duskMix+opt.art.mix()<.999F && !failed[4]) {
                if(wind==null) { wind=new AuroraWind(); mc.getSoundHandler().playSound(wind); }
                wind.strength=simulation.fade*(.65F+simulation.gust*.35F)*(1-opt.constellationMix-opt.duskMix-opt.art.mix());
            } else if(wind!=null) { wind.finish();mc.getSoundHandler().stopSound(wind);wind=null; }
            if(!module.isEnabled() && simulation.fade<=0 && simulation.snowFade<=0) stop(mc);
        } catch(RuntimeException|LinkageError error) { fail(0,error); detach(mc); simulation.reset(); stop(mc); }
    }
    private void stop(Minecraft mc) {
        detach(mc); simulation.reset();petals.reset(); world=null;
        if(registered) { MinecraftForge.EVENT_BUS.unregister(this); FMLCommonHandler.instance().bus().unregister(this); registered=false; }
        renderer.dispose();
        petalRenderer.dispose();
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event) {
        if(event.world==world) { detach(Minecraft.getMinecraft());world=null;simulation.reset();petals.reset(); }
    }
    private void cacheRoofs(double cx,double cz) {
        int x=(int)Math.floor(cx),z=(int)Math.floor(cz);
        if(roofX==x && roofZ==z && simulation.time<roofsAt) return;
        roofX=x;roofZ=z;roofsAt=simulation.time+.25;
        for(int dz=-16;dz<=16;dz++) for(int dx=-16;dx<=16;dx++) {
            int px=x+dx,pz=z+dz;
            roofPos.set(px,128,pz);
            heights[(dz+16)*33+dx+16]=world.isBlockLoaded(roofPos,false)
                    ?world.getChunkFromChunkCoords(px>>4,pz>>4).getHeightValue(px&15,pz&15):Integer.MAX_VALUE;
        }
    }
    @Override public int height(int x,int z) {
        int dx=x-roofX+16,dz=z-roofZ+16;
        return dx<0||dx>=33||dz<0||dz>=33?Integer.MAX_VALUE:heights[dz*33+dx];
    }
    @Override public void render(float partial,WorldClient rendered,Minecraft mc) {
        if(rendered!=world || !attached) return;
        int previousTexture=AuroraGLState.boundTexture0();
        try {
        // Let the previous custom renderer (or vanilla) remain visible during the cross-fade.
        if(simulation.fade<.9999F || failed[0]) {
            if(original!=null) original.render(partial,rendered,mc);
            else {
                rendered.provider.setSkyRenderer(null);
                try { mc.renderGlobal.renderSky(partial,2); }
                finally { if(attached && rendered.provider.getSkyRenderer()==null) rendered.provider.setSkyRenderer(this); }
            }
        }
        if(failed[0]) return;
        try {
            AuroraOptions o=module.options;
            renderer.sky(simulation,o,fogR,fogG,fogB);
        } catch(RuntimeException|LinkageError error) { fail(0,error);detach(mc);simulation.reset();petals.reset(); }
        } finally { AuroraGLState.restoreDefaultState(previousTexture); }
    }
    @SubscribeEvent public void fog(EntityViewRenderEvent.FogColors event) {
        if(!surface() || simulation.fade<=0 || failed[2] || event.entity==null || event.entity.isInsideOfMaterial(net.minecraft.block.material.Material.water)
                || event.entity.isInsideOfMaterial(net.minecraft.block.material.Material.lava)) return;
        try {
            AuroraOptions opt=module.options;
            float t=simulation.fade, cold=opt.cold;
            float wash=opt.reflection?(float)(.5+.5*Math.sin(simulation.phase*.09))*.012F*opt.intensity:0;
            int color=reflectionColor();
            float r=.025F+cold*.09F+wash*(color>>16&255)/255F;
            float g=.048F+cold*.13F+wash*(color>>8&255)/255F,b=.085F+cold*.18F+wash*(color&255)/255F;
            float auroraWeight=1-opt.constellationMix-opt.duskMix-opt.art.mix();
            r=r*auroraWeight+(opt.duskFog?OvercastNoise.horizon(opt,0):event.red)*opt.duskMix;
            g=g*auroraWeight+(opt.duskFog?OvercastNoise.horizon(opt,1):event.green)*opt.duskMix;
            b=b*auroraWeight+(opt.duskFog?OvercastNoise.horizon(opt,2):event.blue)*opt.duskMix;
            for(int i=0;i<3;i++){float w=opt.art.amount(i);r+=(opt.art.fogMatch?ArtSkyPalette.horizon(opt.art,i,0):event.red)*w;g+=(opt.art.fogMatch?ArtSkyPalette.horizon(opt.art,i,1):event.green)*w;b+=(opt.art.fogMatch?ArtSkyPalette.horizon(opt.art,i,2):event.blue)*w;}
            event.red=event.red*(1-t)+r*t;event.green=event.green*(1-t)+g*t;event.blue=event.blue*(1-t)+b*t;
            fogR=event.red;fogG=event.green;fogB=event.blue;
        } catch(RuntimeException|LinkageError error) { fail(2,error); }
    }
    @SubscribeEvent public void snow(RenderWorldLastEvent event) {
        if(!surface() || simulation.snowFade<=0 || module.options.constellationMix+module.options.duskMix+module.options.art.mix()>=.999F || failed[1]) return;
        Minecraft mc=Minecraft.getMinecraft(); Entity camera=mc.getRenderViewEntity(); if(camera==null) return;
        int previousTexture=AuroraGLState.boundTexture0();
        try {
            double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*event.partialTicks;
            double y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*event.partialTicks;
            double z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*event.partialTicks;
            renderer.snow(simulation,x,y,z,ActiveRenderInfo.getRotationX(),ActiveRenderInfo.getRotationZ(),
                    ActiveRenderInfo.getRotationYZ(),ActiveRenderInfo.getRotationXZ(),ActiveRenderInfo.getRotationXY());
        } catch(RuntimeException|LinkageError error) { fail(1,error); }
        finally { AuroraGLState.restoreDefaultState(previousTexture); }
    }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Pre event) {
        if(event.type!=RenderGameOverlayEvent.ElementType.ALL || !surface() || !module.options.vignette || failed[3]) return;
        int previousTexture=AuroraGLState.boundTexture0();
        try { renderer.vignette(simulation.fade*(1-module.options.constellationMix-module.options.duskMix-module.options.art.mix()),event.resolution.getScaledWidth(),event.resolution.getScaledHeight()); }
        catch(RuntimeException|LinkageError error) { fail(3,error); }
        finally { AuroraGLState.restoreDefaultState(previousTexture); }
    }
    @SubscribeEvent public void petals(RenderWorldLastEvent event){
        if(!surface()||petals.fade<=.001F||simulation.fade<=0||failed[5])return;
        Minecraft mc=Minecraft.getMinecraft();Entity camera=mc.getRenderViewEntity();if(camera==null)return;
        int previousTexture=AuroraGLState.boundTexture0();try{
            double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*event.partialTicks,y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*event.partialTicks,z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*event.partialTicks;
            petalRenderer.draw(petals,module.options.art,simulation.fade,x,y,z,ActiveRenderInfo.getRotationX(),ActiveRenderInfo.getRotationZ(),ActiveRenderInfo.getRotationYZ(),ActiveRenderInfo.getRotationXZ(),ActiveRenderInfo.getRotationXY());
        }catch(RuntimeException|LinkageError e){fail(5,e);petals.reset();}finally{AuroraGLState.restoreDefaultState(previousTexture);}
    }
    public boolean visualTime(World query) { return query==world && module.isEnabled() && module.options.lockTime && surface(); }
    public Vec3 clouds(World query,Vec3 original) {
        AuroraOptions o=module.options;
        if(query!=world||!surface()||!module.isEnabled()||!o.cloudTint||original==null) return original;
        float a=o.cloudStrength;
        return new Vec3(original.xCoord*(1-a)+(o.cloudColor>>16&255)/255F*a,
                original.yCoord*(1-a)+(o.cloudColor>>8&255)/255F*a,original.zCoord*(1-a)+(o.cloudColor&255)/255F*a);
    }
    private static int styleIndex(AuroraOptions o){return o.art.style>0?o.art.style+2:o.overcast?2:o.constellation?1:0;}
    public float artTint(){return surface()?module.options.art.worldTint*simulation.fade*module.options.art.mix():0;}
    public int artColor(){return ArtSkyPalette.tint(module.options.art);}
    public float cold() { return surface()?module.options.cold*simulation.fade*(1-module.options.constellationMix-module.options.duskMix-module.options.art.mix()):0; }
    public float dusk(){return surface()?module.options.duskWorld*simulation.fade*module.options.duskMix:0;}
    public float reflection() { return surface()&&module.options.reflection?simulation.fade*.045F*module.options.intensity*(float)(.5+.5*Math.sin(simulation.phase*.09))*(1-module.options.constellationMix-module.options.duskMix-module.options.art.mix()):0; }
    public int reflectionColor() {
        switch(module.options.palette) { case 1:return 0xDD66ED;case 2:return java.awt.Color.HSBtoRGB((float)(simulation.phase*.015%1),.65F,1);case 3:return module.options.accent;default:return 0x44FFAA; }
    }
}
