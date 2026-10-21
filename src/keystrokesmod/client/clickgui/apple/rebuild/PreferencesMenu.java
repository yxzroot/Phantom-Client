package keystrokesmod.client.clickgui.apple.rebuild;

/** Proper GUI settings popover: shared switches, bounded sliders and a real theme list. */
public final class PreferencesMenu {
    private static final String[] LABEL={"Menu animation","Animation speed","GUI scale","Color theme","Interface sounds","Favorites first","Tooltip delay"};
    private static final String[] HELP={"Open and close smoothly","Faster or gentler motion","Release to resize","Interface palette","Clicks and toggles","Your pinned modules first","Delay before showing help"};
    private final Anim[] toggles={new Anim(1,Anim.Mode.SPRING,.16F),new Anim(1,Anim.Mode.SPRING,.16F),new Anim(1,Anim.Mode.SPRING,.16F)};
    private float x,y,w,h,row,trackX,trackW,draftScale=1;private int dragging=-1,themeScroll;private boolean themes;
    private Enum<?>[] known;private String[] themeLabels=new String[0];private final String[] values={"","",""};private final int[] bits={0,0,0};
    public void layout(MainWindow window,int screenW,int screenH){w=Math.min(360,screenW-16);h=Math.min(424,screenH-16);x=Math.max(8,Math.min(screenW-w-8,window.x()+window.width()-GuiTokens.WINDOW_PADDING-w));y=Math.max(8,Math.min(screenH-h-8,window.y()+70));row=(h-86)/7;trackX=x+w-154;trackW=130;}
    public void update(float dt){toggles[0].setTarget(GuiPreferences.menuAnimationEnabled()?1:0);toggles[1].setTarget(GuiPreferences.sounds()?1:0);toggles[2].setTarget(GuiPreferences.favoritesFirst()?1:0);for(int i=0;i<3;i++)toggles[i].update(dt);}
    public boolean contains(float mx,float my){return UiDraw.inside(mx,my,x,y,x+w,y+h);}
    private float top(int index){return y+58+index*row;}
    private String number(int which,float value,String suffix){int next=Float.floatToIntBits(value);if(bits[which]!=next||values[which].isEmpty()){bits[which]=next;values[which]=Float.toString(Math.round(value*100)/100F)+suffix;}return values[which];}
    public void draw(float mx,float my,float alpha){
        float master=UiDraw.masterAlpha();UiDraw.setMasterAlpha(master*alpha);
        try{
            UiDraw.glassRounded(x,y,x+w,y+h,14,0xFC181522,0xFC101018,0xFF43374F,1,0xA0000000,22);
            UiDraw.bold("GUI settings",x+18,y+13,0xFFF5F0FC);GuiIcons.draw(GuiIcons.CLOSE,x+w-26,y+25,16,0xFFB5A9C8);
            if(h>365)UiDraw.small("Appearance and interactions",x+18,y+37,0xFF92859F);
            for(int i=0;i<7;i++){
                float ry=top(i);UiDraw.rounded(x+10,ry,x+w-10,ry+row-4,7,UiDraw.inside(mx,my,x+10,ry,x+w-10,ry+row-4)?0xFF24202E:0xFF1B1723);
                UiDraw.text(LABEL[i],x+20,ry+6,0xFFEDE5F8);if(row>=44)UiDraw.small(HELP[i],x+20,ry+25,0xFF91849F);
                if(i==0||i==4||i==5){float p=toggles[i==0?0:i==4?1:2].get();GuiToggle.draw(x+w-70,ry+9,p,p,0,1,false);}
                else if(i==3){UiDraw.glassRounded(trackX,ry+5,x+w-20,ry+35,8,0xFF2D243A,0xFF241D30,0xFF51405F,1,0,0);String name=GuiPreferences.themeName();UiDraw.small(name,trackX+10,ry+11,0xFFE4D5F7);GuiIcons.draw(GuiIcons.CHEVRON,x+w-32,ry+20,12,0xFFB29CCB);}
                else{
                    float value=i==1?GuiPreferences.animationSpeed():i==2?(dragging==2?draftScale:GuiPreferences.guiScale()):GuiPreferences.tooltipDelay();float min=i==1?.3F:i==2?.75F:.2F,max=i==1?3:i==2?1.5F:5;
                    String text=number(i==1?0:i==2?1:2,value,i==6?"s":"x");UiDraw.small(text,x+w-24-UiDraw.smallWidth(text),ry+5,0xFFB7A0DD);
                    UiDraw.rounded(trackX,ry+30,trackX+trackW,ry+34,2,0xFF3B3047);float p=Math.max(0,Math.min(1,(value-min)/(max-min)));UiDraw.rounded(trackX,ry+30,trackX+trackW*p,ry+34,2,GuiTokens.accent());UiDraw.circle(trackX+trackW*p,ry+32,5,0xFFECE2F8);
                }
            }
            UiDraw.small("Changes save with your active config",x+20,y+h-21,0xFF796E87);
            if(themes)drawThemes(mx,my);
        }finally{UiDraw.setMasterAlpha(master);}
    }
    private void labels(){Enum<?>[] options=GuiPreferences.themeOptions();if(options==known)return;known=options;themeLabels=new String[options.length];for(int i=0;i<options.length;i++)themeLabels[i]=options[i].toString();}
    private float themeTop(){return Math.min(y+h-24-Math.min(6,themeLabels.length)*24,top(3)+38);}
    private void drawThemes(float mx,float my){
        labels();float t=themeTop(),height=Math.min(6,themeLabels.length)*24;UiDraw.glassRounded(trackX,t,x+w-16,t+height+6,8,0xFF21192D,0xFF17121F,0xFF645075,1,0x88000000,12);
        for(int i=themeScroll;i<Math.min(themeLabels.length,themeScroll+6);i++){float ry=t+3+(i-themeScroll)*24;if(UiDraw.inside(mx,my,trackX+3,ry,x+w-19,ry+24))UiDraw.rounded(trackX+3,ry,x+w-19,ry+24,5,0xFF3D2C54);UiDraw.small(themeLabels[i],trackX+10,ry+4,themeLabels[i].equals(GuiPreferences.themeName())?0xFFD4B3FF:0xFFB8AAC8);}
    }
    /** 1=close, 2=relayout, 4=resort. */
    public int click(float mx,float my,int button){
        if(!contains(mx,my))return 1;if(button!=0)return 0;
        if(UiDraw.inside(mx,my,x+w-43,y+8,x+w-9,y+43))return 1;
        if(themes){labels();float t=themeTop();if(UiDraw.inside(mx,my,trackX,t,x+w-16,t+Math.min(6,themeLabels.length)*24+6)){int index=themeScroll+(int)((my-t-3)/24);GuiPreferences.selectTheme(index);themes=false;return 0;}themes=false;return 0;}
        int i=(int)((my-y-58)/row);if(my<y+58||i<0||i>6)return 0;
        if(i==0)GuiPreferences.toggleMenuAnimation();else if(i==4)GuiPreferences.toggleSounds();else if(i==5){GuiPreferences.toggleFavoritesFirst();return 4;}
        else if(i==3){labels();themes=themeLabels.length>0;themeScroll=0;}
        else if(mx>=trackX-8){dragging=i;draftScale=GuiPreferences.guiScale();drag(mx,my);}
        return 0;
    }
    public void drag(float mx,float my){if(dragging<0)return;float p=Math.max(0,Math.min(1,(mx-trackX)/trackW));if(dragging==1)GuiPreferences.setAnimationSpeed(Math.round((.3F+p*2.7F)*20)/20F);else if(dragging==2)draftScale=.75F+p*.75F;else GuiPreferences.previewTooltipDelay(Math.round((.2F+p*4.8F)*10)/10F);}
    public int release(){boolean resized=dragging==2;if(resized)GuiPreferences.setGuiScale(draftScale);dragging=-1;GuiPreferences.save();return resized?2:0;}
    public void reset(){dragging=-1;themes=false;}
    public void wheel(float amount){if(themes){labels();themeScroll=Math.max(0,Math.min(Math.max(0,themeLabels.length-6),themeScroll-(int)Math.signum(amount)));}}
    public boolean escape(){if(themes){themes=false;return false;}return true;}
}
