package keystrokesmod.client.module.modules.render.trail;

/** Reused snapshot, independent of HUD/SwordGlow/theme code. */
public final class TrailOptions {
    public enum Style { Ribbon,Line,Dual,Particles,Wings,Aura,SolidBand;
        @Override public String toString(){return this==SolidBand?"Solid Band":name();}
    }
    public enum ColorMode { Static,Rainbow,Gradient,Theme,Color }
    public enum GradientDirection { HeadToTail,TailToHead }
    public enum Preset {
        Custom(0,0),Plasma(0x9863FF,0x36DDFF),Inferno(0xFFC04A,0xFF3035),
        Frost(0xC5F6FF,0x448EFF),Aurora(0x42FFAF,0xB465FF),Neon(0xFF53DC,0x30F5FF),
        Void(0xA283F5,0x5137B8),Gold(0xFFF3B0,0xFFAF28);
        public final int start,end;Preset(int a,int b){start=a;end=b;}
    }
    public Style style=Style.Ribbon;
    public ColorMode mode=ColorMode.Static;
    public int color=0x8764FF,second=0x37DCFF;
    public float length=1.2F,width=.625F,top=1.5F,hem=0,intensity=.75F,bloom=.4F;
    public float bandHeight=.35F,bandLength=1.6F,heightOffset=1.05F,edgeWidth=1,tailTaper=.65F,opacity=.9F,shimmer=.12F;
    public GradientDirection gradientDirection=GradientDirection.HeadToTail;
    public boolean matchLength=true;
    public boolean first=true,third=true,others,sparkles=true,landing=true,low;
    public static float clamp(float v,float min,float max){return Float.isFinite(v)?Math.max(min,Math.min(max,v)):min;}
    public int tint(float age,double time,double distance){
        if(mode==ColorMode.Static||mode==ColorMode.Color)return color;
        if(mode==ColorMode.Rainbow){double hue=time*.13+distance*.14;return java.awt.Color.HSBtoRGB((float)(hue-Math.floor(hue)),.75F,1)&0xFFFFFF;}
        float t=clamp(age,0,1);
        if(style==Style.SolidBand&&gradientDirection==GradientDirection.TailToHead)t=1-t;
        int r=Math.round((color>>16&255)*(1-t)+(second>>16&255)*t);
        int g=Math.round((color>>8&255)*(1-t)+(second>>8&255)*t);
        int b=Math.round((color&255)*(1-t)+(second&255)*t);
        return r<<16|g<<8|b;
    }
}
