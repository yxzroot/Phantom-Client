package keystrokesmod.client.module.modules.combat.aim;

import keystrokesmod.client.module.modules.combat.AimAssist;
import keystrokesmod.client.module.modules.combat.LeftClicker;
import keystrokesmod.client.module.modules.render.Freelook;
import keystrokesmod.client.module.modules.other.AntiBot;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.*;
import java.util.List;

/** Fixed candidate pool. All history and camera math is allocation-free. */
public final class AssistController {
    private static final class Candidate {
        final PositionHistory history=new PositionHistory();
        EntityLivingBase entity;
        double angle,x,y,z,seen,visibilityAt;
        boolean visible;
        int generation;
        void clear(){entity=null;history.clear();visibilityAt=0;visible=false;}
    }
    private final Candidate[] pool=new Candidate[32];
    public final TrailingPull pull=new TrailingPull();
    public final AttackGate attackGate=new AttackGate();
    private final AimAssist module;
    private Object world,player;
    private Module antiBot;
    private Candidate target,pending;
    private double pendingSince,lastTime,lastMove,inputYaw,inputPitch,lastX,lastY,lastZ;
    private int generation;
    public double aimX,aimY,aimZ,yawError,pitchError;
    public boolean hasTarget;
    public String status="Waiting for frame";
    public AssistController(AimAssist module){this.module=module;for(int i=0;i<pool.length;i++)pool[i]=new Candidate();}
    public Entity getTarget(){return target==null?null:target.entity;}
    public void unload(Object unloaded){if(world==unloaded)reset();}
    public void reset(){for(int i=0;i<pool.length;i++)pool[i].clear();target=pending=null;antiBot=null;world=player=null;lastTime=lastMove=inputYaw=inputPitch=0;generation=0;hasTarget=false;status="Waiting for world";pull.reset();attackGate.reset();}
    public void mouse(double yaw,double pitch){if(Double.isFinite(yaw)&&Double.isFinite(pitch)){inputYaw+=yaw;inputPitch+=pitch;}}
    public void frame(Minecraft mc,float partial,double now){
        if(mc.theWorld==null||mc.thePlayer==null||!mc.thePlayer.isEntityAlive()){reset();return;}
        if(world!=mc.theWorld||player!=mc.thePlayer){reset();world=mc.theWorld;player=mc.thePlayer;lastX=mc.thePlayer.posX;lastY=mc.thePlayer.posY;lastZ=mc.thePlayer.posZ;}
        double jump=sq(mc.thePlayer.posX-lastX)+sq(mc.thePlayer.posY-lastY)+sq(mc.thePlayer.posZ-lastZ);
        if(jump>16){reset();world=mc.theWorld;player=mc.thePlayer;}
        lastX=mc.thePlayer.posX;lastY=mc.thePlayer.posY;lastZ=mc.thePlayer.posZ;
        double dt=lastTime==0?0:now-lastTime;lastTime=now;
        if(dt<0||dt>.25){pull.reset();for(int i=0;i<pool.length;i++)pool[i].clear();target=pending=null;dt=0;}
        double iy=inputYaw,ip=inputPitch;inputYaw=inputPitch=0;
        if(Math.abs(iy)+Math.abs(ip)>.00001)lastMove=now;
        AssistOptions o=module.options;module.snapshot();
        ItemStack held=mc.thePlayer.getHeldItem();
        boolean attack=attackGate.active(org.lwjgl.input.Mouse.isCreated()&&org.lwjgl.input.Mouse.isButtonDown(0),
                mc.gameSettings!=null&&mc.gameSettings.keyBindAttack!=null&&mc.gameSettings.keyBindAttack.isKeyDown(),LeftClicker.autoClickerEnabled,now);
        String blocked=null;
        if(o.noGui&&mc.currentScreen!=null)blocked="GUI open";
        else if(!mc.inGameHasFocus&&(o.noGui||mc.currentScreen==null))blocked="Game unfocused";
        else if(mc.isGamePaused())blocked="Game paused";
        else if(mc.getRenderViewEntity()!=mc.thePlayer||Freelook.rendering(mc.thePlayer))blocked="Other camera / Freelook";
        else if(o.clicking&&!attack)blocked="Hold attack (no active clicks)";
        else if(o.moving&&now-lastMove>=.12)blocked="Move the mouse";
        else if(o.weapon&&(held==null||!(held.getItem() instanceof ItemSword||held.getItem() instanceof ItemAxe)))blocked="Hold a sword or axe";
        else if(o.mining&&mc.playerController.func_181040_m())blocked="Mining block";
        boolean gate=blocked==null;
        if(!gate){status=blocked;hasTarget=false;target=pending=null;pull.step(0,0,0,0,iy,ip,dt,now,false,1,o);return;}
        double pt=PositionHistory.clamp(partial,0,1);
        double px=lerp(mc.thePlayer.prevPosX,mc.thePlayer.posX,pt),py=lerp(mc.thePlayer.prevPosY,mc.thePlayer.posY,pt)+mc.thePlayer.getEyeHeight(),pz=lerp(mc.thePlayer.prevPosZ,mc.thePlayer.posZ,pt);
        double yaw=mc.thePlayer.rotationYaw,pitch=mc.thePlayer.rotationPitch;
        if(antiBot==null&&Raven.moduleManager!=null){java.util.List<Module> modules=Raven.moduleManager.getModules();for(int i=0;i<modules.size();i++)if(modules.get(i) instanceof AntiBot){antiBot=modules.get(i);break;}}
        generation++;
        List<Entity> entities=mc.theWorld.loadedEntityList;
        for(int i=0;i<entities.size();i++){
            Entity entity=entities.get(i);
            if(!(entity instanceof EntityLivingBase)||entity==mc.thePlayer||entity instanceof EntityArmorStand||entity.isDead||!entity.isEntityAlive()
                    ||o.invisible&&entity.isInvisible()||o.targets==AssistOptions.Targets.Players&&!(entity instanceof EntityPlayer))continue;
            EntityLivingBase e=(EntityLivingBase)entity;
            double ex=lerp(e.prevPosX,e.posX,pt),ey=lerp(e.prevPosY,e.posY,pt)+height(e,o.hitbox),ez=lerp(e.prevPosZ,e.posZ,pt);
            if(sq(ex-px)+sq(ey-py)+sq(ez-pz)>o.range*o.range)continue;
            if(e instanceof EntityPlayer){EntityPlayer p=(EntityPlayer)e;
                if(o.creative&&(p.capabilities.isCreativeMode||p.isSpectator())||AimAssist.isFriend(e)||antiBot!=null&&antiBot.isEnabled()&&AntiBot.bot(e))continue;
                // Use actual scoreboard teams, not rank/name prefixes (same-rank opponents aren't teammates).
                if(o.teams&&mc.thePlayer.isOnSameTeam(e))continue;
            }
            double angle=Math.hypot(TrailingPull.wrap(yawTo(ex-px,ez-pz)-yaw),pitchTo(ex-px,ey-py,ez-pz)-pitch);
            if(angle>o.fov*.5)continue;
            Candidate c=slot(e,angle);if(c==null)continue;
            if(c.entity!=e){c.clear();c.entity=e;}
            c.x=ex;c.y=ey;c.z=ez;c.angle=angle;c.seen=now;c.generation=generation;c.history.add(now,ex,ey,ez);
            if(now>=c.visibilityAt){c.visible=mc.thePlayer.canEntityBeSeen(e);c.visibilityAt=now+.1;}
        }
        Candidate best=null;double score=Double.MAX_VALUE;
        for(int i=0;i<pool.length;i++){
            Candidate c=pool[i];if(c.entity==null)continue;
            if(c.generation!=generation){if(now-c.seen>.5)c.clear();continue;}
            if(!c.visible)continue;
            double value=c.angle-(c==target?2:0);if(value<score){score=value;best=c;}
        }
        if(best!=target){
            if(best!=pending){pending=best;pendingSince=now;}
            if(target==null||best==null||now-pendingSince>=o.switchDelay){
                // New selection starts its own history, never a previous target's path.
                for(int i=0;i<pool.length;i++)pool[i].history.clear();
                target=best;pending=null;if(best!=null){pull.reset();best.history.add(now,best.x,best.y,best.z);}else pull.clearMotion();
            }
        }else pending=null;
        hasTarget=target!=null&&target.entity!=null&&target.generation==generation&&target.visible;
        if(!hasTarget){status="No eligible visible target in range / FOV";pull.step(0,0,0,0,iy,ip,dt,now,false,1,o);return;}
        target.history.sample(now-o.delay*o.trail);
        double dx=target.history.x-px,dy=target.history.y-py,dz=target.history.z-pz;
        // Offset trails the measured motion; it disappears naturally when history catches up at rest.
        double mx=target.x-target.history.x,mz=target.z-target.history.z,travel=Math.hypot(mx,mz);
        if(travel>.0001){double extra=o.offset*o.trail*Math.min(1,travel/.08)*(1+.15*o.humanization*TrailingPull.noise(now*.41,53));dx-=mx/travel*extra;dz-=mz/travel*extra;}
        double realYaw=TrailingPull.wrap(yawTo(target.x-px,target.z-pz)-yaw),realPitch=pitchTo(target.x-px,target.y-py,target.z-pz)-pitch;
        double ey=TrailingPull.wrap(yawTo(dx,dz)-yaw),ep=pitchTo(dx,dy,dz)-pitch;
        double oy=TrailingPull.wrap(ey-realYaw),op=ep-realPitch,offset=Math.hypot(oy,op),ratio=offset>o.maxOffset?o.maxOffset/offset:1;
        ey=realYaw+oy*ratio;ep=realPitch+op*ratio;
        double dist=Math.sqrt(dx*dx+dy*dy+dz*dz),ar=Math.toRadians(yaw+ey),pr=Math.toRadians(pitch+ep);
        aimX=px-Math.sin(ar)*Math.cos(pr)*dist;aimY=py-Math.sin(pr)*dist;aimZ=pz+Math.cos(ar)*Math.cos(pr)*dist;
        yawError=ey;pitchError=ep;
        // Taper before crossing the cone/range boundary. Outside it, gates stop
        // rotations immediately rather than steering toward an invalid target.
        optionsActivation(o,Math.sqrt(sq(target.x-px)+sq(target.y-py)+sq(target.z-pz)),target.angle);
        pull.step(ey,ep,realYaw,realPitch,iy,ip,dt,now,true,TrailingPull.quantum(mc.gameSettings.mouseSensitivity),o);
        status=Math.hypot(ey,ep)<=TrailingPull.DEAD_ZONE?"Inside no-centering zone":iy*ey<0||ip*ep<0?"Yielding to your mouse":"Tracking (smooth trailing pull)";
        float sy=(float)pull.yawStep,sp=(float)pull.pitchStep;
        float clamped=(float)PositionHistory.clamp(pitch+sp,-90,90);sp=clamped-(float)pitch;
        mc.thePlayer.rotationYaw+=sy;mc.thePlayer.rotationPitch+=sp;
        // Vanilla's mouse handler shifts previous angles by the same amount. Doing
        // that here keeps interpolated picking and camera rotation in agreement.
        mc.thePlayer.prevRotationYaw+=sy;mc.thePlayer.prevRotationPitch+=sp;
    }
    private Candidate slot(EntityLivingBase entity,double angle){
        Candidate empty=null,worst=null;double worstAngle=-1;
        for(int i=0;i<pool.length;i++){Candidate c=pool[i];if(c.entity==entity)return c;if(c.entity==null)empty=c;
            else if(c!=target&&(c.generation!=generation||c.angle>worstAngle)){worst=c;worstAngle=c.generation==generation?c.angle:Double.MAX_VALUE;}}
        if(empty!=null)return empty;return worst!=null&&angle<worstAngle?worst:null;
    }
    public static double height(EntityLivingBase e,AssistOptions.Hitbox part){return part==AssistOptions.Hitbox.Head?e.getEyeHeight():e.height*(part==AssistOptions.Hitbox.Chest?.72:.5);}
    private static double sq(double x){return x*x;}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static double yawTo(double x,double z){return Math.toDegrees(Math.atan2(z,x))-90;}
    private static double pitchTo(double x,double y,double z){return -Math.toDegrees(Math.atan2(y,Math.hypot(x,z)));}
    private static void optionsActivation(AssistOptions o,double distance,double angle){o.activation=PositionHistory.clamp((o.fov*.5-angle)/4,0,1)*PositionHistory.clamp((o.range-distance)/.3,0,1);}
}
