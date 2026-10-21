package keystrokesmod.client.module.modules.render.aurora;

/** Reused frame snapshot; settings widgets/config stay in Phantom's module. */
public final class AuroraOptions {
    public final ArtSkyOptions art=new ArtSkyOptions();
    public boolean aurora = true, shooting = true, reflection = true, vignette = true, sound, otherSkies;
    public boolean lockTime = true, cloudTint, hideClouds;
    public boolean bloom=true,milkyWay=true,moon=true,lowQuality;
    public float folds=1;
    public boolean constellation,constellationLines=true;
    public float constellationMix,constellationSpeed=1,constellationDensity=.65F,constellationBrightness=1;
    public boolean overcast,duskLightning,duskStars,duskFog=true;
    public int duskColor=0x2D2156,duskSecond=0x526480;
    public float duskMix,duskSpeed=1,duskContrast=1.1F,duskScale=1.3F,duskGlow=.5F,duskDarkness=.55F,duskWorld=.3F;
    public boolean starField=true;
    public float shootingChance=.35F,shootingInterval=8;
    public int cloudMode;
    public float intensity = 1, speed = 1, stars = .7F, wind = .6F, cold = .3F, fadeSeconds = .3F, cloudStrength = .35F;
    public int curtains = 4, palette, snow = 1, accent = 0x66EEAA, cloudColor = 0xA2BFD8;
    public float time = 18000;
}
