package keystrokesmod.client.clickgui.color;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
/** Tiny legacy-row view, not a second editor. */
public final class ColorSwatch {
    private ColorSwatch(){}
    public static void draw(RGBSetting color,int x,int y,int width,int height){
        Gui.drawRect(x,y,x+width,y+height,0xAA20192B);Gui.drawRect(x+width-28,y+4,x+width-5,y+height-4,color.getRGB());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(color.getName(),x+5,y+(height-8)/2,0xFFE7DFF0);
    }
    public static void open(RGBSetting color,int x,int y){Minecraft mc=Minecraft.getMinecraft();ColorPickerPopup.open(mc.currentScreen,color,x,y);}
}
