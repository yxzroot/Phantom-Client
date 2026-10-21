package keystrokesmod.client.clickgui.color;

import java.awt.Color;
import keystrokesmod.client.clickgui.apple.rebuild.UiDraw;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** The single retained popup component used by every RGB setting and GUI adapter. */
public final class ColorPicker {
    public final ColorPickerState state;
    public int tab;private int oldTab,drag,focus=-1;private float dragX,dragY;
    private long opened=System.nanoTime(),switched=opened,previous=opened;
    private final float[] handle=new float[6];private final String[] fields={"","","",""};private final float[] fieldWidths=new float[4];
    private int known=-1,knownAlpha=-1;private String hex="",name;private float nameWidth,selectWidth,cancelWidth;
    private boolean measured,invalid,allSelected;public int action;
    private float panelX,panelY,scale=1;public float x,y,fit=1;
    public ColorPicker(ColorPickerState state){this.state=state;name=state.setting.getName();refresh(-1);}
    private static boolean inside(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
    private static float ease(double n){float t=ColorPickerState.clamp((float)n);return 1-(1-t)*(1-t)*(1-t);}
    public void layout(float width,float height,float anchorX,float anchorY){fit=ColorPickerState.fit(width,height);x=ColorPickerState.placeX(anchorX,width,fit);y=ColorPickerState.placeY(anchorY,height,fit);}
    public float drawX(){return panelX;}public float drawY(){return panelY;}public float drawScale(){return scale;}
    private float fieldX(){return state.alphaEnabled?68:42;}
    private float hueX(){return fieldX()+244;}
    private void refresh(int except){
        int rgb=state.rgb(),a=state.alphaByte();if(known==rgb&&knownAlpha==a)return;known=rgb;knownAlpha=a;
        for(int i=0;i<3;i++)if(i!=except){fields[i]=Integer.toString(rgb>>((2-i)*8)&255);fieldWidths[i]=-1;}
        hex=String.format(state.alphaEnabled?"#%06X%02X":"#%06X",rgb,a);
        if(except!=3){fields[3]=hex;fieldWidths[3]=-1;}
    }
    public void draw(float mouseX,float mouseY){
        long now=System.nanoTime();double dt=Math.max(0,(now-previous)/1e9);previous=now;float a=ease((now-opened)/150000000D);
        scale=fit*(.96F+.04F*a);panelX=x+(376*fit-376*scale)/2;panelY=y+(400*fit-400*scale)/2;
        float mx=(mouseX-panelX)/scale,my=(mouseY-panelY)/scale;
        GL11.glPushMatrix();GL11.glTranslatef(panelX,panelY,0);GL11.glScalef(scale,scale,1);
        try{
        if(!measured){while(name.length()>1&&UiDraw.smallWidth(name)>312)name=name.substring(0,name.length()-1);nameWidth=UiDraw.smallWidth(name);selectWidth=UiDraw.boldWidth("Select");cancelWidth=UiDraw.textWidth("Cancel");measured=true;}
        net.minecraft.client.renderer.GlStateManager.disableDepth();GL11.glDisable(GL11.GL_DEPTH_TEST);net.minecraft.client.renderer.GlStateManager.disableCull();GL11.glDisable(GL11.GL_CULL_FACE);
        refresh(focus);UiDraw.setMasterAlpha(a);
        UiDraw.glassRounded(0,0,376,400,16,0xFB17131F,0xFC100E17,0x664E405F,1,0x99000000,28);
        UiDraw.small(name,20,12,0xFFCAC4D9);UiDraw.small("COLOR",318,12,0xFF80758F);
        float tabs=ease((now-switched)/130000000D);
        for(int i=0;i<3;i++){
            float left=20+i*114,active=(i==tab?tabs:0)+(i==oldTab?1-tabs:0);
            UiDraw.glassRounded(left,40,left+108,76,9,UiDraw.mix(0xFF211B2A,0xFF39274F,active),UiDraw.mix(0xFF1A1721,0xFF2B203D,active),UiDraw.mix(0xFF332A40,0xFF9570EB,active),1,UiDraw.alpha(0xFF9460FF,active*.18F),8);
            icon(i,left+20,58);UiDraw.smallBold(i==0?"HSV":i==1?"RGB":"HEX",left+38,49,i==tab?0xFFF7F1FF:0xFFA9A0B7);
        }
        if(tabs<1&&oldTab!=tab)content(oldTab,mx,my,1-tabs,dt);
        content(tab,mx,my,tabs,dt);UiDraw.setMasterAlpha(a);
        PickerGradient.swatch(20,328,38,18,(int)Math.round(state.originalOpacity/100*255)<<24|state.original&0xFFFFFF,a);
        PickerGradient.swatch(62,328,38,18,state.alphaByte()<<24|state.rgb(),a);
        UiDraw.small(hex,116,329,0xFFC3B8D1);UiDraw.small("BEFORE / AFTER",20,311,0xFF82748F);
        UiDraw.glassRounded(20,354,236,386,10,inside(mx,my,20,354,236,386)?0xFFA17AFF:0xFF9065EE,0xFF6440BC,0xFFAA8AF1,1,0x559F63FF,12);
        UiDraw.bold("Select",128-selectWidth/2,358,0xFFFFFFFF);
        UiDraw.glassRounded(248,354,356,386,10,inside(mx,my,248,354,356,386)?0xFF342A41:0xFF231E2D,0xFF1C1825,0xFF40334E,1,0,0);
        UiDraw.text("Cancel",302-cancelWidth/2,361,0xFFC1B7CE);
        }finally{UiDraw.flushShapes();UiDraw.setMasterAlpha(1);GL11.glPopMatrix();}
    }
    private void icon(int tab,float x,float y){
        if(tab==0){for(int i=0;i<6;i++){double angle=i*Math.PI/3;UiDraw.circle(x+(float)Math.cos(angle)*4,y+(float)Math.sin(angle)*4,3.5F,PickerGradient.HUES[i]);}UiDraw.circle(x,y,2,0xFFFFFFFF);}
        else if(tab==1){UiDraw.circle(x-3,y-2,4,0xFFE96485);UiDraw.circle(x+3,y-2,4,0xFF6BDCAB);UiDraw.circle(x,y+3,4,0xFF8D8AFF);}
        else UiDraw.bold("#",x-5,y-11,0xFFE7D8FF);
    }
    private void content(int which,float mx,float my,float alpha,double dt){
        float master=UiDraw.masterAlpha();UiDraw.setMasterAlpha(master*alpha);float a=master*alpha;
        if(which==0){
            float sx=fieldX(),hx=hueX();PickerGradient.square(sx,92,216,Color.HSBtoRGB(state.hue,1,1),a);PickerGradient.hue(hx,92,20,216,a);
            handle(0,sx+state.saturation*216,92+(1-state.value)*216,inside(mx,my,sx,92,sx+216,308),dt,a);
            handle(1,hx+10,92+state.hue*216,inside(mx,my,hx-4,88,hx+24,312),dt,a);
        }else if(which==1){
            float start=state.alphaEnabled?76:28;
            for(int i=0;i<3;i++){
                int shift=(2-i)*8,rgb=state.rgb(),low=0xFF000000|(rgb&~(255<<shift)),high=low|255<<shift;float y=110+i*68;
                UiDraw.small(i==0?"RED":i==1?"GREEN":"BLUE",start,y-17,0xFFA99BB9);
                PickerGradient.track(start,y+12,260-start,12,low,high,a);
                handle(3+i,start+(rgb>>shift&255)/255F*(260-start),y+18,inside(mx,my,start-5,y+4,265,y+30),dt,a);
                box(i,282,y,68,36);
            }
        }else{
            UiDraw.small(state.alphaEnabled?"HEX / RRGGBBAA":"HEX / RRGGBB",76,104,0xFFA99BB9);box(3,76,128,268,44);
            PickerGradient.swatch(76,198,268,86,state.alphaByte()<<24|state.rgb(),a);
            UiDraw.small(invalid?"Enter a valid hexadecimal color":"Paste a color, with or without #",76,181,invalid?0xFFFF7B91:0xFF8B7C9B);
        }
        if(state.alphaEnabled){PickerGradient.alpha(32,92,20,216,state.rgb(),a);handle(2,42,92+(1-state.alpha)*216,inside(mx,my,28,88,56,312),dt,a);}
        UiDraw.setMasterAlpha(master);
    }
    private void handle(int index,float x,float y,boolean hover,double dt,float a){
        handle[index]+=((hover||drag==index+1?1:0)-handle[index])*(float)(1-Math.exp(-dt*18));float r=5+handle[index]*1.3F;
        UiDraw.circleGradient(x,y,r,0xFF32163D,0xFF32163D,0xFFFFFFFF,1.8F,0x66000000,6);
    }
    private void box(int index,float x,float y,float w,float h){
        UiDraw.glassRounded(x,y,x+w,y+h,8,0xFF15101E,0xFF15101E,invalid&&focus==index?0xFFF16D87:focus==index?0xFFAB81F2:0xFF44344F,1,0,0);
        if(fieldWidths[index]<0)fieldWidths[index]=UiDraw.smallWidth(fields[index]);
        if(focus==index&&allSelected)UiDraw.rounded(x+8,y+8,x+Math.min(w-8,fieldWidths[index]+12),y+h-8,3,0x665E3E99);
        UiDraw.small(fields[index],x+10,y+(h-16)/2,0xFFF0E9FA);
        if(focus==index&&(System.nanoTime()/500000000L&1)==0)UiDraw.rect(x+Math.min(w-8,fieldWidths[index]+11),y+9,x+Math.min(w-7,fieldWidths[index]+12),y+h-9,0xFFDCC6FF);
    }
    public void click(float screenX,float screenY,int button){
        float mx=(screenX-panelX)/scale,my=(screenY-panelY)/scale;
        if(!inside(mx,my,0,0,376,400)){action=-1;return;}if(button!=0)return;
        if(inside(mx,my,20,354,236,386)){if(!invalid)action=1;return;}if(inside(mx,my,248,354,356,386)){action=-1;return;}
        if(inside(mx,my,20,40,356,76)){int next=Math.min(2,(int)((mx-20)/114));oldTab=tab;tab=next;switched=System.nanoTime();focus=-1;invalid=false;known=-1;refresh(-1);return;}
        focus=-1;allSelected=false;invalid=false;
        if(state.alphaEnabled&&inside(mx,my,28,86,56,314))drag=3;
        else if(tab==0&&inside(mx,my,fieldX()-4,88,fieldX()+228,312))drag=1;
        else if(tab==0&&inside(mx,my,hueX()-4,86,hueX()+24,314))drag=2;
        else if(tab==1){for(int i=0;i<3;i++){float top=110+i*68;if(inside(mx,my,282,top,350,top+36)){focus=i;allSelected=true;break;}if(inside(mx,my,state.alphaEnabled?72:24,top,266,top+36))drag=4+i;}}
        else if(tab==2&&inside(mx,my,76,128,344,172)){focus=3;allSelected=true;}
        dragX=mx;dragY=my;if(drag>0)drag(screenX,screenY,false,true);
    }
    public void release(){drag=0;}
    public void drag(float sx,float sy,boolean fine,boolean initial){
        if(drag==0)return;float mx=(sx-panelX)/scale,my=(sy-panelY)/scale,dx=mx-dragX,dy=my-dragY;dragX=mx;dragY=my;
        if(drag==1){float s=fine&&!initial?state.saturation+dx/216*.15F:(mx-fieldX())/216,v=fine&&!initial?state.value-dy/216*.15F:1-(my-92)/216;state.hsv(state.hue,s,v);}
        else if(drag==2)state.hsv(fine&&!initial?state.hue+dy/216*.15F:(my-92)/216,state.saturation,state.value);
        else if(drag==3)state.alpha(fine&&!initial?state.alpha-dy/216*.15F:1-(my-92)/216);
        else{int channel=drag-4;float start=state.alphaEnabled?76:28,old=state.rgb()>>((2-channel)*8)&255;state.channel(channel,Math.round(fine&&!initial?old+dx/(260-start)*255*.15F:(mx-start)/(260-start)*255));}
        focus=-1;invalid=false;refresh(-1);
    }
    public void key(char c,int key,boolean ctrl,boolean shift,String paste){
        if(key==Keyboard.KEY_ESCAPE){action=-1;return;}
        if(key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER){if(!invalid)action=1;return;}
        if(key==Keyboard.KEY_TAB){int count=tab==1?3:tab==2?1:0;if(count>0){focus=tab==2?3:(focus+(shift?-1:1)+count)%count;allSelected=true;invalid=false;}return;}
        if(focus<0)return;if(ctrl&&key==Keyboard.KEY_A){allSelected=true;return;}
        String value=fields[focus];
        if(ctrl&&key==Keyboard.KEY_V)value=allSelected?paste:value+paste;
        else if(key==Keyboard.KEY_BACK)value=allSelected||value.length()==0?"":value.substring(0,value.length()-1);
        else if(key==Keyboard.KEY_DELETE)value="";
        else if(!ctrl&&(Character.digit(c,16)>=0||c=='#'))value=allSelected?Character.toString(c):value+c;
        else return;
        allSelected=false;if(value.length()>9)value=value.substring(0,9);fields[focus]=value;fieldWidths[focus]=-1;
        if(focus==3)invalid=!state.hex(value);
        else try{int n=Integer.parseInt(value);invalid=n<0||n>255;if(!invalid)state.channel(focus,n);}catch(NumberFormatException ex){invalid=true;}
        refresh(focus);
    }
}
