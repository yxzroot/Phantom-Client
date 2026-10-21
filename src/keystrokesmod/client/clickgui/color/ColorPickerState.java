package keystrokesmod.client.clickgui.color;

import java.awt.Color;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;

/** One transactional HSV source of truth. No graphics, global state, or config format changes. */
public final class ColorPickerState {
    public final RGBSetting setting;public final boolean alphaEnabled;
    public final int original;public final double originalOpacity;
    private final SliderSetting opacity;private final float[] hsv=new float[3];
    public float hue,saturation,value,alpha;private boolean finished;
    public ColorPickerState(RGBSetting setting){
        this.setting=setting;original=setting.getRGB();opacity=setting.getOpacity();alphaEnabled=opacity!=null;
        originalOpacity=opacity==null?100:opacity.getInput();alpha=(float)originalOpacity/100;
        rgb(original); // Initializes HSV without touching the color/opacity codec.
    }
    public static float clamp(float n){return Float.isFinite(n)?Math.max(0,Math.min(1,n)):0;}
    public int rgb(){return Color.HSBtoRGB(hue,saturation,value)&0xFFFFFF;}
    public int alphaByte(){return Math.round(clamp(alpha)*255);}
    public void rgb(int color){
        Color.RGBtoHSB(color>>16&255,color>>8&255,color&255,hsv);
        if(hsv[1]>0)hue=hsv[0];if(hsv[2]>0)saturation=hsv[1];value=hsv[2];preview();
    }
    public void hsv(float h,float s,float v){hue=clamp(h);saturation=clamp(s);value=clamp(v);preview();}
    public void alpha(float a){if(alphaEnabled){alpha=clamp(a);preview();}}
    public void channel(int channel,int value){int shift=(2-channel)*8;rgb(rgb()&~(255<<shift)|Math.max(0,Math.min(255,value))<<shift);}
    private void preview(){if(finished)return;int color=rgb();setting.setRed(color>>16&255);setting.setGreen(color>>8&255);setting.setBlue(color&255);if(opacity!=null)opacity.setValue(alpha*100);}
    /** HEX is RRGGBB or RRGGBBAA, never Java's AARRGGBB. */
    public boolean hex(String input){
        String text=input.startsWith("#")?input.substring(1):input;
        if(text.length()!=6&&text.length()!=8)return false;
        long number=0;for(int i=0;i<text.length();i++){int digit=Character.digit(text.charAt(i),16);if(digit<0)return false;number=number*16+digit;}
        if(text.length()==8){int a=(int)number&255;if(!alphaEnabled&&a!=255)return false;alpha=alphaEnabled?a/255F:1;number>>>=8;}
        rgb((int)number);return true;
    }
    public void select(){finished=true;}
    public void cancel(){if(finished)return;setting.setRed(original>>16&255);setting.setGreen(original>>8&255);setting.setBlue(original&255);if(opacity!=null)opacity.setValue(originalOpacity);finished=true;}
    public static float fit(float screenW,float screenH){return Math.max(.05F,Math.min(1,Math.min((screenW-16)/376,(screenH-16)/400)));}
    public static float placeX(float anchor,float screenW,float scale){float w=376*scale;float x=anchor+12;if(x+w>screenW-8)x=anchor-w-12;return Math.max(8,Math.min(screenW-8-w,x));}
    public static float placeY(float anchor,float screenH,float scale){return Math.max(8,Math.min(screenH-8-400*scale,anchor-40));}
}
