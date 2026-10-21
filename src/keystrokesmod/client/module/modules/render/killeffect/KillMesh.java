package keystrokesmod.client.module.modules.render.killeffect;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Cached generated sprite atlas + bounded triangle batch. All geometry helpers reuse this buffer. */
public final class KillMesh {
    public static final int MAX_VERTICES=65536,CELL=64,CELLS=4;
    private final FloatBuffer data=BufferUtils.createFloatBuffer(MAX_VERTICES*9);
    private int vbo,texture,program;
    public int vertices,draws;
    private static ByteBuffer pixels(){
        ByteBuffer pixels=BufferUtils.createByteBuffer(CELL*CELL*CELLS*4);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL*CELLS;x++){
            int cell=x/CELL;float xx=((x%CELL)+.5F)/CELL*2-1,yy=(y+.5F)/CELL*2-1;
            float a=1,light=1;
            if(cell==1){float r=xx*xx+yy*yy;a=(float)Math.exp(-r*5)*KillOptions.clamp((1-r)*4,0,1);}
            if(cell==2){float taper=.17F+.65F*(1-yy)*.5F,dist=xx*xx/(taper*taper)+yy*yy*.7F;a=(float)Math.exp(-dist*3)*KillOptions.clamp((1-Math.abs(yy))*6,0,1);light=.75F+.25F*(1-yy)*.5F;}
            if(cell==3){float d=Math.abs(xx)+Math.abs(yy);a=KillOptions.clamp((.9F-d)*12,0,1);light=xx<0?1:.6F;}
            if(cell!=0)a*=KillOptions.clamp((1-Math.abs(xx))*8,0,1)*KillOptions.clamp((1-Math.abs(yy))*8,0,1);
            int c=(int)(light*255);pixels.put((byte)c).put((byte)c).put((byte)c).put((byte)(a*255));
        }
        pixels.flip();return pixels;
    }
    private static int shader(int type,String source){int id=GL20.glCreateShader(type);GL20.glShaderSource(id,source);GL20.glCompileShader(id);if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String error=GL20.glGetShaderInfoLog(id,2048);GL20.glDeleteShader(id);throw new IllegalStateException(error);}return id;}
    private void initialize(){
        if(program!=0)return;int v=0,f=0;
        try{
            v=shader(GL20.GL_VERTEX_SHADER,"#version 120\nvarying vec2 uv;varying vec4 shade;void main(){gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;uv=gl_MultiTexCoord0.xy;shade=gl_Color;}");
            f=shader(GL20.GL_FRAGMENT_SHADER,"#version 120\nuniform sampler2D image;varying vec2 uv;varying vec4 shade;void main(){vec4 p=texture2D(image,uv);gl_FragColor=vec4(p.rgb*shade.rgb,p.a*shade.a);}");
            program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,2048));
            GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"image"),0);
            texture=GL11.glGenTextures();GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
            try{
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,CELL*CELLS,CELL,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels());
            }finally{GL11.glPopClientAttrib();}
            vbo=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,(long)data.capacity()*4,GL15.GL_STREAM_DRAW);
        }catch(RuntimeException|LinkageError error){dispose();throw error;}
        finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}
    }
    public void begin(){data.clear();vertices=draws=0;}
    public void vertex(float x,float y,float z,int rgb,float alpha,float u,float v){
        if(data.remaining()<9)return;
        data.put(x).put(y).put(z).put((rgb>>16&255)/255F).put((rgb>>8&255)/255F).put((rgb&255)/255F).put(KillOptions.clamp(alpha,0,1)).put(u).put(v);vertices++;
    }
    public void point(float x,float y,float z,int color,float alpha){vertex(x,y,z,color,alpha,.125F,.5F);}
    public void quad(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,int color,float alpha){
        if(data.remaining()<54)return;
        point(ax,ay,az,color,alpha);point(bx,by,bz,color,alpha);point(cx,cy,cz,color,alpha);point(ax,ay,az,color,alpha);point(cx,cy,cz,color,alpha);point(dx,dy,dz,color,alpha);
    }
    public void sprite(float x,float y,float z,float w,float h,float angle,int cell,int color,float alpha,float rx,float rz,float ux,float uy,float uz){
        if(alpha<=.001F||data.remaining()<54)return;float sn=(float)Math.sin(angle),cs=(float)Math.cos(angle);
        for(int i=0;i<6;i++){
            int c=i==0||i==3?0:i==1?1:i==5?3:2;
            float a=(c==0||c==3?-w:w),b=c<2?-h:h,right=a*cs-b*sn,up=a*sn+b*cs;
            float u=(cell+(c==0||c==3?.5F/CELL:1-.5F/CELL))/CELLS,v=c<2?.5F/CELL:1-.5F/CELL;
            vertex(x+rx*right+ux*up,y+uy*up,z+rz*right+uz*up,color,alpha,u,v);
        }
    }
    public void flush(boolean additive){
        data.flip();if(!data.hasRemaining()){data.clear();return;}initialize();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,data);GL20.glUseProgram(program);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDepthMask(false);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,additive?GL11.GL_ONE:GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glVertexPointer(3,GL11.GL_FLOAT,36,0L);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);GL11.glColorPointer(4,GL11.GL_FLOAT,36,12L);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glTexCoordPointer(2,GL11.GL_FLOAT,36,28L);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,data.limit()/9);draws++;data.clear();
    }
    public void dispose(){if(program!=0)GL20.glDeleteProgram(program);if(texture!=0)GL11.glDeleteTextures(texture);if(vbo!=0)GL15.glDeleteBuffers(vbo);program=texture=vbo=0;}
}
