package keystrokesmod.client.module.modules.render.aurora;

import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Cached noise/dome, three scrolling layers. No per-frame geometry or texture creation. */
public final class OvercastRenderer {
    private static final String VERTEX="#version 120\n varying vec3 direction;void main(){direction=gl_Vertex.xyz/46.0;gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;}";
    private static final String FRAGMENT="#version 120\n"
        +"uniform sampler2D clouds;uniform float clock,opacity,contrast,scale,darkness,lightning,stars;uniform vec3 colorA,colorB,horizon;varying vec3 direction;"
        +"float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}"
        +"float band(float n){return smoothstep(.05,.95,(n-.5)*contrast+.5);}"
        +"void main(){vec3 dir=normalize(direction);float h=max(0.0,dir.y);float dim=1.0-darkness*.40;"
        +"vec3 top=(vec3(.008,.012,.025)+colorA*.035)*dim,mid=(vec3(.018,.024,.043)+mix(colorA,colorB,.60)*.085)*dim;"
        +"vec3 base=mix(horizon,mid,smoothstep(0.0,.34,h));base=mix(base,top,smoothstep(.24,1.0,h));"
        // Project three distinct cloud planes onto the dome. Higher frequencies near
        // the horizon create perspective without a polar seam or camera-position drift.
        +"float a=.22+clock*.0014;mat2 rot=mat2(cos(a),-sin(a),sin(a),cos(a));"
        +"vec2 p=rot*dir.xz*scale;"
        +"float n1=texture2D(clouds,p/(h+.25)*.32+vec2(clock*.0026,clock*.0008)).r;"
        +"float n2=texture2D(clouds,mat2(.94,-.342,.342,.94)*p/(h+.18)*.48+vec2(.27-clock*.004,clock*.0013)).r;"
        +"float n3=texture2D(clouds,mat2(.985,.174,-.174,.985)*p/(h+.12)*.70+vec2(clock*.005,-clock*.0015+.41)).r;"
        +"float c1=band(n1),c2=band(n2),c3=band(n3);float edge=smoothstep(.008,.16,h);"
        +"vec3 lit=(vec3(.19,.21,.28)+colorA*.23+colorB*.34)*dim;vec3 shadow=base*.55;"
        +"float rim=exp(-h*7.0)*.26;vec3 result=mix(base,mix(shadow,lit*.82,sqrt(c1)),c1*.82*edge);"
        +"result=mix(result,mix(shadow,lit*(1.0+rim),sqrt(c2)),c2*.78*edge);"
        +"result=mix(result,mix(shadow,lit*1.12,sqrt(c3)),c3*.67*edge);"
        +"float cover=1.0-(1.0-c1*.72)*(1.0-c2*.68)*(1.0-c3*.57);"
        // Low mist is exactly horizon-colored at y=0 and has zero-slope soft edges.
        +"result=mix(horizon,result,smoothstep(0.0,.075,h));"
        +"float age=mod(clock,17.0),gate=step(.82,hash(vec2(floor(clock/17.0),9)));float flash=(exp(-age*30.0)+.45*exp(-abs(age-.16)*40.0))*gate*lightning;result+=vec3(.05,.06,.10)*flash*cover*edge;"
        +"vec2 sp=dir.xz/(.2+h)*450.0;float star=step(.9985,hash(floor(sp)))*(1.0-smoothstep(.0,.4,length(fract(sp)-.5)));result+=vec3(star*.14*stars*(1.0-cover)*edge);"
        +"gl_FragColor=vec4(result,opacity);}";
    private final AuroraGLState state=new AuroraGLState();
    private final int[] uniforms=new int[12];
    private static final String[] NAMES={"clouds","clock","opacity","contrast","scale","darkness","lightning","stars","colorA","colorB","horizon"};
    private int program,texture,dome;private double last=Double.NaN,phase;
    private static int shader(int type,String source){int id=GL20.glCreateShader(type);GL20.glShaderSource(id,source);GL20.glCompileShader(id);if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String error=GL20.glGetShaderInfoLog(id,4096);GL20.glDeleteShader(id);throw new IllegalStateException(error);}return id;}
    private void initialize(){
        if(program!=0)return;int v=0,f=0;
        try{
            v=shader(GL20.GL_VERTEX_SHADER,VERTEX);f=shader(GL20.GL_FRAGMENT_SHADER,FRAGMENT);program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));
            for(int i=0;i<NAMES.length;i++)uniforms[i]=GL20.glGetUniformLocation(program,NAMES[i]);
            IntBuffer noise=BufferUtils.createIntBuffer(OvercastNoise.W*OvercastNoise.H);noise.put(OvercastNoise.pixels()).flip();texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
            // Cached mip levels prevent distant bands shimmering into hard noisy lines.
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL14.GL_GENERATE_MIPMAP,GL11.GL_TRUE);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
            try{GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,OvercastNoise.W,OvercastNoise.H,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,noise);
            }finally{GL11.glPopClientAttrib();}
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR_MIPMAP_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);
            FloatBuffer mesh=BufferUtils.createFloatBuffer(128*48*4*3);
            for(int y=0;y<48;y++)for(int x=0;x<128;x++)for(int corner=0;corner<4;corner++){
                double az=(x+(corner==1||corner==2?1:0))*Math.PI*2/128,el=-Math.PI/2+(y+(corner>=2?1:0))*Math.PI/48;
                mesh.put((float)(Math.cos(az)*Math.cos(el)*46)).put((float)(Math.sin(el)*46)).put((float)(Math.sin(az)*Math.cos(el)*46));
            }
            mesh.flip();dome=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,dome);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,mesh,GL15.GL_STATIC_DRAW);
        }catch(RuntimeException|LinkageError error){dispose();throw error;}finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}
    }
    private void f(int index,float value){GL20.glUniform1f(uniforms[index],value);}
    private void color(int index,int rgb){GL20.glUniform3f(uniforms[index],(rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F);}
    public void draw(AuroraSimulation sim,AuroraOptions opt,float opacity){
        if(opacity<=0)return;
        double dt=Double.isNaN(last)?0:Math.max(0,sim.time-last);last=sim.time;phase+=dt*AuroraSimulation.clamp(opt.duskSpeed,.1F,3);
        state.begin();try{
            initialize();GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL20.glUseProgram(program);GL20.glUniform1i(uniforms[0],0);
            f(1,(float)phase);f(2,opacity);f(3,AuroraSimulation.clamp(opt.duskContrast,.4F,2.5F));f(4,AuroraSimulation.clamp(opt.duskScale,.5F,3));f(5,AuroraSimulation.clamp(opt.duskDarkness,0,1));f(6,opt.duskLightning?1:0);f(7,opt.duskStars?1:0);
            color(8,opt.duskColor);color(9,opt.duskSecond);GL20.glUniform3f(uniforms[10],OvercastNoise.horizon(opt,0),OvercastNoise.horizon(opt,1),OvercastNoise.horizon(opt,2));
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,dome);GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);GL11.glVertexPointer(3,GL11.GL_FLOAT,12,0L);GL11.glDrawArrays(GL11.GL_QUADS,0,128*48*4);
        }finally{state.end();}
    }
    public void dispose(){if(texture!=0)GL11.glDeleteTextures(texture);if(dome!=0)GL15.glDeleteBuffers(dome);if(program!=0)GL20.glDeleteProgram(program);texture=dome=program=0;}
}
