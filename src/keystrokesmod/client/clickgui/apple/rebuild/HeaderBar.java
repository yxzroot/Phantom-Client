package keystrokesmod.client.clickgui.apple.rebuild;
import keystrokesmod.client.utils.font.PhantomWordmark;
/** Labeled utility buttons: actions remain notifications, refresh and GUI preferences. */
public final class HeaderBar {
    public static final int NONE=0,BELL=1,REFRESH=2,GEAR=3;
    private static final String[] LABEL={"Notifications","Refresh","Settings"};
    private static final int[] ICON={GuiIcons.BELL,GuiIcons.REFRESH,GuiIcons.GEAR};
    private final GuiButton[] buttons={new GuiButton(GuiButton.SECONDARY,LABEL[0],ICON[0]),new GuiButton(GuiButton.SECONDARY,LABEL[1],ICON[1]),new GuiButton(GuiButton.SECONDARY,LABEL[2],ICON[2])};
    private final GuiButton[] compact={new GuiButton(GuiButton.GHOST_ICON,"",ICON[0]),new GuiButton(GuiButton.GHOST_ICON,"",ICON[1])};
    private final float[] x=new float[3],width=new float[3];private float y;private boolean wide;
    public void update(float dt){for(int i=0;i<3;i++)buttons[i].update(dt);for(int i=0;i<2;i++)compact[i].update(dt);}
    public void resetInteraction(){for(int i=0;i<3;i++)buttons[i].release();for(int i=0;i<2;i++)compact[i].release();}
    public void draw(MainWindow window,float mx,float my,boolean unread,boolean interactive){
        float left=window.x(),top=window.y(),right=left+window.width();wide=window.width()>=690;
        width[0]=wide?132:36;width[1]=wide?104:36;width[2]=112;
        float total=width[0]+width[1]+width[2]+16;x[0]=right-GuiTokens.WINDOW_PADDING-total;x[1]=x[0]+width[0]+8;x[2]=x[1]+width[1]+8;y=top+22;
        UiDraw.flushShapes();
        if(window.width()>480){if(!PhantomWordmark.draw(left+GuiTokens.WINDOW_PADDING,top+29,20,UiDraw.masterAlpha()))UiDraw.logo("PHANTOM",left+GuiTokens.WINDOW_PADDING,top+28,GuiTokens.TEXT_PRIMARY);}
        else UiDraw.smallBold("P",left+12,top+29,GuiTokens.TEXT_PRIMARY);
        for(int i=0;i<3;i++)(i<2&&!wide?compact[i]:buttons[i]).draw(x[i],y,width[i],mx,my,interactive);
        if(unread)UiDraw.circle(x[0]+width[0]-8,y+7,2.5F,GuiTokens.accent());
    }
    public int click(float mx,float my){for(int i=0;i<3;i++)if((i<2&&!wide?compact[i]:buttons[i]).click(mx,my,0))return i+1;return NONE;}
}
