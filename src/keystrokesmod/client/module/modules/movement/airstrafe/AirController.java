package keystrokesmod.client.module.modules.movement.airstrafe;

import java.lang.reflect.Field;
import keystrokesmod.client.event.impl.MoveInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

/** Consumes Phantom's existing LOCAL-player moveFlying event, once per simulation tick. */
public final class AirController {
    private static final Field WEB=findWeb();
    private final BlockPos.MutableBlockPos block=new BlockPos.MutableBlockPos();
    public final AirSteering steering=new AirSteering();
    private Object world,player;
    private int dimension,lastTick=Integer.MIN_VALUE;
    private double px,py,pz;
    public double speed;
    public String status="Off";
    public void reset(){world=player=null;lastTick=Integer.MIN_VALUE;steering.reset();speed=0;status="Waiting for world";}
    public void unload(Object value){if(world==value)reset();}
    private boolean context(Minecraft mc){
        EntityPlayerSP p=mc.thePlayer;
        if(mc.theWorld==null||p==null){reset();return false;}
        if(world!=mc.theWorld||player!=p||dimension!=p.dimension||p.ticksExisted<lastTick){
            reset();world=mc.theWorld;player=p;dimension=p.dimension;px=p.posX;py=p.posY;pz=p.posZ;
        }
        double dx=p.posX-px,dy=p.posY-py,dz=p.posZ-pz;px=p.posX;py=p.posY;pz=p.posZ;
        if(dx*dx+dy*dy+dz*dz>16){steering.reset();lastTick=p.ticksExisted;status="Teleport reset";return false;}
        speed=Math.hypot(p.motionX,p.motionZ);return true;
    }
    public void afterTick(Minecraft mc){
        if(!context(mc))return;
        if(mc.thePlayer.onGround){steering.reset();status="Grounded";}
        else if(!mc.thePlayer.isEntityAlive()){steering.reset();status="Dead";}
    }
    public void move(Minecraft mc,MoveInputEvent e,AirOptions o)throws IllegalAccessException{
        if(!context(mc))return;EntityPlayerSP p=mc.thePlayer;
        String blocked=blocked(mc,p,o);
        if(blocked!=null){steering.reset();status=blocked;return;}
        if(lastTick==p.ticksExisted)return;lastTick=p.ticksExisted;
        float strafe=e.getStrafe(),forward=e.getForward(),friction=e.getFriction(),yaw=e.getYaw();
        if(!Float.isFinite(strafe)||!Float.isFinite(forward)||!Float.isFinite(friction)||!Float.isFinite(yaw)
                ||!Double.isFinite(p.motionX)||!Double.isFinite(p.motionZ)||friction<0){steering.reset();status="Invalid input";return;}
        if(o.strength<=0){steering.reset();status="Strength zero (vanilla)";return;}
        float square=strafe*strafe+forward*forward;
        if(square<1E-4F){steering.step(p.motionX,p.motionZ,0,0,0,0,o);status="No movement input";return;}
        // Match the original moveFlying arithmetic, including its diagonal/analog
        // normalization and lookup-table trig, before adding our small contribution.
        float denominator=Math.max(1F,MathHelper.sqrt_float(square));
        float sine=MathHelper.sin(yaw*3.1415927F/180F),cosine=MathHelper.cos(yaw*3.1415927F/180F);
        float scale=friction/denominator,s=strafe*scale,f=forward*scale;
        double baseX=s*cosine-f*sine,baseZ=f*cosine+s*sine;
        double wishX=(strafe*cosine-forward*sine)/denominator,wishZ=(forward*cosine+strafe*sine)/denominator;
        steering.step(p.motionX,p.motionZ,baseX,baseZ,wishX,wishZ,o);
        if(!Double.isFinite(steering.x)||!Double.isFinite(steering.z)){steering.reset();status="Invalid output";return;}
        // This event occurs immediately before vanilla adds horizontal acceleration.
        // Ours already includes that base acceleration, so consume it exactly once.
        e.setFriction(0);p.motionX=steering.x;p.motionZ=steering.z;
        speed=Math.hypot(p.motionX,p.motionZ);status="Air steering";
    }
    private String blocked(Minecraft mc,EntityPlayerSP p,AirOptions o)throws IllegalAccessException{
        if(!p.isEntityAlive()||p.isDead)return "Dead";
        if(p.onGround)return "Grounded";
        if(mc.currentScreen!=null||!mc.inGameHasFocus||mc.isGamePaused())return "GUI / paused / unfocused";
        if(p.capabilities.isFlying||p.isSpectator()||p.noClip||p.ridingEntity!=null)return "Flight / riding";
        if(o.disableSpecial&&(p.isInWater()||p.isInLava()||p.isOnLadder()||inWeb(mc,p)))return "Liquid / ladder / web";
        if(o.sprintOnly&&!p.isSprinting())return "Sprinting required";
        if(o.jumpOnly&&(p.movementInput==null||!p.movementInput.jump))return "Hold jump";
        return null;
    }
    private boolean inWeb(Minecraft mc,EntityPlayerSP p)throws IllegalAccessException{
        if(WEB!=null)return WEB.getBoolean(p);
        // Safe bounded fallback if a custom runtime changes the private field name.
        AxisAlignedBB box=p.getEntityBoundingBox();if(box==null)return true;
        int x0=MathHelper.floor_double(box.minX+.001),x1=MathHelper.floor_double(box.maxX-.001);
        int y0=MathHelper.floor_double(box.minY+.001),y1=MathHelper.floor_double(box.maxY-.001);
        int z0=MathHelper.floor_double(box.minZ+.001),z1=MathHelper.floor_double(box.maxZ-.001);
        if((long)(x1-x0+1)*(y1-y0+1)*(z1-z0+1)>64)return true;
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++){
            block.set(x,y,z);if(!mc.theWorld.isBlockLoaded(block,false)||mc.theWorld.getBlockState(block).getBlock()==Blocks.web)return true;
        }
        return false;
    }
    private static Field findWeb(){
        for(String name:new String[]{"field_70134_J","isInWeb"})try{Field f=Entity.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException|RuntimeException ignored){}
        return null;
    }
}
