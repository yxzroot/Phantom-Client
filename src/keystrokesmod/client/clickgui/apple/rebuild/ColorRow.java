package keystrokesmod.client.clickgui.apple.rebuild;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.RGBSetting;
/** Swatch only. Editing is exclusively handled by ColorPickerPopup. */
public final class ColorRow extends SettingRow {
    private final RGBSetting color;private int known=Integer.MIN_VALUE;
    public ColorRow(Module module,RGBSetting setting){super(module,setting,"");color=setting;}
    @Override protected float controlWidth(){return 100F;}
    @Override protected boolean onClick(float x,float y,int button){return true;}
    @Override protected String readValue(){int rgb=color.getRGB()&0xFFFFFF;if(rgb==known)return value;known=rgb;return String.format("#%06X",rgb);}
    @Override protected void drawValue(float right,float textY,float alpha){
        if(hover.get()>.35F)UiDraw.text(value,right-GuiTokens.ROW_PADDING_X-17-valueWidth,textY,UiDraw.alpha(GuiTokens.VALUE_TEXT,alpha*hover.get()));
        UiDraw.circle(right-GuiTokens.ROW_PADDING_X-8,rowY+baseHeight()*.5F,10,UiDraw.alpha(color.getRGB(),alpha));
        UiDraw.outline(right-GuiTokens.ROW_PADDING_X-18,rowY+baseHeight()*.5F-10,right-GuiTokens.ROW_PADDING_X+2,rowY+baseHeight()*.5F+10,10,UiDraw.alpha(GuiTokens.CARD_BORDER_HOVER,alpha));
    }
}
