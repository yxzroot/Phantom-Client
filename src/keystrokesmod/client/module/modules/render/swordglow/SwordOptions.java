package keystrokesmod.client.module.modules.render.swordglow;

/** Shared settings snapshot, reused across all nearby swords. */
public final class SwordOptions {
    public float swingEnergy=.65F,trailStrength=.75F,trailLife=.20F;
    public enum Secondary {
        None(null),EnergyFlow(AnimationStyle.EnergyFlow),Shimmer(AnimationStyle.Shimmer),Pulse(AnimationStyle.Pulse),
        Flame(AnimationStyle.Flame),Lightning(AnimationStyle.Lightning),Galaxy(AnimationStyle.Galaxy),Aurora(AnimationStyle.Aurora),
        RainbowWave(AnimationStyle.RainbowWave),Charge(AnimationStyle.Charge),Glitch(AnimationStyle.Glitch),Void(AnimationStyle.Void);
        public final AnimationStyle style;Secondary(AnimationStyle style){this.style=style;}
    }
    public enum Preset {
        Custom(AnimationStyle.Theme,0xA76BFF,0x55DDFF),Plasma(AnimationStyle.EnergyFlow,0xA855FF,0x44EEFF),
        Inferno(AnimationStyle.Flame,0xFF5518,0xFFD258),Frostbite(AnimationStyle.Shimmer,0x9AEAFF,0xFFFFFF),
        Void(AnimationStyle.Void,0x6624BB,0xCD82FF),Galaxy(AnimationStyle.Galaxy,0x588AFF,0xD97BFF),
        Aurora(AnimationStyle.Aurora,0x55FFAD,0xE66CFF),Neon(AnimationStyle.Glitch,0x00EFFF,0xFF369D),
        Toxic(AnimationStyle.Pulse,0x9DFF35,0x28D980),RoyalGold(AnimationStyle.EnergyFlow,0xFFBF34,0xFFF3BD),
        Sakura(AnimationStyle.Shimmer,0xFF9CCB,0xFFE3F2);
        public final AnimationStyle style;public final int start,end;
        Preset(AnimationStyle style,int start,int end){this.style=style;this.start=start;this.end=end;}
        public String toString(){return this==RoyalGold?"Royal Gold":name();}
    }
    public enum Signature { Auto,Midnight,Aurora,Sakura,Inferno,Cyberpunk,Ocean,Frost,Vaporwave,Amoled }
    public AnimationStyle style=AnimationStyle.EnergyFlow;
    public Secondary secondary=Secondary.None;
    public float intensity=.75F,bandWidth=.3F,brightness=1.2F,bloom=.45F,scale=8,angle=-50,fade=.25F;
    public boolean trail=true,hit=true,block=true,equip=true,particles=true,lowQuality;
    public boolean petals;
    public int stencilMask,atlas;
    public static AnimationStyle signature(String name){
        String lower=name.toLowerCase(java.util.Locale.ROOT);
        if(lower.contains("aurora"))return AnimationStyle.Aurora;
        if(lower.contains("sakura")||lower.contains("frost"))return AnimationStyle.Shimmer;
        if(lower.contains("inferno")||lower.contains("sun")||lower.contains("fire"))return AnimationStyle.Flame;
        if(lower.contains("cyber"))return AnimationStyle.Glitch;
        if(lower.contains("vapor"))return AnimationStyle.RainbowWave;
        if(lower.contains("ocean")||lower.contains("water")||lower.contains("amoled"))return AnimationStyle.EnergyFlow;
        return AnimationStyle.Pulse;
    }
}
