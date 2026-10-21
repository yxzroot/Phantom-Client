package keystrokesmod.client.module.modules.render.aurora;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Original textured billboards, depth-tested after entities. No vanilla particle atlas. */
public final class PetalRenderer {
    private final AuroraGLState state=new AuroraGLState();
    private final FloatBuffer vertices=BufferUtils.createFloatBuffer(PetalSimulation.CAP*4*9);
    private int vbo,texture,program;
    private void initialize(){if(program!=0)return;int v=0,f=0;try{
        v=ArtSkyRenderer.shader(GL20.GL_VERTEX_SHADER,ArtSkyShaders.VERTEX);f=ArtSkyRenderer.shader(GL20.GL_FRAGMENT_SHADER,"#version 120\nuniform sampler2D image;varying vec2 uv;varying vec4 shade;void main(){vec4 p=texture2D(image,uv);gl_FragColor=vec4(p.rgb*shade.rgb,p.a*shade.a);}");
        program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"image"),0);
        texture=ArtSkyRenderer.texture(ArtSkyAssets.petalPixels(),64,false);vbo=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,(long)vertices.capacity()*4,GL15.GL_STREAM_DRAW);
    }catch(RuntimeException|LinkageError e){dispose();throw e;}finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}}
    public void draw(PetalSimulation sim,ArtSkyOptions o,float skyFade,double cx,double cy,double cz,float rx,float rz,float ux,float uy,float uz){
        if(sim.fade*skyFade<.001)return;state.begin();try{initialize();vertices.clear();
            for(int i=0;i<sim.count;i++){float px=(float)(sim.x[i]-cx),py=(float)(sim.y[i]-cy),pz=(float)(sim.z[i]-cz),distance=(float)Math.sqrt(px*px+py*py+pz*pz);
                float a=sim.alpha[i]*sim.fade*skyFade*.85F*AuroraSimulation.clamp((16-distance)/4,0,1)*AuroraSimulation.clamp((distance-.4F)/.8F,0,1);if(a<.001)continue;
                float s=sim.size[i]*o.petalSize*(distance<3?1.2F:1),angle=sim.rotation[i],sn=(float)Math.sin(angle),cs=(float)Math.cos(angle),tumble=.25F+.75F*Math.abs((float)Math.cos(angle*.7));
                for(int c=0;c<4;c++){float x=(c==0||c==3?-s:s)*tumble,y=c<2?-s:s,right=x*cs-y*sn,up=x*sn+y*cs;vertices.put(px+rx*right+ux*up).put(py+uy*up).put(pz+rz*right+uz*up).put(1).put(.91F).put(.97F).put(a).put(c==0||c==3?0:1).put(c<2?0:1);}
            }
            vertices.flip();if(!vertices.hasRemaining())return;GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,vertices);GL20.glUseProgram(program);GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glVertexPointer(3,GL11.GL_FLOAT,36,0L);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);GL11.glColorPointer(4,GL11.GL_FLOAT,36,12L);GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glTexCoordPointer(2,GL11.GL_FLOAT,36,28L);GL11.glDrawArrays(GL11.GL_QUADS,0,vertices.limit()/9);
        }finally{state.end();}
    }
    public void dispose(){if(program!=0)GL20.glDeleteProgram(program);if(texture!=0)GL11.glDeleteTextures(texture);if(vbo!=0)GL15.glDeleteBuffers(vbo);program=texture=vbo=0;}
}
