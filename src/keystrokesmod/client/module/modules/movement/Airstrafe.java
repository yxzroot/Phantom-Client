package keystrokesmod.client.module.modules.movement;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.MoveInputEvent;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.module.modules.movement.airstrafe.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;

/** Horizontal-only air control using Phantom's established movement event and config APIs. */
public class Airstrafe extends Module {
    /** Standard slider widget/serialization, cached reads instead of per-read BigDecimal. */
    private static final class AirSlider extends SliderSetting {
        private double cached;private final double initial;
        AirSlider(String name,double initial,double min,double max,double step){super(name,initial,min,max,step);this.initial=initial;cached=initial;}
        @Override public double getInput(){return cached;}
        @Override public void setValue(double v){if(!Double.isFinite(v))v=getMin();super.setValue(v);cached=super.getInput();}
        @Override public void resetToDefaults(){super.resetToDefaults();cached=initial;}
    }
    private final SliderSetting control=new AirSlider("Air control strength (%)",40,0,100,1);
    private final SliderSetting cap=new AirSlider("Speed cap (blocks/tick)",.5,.1,.8,.01);
    private final SliderSetting smoothing=new AirSlider("Smoothing (%)",60,0,100,1);
    private final TickSetting sprint=new TickSetting("Only while sprinting",false),jump=new TickSetting("Only while holding jump",false);
    private final TickSetting special=new TickSetting("Disable in water/lava/webs/ladders",true),debug=new TickSetting("Debug",false);
    public final AirOptions options=new AirOptions();
    public final AirController controller=new AirController();
    private final AirDebug hud=new AirDebug();
    private boolean listening,reported;
    private int suffixStrength=-1;private String suffix="40%";
    public Airstrafe(){
        super("Airstrafe",ModuleCategory.movement);
        withDescription("Smooth mid-air steering. Horizontal acceleration only; no auto-jump, vertical changes or timer boost. The cap never deletes existing knockback.");
        registerSettings(control,cap,smoothing,sprint,jump,special,debug);
    }
    public void snapshot(){
        options.strength=control.getInput()/100;options.cap=cap.getInput();options.smoothing=smoothing.getInput()/100;
        options.sprintOnly=sprint.isToggled();options.jumpOnly=jump.isToggled();options.disableSpecial=special.isToggled();options.debug=debug.isToggled();
        int value=(int)control.getInput();if(value!=suffixStrength){suffixStrength=value;suffix=value+"%";}
    }
    @Override public String getHudSuffix(){return suffix;}
    @Override public void onEnable(){controller.reset();snapshot();if(!listening){MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);listening=true;}}
    @Override public void onDisable(){controller.reset();if(listening){MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().bus().unregister(this);listening=false;}}
    @Override public void postApplyConfig(){controller.reset();snapshot();}
    @Subscribe public void move(MoveInputEvent event){if(!isEnabled())return;try{snapshot();controller.move(mc,event,options);}catch(RuntimeException|ReflectiveOperationException|LinkageError failure){fail(failure);}}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){if(isEnabled()&&event.phase==TickEvent.Phase.END)try{controller.afterTick(mc);}catch(RuntimeException|LinkageError failure){fail(failure);}}
    @SubscribeEvent public void unload(WorldEvent.Unload event){controller.unload(event.world);}
    @Subscribe public void hud(Render2DEvent event){if(isEnabled()&&debug.isToggled()&&mc.theWorld!=null&&mc.thePlayer!=null&&mc.currentScreen==null)try{hud.draw(mc,controller);}catch(RuntimeException|LinkageError failure){fail(failure);}}
    private void fail(Throwable error){if(!reported){reported=true;LogManager.getLogger("Phantom/Airstrafe").error("Airstrafe disabled after a movement/render error",error);}disable();}
}
