package keystrokesmod.client.module.modules.render.worldparticles;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;

/** One reusable VBO and atlas. Core + glow + optional cross-fade in a single draw call. */
public final class ParticleRenderer {
    private final AuroraGLState state=new AuroraGLState();
    private final FloatBuffer vertices=BufferUtils.createFloatBuffer(ParticleSimulation.CAP*3*4*9);
    private int texture,program,vbo;
    public int lastQuads;
    private static int shader(int kind,String text){
        int id=GL20.glCreateShader(kind);GL20.glShaderSource(id,text);GL20.glCompileShader(id);
        if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String error=GL20.glGetShaderInfoLog(id,2048);GL20.glDeleteShader(id);throw new IllegalStateException(error);}return id;
    }
    private void initialize(){
        if(program!=0)return;int v=0,f=0;
        try{
            v=shader(GL20.GL_VERTEX_SHADER,"#version 120\nvarying vec2 uv;varying vec4 shade;void main(){gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;uv=gl_MultiTexCoord0.xy;shade=gl_Color;}");
            f=shader(GL20.GL_FRAGMENT_SHADER,"#version 120\nuniform sampler2D image;varying vec2 uv;varying vec4 shade;void main(){vec4 p=texture2D(image,uv);gl_FragColor=vec4(p.rgb*shade.rgb,p.a*shade.a);}");
            program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,2048));
            GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"image"),0);
            texture=GL11.glGenTextures();GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,ParticleAssets.CELL*ParticleAssets.CELLS,ParticleAssets.CELL,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,ParticleAssets.pixels());
            vbo=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER,(long)vertices.capacity()*4,GL15.GL_STREAM_DRAW);
        }catch(RuntimeException|LinkageError e){dispose();throw e;}
        finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}
    }
    private void quad(float px,float py,float pz,float size,float angle,int cell,int color,float alpha,
                      float rx,float rz,float ux,float uy,float uz){
        if(alpha<=.001F)return;
        float cs=(float)Math.cos(angle),sn=(float)Math.sin(angle);
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        float lo=(cell+1F/ParticleAssets.CELL)/ParticleAssets.CELLS,hi=(cell+1-1F/ParticleAssets.CELL)/ParticleAssets.CELLS;
        for(int c=0;c<4;c++){
            float x=(c==0||c==3?-size:size),y=c<2?-size:size;
            float right=x*cs-y*sn,up=x*sn+y*cs;
            vertices.put(px+rx*right+ux*up).put(py+uy*up).put(pz+rz*right+uz*up);
            // Image pixels are top-down; flip V for an upright ETH / raindrop.
            vertices.put(r).put(g).put(b).put(alpha).put(c==0||c==3?lo:hi).put(c<2?1-1F/ParticleAssets.CELL:1F/ParticleAssets.CELL);
        }
        lastQuads++;
    }
    public void draw(ParticleSimulation sim,ParticleOptions o,double cx,double cy,double cz,float rx,float rz,float ux,float uy,float uz){
        lastQuads=0;if(sim.fade<=.001F||o.opacity<=0||sim.count==0)return;
        state.begin();try{
            initialize();vertices.clear();
            for(int i=0;i<sim.count;i++){
                float x=(float)(sim.x[i]-cx),y=(float)(sim.y[i]-cy),z=(float)(sim.z[i]-cz);
                float dist=(float)Math.sqrt(x*x+y*y+z*z);
                float edge=Math.max(Math.abs(x),Math.max(Math.abs(z),Math.abs(y)/.65F));
                float a=sim.alpha[i]*sim.fade*o.opacity*ParticleOptions.clamp((o.radius-edge)/1.6F,0,1)*ParticleOptions.clamp((dist-.65F)/1.1F,0,1);
                if(o.twinkle)a*=.72F+.28F*(float)Math.sin(sim.time*(1+sim.phase[i])+sim.phase[i]*25);
                if(a<=.001F)continue;
                float size=o.size*sim.size[i],angle=o.rotate&&o.shape!=ParticleOptions.Shape.Rain?sim.angle[i]:0;
                int color=o.tint(sim.phase[i],sim.time);
                if(!o.low&&o.glow>0)quad(x,y,z,size*2.4F,0,4,color,a*o.glow*.38F,rx,rz,ux,uy,uz);
                if(sim.shapeMix<1)quad(x,y,z,size,angle,sim.previousShape,color,a*(1-sim.shapeMix),rx,rz,ux,uy,uz);
                quad(x,y,z,size,angle,sim.currentShape,color,a*sim.shapeMix,rx,rz,ux,uy,uz);
            }
            vertices.flip();if(!vertices.hasRemaining())return;
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,vertices);
            GL20.glUseProgram(program);GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDepthMask(false);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glVertexPointer(3,GL11.GL_FLOAT,36,0L);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);GL11.glColorPointer(4,GL11.GL_FLOAT,36,12L);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glTexCoordPointer(2,GL11.GL_FLOAT,36,28L);
            GL11.glDrawArrays(GL11.GL_QUADS,0,vertices.limit()/9);
        }finally{state.end();}
    }
    public void dispose(){
        if(program!=0)GL20.glDeleteProgram(program);if(texture!=0)GL11.glDeleteTextures(texture);if(vbo!=0)GL15.glDeleteBuffers(vbo);
        program=texture=vbo=0;
    }
}
