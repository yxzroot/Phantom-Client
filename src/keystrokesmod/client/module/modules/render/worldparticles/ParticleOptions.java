package keystrokesmod.client.module.modules.render.worldparticles;

/** Reused per-frame snapshot; no weather or SkyChanger dependency. */
public final class ParticleOptions {
    public enum Shape { Stars, Snow, Rain, ETH }
    public enum Motion { Still, Drift }
    public enum ColorMode { Static, Rainbow, Gradient }
    public Shape shape=Shape.Stars;
    public Motion motion=Motion.Drift;
    public ColorMode mode=ColorMode.Static;
    public int color=0xDBD5FF,second=0x70DFFF,count=140;
    public float size=.13F,radius=10,speed=1,wind=.35F,opacity=.8F,glow=.35F;
    public boolean rotate=true,twinkle=true,outdoors=true,first=true,third=true,low;
    public int limit(){return Math.min(low?120:300,Math.max(0,count));}
    public static float clamp(float v,float lo,float hi){return Float.isFinite(v)?Math.max(lo,Math.min(hi,v)):lo;}
    public int tint(float phase,double time){
        if(mode==ColorMode.Static)return color;
        if(mode==ColorMode.Rainbow){double h=phase+time*.075;return java.awt.Color.HSBtoRGB((float)(h-Math.floor(h)),.65F,1)&0xFFFFFF;}
        float t=.5F+.5F*(float)Math.sin(phase*6.2831853+time*.25);
        int r=Math.round((color>>16&255)*(1-t)+(second>>16&255)*t);
        int g=Math.round((color>>8&255)*(1-t)+(second>>8&255)*t);
        int b=Math.round((color&255)*(1-t)+(second&255)*t);
        return r<<16|g<<8|b;
    }
}
