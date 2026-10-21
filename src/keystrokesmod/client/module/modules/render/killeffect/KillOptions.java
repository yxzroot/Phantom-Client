package keystrokesmod.client.module.modules.render.killeffect;

/** Reusable settings snapshot, shared by simulation and renderers. */
public final class KillOptions {
    public enum Effect {
        Lightning,Shockwave,SoulRise,Shatter,Confetti,Firework,FlameBurst,IceShatter,BlackHole,LightPillar,Random;
        @Override public String toString(){switch(this){case SoulRise:return "Soul Rise";case FlameBurst:return "Flame Burst";case IceShatter:return "Ice Shatter";case BlackHole:return "Black Hole";case LightPillar:return "Light Pillar";default:return name();}}
    }
    public enum ColorMode { Static,Rainbow,Gradient }
    public enum Targets { Players,PlayersAndMobs,Everything;
        @Override public String toString(){return this==PlayersAndMobs?"Players and mobs":name();}
    }
    public enum Preset {
        Custom(0,0),Plasma(0x9863FF,0x36DDFF),Inferno(0xFFC04A,0xFF3035),Frost(0xC5F6FF,0x448EFF),
        Aurora(0x42FFAF,0xB465FF),Neon(0xFF53DC,0x30F5FF),Void(0xA283F5,0x5137B8),Gold(0xFFF3B0,0xFFAF28);
        public final int a,b;Preset(int a,int b){this.a=a;this.b=b;}
    }
    public Effect effect=Effect.Lightning;
    public ColorMode mode=ColorMode.Static;
    public Targets targets=Targets.PlayersAndMobs;
    public int color=0xA998FF,second=0x55DEFF;
    public float size=1,intensity=.8F,duration=1.6F,distance=64,volume=.45F,flashStrength=.12F,pulseStrength=.025F;
    public boolean byEffect=true,others,sound=true,flash,pulse,streak=true,popup=true,low;
    public static float clamp(float x,float lo,float hi){return Float.isFinite(x)?Math.max(lo,Math.min(hi,x)):lo;}
    public static int mix(int a,int b,float t){t=clamp(t,0,1);return Math.round((a>>16&255)*(1-t)+(b>>16&255)*t)<<16|Math.round((a>>8&255)*(1-t)+(b>>8&255)*t)<<8|Math.round((a&255)*(1-t)+(b&255)*t);}
    public static int tint(int mode,int a,int b,float phase,float age,double time){
        if(mode==ColorMode.Rainbow.ordinal()){double h=phase+time*.16;return java.awt.Color.HSBtoRGB((float)(h-Math.floor(h)),.7F,1)&0xFFFFFF;}
        return mode==ColorMode.Gradient.ordinal()?mix(a,b,clamp(phase*.65F+age*.35F,0,1)):a;
    }
    public static int primary(int style){switch(style){case 1:return 0x74CAFF;case 2:return 0x91FFE0;case 4:return 0xFFA1DC;case 5:return 0xFFCF69;case 6:return 0xFF873A;case 7:return 0xA5ECFF;case 8:return 0xA080FF;case 9:return 0xFFEAC4;default:return 0xBCA8FF;}}
    public static int secondary(int style){switch(style){case 2:return 0x6B9AFF;case 6:return 0xFF3459;case 7:return 0x6A9CFF;case 8:return 0xFF6DCD;default:return 0x64DDFF;}}
}
