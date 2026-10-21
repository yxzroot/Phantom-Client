package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.render.worldparticles.*;

/** Cosmetic ambient shapes: independent of server weather, with standard config/widgets. */
public final class WorldParticles extends Module {
    private final ComboSetting<ParticleOptions.Shape> shape=new ComboSetting<>("Shape",ParticleOptions.Shape.Stars);
    private final ComboSetting<ParticleOptions.Motion> motion=new ComboSetting<>("Motion",ParticleOptions.Motion.Drift);
    private final ComboSetting<ParticleOptions.ColorMode> mode=new ComboSetting<>("Color mode",ParticleOptions.ColorMode.Static);
    private final PickerRGBSetting color=new PickerRGBSetting("Color",219,213,255,null);
    private final PickerRGBSetting second=new PickerRGBSetting("Second color",112,223,255,null);
    private final SliderSetting count=new SliderSetting("Particle count",140,20,300,10);
    private final SliderSetting size=new SliderSetting("Size",.13,.04,.4,.01);
    private final SliderSetting radius=new SliderSetting("Radius",10,4,24,1);
    private final SliderSetting speed=new SliderSetting("Speed",1,0,3,.1);
    private final SliderSetting wind=new SliderSetting("Wind",.35,0,2,.05);
    private final SliderSetting opacity=new SliderSetting("Opacity (%)",80,0,100,1);
    private final SliderSetting glow=new SliderSetting("Glow (%)",35,0,100,1);
    private final TickSetting rotate=new TickSetting("Rotate",true),twinkle=new TickSetting("Twinkle",true);
    private final TickSetting outdoors=new TickSetting("Outdoors only",true);
    private final TickSetting first=new TickSetting("Show in first person",true),third=new TickSetting("Show in third person",true);
    private final TickSetting low=new TickSetting("Low quality",false);
    public final ParticleOptions options=new ParticleOptions();
    public final ParticleController controller=new ParticleController(this);
    public WorldParticles(){
        super("WorldParticles",ModuleCategory.render);
        withDescription("Floating stars, snow, raindrops and ETH symbols. No rain required. Still or drifting in the world.");
        registerSettings(shape,motion,mode,color,second,count,size,radius,speed,wind,opacity,glow,rotate,twinkle,outdoors,first,third,low);
    }
    private static int rgb(PickerRGBSetting c){return c.getRed()<<16|c.getGreen()<<8|c.getBlue();}
    public void snapshot(){
        options.shape=shape.getMode();options.motion=motion.getMode();options.mode=mode.getMode();
        options.color=rgb(color);options.second=rgb(second);
        options.count=(int)ParticleOptions.clamp((float)count.getInput(),20,300);
        options.size=ParticleOptions.clamp((float)size.getInput(),.04F,.4F);
        options.radius=ParticleOptions.clamp((float)radius.getInput(),4,24);
        options.speed=ParticleOptions.clamp((float)speed.getInput(),0,3);
        options.wind=ParticleOptions.clamp((float)wind.getInput(),0,2);
        options.opacity=ParticleOptions.clamp((float)opacity.getInput()/100,0,1);
        options.glow=ParticleOptions.clamp((float)glow.getInput()/100,0,1);
        options.rotate=rotate.isToggled();options.twinkle=twinkle.isToggled();options.outdoors=outdoors.isToggled();
        options.first=first.isToggled();options.third=third.isToggled();options.low=low.isToggled();
    }
    @Override public void onEnable(){snapshot();controller.start();}
    @Override public void onDisable(){controller.fadeOut();}
    @Override public String getHudSuffix(){return shape.getMode().toString();}
}
