import keystrokesmod.client.module.modules.render.aspect.*;
import keystrokesmod.client.module.modules.render.AspectRatio;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import java.nio.*;import org.lwjgl.BufferUtils;import org.lwjgl.opengl.*;import org.lwjgl.util.glu.Project;

public final class AspectTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void cpu(){
        for(int fps:new int[]{60,240}){AspectAnimator a=new AspectAnimator();a.reset(16F/9);float value=0;for(int i=0;i<fps/5;i++)value=a.update(4F/3,1D/fps,true);check(value==4F/3,"200ms target not reached");for(int i=0;i<fps/5;i++)value=a.update(16F/9,1D/fps,true);check(value==16F/9,"OFF not exact vanilla");a.reset(1.6F);check(a.update(1.25F,0,false)==1.25F,"Snap ignored");a.reset(21F/9);check(a.update(21F/9,0,true)==21F/9,"Resize reset failed");}
        check(AspectAnimator.fov(70,16F/9,4F/3,true)==70,"Vertical FOV changed");float f=AspectAnimator.fov(70,16F/9,4F/3,false);check(f>70&&Math.abs(Math.tan(Math.toRadians(f)/2)*(4D/3)-Math.tan(Math.toRadians(70)/2)*(16D/9))<.00001,"Horizontal-FOV compensation wrong");
        ComboSetting<AspectRatio.Mode> modes=new ComboSetting<>("Mode",AspectRatio.Mode.Custom);for(AspectRatio.Mode mode:AspectRatio.Mode.values()){modes.setMode(mode);com.google.gson.JsonObject json=modes.getConfigAsJson();modes.resetToDefaults();modes.applyConfigFromJson(json);check(modes.getMode()==mode,"Mode config failed");}
        check(Float.isFinite(AspectAnimator.safe(Float.NaN)),"NaN ratio");System.out.println("PASS: 60/240Hz 200ms transitions, exact OFF/snap/resize endpoints, vertical/horizontal FOV and all preset config codecs");
    }
    static FloatBuffer matrix(){FloatBuffer b=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,b);return b;}
    static void gl()throws Exception{
        Pbuffer p=new Pbuffer(64,64,new PixelFormat().withDepthBits(24),null);p.makeCurrent();try{
            AspectProjection scope=new AspectProjection();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();Project.gluPerspective(70,16F/9,.05F,256);FloatBuffer vanilla=matrix();
            scope.begin();GL11.glLoadIdentity();Project.gluPerspective(70,4F/3,.05F,256);FloatBuffer wide=matrix();check(Math.abs(wide.get(0)/vanilla.get(0)-4F/3)<.0001,"4:3 stretch projection wrong");check(wide.get(5)==vanilla.get(5),"Vertical projection changed");check(wide.get(8)==0&&wide.get(9)==0,"Center/crosshair ray shifted");scope.restore();
            FloatBuffer after=matrix();for(int i=0;i<16;i++)check(after.get(i)==vanilla.get(i),"Projection not restored");check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"Matrix mode leak");
            scope.begin();scope.retarget(4F/3,true);wide=matrix();check(Math.abs(wide.get(0)/vanilla.get(0)-4F/3)<.0001&&wide.get(5)==vanilla.get(5),"Completed projection retarget failed");
            scope.retarget(4F/3,true);after=matrix();for(int i=0;i<16;i++)check(after.get(i)==wide.get(i),"Repeated projection patch accumulates stretch");
            scope.retarget(16F/9,true);after=matrix();for(int i=0;i<16;i++)check(Math.abs(after.get(i)-vanilla.get(i))<.000001,"Normal hand ratio not restored");scope.restore();
            scope.begin();scope.retarget(4F/3,false);after=matrix();check(after.get(0)==vanilla.get(0)&&Math.abs(after.get(5)-vanilla.get(0)*4F/3)<.000001,"Keep horizontal FOV failed");scope.restore();
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,64,64,0,-1,1);FloatBuffer ortho=matrix();scope.retarget(4F/3,true);after=matrix();for(int i=0;i<16;i++)check(after.get(i)==ortho.get(i),"Orthographic HUD was changed");GL11.glLoadMatrix(vanilla);GL11.glMatrixMode(GL11.GL_MODELVIEW);
            for(int frame=0;frame<240;frame++){scope.begin();try{GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();Project.gluPerspective(70,.5F+frame/100F,.05F,256);}finally{scope.restore();}}
            scope.begin();try{GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();throw new IllegalStateException("fixture");}catch(IllegalStateException expected){}finally{scope.restore();}
            after=matrix();for(int i=0;i<16;i++)check(after.get(i)==vanilla.get(i),"Exception cleanup failed");check(GL11.glGetError()==0,"Projection GL error");System.out.println("PASS: real GL horizontal stretch, unchanged vertical/center ray, exact projection restoration, 240 passes and exception cleanup; hand projection remains native unless opted in");
        }finally{p.destroy();}
    }
    public static void main(String[]args)throws Exception{if(args.length>0)gl();else cpu();}
}
