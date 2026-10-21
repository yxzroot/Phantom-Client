package keystrokesmod.client.module.modules.combat.aim;

import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

/** Debug only: a depth-tested cross and throttled angle label, isolated from world/HUD state. */
public final class AssistDebug {
    private final AuroraGLState state=new AuroraGLState();
    private String label="";private long nextLabel;
    private String statusLabel="";private long nextStatus;
    public void hud(Minecraft mc,AssistController c,String driver){
        long now=System.nanoTime();if(now>=nextStatus){statusLabel="AimAssist ["+driver+"]: "+c.status;nextStatus=now+100000000;}
        state.begin();try{GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);mc.fontRendererObj.drawString(statusLabel,8,58,0xFF99FFFF);}finally{state.end();}
    }
    public void draw(Minecraft mc,AssistController c){
        RenderManager rm=mc.getRenderManager();double x=c.aimX-rm.viewerPosX,y=c.aimY-rm.viewerPosY,z=c.aimZ-rm.viewerPosZ;
        long now=System.nanoTime();if(now>=nextLabel){label="Aim error "+Math.round(Math.hypot(c.yawError,c.pitchError)*10)/10D+" deg";nextLabel=now+100000000;}
        state.begin();try{
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(.4F,1,1,.9F);GL11.glLineWidth(1.5F);
            GL11.glBegin(GL11.GL_LINES);try{GL11.glVertex3d(x-.045,y,z);GL11.glVertex3d(x+.045,y,z);GL11.glVertex3d(x,y-.045,z);GL11.glVertex3d(x,y+.045,z);GL11.glVertex3d(x,y,z-.045);GL11.glVertex3d(x,y,z+.045);}finally{GL11.glEnd();}
            GL11.glTranslated(x,y+.15,z);GL11.glRotatef(-rm.playerViewY,0,1,0);GL11.glRotatef(rm.playerViewX,1,0,0);GL11.glScalef(-.012F,-.012F,.012F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);mc.fontRendererObj.drawString(label,-mc.fontRendererObj.getStringWidth(label)/2,0,0xFF99FFFF);
        }finally{state.end();}
    }
}
