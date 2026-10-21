package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.render.killeffect.*;

/** Cosmetic kill celebrations. Uses the same registry, widgets and config codec as Trail. */
public final class KillEffect extends Module {
    private final ComboSetting<KillOptions.Effect> effect=new ComboSetting<>("Effect",KillOptions.Effect.Lightning);
    private final ComboSetting<KillOptions.ColorMode> mode=new ComboSetting<>("Color mode",KillOptions.ColorMode.Static);
    private final PickerRGBSetting color=new PickerRGBSetting("Color",169,152,255,null),second=new PickerRGBSetting("Second color",85,222,255,null);
    private final ComboSetting<KillOptions.Preset> preset=new ComboSetting<>("Preset",KillOptions.Preset.Custom);
    private final TickSetting byEffect=new TickSetting("Color by effect",true);
    private final SliderSetting size=new SliderSetting("Size",1,.3,3,.05),intensity=new SliderSetting("Intensity (%)",80,0,100,1),duration=new SliderSetting("Duration (s)",1.6,.4,4,.1);
    private final ComboSetting<KillOptions.Targets> targets=new ComboSetting<>("Targets",KillOptions.Targets.PlayersAndMobs);
    private final SliderSetting distance=new SliderSetting("Max distance",64,8,128,1);
    private final TickSetting others=new TickSetting("Others' kills",false),sound=new TickSetting("Sound",true);
    private final SliderSetting volume=new SliderSetting("Volume (%)",45,0,100,1);
    private final TickSetting flash=new TickSetting("Screen flash",false),pulse=new TickSetting("Camera pulse",false);
    private final SliderSetting flashStrength=new SliderSetting("Flash strength (%)",12,0,40,1),pulseStrength=new SliderSetting("Camera pulse strength (%)",2.5,0,8,.25);
    private final TickSetting streak=new TickSetting("Streak scaling",true),popup=new TickSetting("Popup",true),low=new TickSetting("Low quality",false);
    public final KillCounter counter=new KillCounter();
    public final KillOptions options=new KillOptions();
    public final KillController controller=new KillController(this);
    private KillOptions.Preset previous=KillOptions.Preset.Custom;
    private int knownColor,knownSecond;private boolean colorsKnown;
    public KillEffect(){
        super("KillEffect",ModuleCategory.render);withDescription("Client-only kill celebrations: ten effects, no damage or world changes. Kill credit is best-effort on multiplayer.");
        registerSettings(effect,mode,color,second,preset,byEffect,size,intensity,duration,targets,distance,others,sound,volume,flash,flashStrength,pulse,pulseStrength,streak,popup,counter,low);
    }
    private static int rgb(PickerRGBSetting c){return c.getRed()<<16|c.getGreen()<<8|c.getBlue();}
    private void remember(){knownColor=rgb(color);knownSecond=rgb(second);colorsKnown=true;}
    private void applyPreset(){previous=preset.getMode();if(previous!=KillOptions.Preset.Custom){int a=previous.a,b=previous.b;color.setRed(a>>16&255);color.setGreen(a>>8&255);color.setBlue(a&255);second.setRed(b>>16&255);second.setGreen(b>>8&255);second.setBlue(b&255);mode.setMode(KillOptions.ColorMode.Gradient);byEffect.disable();}remember();}
    @Override public void guiUpdate(){
        if(previous!=preset.getMode())applyPreset();
        if(colorsKnown&&(knownColor!=rgb(color)||knownSecond!=rgb(second))){preset.setMode(KillOptions.Preset.Custom);previous=KillOptions.Preset.Custom;byEffect.disable();if(mode.getMode()==KillOptions.ColorMode.Rainbow)mode.setMode(KillOptions.ColorMode.Static);}
        remember();
    }
    @Override public void guiButtonToggled(Setting setting){if(setting==preset)applyPreset();}
    @Override public void onGuiClose(){guiUpdate();}
    @Override public void postApplyConfig(){previous=preset.getMode();remember();}
    public void snapshot(){
        guiUpdate();options.effect=effect.getMode();options.mode=mode.getMode();options.targets=targets.getMode();options.color=rgb(color);options.second=rgb(second);options.byEffect=byEffect.isToggled();
        options.size=KillOptions.clamp((float)size.getInput(),.3F,3);options.intensity=KillOptions.clamp((float)intensity.getInput()/100,0,1);options.duration=KillOptions.clamp((float)duration.getInput(),.4F,4);
        options.distance=KillOptions.clamp((float)distance.getInput(),8,128);options.volume=KillOptions.clamp((float)volume.getInput()/100,0,1);options.flashStrength=KillOptions.clamp((float)flashStrength.getInput()/100,0,.4F);options.pulseStrength=KillOptions.clamp((float)pulseStrength.getInput()/100,0,.08F);
        options.others=others.isToggled();options.sound=sound.isToggled();options.flash=flash.isToggled();options.pulse=pulse.isToggled();options.streak=streak.isToggled();options.popup=popup.isToggled();options.low=low.isToggled();
    }
    @Override public void onEnable(){snapshot();controller.start();}
    @Override public void onDisable(){controller.stop();}
    @Override public String getHudSuffix(){return effect.getMode().toString();}
}
