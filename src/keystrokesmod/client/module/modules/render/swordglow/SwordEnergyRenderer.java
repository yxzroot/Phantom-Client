package keystrokesmod.client.module.modules.render.swordglow;

import net.minecraft.client.resources.model.IBakedModel;
import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Shader-tinted native model copies. Core=depth equal; outline=texture alpha AND reserved stencil. */
public final class SwordEnergyRenderer {
    private final SwordGLState state=new SwordGLState();
    private final FloatBuffer matrix=BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport=BufferUtils.createIntBuffer(16);
    private final java.nio.ByteBuffer colorMask=BufferUtils.createByteBuffer(16);
    private int program;
    private final int[] uniform=new int[28];
    private static final String[] NAMES={"energy","atlas","axis","limits","pixelOffset","colorA","colorB","clock","amount","width","bright","block","charge","burst","hit","equip","style","oldStyle","secondary","transition","mode","layer","colorMode","oldSecondary","secondaryMix","swingEnergy","swingSweep"};
    private boolean bloomFailed,motionFailed;
    public void retryParts(){bloomFailed=motionFailed=false;}
    private static final float[] DX={1,.7071F,0,-.7071F,-1,-.7071F,0,.7071F},DY={0,.7071F,1,.7071F,0,-.7071F,-1,-.7071F};
    private static final float[] LANES={-1,-.40F,0,.40F,1},SOFT={0,.38F,1,.38F,0};
    private int shader(int type,String source){int id=GL20.glCreateShader(type);GL20.glShaderSource(id,source);GL20.glCompileShader(id);
        if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String log=GL20.glGetShaderInfoLog(id,4096);GL20.glDeleteShader(id);throw new IllegalStateException(log);}return id;}
    private void initialize(){if(program!=0)return;int vertex=0,fragment=0;
        try{vertex=shader(GL20.GL_VERTEX_SHADER,SwordEnergyShaders.VERTEX);fragment=shader(GL20.GL_FRAGMENT_SHADER,SwordEnergyShaders.FRAGMENT);
            program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));
            for(int i=0;i<NAMES.length;i++)uniform[i]=GL20.glGetUniformLocation(program,NAMES[i]);
        }catch(RuntimeException e){if(program!=0)GL20.glDeleteProgram(program);program=0;throw e;}
        finally{if(vertex!=0)GL20.glDeleteShader(vertex);if(fragment!=0)GL20.glDeleteShader(fragment);}
    }
    private void f(int index,float value){GL20.glUniform1f(uniform[index],value);}
    private void i(int index,int value){GL20.glUniform1i(uniform[index],value);}
    private void color(int index,int value){GL20.glUniform3f(uniform[index],(value>>16&255)/255F,(value>>8&255)/255F,(value&255)/255F);}
    public void draw(SwordAnimator.Frame frame,GlintPalette.Palette colors,SwordOptions opt,IBakedModel model,SwordModelBounds bounds,
                     int texture,NativeSwordGlint.ModelDraw nativeDraw,SwordMotion motion,SwordParticles particles,int slot,long frameId,double now){
        if(frame.visibility<=.0001||opt.intensity<=0||texture==0||model==null||model.isBuiltInRenderer())return;
        state.begin();
        try{
            initialize();GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,opt.atlas);GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDepthFunc(GL11.GL_EQUAL);GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);
            GL20.glUseProgram(program);i(0,0);i(1,1);GL20.glUniform3f(uniform[2],bounds.ax,bounds.ay,bounds.az);GL20.glUniform2f(uniform[3],bounds.min,bounds.extent);
            GL20.glUniform2f(uniform[4],0,0);color(5,colors.start);color(6,colors.mode==GlintPalette.ColorMode.Static?colors.start:colors.end);
            f(7,frame.phase);f(9,opt.bandWidth);f(10,opt.brightness);f(11,frame.blocking);f(12,frame.charge);f(13,frame.burst);f(14,opt.hit?frame.hitFlash:0);f(15,frame.equipSweep);
            i(16,frame.style.effect());i(17,frame.previousStyle.effect());i(18,opt.lowQuality?-1:frame.secondary);f(19,frame.transition);
            i(23,opt.lowQuality?-1:frame.previousSecondary);f(24,frame.secondaryTransition);
            f(25,frame.swingEnergy*opt.swingEnergy);f(26,frame.swingSweep);
            i(22,colors.mode.ordinal());int layers=opt.lowQuality?1:3;
            if(frame.style==AnimationStyle.Void||frame.previousStyle==AnimationStyle.Void&&frame.transition<1){i(20,3);f(8,frame.visibility*opt.intensity*(frame.style==AnimationStyle.Void?frame.transition:1-frame.transition));GL11.glBlendFunc(GL11.GL_ZERO,GL11.GL_SRC_COLOR);nativeDraw.swordglow$drawNativeModel(model,0xFFFFFFFF);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);}
            i(20,0);
            for(int layer=0;layer<layers;layer++){
                GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();float scale=frame.textureScale/8*1.4F;
                GL11.glScalef(scale,scale,1);GL11.glTranslatef(frame.phase*(layer==1?-.11F:.08F+layer*.04F),frame.phase*(layer==1?.04F:-.07F),0);
                GL11.glRotatef(frame.angle[0]+layer*63,0,0,1);GL11.glMatrixMode(GL11.GL_MODELVIEW);
                i(21,layer);f(8,frame.visibility*opt.intensity*(opt.lowQuality?.56F:.30F));nativeDraw.swordglow$drawNativeModel(model,0xFFFFFFFF);
            }
            if(opt.bloom>0&&!opt.lowQuality&&!bloomFailed&&opt.stencilMask!=0&&GL11.glGetInteger(GL11.GL_STENCIL_BITS)>0)
                try{halo(frame,opt,model,nativeDraw);}catch(RuntimeException|LinkageError e){bloomFailed=true;java.util.logging.Logger.getLogger("Phantom/SwordGlow").log(java.util.logging.Level.WARNING,"Sword bloom disabled",e);}
            if(motion!=null&&particles!=null&&!motionFailed)
                try{motion(frame,colors,opt,bounds,motion,particles,slot,frameId,now);}catch(RuntimeException|LinkageError e){motionFailed=true;java.util.logging.Logger.getLogger("Phantom/SwordGlow").log(java.util.logging.Level.WARNING,"Sword trails/particles disabled",e);}
        }finally{state.end();}
    }
    private void halo(SwordAnimator.Frame frame,SwordOptions opt,IBakedModel model,NativeSwordGlint.ModelDraw draw){
        int mask=opt.stencilMask;GL11.glEnable(GL11.GL_STENCIL_TEST);GL11.glStencilMask(mask);GL11.glClearStencil(0);GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS,mask,mask);GL11.glStencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_REPLACE);
        colorMask.clear();GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK,colorMask);
        GL11.glColorMask(false,false,false,false);i(20,1);GL11.glDepthFunc(GL11.GL_EQUAL);draw.swordglow$drawNativeModel(model,0xFFFFFFFF);
        GL11.glColorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);GL11.glStencilMask(0);GL11.glStencilFunc(GL11.GL_EQUAL,0,mask);GL11.glStencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_KEEP);
        GL11.glDepthFunc(GL11.GL_LEQUAL);i(20,2);viewport.clear();GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);
        for(int ring=0;ring<2;ring++)for(int j=0;j<8;j++){
            float pixels=ring==0?.65F:1.5F;GL20.glUniform2f(uniform[4],DX[j]*pixels*2/Math.max(1,viewport.get(2)),DY[j]*pixels*2/Math.max(1,viewport.get(3)));
            f(8,frame.visibility*opt.intensity*opt.bloom*(ring==0?.020F:.009F));draw.swordglow$drawNativeModel(model,0xFFFFFFFF);
        }
        GL20.glUniform2f(uniform[4],0,0);GL11.glStencilMask(mask);GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);GL11.glDisable(GL11.GL_STENCIL_TEST);i(20,0);
    }
    private void motion(SwordAnimator.Frame frame,GlintPalette.Palette colors,SwordOptions opt,SwordModelBounds bounds,
                        SwordMotion motion,SwordParticles particles,int slot,long frameId,double now){
        matrix.clear();GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,matrix);
        float tx=matrix.get(0)*bounds.tipX+matrix.get(4)*bounds.tipY+matrix.get(8)*bounds.tipZ+matrix.get(12);
        float ty=matrix.get(1)*bounds.tipX+matrix.get(5)*bounds.tipY+matrix.get(9)*bounds.tipZ+matrix.get(13);
        float tz=matrix.get(2)*bounds.tipX+matrix.get(6)*bounds.tipY+matrix.get(10)*bounds.tipZ+matrix.get(14);
        if(!Float.isFinite(tx)||!Float.isFinite(ty)||!Float.isFinite(tz))return;
        float hx=bounds.tipX-bounds.ax*bounds.extent,hy=bounds.tipY-bounds.ay*bounds.extent,hz=bounds.tipZ-bounds.az*bounds.extent;
        motion.hiltX=matrix.get(0)*hx+matrix.get(4)*hy+matrix.get(8)*hz+matrix.get(12);motion.hiltY=matrix.get(1)*hx+matrix.get(5)*hy+matrix.get(9)*hz+matrix.get(13);motion.hiltZ=matrix.get(2)*hx+matrix.get(6)*hy+matrix.get(10)*hz+matrix.get(14);
        float bx=tx-motion.hiltX,by=ty-motion.hiltY,bz=tz-motion.hiltZ;
        motion.bladeLength=Math.max(.0001F,(float)Math.sqrt(bx*bx+by*by+bz*bz));
        if(motion.frame!=frameId){
            float dt=motion.observe(tx,ty,tz,now,frame.swinging,frame.swingId,frame.swingEnergy);motion.frame=frameId;
            if(opt.particles&&!opt.lowQuality){motion.emit+=dt*((frame.style==AnimationStyle.Flame?8:3)+frame.swingEnergy*opt.swingEnergy*10);int tint=GlintPalette.motionTint(frame,colors);
                while(motion.emit>=1){motion.emit--;particles.spawn(slot,motion,tint,opt.petals?3:frame.style==AnimationStyle.Flame?1:frame.style==AnimationStyle.Galaxy?2:0,false);}
                if(frame.swinging&&frame.swingEnergy>.7F&&opt.swingEnergy>0&&motion.releaseId!=frame.swingId){
                    motion.releaseId=frame.swingId;for(int j=0;j<3;j++)particles.swing(slot,motion,tint);
                }
                if(frame.hitFlash<.5F)motion.hitArmed=true;
                if(opt.hit&&frame.hitFlash>.9F&&motion.hitArmed){motion.hitArmed=false;for(int j=0;j<8;j++)particles.spawn(slot,motion,tint,0,true);}
                if(frame.style==AnimationStyle.Lightning){double tick=Math.floor(frame.phase/.115);if(tick!=motion.lastStrike&&SwordEnergyTexture.hash((int)tick,17)>.8F){for(int j=0;j<3;j++)particles.spawn(slot,motion,tint,0,true);}motion.lastStrike=tick;}
            }
        }
        GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
        try{if(opt.trail&&opt.trailStrength>0)trail(motion,now,GlintPalette.motionTint(frame,colors),frame.visibility*opt.intensity*opt.trailStrength,opt);if(opt.particles&&!opt.lowQuality)particles.draw(slot,frame.visibility*opt.intensity);
            if(!opt.lowQuality&&frame.style==AnimationStyle.Lightning&&SwordEnergyTexture.hash((int)Math.floor(frame.phase/.115),17)>.8F)arc(motion,frame,colors.start);}
        finally{GL11.glPopMatrix();}
    }
    private void trail(SwordMotion motion,double now,int color,float visibility,SwordOptions options){
        SwordTrail trail=motion.trail;trail.prepare(motion,now,options.trailLife,options.lowQuality);
        if(trail.count<2)return;
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        for(int lane=0;lane<4;lane++){
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for(int j=0;j<trail.count;j++){
                if(j>0&&trail.start[j]){GL11.glEnd();GL11.glBegin(GL11.GL_TRIANGLE_STRIP);}
                for(int side=0;side<2;side++){
                    int edge=lane+side;float core=edge==2?.32F:0;
                    GL11.glColor4f(r+(1-r)*core,g+(1-g)*core,b+(1-b)*core,trail.alpha[j]*SOFT[edge]*visibility*.48F);
                    GL11.glVertex3f(trail.x[j]+trail.nx[j]*LANES[edge],trail.y[j]+trail.ny[j]*LANES[edge],trail.z[j]);
                }
            }GL11.glEnd();
        }
    }
    private void arc(SwordMotion motion,SwordAnimator.Frame frame,int color){
        int tick=(int)Math.floor(frame.phase/.115);
        for(int pass=0;pass<2;pass++){GL11.glLineWidth(pass==0?3:1);GL11.glColor4f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,frame.visibility*(pass==0?.045F:.26F));
            GL11.glBegin(GL11.GL_LINE_STRIP);for(int j=0;j<=8;j++){float t=j/8F,jitter=(j==0||j==8)?0:(SwordEnergyTexture.hash(tick,j)-.5F)*.012F;
                GL11.glVertex3f(motion.hiltX+(motion.tipX-motion.hiltX)*t+jitter,motion.hiltY+(motion.tipY-motion.hiltY)*t-jitter*.5F,motion.hiltZ+(motion.tipZ-motion.hiltZ)*t);}
            GL11.glEnd();}
    }
}
