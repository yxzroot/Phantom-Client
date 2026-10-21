package keystrokesmod.client.module.modules.render.trail;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** One persistent streaming VBO, preallocated curves/batch. Called ONLY from RenderWorldLast. */
public final class TrailRenderer {
    private final TrailGLState state=new TrailGLState();
    private final TrailCurve curve=new TrailCurve();
    private final FloatBuffer vertices=BufferUtils.createFloatBuffer(TrailCurve.CAP*8*7);
    private final FloatBuffer matrix=BufferUtils.createFloatBuffer(16);
    private static final float[] EDGE={-1,-.36F,0,.36F,1},SOFT={0,.38F,1,.38F,0};
    private int vbo;
    private float rx,ry,rz,ux,uy,uz;
    private double eyeX,eyeY,eyeZ;

    public void render(TrailBuffer[] paths,TrailEffects effects,TrailOptions opt,double now,float fade,
                       double cx,double cy,double cz,boolean first){
        if(fade<=0||opt.intensity<=0)return;
        state.begin();
        try{
            matrix.clear();GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,matrix);
            rx=matrix.get(0);ry=matrix.get(4);rz=matrix.get(8);ux=matrix.get(1);uy=matrix.get(5);uz=matrix.get(9);
            eyeX=-(matrix.get(0)*matrix.get(12)+matrix.get(1)*matrix.get(13)+matrix.get(2)*matrix.get(14));
            eyeY=-(matrix.get(4)*matrix.get(12)+matrix.get(5)*matrix.get(13)+matrix.get(6)*matrix.get(14));
            eyeZ=-(matrix.get(8)*matrix.get(12)+matrix.get(9)*matrix.get(13)+matrix.get(10)*matrix.get(14));
            GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE1);GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glEnable(GL11.GL_BLEND);GL11.glShadeModel(GL11.GL_SMOOTH);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);
            float strength=fade*opt.intensity;
            for(int actor=0;actor<paths.length;actor++){
                TrailBuffer path=paths[actor];if(!path.visible)continue;
                boolean localFirst=actor==0&&first;
                curve.prepare(path,opt,now,cx,cy,cz,eyeX,eyeY,eyeZ,localFirst);
                if(curve.count>=2){
                    if(opt.style==TrailOptions.Style.SolidBand){
                        if(opt.bloom>0&&!opt.low)band(opt,strength*opt.opacity*opt.bloom*.12F,true);
                        band(opt,strength*opt.opacity,false);
                        bandEdges(opt,strength*opt.opacity);
                    }
                    else if(opt.style==TrailOptions.Style.Particles||opt.style==TrailOptions.Style.Aura)dots(opt,strength,now);
                    else{
                        if(opt.bloom>0&&!opt.low)ribbons(opt,strength*opt.bloom*.15F,2.4F,true);
                        ribbons(opt,strength*.32F,1,false);
                    }
                }
                if(opt.landing&&now-path.landingAt<.5)landing(path,opt,now,cx,cy,cz,strength,localFirst);
            }
            unbuffer();
            if(opt.sparkles&&!opt.low){
                GL11.glBegin(GL11.GL_TRIANGLES);
                for(int i=0;i<TrailEffects.CAP;i++)if(effects.life[i]>0&&effects.owner[i]<paths.length&&paths[effects.owner[i]].visible){
                    float age=(float)(now-effects.born[i]),t=age/effects.life[i];if(t<0||t>=1)continue;
                    double x=effects.x[i]-cx+effects.vx[i]*age,y=effects.y[i]-cy+effects.vy[i]*age-.09*age*age,z=effects.z[i]-cz+effects.vz[i]*age;
                    float a=(1-t)*(1-t)*Math.min(1,age*20)*strength;
                    if(first&&effects.owner[i]==0)a*=nearFade(x,y,z);
                    dot((float)x,(float)y,(float)z,.018F,effects.color[i],a*.6F,.5F,8);
                }GL11.glEnd();
            }
        }finally{try{state.end();}finally{state.restoreDefaultState();}}
    }
    private void unbuffer(){GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,0);GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);}
    private void band(TrailOptions opt,float strength,boolean glow){
        if(vbo==0)vbo=GL15.glGenBuffers();vertices.clear();
        for(int lane=0;lane<4;lane++)for(int i=0;i<curve.count;i++)for(int side=0;side<2;side++){
            int edge=lane+side,tint=curve.color[i];float p=EDGE[edge],profile=curve.width[i];
            float v=.5F+p*.5F*profile;
            float pad=glow?p*.035F*profile:0;
            vertices.put(curve.x[i]+curve.hemX[i]+(curve.topX[i]-curve.hemX[i])*v);
            vertices.put(curve.y[i]+curve.hemY[i]+(curve.topY[i]-curve.hemY[i])*v+pad);
            vertices.put(curve.z[i]+curve.hemZ[i]+(curve.topZ[i]-curve.hemZ[i])*v);
            float core=glow?0:.06F*(1-Math.abs(p));
            float r=(tint>>16&255)/255F,g=(tint>>8&255)/255F,b=(tint&255)/255F;
            vertices.put(r+(1-r)*core).put(g+(1-g)*core).put(b+(1-b)*core);
            // Filled across its entire width; only the bloom has transparent edges.
            vertices.put(Math.min(1,curve.alpha[i]*strength)*(glow?SOFT[edge]:1));
        }
        vertices.flip();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,vertices,GL15.GL_STREAM_DRAW);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3,GL11.GL_FLOAT,28,0L);GL11.glColorPointer(4,GL11.GL_FLOAT,28,12L);
        for(int lane=0;lane<4;lane++)GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP,lane*curve.count*2,curve.count*2);
        unbuffer();
    }
    private void bandEdges(TrailOptions opt,float strength){
        GL11.glLineWidth(opt.edgeWidth);GL11.glEnable(GL11.GL_LINE_SMOOTH);
        for(int side=0;side<2;side++){
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for(int i=0;i<curve.count;i++){
                float v=.5F+(side==0?-.5F:.5F)*curve.width[i];int tint=curve.color[i];
                GL11.glColor4f((tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,Math.min(1,curve.alpha[i]*strength*.65F));
                GL11.glVertex3f(curve.x[i]+curve.hemX[i]+(curve.topX[i]-curve.hemX[i])*v,curve.y[i]+curve.hemY[i]+(curve.topY[i]-curve.hemY[i])*v,curve.z[i]+curve.hemZ[i]+(curve.topZ[i]-curve.hemZ[i])*v);
            }GL11.glEnd();
        }
        GL11.glDisable(GL11.GL_LINE_SMOOTH);GL11.glLineWidth(1);
    }
    private void ribbons(TrailOptions opt,float strength,float size,boolean bloom){
        boolean dual=opt.style==TrailOptions.Style.Dual;
        float styleWidth=opt.style==TrailOptions.Style.Line||dual?.17F:opt.style==TrailOptions.Style.Wings?2.5F:1;
        if(vbo==0)vbo=GL15.glGenBuffers();
        for(int strand=0;strand<(dual?2:1);strand++){
            vertices.clear();float offset=dual?(strand==0?-.16F:.16F):0;
            for(int lane=0;lane<4;lane++)for(int i=0;i<curve.count;i++)for(int side=0;side<2;side++){
                int edge=lane+side,tint=curve.color[i];float core=!bloom&&edge==2?.46F:0;
                float width=curve.width[i]*styleWidth*size;
                float v=(EDGE[edge]+1)*.5F;
                if(opt.style==TrailOptions.Style.Line)v=.5F+EDGE[edge]*.025F;
                else if(dual)v=(strand==0?.04F:.96F)+EDGE[edge]*.025F;
                float spread=width*EDGE[edge]*(float)Math.sin(v*Math.PI);
                if(opt.style==TrailOptions.Style.Wings)spread*=1.8F;
                float extra=bloom?EDGE[edge]*.025F:0;
                float r=(tint>>16&255)/255F,g=(tint>>8&255)/255F,b=(tint&255)/255F;
                float alpha=Math.min(.75F,curve.alpha[i]*strength)*SOFT[edge];
                vertices.put(curve.x[i]+curve.hemX[i]+(curve.topX[i]-curve.hemX[i])*v+curve.footX[i]*(offset+spread));
                vertices.put(curve.y[i]+curve.hemY[i]+(curve.topY[i]-curve.hemY[i])*v+extra);
                vertices.put(curve.z[i]+curve.hemZ[i]+(curve.topZ[i]-curve.hemZ[i])*v+curve.footZ[i]*(offset+spread));
                vertices.put(r+(1-r)*core).put(g+(1-g)*core).put(b+(1-b)*core).put(alpha);
            }
            vertices.flip();GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL15.glBufferData(GL15.GL_ARRAY_BUFFER,vertices,GL15.GL_STREAM_DRAW);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glVertexPointer(3,GL11.GL_FLOAT,28,0L);GL11.glColorPointer(4,GL11.GL_FLOAT,28,12L);
            for(int lane=0;lane<4;lane++)GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP,lane*curve.count*2,curve.count*2);
        }
        unbuffer();
    }
    private void dots(TrailOptions opt,float strength,double now){
        unbuffer();boolean smoke=opt.style==TrailOptions.Style.Aura;double next=Double.POSITIVE_INFINITY;
        GL11.glBegin(GL11.GL_TRIANGLES);
        for(int i=0;i<curve.count;i++)if(curve.distance[i]<next){
            next=curve.distance[i]-(opt.low?.22:smoke?.11:.15);
            float age=curve.age[i],size=opt.width*(smoke?.6F*(1+age*2.2F):.14F);
            float vertical=smoke?.5F:(float)(.5+.45*Math.sin(curve.distance[i]*11));
            float x=curve.x[i]+curve.hemX[i]+(curve.topX[i]-curve.hemX[i])*vertical,y=curve.y[i]+curve.hemY[i]+(curve.topY[i]-curve.hemY[i])*vertical,z=curve.z[i]+curve.hemZ[i]+(curve.topZ[i]-curve.hemZ[i])*vertical;
            if(smoke){x+=(float)Math.sin(curve.distance[i]*8+now*.22)*opt.width*age*.4F;y+=age*.3F;z+=(float)Math.cos(curve.distance[i]*7+now*.2)*opt.width*age*.4F;}
            dot(x,y,z,size,curve.color[i],Math.min(.7F,curve.alpha[i]*strength)*(smoke?.18F:.75F),smoke?0:.45F,opt.low?8:12);
        }GL11.glEnd();
    }
    private void dot(float x,float y,float z,float size,int color,float alpha,float core,int sides){
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        for(int j=0;j<sides;j++)for(int k=0;k<3;k++){
            double angle=(j+(k==2?1:0))*Math.PI*2/sides;float a=k==0?0:(float)Math.cos(angle)*size,c=k==0?0:(float)Math.sin(angle)*size;
            GL11.glColor4f(r+(1-r)*core,g+(1-g)*core,b+(1-b)*core,k==0?alpha:0);
            GL11.glVertex3f(x+rx*a+ux*c,y+ry*a+uy*c,z+rz*a+uz*c);
        }
    }
    private float nearFade(double x,double y,double z){
        double dx=x-eyeX,dy=y-eyeY,dz=z-eyeZ;return TrailOptions.clamp((float)(Math.sqrt(dx*dx+dy*dy+dz*dz)-.7)/.7F,0,1);
    }
    private void landing(TrailBuffer path,TrailOptions opt,double now,double cx,double cy,double cz,float strength,boolean first){
        unbuffer();float t=(float)(now-path.landingAt)/.5F;if(t<0||t>=1)return;
        int color=opt.tint(t,now,0);float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        float radius=.12F+t*.75F,alpha=(1-t)*(1-t)*strength*.3F;
        double x=path.landingX-cx,y=path.landingY-cy+.015,z=path.landingZ-cz;if(first)alpha*=nearFade(x,y,z);
        int segments=opt.low?24:48;
        for(int side=0;side<2;side++){
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for(int j=0;j<=segments;j++)for(int edge=0;edge<2;edge++){
                int lane=side+edge;double angle=j*Math.PI*2/segments,rad=radius+(lane-1)*.055*(1-t);
                GL11.glColor4f(r,g,b,lane==1?alpha:0);GL11.glVertex3d(x+Math.cos(angle)*rad,y,z+Math.sin(angle)*rad);
            }GL11.glEnd();
        }
    }
    public void dispose(){if(vbo!=0){GL15.glDeleteBuffers(vbo);vbo=0;}}
}
