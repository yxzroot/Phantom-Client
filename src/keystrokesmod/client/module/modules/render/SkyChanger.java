package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.render.aurora.*;
import keystrokesmod.client.module.setting.impl.*;

/** Client-only skies; no packets, biome edits, block replacements, or world-time writes. */
public final class SkyChanger extends Module {
    public enum Style { Vanilla, AntarcticAurora,Constellation,OvercastDusk,CosmicNebula,SynthwaveSunset,CherryBlossomDusk; public String toString() { return this == AntarcticAurora ? "Antarctic Aurora" : this==OvercastDusk?"Overcast Dusk":this==CosmicNebula?"Cosmic Nebula":this==SynthwaveSunset?"Synthwave Sunset":this==CherryBlossomDusk?"Cherry Blossom Dusk":name(); } }
    public enum ArtPalette { Automatic,MagentaBlue,TealGold,CrimsonViolet,Classic,Miami,SunsetGold,ClassicSakura,Twilight,PaleSpring,Theme;
        public String toString(){return name().replace("MagentaBlue","Magenta-Blue").replace("TealGold","Teal-Gold").replace("CrimsonViolet","Crimson-Violet").replace("SunsetGold","Sunset Gold").replace("ClassicSakura","Classic Sakura").replace("PaleSpring","Pale Spring");} }
    private final ComboSetting<ArtPalette> artPalette=new ComboSetting<>("Sky palette",ArtPalette.Automatic);
    private final SliderSetting artBrightness=new SliderSetting("Sky brightness",1,.1,2,.05),artSpeed=new SliderSetting("Style cloud speed",1,.1,3,.1),artHaze=new SliderSetting("Horizon haze",1,0,2,.05),artWorld=new SliderSetting("Style world tint (%)",30,0,100,1);
    private final TickSetting artFog=new TickSetting("Style fog match",true),planet=new TickSetting("Planet",true);
    private final SliderSetting sunSize=new SliderSetting("Sun size",1.25,.4,2.5,.05),stripeSpeed=new SliderSetting("Stripe speed",1,0,3,.1);
    private final TickSetting skyline=new TickSetting("Skyline",true),grid=new TickSetting("Grid",false);
    public enum SkyBody { Sun,Moon }
    private final ComboSetting<SkyBody> cherryBody=new ComboSetting<>("Cherry sun or moon",SkyBody.Sun);
    private final ComboSetting<Snow> petals=new ComboSetting<>("Petal amount",Snow.Light);
    private final SliderSetting petalSize=new SliderSetting("Petal size",1,.3,2,.1),petalWind=new SliderSetting("Petal wind strength",.6,0,2,.1);
    private final TickSetting trees=new TickSetting("Blossom trees",true),lanterns=new TickSetting("Lanterns",false);
    public enum Palette { ClassicGreen, PinkPurple, FullSpectrum, Theme; public String toString() { return name().replace("ClassicGreen","Classic green").replace("PinkPurple","Pink-purple").replace("FullSpectrum","Full spectrum"); } }
    public enum Snow { Off, Light, Heavy }
    public enum CloudMode { Off, DarkTint, Wispy;public String toString(){return this==DarkTint?"Dark tint":name();} }
    private final ComboSetting<Style> style = new ComboSetting<>("Style", Style.AntarcticAurora);
    private final SliderSetting constellationSpeed=new SliderSetting("Constellation speed",1,.1,3,.1);
    private final SliderSetting constellationDensity=new SliderSetting("Constellation density (%)",65,0,100,1);
    private final SliderSetting constellationBrightness=new SliderSetting("Constellation brightness (%)",100,0,200,1);
    private final TickSetting constellationLines=new TickSetting("Constellation lines",true);
    private final PickerRGBSetting duskColor=new PickerRGBSetting("Dusk color tint",45,33,86,null),duskSecond=new PickerRGBSetting("Dusk second color",82,100,128,null);
    private final SliderSetting duskSpeed=new SliderSetting("Cloud speed",1,.1,3,.1),duskContrast=new SliderSetting("Cloud contrast",1.1,.4,2.5,.05),duskScale=new SliderSetting("Cloud scale",1.3,.5,3,.05);
    private final SliderSetting duskGlow=new SliderSetting("Horizon glow",.5,0,2,.05),duskDarkness=new SliderSetting("Darkness",.55,0,1,.05),duskWorld=new SliderSetting("World tint strength (%)",30,0,100,1);
    private final TickSetting duskLightning=new TickSetting("Dusk lightning",false),duskStars=new TickSetting("Dusk stars",false),duskFog=new TickSetting("Fog match",true);
    private final SliderSetting intensity = new SliderSetting("Aurora intensity (%)", 100, 0, 200, 1);
    private final SliderSetting speed = new SliderSetting("Aurora speed", 1, .1, 3, .1);
    private final SliderSetting curtains = new SliderSetting("Curtain count", 4, 4, 6, 1);
    private final SliderSetting folds = new SliderSetting("Fold strength", 1, 0, 2, .1);
    private final TickSetting bloom = new TickSetting("Bloom", true);
    private final TickSetting milkyWay = new TickSetting("Milky Way", true);
    private final TickSetting moon = new TickSetting("Moon", true);
    private final TickSetting lowQuality = new TickSetting("Low quality", false);
    private final ComboSetting<CloudMode> cloudMode=new ComboSetting<>("Cloud mode",CloudMode.Off);
    private final ComboSetting<Palette> palette = new ComboSetting<>("Aurora palette", Palette.ClassicGreen);
    private final SliderSetting stars = new SliderSetting("Star density (%)", 70, 0, 100, 1);
    private final TickSetting starField = new TickSetting("Stars", true);
    private final TickSetting shooting = new TickSetting("Shooting stars", true);
    private final SliderSetting shootingChance = new SliderSetting("Shooting star chance (%)", 35, 0, 100, 1);
    private final SliderSetting shootingInterval = new SliderSetting("Shooting star interval (s)", 8, 3, 60, 1);
    private final ComboSetting<Snow> snow = new ComboSetting<>("Snow", Snow.Light);
    private final SliderSetting wind = new SliderSetting("Wind strength", .6, 0, 2, .1);
    private final SliderSetting cold = new SliderSetting("Cold tint strength (%)", 30, 0, 100, 1);
    private final TickSetting reflection = new TickSetting("Aurora reflection", true);
    private final TickSetting vignette = new TickSetting("Vignette", true);
    private final TickSetting sound = new TickSetting("Wind sound", false);
    private final SliderSetting fade = new SliderSetting("Fade time (ms)", 300, 50, 2000, 50);
    private final TickSetting otherSkies = new TickSetting("Other dimensions (sky only)", false);
    private final TickSetting lockTime = new TickSetting("Time lock", true);
    private final SliderSetting time = new SliderSetting("Locked time", 18000, 0, 23999, 100);
    private final TickSetting hideClouds = new TickSetting("Hide clouds", false);
    private final TickSetting hideWorldBorder = new TickSetting("Hide world border", true);
    private final TickSetting cloudTint = new TickSetting("Cloud tint", false);
    private final PickerRGBSetting cloudColor = new PickerRGBSetting("Cloud color", 162, 191, 216, null);
    private final SliderSetting cloudStrength = new SliderSetting("Cloud tint strength (%)", 35, 0, 100, 1);
    public final AuroraOptions options = new AuroraOptions();
    public final AuroraController aurora = new AuroraController(this);
    private static SkyChanger instance;
    public SkyChanger() {
        super("SkyChanger", ModuleCategory.render);
        withDescription("Procedural aurora, constellation, overcast, cosmic, synthwave and sakura skies.");
        ArtSkyAssets.prewarm();
        registerSettings(style,duskColor,duskSecond,duskSpeed,duskContrast,duskScale,duskGlow,duskDarkness,duskLightning,duskStars,duskFog,duskWorld,constellationSpeed,constellationDensity,constellationBrightness,constellationLines,intensity,speed,curtains,folds,palette,bloom,milkyWay,moon,starField,stars,shooting,shootingChance,shootingInterval,cloudMode,lowQuality,snow,wind,cold,reflection,vignette,sound,fade,
                otherSkies,lockTime,time,hideClouds,hideWorldBorder,cloudTint,cloudColor,cloudStrength);
        registerSettings(artPalette,artBrightness,artSpeed,artHaze,artWorld,artFog,planet);
        registerSettings(sunSize,stripeSpeed,skyline,grid);
        registerSettings(cherryBody,petals,petalSize,petalWind,trees,lanterns);
        instance = this;
    }
    public void snapshot() {
        options.aurora=style.getMode()!=Style.Vanilla; // Existing controller's custom-sky activation gate.
        options.constellation=style.getMode()==Style.Constellation;
        options.overcast=style.getMode()==Style.OvercastDusk;
        options.art.style=style.getMode()==Style.CosmicNebula?1:style.getMode()==Style.SynthwaveSunset?2:style.getMode()==Style.CherryBlossomDusk?3:0;options.art.palette=artPalette.getMode().ordinal();
        options.art.body=cherryBody.getMode().ordinal();options.art.petals=petals.getMode().ordinal();options.art.petalSize=(float)petalSize.getInput();options.art.wind=(float)petalWind.getInput();options.art.trees=trees.isToggled();options.art.lanterns=lanterns.isToggled();
        options.art.sunSize=(float)sunSize.getInput();options.art.stripeSpeed=(float)stripeSpeed.getInput();options.art.skyline=skyline.isToggled();options.art.grid=grid.isToggled();
        options.art.brightness=(float)artBrightness.getInput();options.art.cloudSpeed=(float)artSpeed.getInput();options.art.haze=(float)artHaze.getInput();options.art.worldTint=(float)artWorld.getInput()/100;
        options.art.fogMatch=artFog.isToggled();options.art.planet=planet.isToggled();
        options.art.palette(options.art.palette,options.art.palette==10?PhantomHUD.visualAccent(false):0xA647BF,options.art.palette==10?PhantomHUD.visualAccent(true):0x2497C9);
        options.duskColor=duskColor.getRGB()&0xFFFFFF;options.duskSecond=duskSecond.getRGB()&0xFFFFFF;
        options.duskSpeed=(float)duskSpeed.getInput();options.duskContrast=(float)duskContrast.getInput();options.duskScale=(float)duskScale.getInput();
        options.duskGlow=(float)duskGlow.getInput();options.duskDarkness=(float)duskDarkness.getInput();options.duskWorld=(float)duskWorld.getInput()/100;
        options.duskLightning=duskLightning.isToggled();options.duskStars=duskStars.isToggled();options.duskFog=duskFog.isToggled();
        options.constellationSpeed=AuroraSimulation.clamp((float)constellationSpeed.getInput(),.1F,3);
        options.constellationDensity=AuroraSimulation.clamp((float)constellationDensity.getInput()/100,0,1);
        options.constellationBrightness=AuroraSimulation.clamp((float)constellationBrightness.getInput()/100,0,2);
        options.constellationLines=constellationLines.isToggled();
        options.intensity=(float)intensity.getInput()/100; options.speed=(float)speed.getInput();
        options.curtains=(int)curtains.getInput(); options.palette=palette.getMode().ordinal();
        options.folds=(float)folds.getInput();options.bloom=bloom.isToggled();options.milkyWay=milkyWay.isToggled();
        options.moon=moon.isToggled();options.lowQuality=lowQuality.isToggled();options.cloudMode=cloudMode.getMode().ordinal();
        options.stars=(float)stars.getInput()/100; options.shooting=shooting.isToggled(); options.snow=snow.getMode().ordinal();
        options.starField=starField.isToggled();options.shootingChance=(float)shootingChance.getInput()/100;options.shootingInterval=(float)shootingInterval.getInput();
        options.wind=(float)wind.getInput(); options.cold=(float)cold.getInput()/100;
        options.reflection=reflection.isToggled(); options.vignette=vignette.isToggled(); options.sound=sound.isToggled();
        options.fadeSeconds=(float)fade.getInput()/1000; options.otherSkies=otherSkies.isToggled();
        options.lockTime=lockTime.isToggled(); options.time=(float)time.getInput();
        options.hideClouds=hideClouds.isToggled();
        options.cloudTint=cloudTint.isToggled(); options.cloudColor=cloudColor.getRGB(); options.cloudStrength=(float)cloudStrength.getInput()/100;
        if(options.palette==3) options.accent=PhantomHUD.visualAccent(false);
    }
    @Override public void onEnable() { snapshot(); aurora.start(); }
    @Override public void onDisable() { aurora.beginFadeOut(); }
    @Override public String getHudSuffix() { return style.getMode().toString(); }
    public static SkyChanger current() { return instance; }
    /** Render-only gate, read directly so toggling restores the border immediately. */
    public static boolean hidesWorldBorder() {
        SkyChanger sky=instance;
        return sky!=null && sky.isEnabled() && sky.hideWorldBorder.isToggled();
    }
}
