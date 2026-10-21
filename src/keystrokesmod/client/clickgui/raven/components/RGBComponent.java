package keystrokesmod.client.clickgui.raven.components;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.clickgui.color.ColorSwatch;
public class RGBComponent extends SettingComponent {
    private final RGBSetting color;
    public RGBComponent(Setting setting,ModuleComponent parent){super(setting,parent);color=(RGBSetting)setting;}
    @Override public void draw(int mx,int my){setDimensions(moduleComponent.getWidth()-10,22);ColorSwatch.draw(color,x+5,y,width-5,height);}
    @Override public void clicked(int mx,int my,int button){if(button==0)ColorSwatch.open(color,mx,my);}
    @Override public void mouseReleased(int x,int y,int button){}
}
