package keystrokesmod.client.module.modules.render.island;

import java.nio.*;import org.lwjgl.BufferUtils;import org.lwjgl.opengl.*;
import net.minecraft.client.renderer.GlStateManager;

/** Private cached content framebuffer; final SDF mask clips EVERYTHING to rounded ends.
 * Does not alter Minecraft's depth/stencil attachments or clear its stencil contents. */
public final class IslandCanvas {
    private int fbo,texture,program,width,height,previousFbo;private float logicalW,logicalH;
    private final IntBuffer viewport=BufferUtils.createIntBuffer(16);
    private int sizeUniform,radiusUniform,originUniform,spanUniform,aaUniform,opacityUniform;
    private static int shader(int type,String text){int id=GL20.glCreateShader(type);GL20.glShaderSource(id,text);GL20.glCompileShader(id);if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String log=GL20.glGetShaderInfoLog(id,2048);GL20.glDeleteShader(id);throw new IllegalStateException(log);}return id;}
    private void initialize(){
        if(program!=0)return;int v=shader(GL20.GL_VERTEX_SHADER,"#version 120\nvoid main(){gl_TexCoord[0]=gl_MultiTexCoord0;gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;}"),f=0;
        try{f=shader(GL20.GL_FRAGMENT_SHADER,"#version 120\nuniform sampler2D image;uniform vec2 size,origin,span;uniform float radius,aa,opacity;void main(){vec2 uv=gl_TexCoord[0].xy;vec2 q=abs((uv-.5)*size)-size*.5+radius;float d=length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;float a=(1.0-smoothstep(-aa,aa,d))*opacity;vec3 rgb=texture2D(image,origin+uv*span).rgb;gl_FragColor=vec4(rgb*a,a);}");
            program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,2048));
            GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"image"),0);
            sizeUniform=GL20.glGetUniformLocation(program,"size");radiusUniform=GL20.glGetUniformLocation(program,"radius");originUniform=GL20.glGetUniformLocation(program,"origin");spanUniform=GL20.glGetUniformLocation(program,"span");aaUniform=GL20.glGetUniformLocation(program,"aa");opacityUniform=GL20.glGetUniformLocation(program,"opacity");GL20.glUseProgram(0);
        }catch(RuntimeException failure){if(program!=0)GL20.glDeleteProgram(program);program=0;throw failure;}finally{GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}
    }
    public void begin(float maxWidth,float maxHeight,float pixelsPerUnit){
        initialize();previousFbo=GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);viewport.clear();GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);
        int w=Math.min(4096,Math.max(64,((int)Math.ceil(maxWidth*pixelsPerUnit)+63)/64*64)),h=Math.min(2048,Math.max(64,((int)Math.ceil(maxHeight*pixelsPerUnit)+63)/64*64));
        if(fbo==0||width!=w||height!=h){releaseBuffer();width=w;height=h;texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,w,h,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(ByteBuffer)null);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);fbo=EXTFramebufferObject.glGenFramebuffersEXT();EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,fbo);EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,GL11.GL_TEXTURE_2D,texture,0);if(EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)!=EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT){EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,previousFbo);throw new IllegalStateException("Island framebuffer unavailable");}}
        logicalW=maxWidth;logicalH=maxHeight;EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,fbo);GL11.glViewport(0,0,w,h);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glClearColor(.015F,.017F,.021F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,maxWidth,maxHeight,0,-100,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL20.glUseProgram(0);
        // Lyrics-only contains no drawHud call to establish this baseline for us.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);GlStateManager.enableTexture2D();
        GL11.glDisable(GL11.GL_DEPTH_TEST);GlStateManager.disableDepth();GL11.glDepthMask(false);GlStateManager.depthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);GlStateManager.disableCull();
    }
    public void finish(){GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,previousFbo);GL11.glViewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));}
    public void clip(float x,float y,float w,float h){GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(Math.max(0,(int)(x/logicalW*width)),Math.max(0,(int)((logicalH-y-h)/logicalH*height)),Math.max(0,(int)(w/logicalW*width)),Math.max(0,(int)(h/logicalH*height)));}
    public void unclip(){GL11.glDisable(GL11.GL_SCISSOR_TEST);}
    public void composite(float x,float y,float w,float h,float radius,float pixelsPerUnit){masked(texture,x,y,w,h,radius,0,1,w/logicalW,-h/logicalH,pixelsPerUnit,1);}
    public void artwork(int image,float x,float y,float side,float pixelsPerUnit,float opacity){masked(image,x,y,side,side,6,0,0,1,1,pixelsPerUnit,opacity);}
    private void masked(int image,float x,float y,float w,float h,float radius,float u,float v,float su,float sv,float pixelsPerUnit,float opacity){
        GL11.glDisable(GL11.GL_DEPTH_TEST);GlStateManager.disableDepth();GL11.glDisable(GL11.GL_CULL_FACE);GlStateManager.disableCull();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,image);GL20.glUseProgram(program);GL20.glUniform2f(sizeUniform,w,h);GL20.glUniform1f(radiusUniform,Math.min(radius,Math.min(w,h)/2));GL20.glUniform2f(originUniform,u,v);GL20.glUniform2f(spanUniform,su,sv);GL20.glUniform1f(aaUniform,.7F/Math.max(.5F,pixelsPerUnit));GL20.glUniform1f(opacityUniform,opacity);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(1,1,1,1);
        GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2f(0,0);GL11.glVertex2f(x,y);GL11.glTexCoord2f(1,0);GL11.glVertex2f(x+w,y);GL11.glTexCoord2f(1,1);GL11.glVertex2f(x+w,y+h);GL11.glTexCoord2f(0,1);GL11.glVertex2f(x,y+h);GL11.glEnd();GL20.glUseProgram(0);
    }
    private void releaseBuffer(){if(fbo!=0)EXTFramebufferObject.glDeleteFramebuffersEXT(fbo);if(texture!=0)GL11.glDeleteTextures(texture);fbo=texture=0;}
    public void dispose(){releaseBuffer();if(program!=0)GL20.glDeleteProgram(program);program=0;}
}
