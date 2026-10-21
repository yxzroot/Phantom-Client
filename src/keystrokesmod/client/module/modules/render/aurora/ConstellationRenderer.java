package keystrokesmod.client.module.modules.render.aurora;

import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Cached graph, analytically deformed endpoints shared by every connected edge. */
public final class ConstellationRenderer {
    private static final String VERTEX="#version 120\n"
        +"uniform float clock;uniform vec2 viewport;uniform int kind;varying vec2 uv;varying float glow;"
        +"vec3 move(vec3 p,float s){p+=vec3(sin(clock*.09+s*19.0)*.022,cos(clock*.07+s*13.0)*.013,sin(clock*.08+s*23.0)*.022);return normalize(p);}"
        +"void main(){vec4 data=gl_MultiTexCoord0;vec3 a=move(gl_Vertex.xyz,data.y),b=move(gl_Color.rgb*2.0-1.0,gl_Color.a);"
        +"vec3 p=normalize(mix(a,b,data.x))*44.0;gl_Position=gl_ModelViewProjectionMatrix*vec4(p,1);uv=data.zw;"
        +"glow=.65+.35*sin(clock*.4+data.y*31.0+data.x*2.0);"
        +"if(kind==1){float size=1.15+data.y*1.1;gl_Position.xy+=data.zw*size*2.0/viewport*gl_Position.w;}}";
    private static final String FRAGMENT="#version 120\n"
        +"uniform float amount;uniform int kind;varying vec2 uv;varying float glow;"
        +"void main(){float a=amount*glow;if(kind==1){float d=dot(uv,uv);a*=exp(-d*3.5)*(1.0-smoothstep(.45,1.0,d));}gl_FragColor=vec4(1,1,1,a);}";
    private final AuroraGLState state=new AuroraGLState();
    private final IntBuffer viewport=BufferUtils.createIntBuffer(16);
    private int program,lines,nodes,clock,kind,amount,size;
    private double phase,lastTime=Double.NaN;
    private static int shader(int type,String text){
        int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,text);GL20.glCompileShader(shader);
        if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0){String error=GL20.glGetShaderInfoLog(shader,4096);GL20.glDeleteShader(shader);throw new IllegalStateException(error);}return shader;
    }
    private static int buffer(float[] data){
        FloatBuffer upload=BufferUtils.createFloatBuffer(data.length);upload.put(data).flip();int vbo=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,upload,GL15.GL_STATIC_DRAW);return vbo;
    }
    private void initialize(){
        if(program!=0)return;int v=0,f=0;
        try{
            v=shader(GL20.GL_VERTEX_SHADER,VERTEX);f=shader(GL20.GL_FRAGMENT_SHADER,FRAGMENT);program=GL20.glCreateProgram();
            GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);
            if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));
            clock=GL20.glGetUniformLocation(program,"clock");kind=GL20.glGetUniformLocation(program,"kind");amount=GL20.glGetUniformLocation(program,"amount");size=GL20.glGetUniformLocation(program,"viewport");
            ConstellationGeometry mesh=new ConstellationGeometry();lines=buffer(mesh.lines());nodes=buffer(mesh.nodes());
        }catch(RuntimeException|LinkageError error){dispose();throw error;}
        finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}
    }
    private void arrays(int vbo){
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3,GL11.GL_FLOAT,44,0L);GL11.glColorPointer(4,GL11.GL_FLOAT,44,12L);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glTexCoordPointer(4,GL11.GL_FLOAT,44,28L);
    }
    public void draw(AuroraSimulation sim,AuroraOptions opt,float opacity){
        if(!Double.isFinite(sim.time)||opacity<=0)return;
        double elapsed=Double.isNaN(lastTime)?0:Math.max(0,sim.time-lastTime);lastTime=sim.time;phase+=elapsed*AuroraSimulation.clamp(opt.constellationSpeed,.1F,3);
        int count=Math.round(ConstellationGeometry.NODES*AuroraSimulation.clamp(opt.constellationDensity,0,1));if(count==0||opt.constellationBrightness<=0)return;
        state.begin();
        try{
            initialize();GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);
            GL11.glRotatef((float)(phase*.22%360),0,1,0);
            GL20.glUseProgram(program);GL20.glUniform1f(clock,(float)phase);viewport.clear();GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);
            GL20.glUniform2f(size,Math.max(1,viewport.get(2)),Math.max(1,viewport.get(3)));
            float gain=opacity*AuroraSimulation.clamp(opt.constellationBrightness,0,2);
            if(opt.constellationLines&&count>1){
                arrays(lines);GL20.glUniform1i(kind,0);int vertices=ConstellationGeometry.edgeCount(count)*ConstellationGeometry.SEGMENTS*2;
                GL11.glEnable(GL11.GL_LINE_SMOOTH);GL11.glHint(GL11.GL_LINE_SMOOTH_HINT,GL11.GL_NICEST);
                if(opt.bloom&&!opt.lowQuality){GL11.glLineWidth(3);GL20.glUniform1f(amount,gain*.045F);GL11.glDrawArrays(GL11.GL_LINES,0,vertices);}
                GL11.glLineWidth(1);GL20.glUniform1f(amount,gain*.44F);GL11.glDrawArrays(GL11.GL_LINES,0,vertices);
            }
            if(opt.starField){arrays(nodes);GL20.glUniform1i(kind,1);GL20.glUniform1f(amount,gain*.9F);
                GL11.glDrawArrays(GL11.GL_QUADS,0,Math.round(count*AuroraSimulation.clamp(opt.stars,0,1))*4);}
        }finally{state.end();}
    }
    public void dispose(){
        if(lines!=0)GL15.glDeleteBuffers(lines);if(nodes!=0)GL15.glDeleteBuffers(nodes);if(program!=0)GL20.glDeleteProgram(program);lines=nodes=program=0;
    }
}
