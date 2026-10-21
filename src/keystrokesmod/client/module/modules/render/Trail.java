package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.render.trail.*;

/** Independent cosmetic world trail. Uses the standard registry, widgets and config codec. */
public final class Trail extends Module {
    private final ComboSetting<TrailOptions.Style> style=new ComboSetting<>("Style",TrailOptions.Style.Ribbon);
    private final ComboSetting<TrailOptions.ColorMode> mode=new ComboSetting<>("Color mode",TrailOptions.ColorMode.Static);
    private final PickerRGBSetting color=new PickerRGBSetting("Color",135,100,255,null);
    private final PickerRGBSetting second=new PickerRGBSetting("Second color",55,220,255,null);
    private final ComboSetting<TrailOptions.Preset> preset=new ComboSetting<>("Preset",TrailOptions.Preset.Custom);
    private final SliderSetting length=new SliderSetting("Length (s)",1.2,.25,3,.05);
    private final SliderSetting width=new SliderSetting("Width",.625,.04,1.2,.005);
    private final SliderSetting topHeight=new SliderSetting("Top height",1.5,.8,1.8,.025);
    private final SliderSetting hemHeight=new SliderSetting("Hem height",0,0,1.2,.025);
    private final SliderSetting intensity=new SliderSetting("Intensity (%)",75,0,100,1);
    private final SliderSetting bloom=new SliderSetting("Bloom strength (%)",40,0,100,1);
    private final SliderSetting bandHeight=new SliderSetting("Band height",.35,.1,1.5,.01);
    private final SliderSetting bandLength=new SliderSetting("Band length",1.6,.4,6,.05);
    private final SliderSetting heightOffset=new SliderSetting("Height offset",1.05,0,4,.025);
    private final TickSetting matchLength=new TickSetting("Match trail length",true);
    private final SliderSetting edgeWidth=new SliderSetting("Edge width",1,.5,3,.25);
    private final SliderSetting tailTaper=new SliderSetting("Tail taper amount",65,0,100,1);
    private final SliderSetting opacity=new SliderSetting("Opacity",90,0,100,1);
    private final SliderSetting shimmer=new SliderSetting("Shimmer",12,0,100,1);
    private final ComboSetting<TrailOptions.GradientDirection> gradientDirection=new ComboSetting<>("Gradient direction",TrailOptions.GradientDirection.HeadToTail);
    private final TickSetting first=new TickSetting("Show in first person",true);
    private final TickSetting third=new TickSetting("Show in third person",true);
    private final TickSetting others=new TickSetting("Show on other players",false);
    private final TickSetting sparkles=new TickSetting("Sparkles",true);
    private final TickSetting landing=new TickSetting("Landing burst",true);
    private final TickSetting low=new TickSetting("Low quality",false);
    private TrailOptions.Preset previousPreset=TrailOptions.Preset.Custom;
    private int knownColor,knownSecond;private boolean colorsKnown;
    public final TrailOptions options=new TrailOptions();
    public final TrailController controller=new TrailController(this);

    public Trail(){
        super("Trail",ModuleCategory.render);
        withDescription("Smooth cosmetic player trails, including a slim solid light band.");
        registerSettings(style,mode,color,second,preset,length,width,topHeight,hemHeight,bandHeight,bandLength,matchLength,heightOffset,edgeWidth,tailTaper,opacity,shimmer,gradientDirection,intensity,bloom,first,third,others,sparkles,landing,low);
    }
    private void applyPreset(){
        previousPreset=preset.getMode();if(previousPreset==TrailOptions.Preset.Custom)return;
        int a=previousPreset.start,b=previousPreset.end;
        color.setRed(a>>16&255);color.setGreen(a>>8&255);color.setBlue(a&255);
        second.setRed(b>>16&255);second.setGreen(b>>8&255);second.setBlue(b&255);
        mode.setMode(TrailOptions.ColorMode.Gradient);
        rememberColors();
    }
    private static int rgb(PickerRGBSetting c){return c.getRed()<<16|c.getGreen()<<8|c.getBlue();}
    private void rememberColors(){knownColor=rgb(color);knownSecond=rgb(second);colorsKnown=true;}
    @Override public void guiUpdate(){
        if(previousPreset!=preset.getMode())applyPreset();
        if(colorsKnown&&(knownColor!=rgb(color)||knownSecond!=rgb(second))){
            // A preset is a starting point, never an override of the user's picker.
            preset.setMode(TrailOptions.Preset.Custom);previousPreset=TrailOptions.Preset.Custom;
            if(mode.getMode()==TrailOptions.ColorMode.Theme||mode.getMode()==TrailOptions.ColorMode.Rainbow)mode.setMode(TrailOptions.ColorMode.Static);
        }
        rememberColors();
    }
    @Override public void onGuiClose(){guiUpdate();}
    @Override public void guiButtonToggled(Setting setting){if(setting==preset)applyPreset();}
    @Override public void postApplyConfig(){previousPreset=preset.getMode();rememberColors();}
    public void snapshot(){
        guiUpdate();options.style=style.getMode();options.mode=mode.getMode();
        options.color=rgb(color);options.second=rgb(second);
        if(options.mode==TrailOptions.ColorMode.Theme){options.color=PhantomHUD.visualAccent(false)&0xFFFFFF;options.second=PhantomHUD.visualAccent(true)&0xFFFFFF;}
        options.length=TrailOptions.clamp((float)length.getInput(),.25F,3);
        options.width=TrailOptions.clamp((float)width.getInput(),.04F,1.2F);
        options.top=TrailOptions.clamp((float)topHeight.getInput(),.8F,1.8F);
        options.hem=TrailOptions.clamp((float)hemHeight.getInput(),0,options.top-.15F);
        options.intensity=TrailOptions.clamp((float)intensity.getInput()/100,0,1);
        options.bloom=TrailOptions.clamp((float)bloom.getInput()/100,0,1);
        options.bandHeight=TrailOptions.clamp((float)bandHeight.getInput(),.1F,1.5F);
        options.bandLength=TrailOptions.clamp((float)bandLength.getInput(),.4F,6);
        options.heightOffset=TrailOptions.clamp((float)heightOffset.getInput(),0,4);
        options.matchLength=matchLength.isToggled();
        options.edgeWidth=TrailOptions.clamp((float)edgeWidth.getInput(),.5F,3);
        options.tailTaper=TrailOptions.clamp((float)tailTaper.getInput()/100,0,1);
        options.opacity=TrailOptions.clamp((float)opacity.getInput()/100,0,1);
        options.shimmer=TrailOptions.clamp((float)shimmer.getInput()/100,0,1);
        options.gradientDirection=gradientDirection.getMode();
        options.first=first.isToggled();options.third=third.isToggled();options.others=others.isToggled();
        options.sparkles=sparkles.isToggled();options.landing=landing.isToggled();options.low=low.isToggled();
    }
    @Override public void onEnable(){snapshot();controller.start();}
    @Override public void onDisable(){controller.fadeOut();}
    @Override public String getHudSuffix(){return style.getMode().toString();}
}
