package keystrokesmod.client.module.modules.render.killeffect;

import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import keystrokesmod.client.module.modules.render.island.IslandFont;
import org.lwjgl.opengl.*;

/** Edge-only flash and Unicode HUD fallback. Independent of the island's music/lyrics state. */
public final class KillOverlay {
    private final AuroraGLState state=new AuroraGLState();
    private final IslandFont font=new IslandFont(11,java.awt.Font.PLAIN);
    private static void color(int rgb,float a){GL11.glColor4f((rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F,a);}
    private static void edge(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy,int rgb,float a){
        color(rgb,a);GL11.glVertex2f(ax,ay);GL11.glVertex2f(bx,by);color(rgb,0);GL11.glVertex2f(cx,cy);GL11.glVertex2f(dx,dy);
    }
    public void draw(int w,int h,int rgb,float flash,String popup,float popupFade,String counter){
        if(flash<=0&&(popup==null||popupFade<=0)&&counter==null)return;
        state.begin();try{
            GL20.glUseProgram(0);GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,0);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,w,h,0,-1,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            if(flash>0){float edge=Math.min(48,Math.min(w,h)*.11F);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glBegin(GL11.GL_QUADS);
                edge(0,0,w,0,w,edge,0,edge,rgb,flash);edge(w,h,0,h,0,h-edge,w,h-edge,rgb,flash);
                edge(0,h,0,0,edge,0,edge,h,rgb,flash);edge(w,0,w,h,w-edge,h,w-edge,0,rgb,flash);GL11.glEnd();}
            if(popup!=null&&popupFade>.001F){int alpha=Math.round(KillOptions.clamp(popupFade,0,1)*255);float x=(w-font.width(popup))*.5F,y=h*.38F-(1-popupFade)*6;
                font.drawHud(popup,x+1,y+1,alpha<<24);font.drawHud(popup,x,y,alpha<<24|KillOptions.mix(rgb,0xFFFFFF,.7F));}
            if(counter!=null)font.drawHud(counter,w-font.width(counter)-8,94,0xE8FFFFFF);
        }finally{state.end();}
    }
    public void dispose(){font.dispose();}
}
