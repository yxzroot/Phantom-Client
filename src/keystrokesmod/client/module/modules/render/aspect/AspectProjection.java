package keystrokesmod.client.module.modules.render.aspect;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** World-pass boundary only; never changes viewport, textures, mouse input or modelview. */
public final class AspectProjection {
    private final FloatBuffer saved=BufferUtils.createFloatBuffer(16);
    private final FloatBuffer working=BufferUtils.createFloatBuffer(16);
    private boolean pending;
    public void begin(){
        if(pending)restore();saved.clear();GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,saved);pending=true;
    }
    /** Operates on the finished projection, independent of Lunar's perspective helper. */
    public void retarget(float aspect,boolean keepVertical){
        if(!Float.isFinite(aspect)||aspect<=0)return;
        working.clear();GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,working);
        float x=working.get(0),y=working.get(5);
        // Never modify orthographic overlays or a broken/singular projection.
        if(!Float.isFinite(x)||!Float.isFinite(y)||x<=0||y<=0||Math.abs(working.get(11)+1)>0.001F||working.get(15)!=0)return;
        int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        try{
            if(keepVertical)working.put(0,y/aspect);else working.put(5,x*aspect);
            GL11.glMatrixMode(GL11.GL_PROJECTION);working.position(0);GL11.glLoadMatrix(working);
        }finally{GL11.glMatrixMode(mode);}
    }
    public void restore(){
        if(!pending)return;
        try{GL11.glMatrixMode(GL11.GL_PROJECTION);saved.position(0);GL11.glLoadMatrix(saved);}
        finally{GL11.glMatrixMode(GL11.GL_MODELVIEW);pending=false;}
    }
}
