package keystrokesmod.client.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.clickgui.kv.KvComponent;
import keystrokesmod.client.clickgui.kv.components.KvRgbComponent;
import keystrokesmod.client.clickgui.raven.Component;
import keystrokesmod.client.clickgui.raven.components.ModuleComponent;
import keystrokesmod.client.clickgui.raven.components.RGBComponent;
import keystrokesmod.client.clickgui.raven.components.SettingComponent;
import keystrokesmod.client.utils.ColorM;
import keystrokesmod.client.utils.Utils;

/** Existing RGB API/config codec. All GUI implementations delegate to the shared popup. */
public class RGBSetting extends Setting {
    private final int[] colour,defaultColour;
    private SliderSetting opacity;
    public RGBSetting(String name,int red,int green,int blue){super(name);colour=new int[]{clamp(red),clamp(green),clamp(blue)};defaultColour=colour.clone();}
    private static int clamp(int c){return Math.max(0,Math.min(255,c));}
    @Override public void resetToDefaults(){System.arraycopy(defaultColour,0,colour,0,3);}
    @Override public JsonObject getConfigAsJson(){JsonObject out=new JsonObject();out.addProperty("type",getSettingType());out.addProperty("red",getRed());out.addProperty("green",getGreen());out.addProperty("blue",getBlue());return out;}
    @Override public String getSettingType(){return "rgbsetting";}
    @Override public void applyConfigFromJson(JsonObject data){
        if(data==null||!data.has("type")||!getSettingType().equals(data.get("type").getAsString()))return;
        try{int r=data.get("red").getAsInt(),g=data.get("green").getAsInt(),b=data.get("blue").getAsInt();setRed(r);setGreen(g);setBlue(b);}catch(RuntimeException ignored){/* Keep prior complete color on corrupt input. */}
    }
    @Override public Component createComponent(ModuleComponent parent){return null;}
    public int getRed(){return clamp(colour[0]);}public int getGreen(){return clamp(colour[1]);}public int getBlue(){return clamp(colour[2]);}
    public int[] getColors(){return colour;}public int getColor(int channel){return colour[channel];}
    public int getRGB(){return 0xFF000000|getRed()<<16|getGreen()<<8|getBlue();}
    public void setRed(int c){setColor(0,c);}public void setGreen(int c){setColor(1,c);}public void setBlue(int c){setColor(2,c);}
    public void setColor(int channel,int value){if(channel>=0&&channel<3)colour[channel]=clamp(value);}
    public void setColors(int[] channels){if(channels!=null&&channels.length>=3)for(int i=0;i<3;i++)colour[i]=clamp(channels[i]);}
    /** RGB-only by default. Existing PickerRGBSetting's optional opacity override remains compatible. */
    public SliderSetting getOpacity(){return opacity;}
    public RGBSetting withOpacity(SliderSetting slider){opacity=slider;return this;}
    @Override public Class<? extends KvComponent> getComponentType(){return KvRgbComponent.class;}
    @Override public Class<? extends SettingComponent> getRavenComponentType(){return RGBComponent.class;}
    public enum NSColor {
        Staic(d->0xFFFFFFFF),Rainbow(d->Utils.Client.rainbowDraw(1,d)),Astolfo(d->Utils.Client.astolfoColorsDraw(10,14,d)),Kv(Utils.Client::customDraw);
        private final ColorM c;private static final NSColor[] vals=values();NSColor(ColorM c){this.c=c;}
        public int getColor(){return c.color(0);}public int getColor(int delay){return c.color(delay);}public NSColor next(){return vals[(ordinal()+1)%vals.length];}
    }
}
