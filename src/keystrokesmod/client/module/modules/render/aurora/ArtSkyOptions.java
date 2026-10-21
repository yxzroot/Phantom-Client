package keystrokesmod.client.module.modules.render.aurora;

/** New-style-only settings. None of the legacy sky knobs are repurposed. */
public final class ArtSkyOptions {
    public int style,palette;
    public float nebulaMix,sunsetMix,cherryMix,brightness=1,cloudSpeed=1,haze=1,worldTint=.3F;
    public boolean fogMatch=true,planet=true,grid,skyline=true,trees=true,lanterns;
    public float sunSize=1.25F,stripeSpeed=1,petalSize=1,wind=.6F;
    public int petals=1,body;
    public final int[] colors=new int[15];
    public void palette(int selected,int topAccent,int bottomAccent){palette=selected;ArtSkyPalette.update(this,topAccent,bottomAccent);}
    public float mix(){return nebulaMix+sunsetMix+cherryMix;}
    public float amount(int style){return style==0?nebulaMix:style==1?sunsetMix:cherryMix;}
}
