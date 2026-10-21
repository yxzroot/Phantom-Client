package keystrokesmod.client.clickgui.kv.components;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.clickgui.kv.KvComponent;
import keystrokesmod.client.clickgui.color.ColorSwatch;
public class KvRgbComponent extends KvComponent {
    private final RGBSetting color;
    public KvRgbComponent(Setting setting){color=(RGBSetting)setting;}
    @Override public void draw(int mx,int my){ColorSwatch.draw(color,x,y,width,Math.max(18,height));}
    @Override public void clicked(int button,int mx,int my){if(button==0)ColorSwatch.open(color,mx,my);}
    @Override public void mouseReleased(int x,int y,int button){}
}
