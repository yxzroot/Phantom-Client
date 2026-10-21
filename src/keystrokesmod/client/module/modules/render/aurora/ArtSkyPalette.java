package keystrokesmod.client.module.modules.render.aurora;

/** Original palettes: top, middle, horizon, cloud core A, cloud core B. */
public final class ArtSkyPalette {
    private static final int[][] COLORS={
        {0x020309,0x151035,0x213F58,0xA647BF,0x2497C9},
        {0x080D18,0x173C48,0x63705C,0x23C7BE,0xD5AF59},
        {0x08030E,0x321036,0x58254B,0xC94774,0x754ABF},
        {0x190B39,0x8D316F,0xEC915F,0xF18CB4,0xC750B1},
        {0x170D37,0x883B90,0x6FBCD0,0xF77DC7,0x67D3D8},
        {0x23103A,0xAC4E72,0xF7BE75,0xF2A2B3,0xD38B94},
        {0x52476F,0xAE8199,0xF0C09D,0xF4B9CE,0xDB9BC1},
        {0x291636,0x793759,0xCA8A91,0xE891BE,0xB16DCA},
        {0x827D9B,0xC8B5C8,0xF3DBBB,0xFFF0EF,0xEBC6DA}
    };
    private ArtSkyPalette(){}
    public static int mix(int a,int b,float t){t=AuroraSimulation.clamp(t,0,1);int r=Math.round((a>>16&255)*(1-t)+(b>>16&255)*t),g=Math.round((a>>8&255)*(1-t)+(b>>8&255)*t),c=Math.round((a&255)*(1-t)+(b&255)*t);return r<<16|g<<8|c;}
    public static void update(ArtSkyOptions o,int accent,int second){
        for(int style=0;style<3;style++){
            int preset=o.palette==0?style*3:Math.max(0,Math.min(8,o.palette-1));
            for(int c=0;c<5;c++)o.colors[style*5+c]=COLORS[preset][c];
            if(o.palette==10){
                o.colors[style*5]=mix(COLORS[style*3][0],accent,style==0?.025F:.16F);
                o.colors[style*5+1]=mix(COLORS[style*3][1],accent,.4F);
                o.colors[style*5+2]=mix(COLORS[style*3][2],second,.38F);
                o.colors[style*5+3]=accent&0xFFFFFF;o.colors[style*5+4]=second&0xFFFFFF;
            }
        }
    }
    public static float horizon(ArtSkyOptions o,int style,int channel){return(o.colors[style*5+2]>>((2-channel)*8)&255)/255F*AuroraSimulation.clamp(o.brightness,.1F,2)*(.8F+.2F*AuroraSimulation.clamp(o.haze,0,2));}
    public static int tint(ArtSkyOptions o){
        float total=o.mix();if(total<=0)return 0xFFFFFF;int rgb=0;
        for(int c=0;c<3;c++){float value=0;for(int s=0;s<3;s++)value+=(o.colors[s*5+2]>>((2-c)*8)&255)*o.amount(s);rgb|=Math.min(255,Math.round(value/total))<<((2-c)*8);}return rgb;
    }
}
