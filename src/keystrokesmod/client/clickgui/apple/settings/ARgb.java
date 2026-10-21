package keystrokesmod.client.clickgui.apple.settings;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.clickgui.color.ColorSwatch;
public class ARgb extends ASettingComponent {
    private final RGBSetting color;
    public ARgb(RGBSetting setting){color=setting;height=26;}
    @Override public void draw(int mx,int my){ColorSwatch.draw(color,x,y,width,height);}
    @Override public void clicked(int mx,int my,int button){if(button==0)ColorSwatch.open(color,mx,my);}
    @Override public void mouseReleased(int x,int y,int button){}
}
