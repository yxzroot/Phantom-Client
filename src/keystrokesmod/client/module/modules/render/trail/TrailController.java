package keystrokesmod.client.module.modules.render.trail;

import keystrokesmod.client.module.modules.render.Trail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** SkyChanger-style Forge lifetime: listeners outlive toggle-off only for the 200ms fade. */
public final class TrailController {
    public static final int ACTORS=8;
    private final Trail module;
    private final EntityPlayer[] players=new EntityPlayer[ACTORS];
    private final boolean[] seen=new boolean[ACTORS];
    public final TrailBuffer[] paths=new TrailBuffer[ACTORS];
    public final TrailEffects effects=new TrailEffects();
    private final TrailRenderer renderer=new TrailRenderer();
    private WorldClient world;
    private boolean registered,failed;
    private long lastNanos;
    private double now;
    private final TrailFade fade=new TrailFade();
    public TrailController(Trail module){this.module=module;for(int i=0;i<ACTORS;i++)paths[i]=new TrailBuffer();}
    public void start(){
        failed=false;if(registered)return;
        registered=true;lastNanos=0;
        MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);
    }
    public void fadeOut(){/* Render/frame listeners remain until fade reaches zero. */}
    private void clear(){for(int i=0;i<ACTORS;i++){players[i]=null;paths[i].clear();}effects.clear();}
    private void stop(){
        clear();fade.clear();world=null;lastNanos=0;
        if(registered){registered=false;MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().bus().unregister(this);}
    }
    private void fail(Throwable cause){
        if(!failed){failed=true;java.util.logging.Logger.getLogger("Phantom/Trail").log(java.util.logging.Level.WARNING,"Trail disabled after a render failure",cause);}
        stop();if(module.isEnabled())module.disable();
    }
    private int find(EntityPlayer player){for(int i=0;i<ACTORS;i++)if(players[i]==player)return i;return -1;}
    private int empty(){for(int i=1;i<ACTORS;i++)if(players[i]==null)return i;return -1;}
    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.START||failed)return;
        Minecraft mc=Minecraft.getMinecraft();
        try{
            long nanos=System.nanoTime();double dt=lastNanos==0?0:Math.max(0,(nanos-lastNanos)/1e9);lastNanos=nanos;
            // Do not leave trails running while a single-player pause menu freezes the world.
            boolean paused=mc.isGamePaused();now+=paused?0:dt;
            module.snapshot();
            if(world!=mc.theWorld){clear();world=mc.theWorld;fade.clear();}
            if(world==null||mc.thePlayer==null){clear();if(!module.isEnabled())stop();return;}
            fade.advance(module.isEnabled(),dt);
            effects.expire(now);if(!module.options.sparkles||module.options.low)effects.clear();
            for(int i=0;i<ACTORS;i++){seen[i]=false;paths[i].visible=false;}
            if(!module.isEnabled()){
                for(int i=0;i<ACTORS;i++)paths[i].expire(now,module.options.length);
                if(fade.gain()<=0)stop();return;
            }
            if(paused)return; // Fade uses wall time; world positions/spark ages stay frozen.
            for(int i=0;i<world.playerEntities.size();i++){
                EntityPlayer player=world.playerEntities.get(i);boolean local=player==mc.thePlayer;
                if(player.isDead||player.getHealth()<=0||!local&&(!module.options.others||player.isInvisible()||mc.thePlayer.getDistanceSqToEntity(player)>256))continue;
                int slot=find(player);if(slot<0){slot=local?0:empty();if(slot<0)continue;players[slot]=player;paths[slot].clear();effects.clearOwner(slot);}
                seen[slot]=true;TrailBuffer path=paths[slot];
                double dx=player.posX-player.lastTickPosX,dy=player.posY-player.lastTickPosY,dz=player.posZ-player.lastTickPosZ;
                if(dx*dx+dy*dy+dz*dz>64){path.clear();effects.clearOwner(slot);continue;}
                float partial=TrailOptions.clamp(event.renderTickTime,0,1);
                float yawDelta=(player.renderYawOffset-player.prevRenderYawOffset)%360;if(yawDelta< -180)yawDelta+=360;else if(yawDelta>=180)yawDelta-=360;
                path.pose(player.prevRenderYawOffset+yawDelta*partial,player.isSneaking());
                double moved=path.record(player.lastTickPosX+dx*partial,player.lastTickPosY+dy*partial,player.lastTickPosZ+dz*partial,now,player.onGround,player.isSprinting(),module.options);
                if(module.options.sparkles&&!module.options.low){
                    path.emit+=(float)moved*1.5F;
                    while(path.emit>=1){path.emit--;effects.spawn(slot,path.lastX,path.lastY,path.lastZ,module.options.tint(0,now,path.totalDistance),now);}
                }
            }
            for(int i=0;i<ACTORS;i++)if(!seen[i]){players[i]=null;paths[i].clear();effects.clearOwner(i);}
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent public void render(RenderWorldLastEvent event){
        if(failed||fade.gain()<=0||world==null)return;
        Minecraft mc=Minecraft.getMinecraft();if(world!=mc.theWorld||mc.thePlayer==null)return;
        try{
            boolean first=mc.gameSettings.thirdPersonView==0;
            for(int i=0;i<ACTORS;i++)paths[i].visible=players[i]!=null&&!players[i].isDead&&(i==0?(first?module.options.first:module.options.third):module.options.others);
            RenderManager camera=mc.getRenderManager();float smooth=fade.gain();
            renderer.render(paths,effects,module.options,now,smooth,camera.viewerPosX,camera.viewerPosY,camera.viewerPosZ,first);
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event){if(event.world==world){clear();world=null;fade.clear();if(!module.isEnabled())stop();}}
}
