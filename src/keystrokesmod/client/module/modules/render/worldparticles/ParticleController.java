package keystrokesmod.client.module.modules.render.worldparticles;

import keystrokesmod.client.module.modules.render.WorldParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

/** Separate world pass. Does not replace weather, sky, player renderers, or server data. */
public final class ParticleController implements ParticleSimulation.Exposure {
    private final WorldParticles module;
    public final ParticleSimulation simulation=new ParticleSimulation();
    private final ParticleRenderer renderer=new ParticleRenderer();
    private final BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
    private final int[] roofX=new int[1024],roofZ=new int[1024],roofHeight=new int[1024];
    private final double[] roofAt=new double[1024];
    private WorldClient world;
    private Entity camera;
    private long lastNanos;
    private boolean registered,failed;
    public ParticleController(WorldParticles module){this.module=module;}
    public void start(){
        failed=false;if(registered)return;registered=true;lastNanos=0;
        MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);
    }
    public void fadeOut(){/* Keep listeners for 200ms, then free GPU objects on the render thread. */}
    private void clear(){simulation.reset();camera=null;java.util.Arrays.fill(roofAt,-1);}
    private void stop(){
        clear();world=null;lastNanos=0;
        if(registered){registered=false;MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().bus().unregister(this);}
        if(Display.isCreated())renderer.dispose();
    }
    private void fail(Throwable error){
        if(!failed){failed=true;java.util.logging.Logger.getLogger("Phantom/WorldParticles").log(java.util.logging.Level.WARNING,"WorldParticles disabled after a render failure",error);}
        stop();if(module.isEnabled())module.disable();
    }
    @Override public int height(int x,int z){
        if(world==null)return Integer.MAX_VALUE;
        int slot=(x*73428767^z*912931)&1023;
        if(roofAt[slot]<0||simulation.time-roofAt[slot]>.25||roofX[slot]!=x||roofZ[slot]!=z){
            pos.set(x,64,z);roofX[slot]=x;roofZ[slot]=z;roofAt[slot]=simulation.time;
            roofHeight[slot]=world.isBlockLoaded(pos,false)?world.getChunkFromChunkCoords(x>>4,z>>4).getHeightValue(x&15,z&15):Integer.MAX_VALUE;
        }
        return roofHeight[slot];
    }
    @Override public boolean open(double x,double y,double z){
        if(world==null||y<0||y>=256)return false;
        pos.set((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));
        return world.isBlockLoaded(pos,false)&&world.isAirBlock(pos);
    }
    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.START||failed)return;
        Minecraft mc=Minecraft.getMinecraft();
        try{
            long nanos=System.nanoTime();double dt=lastNanos==0?0:(nanos-lastNanos)/1e9;lastNanos=nanos;
            module.snapshot();
            if(world!=mc.theWorld){clear();world=mc.theWorld;}
            if(world==null||mc.thePlayer==null){if(!module.isEnabled())stop();return;}
            Entity view=mc.getRenderViewEntity();
            if(view==null||mc.thePlayer.isDead||mc.thePlayer.getHealth()<=0){clear();if(!module.isEnabled())stop();return;}
            if(camera!=view){clear();camera=view;}
            double jumpX=view.posX-view.lastTickPosX,jumpY=view.posY-view.lastTickPosY,jumpZ=view.posZ-view.lastTickPosZ;
            if(jumpX*jumpX+jumpY*jumpY+jumpZ*jumpZ>256){clear();if(!module.isEnabled())stop();return;}
            float p=ParticleOptions.clamp(event.renderTickTime,0,1);
            double x=view.lastTickPosX+(view.posX-view.lastTickPosX)*p;
            double y=view.lastTickPosY+(view.posY-view.lastTickPosY)*p+view.getEyeHeight();
            double z=view.lastTickPosZ+(view.posZ-view.lastTickPosZ)*p;
            simulation.advance(dt,module.isEnabled(),mc.isGamePaused(),module.options,x,y,z,this);
            if(!module.isEnabled()&&simulation.fade<=0)stop();
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent public void render(RenderWorldLastEvent event){
        if(failed||world==null||simulation.fade<=0)return;
        Minecraft mc=Minecraft.getMinecraft();if(world!=mc.theWorld||mc.thePlayer==null||mc.thePlayer.isDead)return;
        boolean first=mc.gameSettings.thirdPersonView==0;if(first?!module.options.first:!module.options.third)return;
        try{
            RenderManager view=mc.getRenderManager();
            renderer.draw(simulation,module.options,view.viewerPosX,view.viewerPosY,view.viewerPosZ,
                    ActiveRenderInfo.getRotationX(),ActiveRenderInfo.getRotationZ(),ActiveRenderInfo.getRotationYZ(),ActiveRenderInfo.getRotationXZ(),ActiveRenderInfo.getRotationXY());
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event){if(event.world==world){clear();world=null;if(!module.isEnabled())stop();}}
}
