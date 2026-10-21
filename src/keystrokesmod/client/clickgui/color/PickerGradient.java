package keystrokesmod.client.clickgui.color;

import keystrokesmod.client.clickgui.apple.rebuild.UiDraw;
import org.lwjgl.opengl.*;
import net.minecraft.client.renderer.GlStateManager;

/** Vertex-color gradients, with a cached rounded coverage mask (no palette textures). */
final class PickerGradient {
    static final int[] HUES={0xFFFF0000,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,0xFF0000FF,0xFFFF00FF,0xFFFF0000};
    private static int program,rect,radius;private static boolean attempted;
    private static final String VERT="#version 120\nvarying vec2 p;void main(){p=gl_Vertex.xy;gl_FrontColor=gl_Color;gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;}";
    private static final String FRAG="#version 120\nvarying vec2 p;uniform vec4 box;uniform float rad;void main(){vec2 q=abs(p-box.xy)-box.zw+rad;float d=length(max(q,0.0))+min(max(q.x,q.y),0.0)-rad;float a=1.0-smoothstep(-0.6,0.6,d);gl_FragColor=vec4(gl_Color.rgb,gl_Color.a*a);}";
    private static int shader(int type,String source){int s=GL20.glCreateShader(type);GL20.glShaderSource(s,source);GL20.glCompileShader(s);if(GL20.glGetShaderi(s,GL20.GL_COMPILE_STATUS)==0){GL20.glDeleteShader(s);throw new IllegalStateException("Picker coverage shader unavailable");}return s;}
    private static void initialize(){
        if(attempted)return;attempted=true;int v=0,f=0;
        try{v=shader(GL20.GL_VERTEX_SHADER,VERT);f=shader(GL20.GL_FRAGMENT_SHADER,FRAG);program=GL20.glCreateProgram();GL20.glAttachShader(program,v);GL20.glAttachShader(program,f);GL20.glLinkProgram(program);if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException("Picker shader link failed");rect=GL20.glGetUniformLocation(program,"box");radius=GL20.glGetUniformLocation(program,"rad");}
        catch(RuntimeException e){if(program!=0)GL20.glDeleteProgram(program);program=0;java.util.logging.Logger.getLogger("Phantom/ColorPicker").warning("Rounded gradient shader unavailable; using vertex gradients without coverage mask");}
        finally{if(v!=0)GL20.glDeleteShader(v);if(f!=0)GL20.glDeleteShader(f);}
    }
    private static void begin(float x,float y,float w,float h,float r){
        UiDraw.flushShapes();initialize();GL20.glUseProgram(program);GlStateManager.disableTexture2D();GL11.glDisable(GL11.GL_TEXTURE_2D);GlStateManager.disableDepth();GL11.glDisable(GL11.GL_DEPTH_TEST);GlStateManager.disableCull();GL11.glDisable(GL11.GL_CULL_FACE);GlStateManager.enableBlend();GL11.glEnable(GL11.GL_BLEND);GL11.glShadeModel(GL11.GL_SMOOTH);GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
        if(program!=0){GL20.glUniform4f(rect,x+w/2,y+h/2,w/2,h/2);GL20.glUniform1f(radius,r);}
        GL11.glBegin(GL11.GL_QUADS);
    }
    private static void vertex(float x,float y,int c,float alpha){GL11.glColor4f((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F,(c>>>24)/255F*alpha);GL11.glVertex2f(x,y);}
    private static void quad(float x,float y,float w,float h,int tl,int tr,int br,int bl,float a){vertex(x,y,tl,a);vertex(x+w,y,tr,a);vertex(x+w,y+h,br,a);vertex(x,y+h,bl,a);}
    private static void end(){GL11.glEnd();GL20.glUseProgram(0);GL11.glColor4f(1,1,1,1);}
    static void square(float x,float y,float size,int hue,float a){
        begin(x,y,size,size,5);quad(x,y,size,size,0xFFFFFFFF,hue,hue,0xFFFFFFFF,a);end();
        begin(x,y,size,size,5);quad(x,y,size,size,0,0,0xFF000000,0xFF000000,a);end();
    }
    static void hue(float x,float y,float w,float h,float a){begin(x,y,w,h,w/2);for(int i=0;i<6;i++)quad(x,y+h*i/6,w,h/6,HUES[i],HUES[i],HUES[i+1],HUES[i+1],a);end();}
    static void checker(float x,float y,float w,float h,float a){
        begin(x,y,w,h,Math.min(7,w/2));
        for(int row=0;row<(int)Math.ceil(h/6);row++)for(int col=0;col<(int)Math.ceil(w/6);col++){
            int c=((row+col)&1)==0?0xFF393640:0xFF222129;quad(x+col*6,y+row*6,Math.min(6,w-col*6),Math.min(6,h-row*6),c,c,c,c,a);
        }end();
    }
    static void alpha(float x,float y,float w,float h,int rgb,float a){checker(x,y,w,h,a);begin(x,y,w,h,w/2);quad(x,y,w,h,0xFF000000|rgb,0xFF000000|rgb,rgb,rgb,a);end();}
    static void track(float x,float y,float w,float h,int left,int right,float a){begin(x,y,w,h,h/2);quad(x,y,w,h,left,right,right,left,a);end();}
    static void swatch(float x,float y,float w,float h,int color,float a){checker(x,y,w,h,a);begin(x,y,w,h,7);quad(x,y,w,h,color,color,color,color,a);end();}
}
