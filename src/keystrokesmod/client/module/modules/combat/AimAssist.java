package keystrokesmod.client.module.modules.combat;

import com.google.common.eventbus.Subscribe;
import com.google.gson.*;
import java.util.ArrayList;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.combat.aim.*;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import keystrokesmod.client.event.impl.Render2DEvent;
import org.apache.logging.log4j.LogManager;

/** Gentle trailing camera assistance; keeps the legacy module/config/friends ABI. */
public class AimAssist extends Module {
    private static AimAssist instance;
    // Public legacy field/method signatures are retained for existing client code.
    public static SliderSetting speedYaw,complimentYaw,speedPitch,complimentPitch,fov,distance,pitchOffSet;
    public static TickSetting clickAim,stopWhenOver,aimPitch,weaponOnly,aimInvis,breakBlocks,blatantMode;
    public static ArrayList<Entity> friends=new ArrayList<>();
    private final SliderSetting trail=new AssistSlider("Trail strength (%)",100,0,100,1);
    private final SliderSetting delay=new AssistSlider("Trail delay (ms)",120,0,450,5);
    private final SliderSetting offset=new AssistSlider("Trail offset (blocks)",.035,0,.2,.005);
    private final SliderSetting maxOffset=new AssistSlider("Max offset (degrees)",4,0,12,.1);
    private final ComboSetting<AssistOptions.Hitbox> hitbox=new ComboSetting<>("Hitbox target",AssistOptions.Hitbox.Chest);
    private final ComboSetting<AssistOptions.Targets> targets=new ComboSetting<>("Targets",AssistOptions.Targets.Players);
    private final SliderSetting human=new AssistSlider("Humanization (%)",20,0,100,1);
    private final SliderSetting fade=new AssistSlider("Fade time (ms)",150,50,400,10);
    private final SliderSetting switchDelay=new AssistSlider("Switch delay (ms)",180,50,500,10);
    private final TickSetting moving=new TickSetting("Only while moving mouse",false),noGui=new TickSetting("No GUI",true);
    private final TickSetting teams=new TickSetting("Ignore teammates",true),invis=new TickSetting("Ignore invisible",true);
    private final TickSetting creative=new TickSetting("Ignore creative / spectator",true),debug=new TickSetting("Debug",false);
    public final AssistOptions options=new AssistOptions();
    public final AssistController controller=new AssistController(this);
    private final AssistDebug marker=new AssistDebug();
    private boolean forgeRegistered,reported;
    private Object lastTick,lastWorld;
    private boolean frameConsumed;
    private String driver="Waiting for callback";
    public AimAssist(){
        super("AimAssist",ModuleCategory.combat);instance=this;
        withDescription("Gentle delayed tracking. Stops short of the aim point; your mouse always wins. No snapping or lock-on.");
        speedYaw=new AssistSlider("Speed",4,.5,60,.1);
        complimentYaw=new AssistSlider("Yaw strength (%)",80,0,100,1);
        speedPitch=new AssistSlider("Pitch strength (%)",15,0,100,1);complimentPitch=speedPitch;
        fov=new AssistSlider("FOV",60,5,180,1);distance=new AssistSlider("Range",4.5,1,6,.1);
        clickAim=new TickSetting("Only while clicking",false);weaponOnly=new TickSetting("Weapon only",false);
        breakBlocks=new TickSetting("Pause while mining",true);
        pitchOffSet=new AssistSlider("Legacy pitch offset",0,-2,2,.05);stopWhenOver=new TickSetting("Legacy stop",true);
        aimPitch=new TickSetting("Legacy pitch",true);aimInvis=new TickSetting("Legacy invisible",false);blatantMode=new TickSetting("Legacy blatant (unused)",false);
        registerSettings(speedYaw,complimentYaw,speedPitch,fov,distance,trail,delay,offset,maxOffset,hitbox,human,fade,switchDelay,
                clickAim,moving,weaponOnly,noGui,teams,invis,creative,targets,breakBlocks,debug);
    }
    public void snapshot(){
        options.speed=speedYaw.getInput();options.yawStrength=complimentYaw.getInput()/100;options.pitchStrength=speedPitch.getInput()/100;
        options.fov=fov.getInput();options.range=distance.getInput();options.trail=trail.getInput()/100;options.delay=delay.getInput()/1000;
        options.offset=offset.getInput();options.maxOffset=maxOffset.getInput();options.hitbox=hitbox.getMode();options.humanization=human.getInput()/100;
        options.fade=fade.getInput()/1000;options.switchDelay=switchDelay.getInput()/1000;options.targets=targets.getMode();
        options.clicking=clickAim.isToggled();options.moving=moving.isToggled();options.weapon=weaponOnly.isToggled();options.noGui=noGui.isToggled();
        options.teams=teams.isToggled();options.invisible=invis.isToggled();options.creative=creative.isToggled();options.mining=breakBlocks.isToggled();options.debug=debug.isToggled();
    }
    protected double clock(){return System.nanoTime()*1E-9;}
    private boolean runFrame(float partial,String route){
        if(!isEnabled()||frameConsumed)return false;
        frameConsumed=true;driver=route;
        try{controller.frame(mc,partial,clock());return true;}catch(RuntimeException|LinkageError e){fail(e);return false;}
    }
    public static void renderFrame(float partial){AimAssist m=instance;if(m!=null)m.runFrame(partial,"World hook");}
    /** Lunar can bypass renderWorld; this existing camera hook still runs before world drawing. */
    public static void cameraFrame(float partial){AimAssist m=instance;if(m!=null&&m.runFrame(partial,"Camera fallback"))m.refreshPicking(partial);}
    private void refreshPicking(float partial){try{if(controller.hasTarget&&(controller.pull.yawStep!=0||controller.pull.pitchStep!=0)&&mc.entityRenderer!=null)mc.entityRenderer.getMouseOver(partial);}catch(RuntimeException|LinkageError failure){fail(failure);}}
    /** Entity.setAngles arguments precede vanilla's 0.15 factor and inverted pitch. */
    public static void observeMouseTurn(Entity entity,float yaw,float pitch){AimAssist m=instance;if(m!=null&&m.isEnabled()&&entity==mc.thePlayer)m.controller.mouse(yaw*.15,-pitch*.15);}
    @Override public void onEnable(){controller.reset();frameConsumed=false;lastTick=lastWorld=null;snapshot();if(!forgeRegistered){MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);forgeRegistered=true;}}
    @Override public void onDisable(){controller.reset();frameConsumed=false;lastTick=lastWorld=null;if(forgeRegistered){MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().bus().unregister(this);forgeRegistered=false;}}
    @Override public void postApplyConfig(){controller.reset();snapshot();}
    @Override public String getHudSuffix(){return "Trailing";}
    @SubscribeEvent public void unload(WorldEvent.Unload e){controller.unload(e.world);}
    @SubscribeEvent public void renderTick(TickEvent.RenderTickEvent e){if(isEnabled()&&e.phase==TickEvent.Phase.START&&e!=lastTick){lastTick=e;frameConsumed=false;}}
    @SubscribeEvent public void mouse(MouseEvent e){if(isEnabled()&&e.button==0&&e.buttonstate)controller.attackGate.press(clock());}
    @SubscribeEvent public void worldLast(RenderWorldLastEvent e){
        if(!isEnabled()||e==lastWorld)return;lastWorld=e;
        try{if(runFrame(e.partialTicks,"Forge fallback"))refreshPicking(e.partialTicks);if(options.debug&&controller.hasTarget)marker.draw(mc,controller);}
        catch(RuntimeException|LinkageError failure){fail(failure);}
    }
    @Subscribe public void onRender(ForgeEvent e){if(e.getEvent() instanceof TickEvent.RenderTickEvent)renderTick((TickEvent.RenderTickEvent)e.getEvent());else if(e.getEvent() instanceof RenderWorldLastEvent)worldLast((RenderWorldLastEvent)e.getEvent());else if(e.getEvent() instanceof MouseEvent)mouse((MouseEvent)e.getEvent());}
    @Subscribe public void debugHud(Render2DEvent e){if(isEnabled()&&debug.isToggled()&&mc.thePlayer!=null&&mc.currentScreen==null)try{marker.hud(mc,controller,driver);}catch(RuntimeException|LinkageError failure){fail(failure);}}
    private void fail(Throwable failure){if(!reported){reported=true;LogManager.getLogger("Phantom/AimAssist").error("AimAssist disabled after a render/controller error",failure);}disable();}
    @Override public void applyConfigFromJson(JsonObject data){
        // Convert controls once in memory. Nothing rewrites the user's saved profile.
        JsonObject s=data.has("settings")?data.getAsJsonObject("settings"):null;
        if(s!=null&&s.has("Speed 1 (yaw)")){
            JsonObject copy=new JsonParser().parse(data.toString()).getAsJsonObject();s=copy.getAsJsonObject("settings");
            legacyNumber(s,"Speed 1 (yaw)","Speed",.1,.5,20);
            legacyNumber(s,"Speed 2 (yaw)","Yaw strength (%)",3,0,100);
            legacyNumber(s,"Distance","Range",1,1,6);
            legacyToggle(s,"Click aim","Only while clicking");legacyToggle(s,"Break blocks","Pause while mining");
            if(s.has("Aim pitch")&&!s.getAsJsonObject("Aim pitch").get("value").getAsBoolean())number(s,"Pitch strength (%)",0);
            super.applyConfigFromJson(copy);
        }else super.applyConfigFromJson(data);
    }
    private static void legacyNumber(JsonObject s,String old,String name,double scale,double min,double max){if(s.has(old))number(s,name,PositionHistory.clamp(s.getAsJsonObject(old).get("value").getAsDouble()*scale,min,max));}
    private static void number(JsonObject s,String key,double value){JsonObject item=new JsonObject();item.addProperty("type","slider");item.addProperty("value",value);s.add(key,item);}
    private static void legacyToggle(JsonObject s,String old,String key){if(s.has(old))s.add(key,s.get(old));}
    public Entity getEnemy(){return controller.getTarget();}
    public static boolean isFriend(Entity entity){for(int i=0;i<friends.size();i++){Entity f=friends.get(i);if(f!=null&&(f==entity||f.getUniqueID().equals(entity.getUniqueID())))return true;}return false;}
    public static void addFriend(Entity entity){if(entity!=null&&!isFriend(entity))friends.add(entity);}
    public static boolean addFriend(String name){if(mc.theWorld==null||name==null)return false;boolean found=false;java.util.List<Entity> list=mc.theWorld.loadedEntityList;for(int i=0;i<list.size();i++){Entity e=list.get(i);if(e.getName().equalsIgnoreCase(name)&&!isFriend(e)){addFriend(e);found=true;}}return found;}
    public static boolean removeFriend(String name){boolean removed=false;for(int i=friends.size()-1;i>=0;i--){Entity e=friends.get(i);if(e!=null&&e.getName().equalsIgnoreCase(name)){friends.remove(i);removed=true;}}return removed;}
    public static boolean removeFriend(Entity entity){return friends.remove(entity);}
    public static ArrayList<Entity> getFriends(){return friends;}
}
