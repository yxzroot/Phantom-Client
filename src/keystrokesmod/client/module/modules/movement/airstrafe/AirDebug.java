package keystrokesmod.client.module.modules.movement.airstrafe;

import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public final class AirDebug {
    private final AuroraGLState state=new AuroraGLState();
    private String text="";private long next;
    public void draw(Minecraft mc,AirController c){
        long now=System.nanoTime();if(now>=next){text="Airstrafe: "+Math.round(c.speed*2000)/100D+" b/s ("+Math.round(c.speed*1000)/1000D+" b/t) | "+c.status;next=now+100000000;}
        state.begin();try{GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glColor4f(1,1,1,1);mc.fontRendererObj.drawString(text,8,72,0xFF99DDFF);}finally{state.end();}
    }
}
