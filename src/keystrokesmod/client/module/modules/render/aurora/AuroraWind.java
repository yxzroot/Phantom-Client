package keystrokesmod.client.module.modules.render.aurora;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.util.ResourceLocation;

/** Optional low-volume vanilla rain hiss approximates wind; no downloads or sound assets. */
public final class AuroraWind implements ITickableSound {
    private final ResourceLocation location=new ResourceLocation("ambient.weather.rain");
    private boolean done;
    public float strength;
    public void finish(){done=true;}
    @Override public boolean isDonePlaying(){return done;}
    @Override public void update(){}
    @Override public ResourceLocation getSoundLocation(){return location;}
    @Override public boolean canRepeat(){return !done;}
    @Override public int getRepeatDelay(){return 0;}
    @Override public float getVolume(){return .035F*strength;}
    @Override public float getPitch(){return .65F;}
    @Override public float getXPosF(){return 0;}
    @Override public float getYPosF(){return 0;}
    @Override public float getZPosF(){return 0;}
    @Override public ISound.AttenuationType getAttenuationType(){return ISound.AttenuationType.NONE;}
}
