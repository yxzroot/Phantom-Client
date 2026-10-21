package keystrokesmod.client.module.modules.render.killeffect;

import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.render.DynamicIsland;
import keystrokesmod.client.module.modules.render.KillEffect;
import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.*;
import net.minecraft.util.DamageSource;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

/** Client-only observation and rendering. Never sends a packet or modifies a world/entity. */
public final class KillController {
    private final KillEffect module;
    public final KillPool pool=new KillPool();
    private final KillCredit credit=new KillCredit();
    private final KillRenderer renderer=new KillRenderer();
    private final KillOverlay overlay=new KillOverlay();
    private WorldClient world;
    private EntityPlayer player;
    private Entity lastTarget;
    private double lastHit=-100,now,lastFeedback=-100;
    private long lastNanos;
    private float partial,feedbackGain=1;
    private int feedbackColor=0xFFFFFF,scanCursor;
    private String popupText;
    private boolean registered,failed;
    public KillController(KillEffect module){this.module=module;}
    public void start(){
        failed=false;if(registered)return;registered=true;lastNanos=0;
        MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);
    }
    private void clear(){pool.clear();credit.clear();lastTarget=null;lastHit=lastFeedback=-100;popupText=null;scanCursor=0;}
    public void stop(){
        clear();world=null;player=null;lastNanos=0;
        if(registered){registered=false;MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().bus().unregister(this);}
        if(Display.isCreated()){renderer.dispose();overlay.dispose();}
    }
    private void fail(Throwable error){
        if(!failed){failed=true;java.util.logging.Logger.getLogger("Phantom/KillEffect").log(java.util.logging.Level.WARNING,"KillEffect disabled after a rendering/observation failure",error);}
        stop();if(module.isEnabled())module.disable();
    }
    private boolean ready(){Minecraft mc=Minecraft.getMinecraft();return module.isEnabled()&&!failed&&world!=null&&world==mc.theWorld&&player==mc.thePlayer&&player!=null&&!player.isDead&&player.getHealth()>0;}
    private boolean allowed(Entity e){
        if(e==null||e==player||e.worldObj!=world||player.getDistanceSqToEntity(e)>module.options.distance*module.options.distance)return false;
        if(e instanceof EntityPlayer)return true;
        if(module.options.targets==KillOptions.Targets.Players)return false;
        if(e instanceof EntityLivingBase&&!(e instanceof EntityArmorStand))return true;
        return module.options.targets==KillOptions.Targets.Everything&&(e instanceof EntityArmorStand||e instanceof EntityBoat||e instanceof EntityMinecart||e instanceof EntityHanging);
    }
    private static boolean dead(Entity e){
        if(e instanceof EntityLivingBase){EntityLivingBase living=(EntityLivingBase)e;return living.getHealth()<=0||living.deathTime>0||e.isDead&&living.hurtTime>0;}
        return e.isDead;
    }
    private int owner(Entity attacker){return attacker==null?KillCredit.NONE:attacker==player?KillCredit.LOCAL:attacker instanceof EntityPlayer?KillCredit.OTHER:attacker instanceof EntityLivingBase?KillCredit.BLOCKED:KillCredit.NONE;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void attack(AttackEntityEvent event){
        if(event.isCanceled()||!ready()||event.entityPlayer!=player||!allowed(event.target)||dead(event.target))return;
        try{lastTarget=event.target;lastHit=now;credit.attack(lastTarget,now);}catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void hurt(LivingHurtEvent event){
        if(event.isCanceled()||!ready()||!allowed(event.entityLiving))return;
        try{
            int owner=owner(event.source.getEntity());
            if(owner==KillCredit.LOCAL){lastTarget=event.entityLiving;lastHit=now;credit.attack(lastTarget,now);}
            else if(owner==KillCredit.OTHER||owner==KillCredit.BLOCKED){credit.watch(event.entityLiving,!dead(event.entityLiving),now);credit.credit(event.entityLiving,owner);}
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void death(LivingDeathEvent event){
        if(event.isCanceled()||!ready()||!allowed(event.entityLiving))return;
        try{observe(event.entityLiving,true,owner(event.source.getEntity()));}catch(RuntimeException|LinkageError error){fail(error);}
    }
    private void observe(Entity entity,boolean died,int known){
        if(!allowed(entity))return;
        if(known==KillCredit.NONE&&entity instanceof EntityLivingBase){EntityLivingBase living=(EntityLivingBase)entity;known=owner(living.getAITarget());}
        int result=credit.observe(entity,died,known,now,module.options.others);
        if(result!=KillCredit.NONE)celebrate(entity,result==KillCredit.LOCAL);
        if(died&&entity==lastTarget)lastTarget=null;
    }
    private void celebrate(Entity victim,boolean mine){
        double x=victim.lastTickPosX+(victim.posX-victim.lastTickPosX)*partial,y=victim.lastTickPosY+(victim.posY-victim.lastTickPosY)*partial,z=victim.lastTickPosZ+(victim.posZ-victim.lastTickPosZ)*partial;
        int slot=pool.spawn(module.options,x,y,z,now,mine);if(slot<0||!mine)return;
        module.counter.increment();lastFeedback=now;feedbackColor=pool.color[slot];feedbackGain=pool.gain[slot];
        if(module.options.sound&&module.options.volume>0)player.playSound("note.pling",module.options.volume,Math.min(1.65F,1.05F+Math.max(0,pool.streak-1)*.045F));
        popupText=null;
        if(module.options.popup){
            String name=victim.getName();if(name==null||name.trim().isEmpty())name="Target";if(name.length()>48)name=name.substring(0,48);
            boolean delivered=false;
            try{
                Module island=Raven.moduleManager==null?null:Raven.moduleManager.getModuleByName("DynamicIsland");
                if(island!=null&&island.isEnabled()){
                    Setting notifications=island.getSettingByName("Notification Events"),lyricsOnly=island.getSettingByName("Lyrics only");
                    if(notifications instanceof TickSetting&&((TickSetting)notifications).isToggled()&&!(lyricsOnly instanceof TickSetting&&((TickSetting)lyricsOnly).isToggled())){DynamicIsland.post("+1 Kill",name,"+",1800);delivered=true;}
                }
            }catch(LinkageError ignored){/* Optional island unavailable: the HUD fallback remains usable. */}
            if(!delivered)popupText="+1 Kill  "+name;
        }
    }
    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.START||failed)return;Minecraft mc=Minecraft.getMinecraft();
        try{
            long nanos=System.nanoTime();double dt=lastNanos==0?0:Math.max(0,(nanos-lastNanos)/1e9);lastNanos=nanos;
            if(!mc.isGamePaused())now+=dt;partial=KillOptions.clamp(event.renderTickTime,0,1);module.snapshot();
            if(world!=mc.theWorld||player!=mc.thePlayer){clear();world=mc.theWorld;player=mc.thePlayer;}
            if(player==null||world==null||player.isDead||player.getHealth()<=0){clear();return;}
            pool.expire(now);
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!ready())return;
        try{
            if(lastTarget!=null){if(now-lastHit>3)lastTarget=null;else observe(lastTarget,dead(lastTarget),KillCredit.NONE);}
            if(!module.options.others)return;
            int count=world.loadedEntityList.size(),budget=Math.min(512,count);
            for(int i=0;i<budget;i++){
                if(scanCursor>=count)scanCursor=0;Entity entity=world.loadedEntityList.get(scanCursor++);
                if(!allowed(entity))continue;boolean dead=dead(entity);
                if(!dead){credit.watch(entity,true,now);if(entity instanceof EntityLivingBase&&((EntityLivingBase)entity).hurtTime>0)credit.damage(entity,now);}
                else observe(entity,true,KillCredit.NONE);
            }
        }catch(RuntimeException|LinkageError error){fail(error);}
    }
    @SubscribeEvent public void world(RenderWorldLastEvent event){
        if(!ready()||!pool.any()||module.options.intensity<=0)return;int texture=AuroraGLState.boundTexture0();
        try{RenderManager camera=Minecraft.getMinecraft().getRenderManager();renderer.draw(pool,module.options,now,camera.viewerPosX,camera.viewerPosY,camera.viewerPosZ,ActiveRenderInfo.getRotationX(),ActiveRenderInfo.getRotationZ(),ActiveRenderInfo.getRotationYZ(),ActiveRenderInfo.getRotationXZ(),ActiveRenderInfo.getRotationXY());}
        catch(RuntimeException|LinkageError error){fail(error);}finally{AuroraGLState.restoreDefaultState(texture);}
    }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event){
        if(event.type!=RenderGameOverlayEvent.ElementType.ALL||!ready())return;Minecraft mc=Minecraft.getMinecraft();if(mc.gameSettings.hideGUI||mc.gameSettings.showDebugInfo||mc.currentScreen!=null)return;
        double age=now-lastFeedback;float flash=module.options.flash?KillPool.flash(age)*module.options.flashStrength*Math.min(1.3F,feedbackGain):0;
        float fade=module.options.popup?KillOptions.clamp((float)(age/.12),0,1)*KillOptions.clamp((float)((1.8-age)/.35),0,1):0;
        if(flash<=0&&(popupText==null||fade<=0)&&!module.counter.isToggled())return;
        int texture=AuroraGLState.boundTexture0();
        try{overlay.draw(event.resolution.getScaledWidth(),event.resolution.getScaledHeight(),feedbackColor,flash,popupText,fade,module.counter.isToggled()?module.counter.label():null);}
        catch(RuntimeException|LinkageError error){fail(error);}finally{AuroraGLState.restoreDefaultState(texture);}
    }
    @SubscribeEvent public void fov(EntityViewRenderEvent.FOVModifier event){if(ready()&&module.options.pulse){float punch=KillPool.pulse(now-lastFeedback)*module.options.pulseStrength;event.setFOV(event.getFOV()*(1-punch));}}
    @SubscribeEvent public void unload(WorldEvent.Unload event){if(event.world==world){clear();world=null;player=null;}}
}
