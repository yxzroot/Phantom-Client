package keystrokesmod.client.module.modules.render.aurora;

import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** New-style-only renderer. Cached original textures/VBOs, no per-frame heap work. */
public final class ArtSkyRenderer {
    private final AuroraGLState state=new AuroraGLState();
    private final AuroraShootingStar meteor=new AuroraShootingStar();
    private static final String[] NAMES={"image","kind","clock","opacity","brightness","haze","topColor","midColor","horizon","colorA","colorB","style","profile","stripe","grid","skyline"};
    private final int[] uniforms=new int[NAMES.length];
    private int program,dome,stars,quad,noiseHi,noiseLo,star,planet,moon,skyline,trees,treesLow;
    private double last=Double.NaN,phase;
    private boolean smallTreeTexture;
    static int shader(int type,String source){int id=GL20.glCreateShader(type);GL20.glShaderSource(id,source);GL20.glCompileShader(id);if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String log=GL20.glGetShaderInfoLog(id,4096);GL20.glDeleteShader(id);throw new IllegalStateException(log);}return id;}
    static int buffer(float[] data){FloatBuffer out=BufferUtils.createFloatBuffer(data.length);out.put(data).flip();int id=GL15.glGenBuffers();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,id);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,out,GL15.GL_STATIC_DRAW);return id;}
    static int texture(int[] pixels,int size,boolean repeat){
        int id=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try{GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);
            IntBuffer data=BufferUtils.createIntBuffer(pixels.length);data.put(pixels).flip();GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,size,size,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,repeat?GL11.GL_REPEAT:GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,repeat?GL11.GL_REPEAT:GL12.GL_CLAMP_TO_EDGE);
        }catch(RuntimeException|LinkageError e){GL11.glDeleteTextures(id);throw e;}finally{GL11.glPopClientAttrib();}return id;
    }
    private static int treeTexture(boolean low){
        int w=CherryTreeArt.WIDTH/(low?CherryTreeArt.LOW_DIVISOR:1),h=CherryTreeArt.HEIGHT/(low?CherryTreeArt.LOW_DIVISOR:1);
        int id=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try{GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);
            IntBuffer data=BufferUtils.createIntBuffer(w*h);data.put(CherryTreeArt.pixels(low)).flip();
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL14.GL_GENERATE_MIPMAP,GL11.GL_TRUE);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,w,h,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR_MIPMAP_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        }catch(RuntimeException|LinkageError e){GL11.glDeleteTextures(id);throw e;}finally{GL11.glPopClientAttrib();}return id;
    }
    private void initialize(){if(program!=0)return;int v=0,f=0;try{
        smallTreeTexture=GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)<CherryTreeArt.WIDTH;
        v=shader(GL20.GL_VERTEX_SHADER,ArtSkyShaders.VERTEX);f=shader(GL20.GL_FRAGMENT_SHADER,ArtSkyShaders.FRAGMENT);program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);
        if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,4096));
        for(int i=0;i<NAMES.length;i++)uniforms[i]=GL20.glGetUniformLocation(program,NAMES[i]);
        dome=buffer(ArtSkyAssets.domeMesh());stars=buffer(ArtSkyAssets.starMesh());quad=buffer(new float[]{-1,-1,0,0,0,1,-1,0,1,0,1,1,0,1,1,-1,1,0,0,1});
        noiseHi=texture(ArtSkyAssets.noise(false),512,true);noiseLo=texture(ArtSkyAssets.noise(true),256,true);star=texture(ArtSkyAssets.star(),64,false);planet=texture(ArtSkyAssets.planet(),384,false);moon=texture(ArtSkyAssets.moon(),128,false);
    }catch(RuntimeException|LinkageError e){dispose();throw e;}finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}}
    private void f(int n,float value){GL20.glUniform1f(uniforms[n],value);}private void kind(int value){GL20.glUniform1i(uniforms[1],value);}
    private void color(int n,int rgb){GL20.glUniform3f(uniforms[n],(rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F);}
    private void mesh(int id,int stride,boolean colors,boolean uv){GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,id);GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glVertexPointer(3,GL11.GL_FLOAT,stride,0L);
        if(colors){GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);GL11.glColorPointer(4,GL11.GL_FLOAT,stride,12L);}else GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0);if(uv){GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glTexCoordPointer(2,GL11.GL_FLOAT,stride,colors?28L:12L);}else GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);}
    private void sprite(int tex,float yaw,float elevation,float sx,float sy,float rotation,int tint,float alpha){sprite(tex,yaw,elevation,sx,sy,rotation,tint,alpha,2);}
    private void sprite(int tex,float yaw,float elevation,float sx,float sy,float rotation,int tint,float alpha,int mode){
        kind(mode);GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex);mesh(quad,20,false,true);GL11.glColor4f((tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,alpha);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();try{GL11.glRotatef(yaw,0,1,0);GL11.glRotatef(elevation,1,0,0);GL11.glTranslatef(0,0,-40);GL11.glRotatef(rotation,0,0,1);GL11.glScalef(sx,sy,1);GL11.glDrawArrays(GL11.GL_QUADS,0,4);}finally{GL11.glPopMatrix();}}
    public static int starCount(AuroraOptions opt){return Math.round(ArtSkyAssets.STAR_CAP*AuroraSimulation.clamp(opt.stars,0,1)*(opt.lowQuality?.5F:1));}
    public void draw(AuroraSimulation sim,AuroraOptions opt){
        if(opt.art.mix()<=0)return;double dt=Double.isNaN(last)?0:Math.max(0,sim.time-last);last=sim.time;phase+=dt*AuroraSimulation.clamp(opt.art.cloudSpeed,.1F,3);
        state.begin();try{initialize();GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL20.glUseProgram(program);GL20.glUniform1i(uniforms[0],0);
            for(int style=0;style<3;style++){
                float weight=sim.fade*opt.art.amount(style);if(weight<=0)continue;f(2,(float)(phase%100000));f(3,weight);f(4,opt.art.brightness);f(5,opt.art.haze);
                color(6,opt.art.colors[style*5]);color(7,opt.art.colors[style*5+1]);color(9,opt.art.colors[style*5+3]);color(10,opt.art.colors[style*5+4]);
                GL20.glUniform1i(uniforms[11],style);GL20.glUniform1i(uniforms[12],1);f(13,opt.art.stripeSpeed);f(14,style==1&&opt.art.grid?1:0);f(15,(style==1?opt.art.skyline:opt.art.trees)?1:0);
                if(style==1){if(skyline==0)skyline=texture(ArtSkyAssets.skylinePixels(),512,true);GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,skyline);GL13.glActiveTexture(GL13.GL_TEXTURE0);}
                if(style==2){boolean low=opt.lowQuality||smallTreeTexture;int treeId;
                    if(low){if(treesLow==0)treesLow=treeTexture(true);treeId=treesLow;}else{if(trees==0)trees=treeTexture(false);treeId=trees;}
                    GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,treeId);GL13.glActiveTexture(GL13.GL_TEXTURE0);}
                GL20.glUniform3f(uniforms[8],ArtSkyPalette.horizon(opt.art,style,0),ArtSkyPalette.horizon(opt.art,style,1),ArtSkyPalette.horizon(opt.art,style,2));
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glBindTexture(GL11.GL_TEXTURE_2D,opt.lowQuality?noiseLo:noiseHi);kind(0);mesh(dome,12,false,false);GL11.glDrawArrays(GL11.GL_QUADS,0,ArtSkyAssets.domeMesh().length/3);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);if(opt.starField){kind(1);f(2,(float)(sim.time%100000));GL11.glBindTexture(GL11.GL_TEXTURE_2D,star);mesh(stars,36,true,true);GL11.glDrawArrays(GL11.GL_QUADS,0,starCount(opt)*4);}
                f(2,(float)(sim.time%100000));
                if(style==0&&opt.art.planet){
                    float drift=(float)(sim.time*.012%360);if(!opt.lowQuality)sprite(star,62+drift,17,14,11,0,0x7587CF,.20F);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);sprite(planet,62+drift,17,12,12,0,0xFFFFFF,1);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);if(!opt.lowQuality)sprite(star,-37+drift,38,2.8F,2.8F,0,0xBBD3FF,.3F);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);sprite(moon,-37+drift,38,1.1F,1.1F,0,0xFFFFFF,1);
                }
                if(style==1){float size=opt.art.sunSize*8;GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);if(!opt.lowQuality)sprite(star,62,8,size*2.8F,size*2.8F,0,opt.art.colors[8],.65F);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);sprite(star,62,8,size,size,0,0xFFFFFF,1,3);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);for(int i=0;i<9;i++){float t=(float)sim.time*.06F+i*2.4F;sprite(star,62+(float)Math.sin(t)*24,12+(float)Math.cos(t*1.3)*9,.06F,.06F,0,opt.art.colors[9],.4F+.35F*(float)Math.sin(t*3));}}
                if(style==2){GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);if(!opt.lowQuality)sprite(star,62,12,18,18,0,0xFFD7C5,.46F);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);sprite(opt.art.body==1?moon:star,62,12,5,5,0,0xFFF1DF,.85F,opt.art.body==1?2:4);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);for(int i=0;i<12;i++){float t=(float)sim.time*.05F+i*2.1F;sprite(star,i*31+(float)Math.sin(t)*6,18+(float)Math.sin(t*.6)*12,.04F,.04F,0,0xFFF0D7,.25F+.15F*(float)Math.sin(t*2));}
                    if(opt.art.lanterns)for(int i=0;i<4;i++){float t=(float)((sim.time+i*29)%110)/110;sprite(star,i*89+10,3+t*35,.12F,.20F,0,0xFFD09C,AuroraSimulation.smooth(t*8)*AuroraSimulation.smooth((1-t)*8)*.8F);}}
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);if(style==0&&opt.shooting&&meteor.sample(sim.time,opt.shootingChance,opt.shootingInterval))sprite(star,meteor.yaw+meteor.direction*meteor.progress*28,meteor.elevation-meteor.progress*8,2.8F,.055F,-18,0xDCEAFF,meteor.alpha*.8F);
                float comet=(float)(sim.time%157);if(style==0&&comet<21){float fade=AuroraSimulation.smooth(comet/3)*AuroraSimulation.smooth((21-comet)/3);sprite(star,-88+comet*.6F,42-comet*.12F,5.2F,.12F,15,0x80BDDD,fade*.4F);sprite(star,-88+comet*.6F+5,42-comet*.12F,.15F,.15F,0,0xE9FFFF,fade);}
            }
        }finally{state.end();}
    }
    public void dispose(){if(program!=0)GL20.glDeleteProgram(program);if(dome!=0)GL15.glDeleteBuffers(dome);if(stars!=0)GL15.glDeleteBuffers(stars);if(quad!=0)GL15.glDeleteBuffers(quad);if(noiseHi!=0)GL11.glDeleteTextures(noiseHi);if(noiseLo!=0)GL11.glDeleteTextures(noiseLo);if(star!=0)GL11.glDeleteTextures(star);if(planet!=0)GL11.glDeleteTextures(planet);if(moon!=0)GL11.glDeleteTextures(moon);if(skyline!=0)GL11.glDeleteTextures(skyline);if(trees!=0)GL11.glDeleteTextures(trees);if(treesLow!=0)GL11.glDeleteTextures(treesLow);program=dome=stars=quad=noiseHi=noiseLo=star=planet=moon=skyline=trees=treesLow=0;last=Double.NaN;}
}
