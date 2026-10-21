package keystrokesmod.client.module.modules.render.killeffect;

import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;

/** Stylized light/mesh effects only: no Minecraft entities, particles, drops or world mutations. */
public final class KillRenderer {
    public static final double LIGHTNING_STEP_SECONDS=.07;
    public static final float LIGHTNING_HEIGHT=7.5F,LIGHTNING_WIDTH=.022F;
    private final AuroraGLState state=new AuroraGLState();
    public final KillMesh mesh=new KillMesh();
    private float rx,rz,ux,uy,uz;
    private static float sin(double x){return(float)Math.sin(x);}
    private static float cos(double x){return(float)Math.cos(x);}
    private void sprite(float x,float y,float z,float w,float h,float angle,int cell,int color,float a){mesh.sprite(x,y,z,w,h,angle,cell,color,a,rx,rz,ux,uy,uz);}
    private void spark(float x,float y,float z,float s,int color,float a,boolean low){
        if(!low)sprite(x,y,z,s*3,s*3,0,1,color,a*.25F);
        sprite(x,y,z,s,s,0,1,KillOptions.mix(color,0xFFFFFF,.6F),a);
    }
    private void strip(float ax,float ay,float az,float bx,float by,float bz,float width,int color,float a){
        float dx=bx-ax,dy=by-ay,dz=bz-az,mx=(ax+bx)*-.5F,my=(ay+by)*-.5F,mz=(az+bz)*-.5F;
        float nx=dy*mz-dz*my,ny=dz*mx-dx*mz,nz=dx*my-dy*mx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(len<.0001F){nx=rx;ny=0;nz=rz;len=1;}width/=len;nx*=width;ny*=width;nz*=width;
        mesh.quad(ax-nx,ay-ny,az-nz,bx-nx,by-ny,bz-nz,bx+nx,by+ny,bz+nz,ax+nx,ay+ny,az+nz,color,a);
    }
    private void line(float ax,float ay,float az,float bx,float by,float bz,float width,int color,float a,boolean low){
        if(!low)strip(ax,ay,az,bx,by,bz,width*5,color,a*.065F);
        strip(ax,ay,az,bx,by,bz,width*2,color,a*.3F);strip(ax,ay,az,bx,by,bz,width,KillOptions.mix(color,0xFFFFFF,.7F),a);
    }
    private void ring(float x,float y,float z,float radius,float width,int color,float alpha,boolean low,float tilt){
        int n=low?32:64;
        for(int pass=low?1:0;pass<3;pass++){
            float w=width*(pass==0?3:pass==1?1.6F:.55F),a=alpha*(pass==0?.08F:pass==1?.3F:.85F);int tint=pass==2?KillOptions.mix(color,0xFFFFFF,.45F):color;
            for(int i=0;i<n;i++){
                double p=i*Math.PI*2/n,q=(i+1)*Math.PI*2/n;float cp=cos(p),sp=sin(p),cq=cos(q),sq=sin(q);
                mesh.quad(x+cp*(radius-w),y+sp*(radius-w)*tilt,z+sp*(radius-w),x+cq*(radius-w),y+sq*(radius-w)*tilt,z+sq*(radius-w),x+cq*(radius+w),y+sq*(radius+w)*tilt,z+sq*(radius+w),x+cp*(radius+w),y+sp*(radius+w)*tilt,z+sp*(radius+w),tint,a);
            }
        }
    }
    private void sphere(float x,float y,float z,float radius,int color,float alpha,boolean low,boolean dome){
        int bands=low?5:8,steps=low?16:24;double range=dome?Math.PI*.5:Math.PI;
        for(int v=0;v<bands;v++)for(int u=0;u<steps;u++){
            double p=v*range/bands,q=(v+1)*range/bands,a=u*Math.PI*2/steps,b=(u+1)*Math.PI*2/steps;
            mesh.quad(x+sin(p)*cos(a)*radius,y+cos(p)*radius,z+sin(p)*sin(a)*radius,x+sin(p)*cos(b)*radius,y+cos(p)*radius,z+sin(p)*sin(b)*radius,x+sin(q)*cos(b)*radius,y+cos(q)*radius,z+sin(q)*sin(b)*radius,x+sin(q)*cos(a)*radius,y+cos(q)*radius,z+sin(q)*sin(a)*radius,color,alpha);
        }
    }
    private void cube(float x,float y,float z,float s,float angle,int color,float alpha){
        float cs=cos(angle),sn=sin(angle),ct=cos(angle*.73),st=sin(angle*.73);
        for(int face=0;face<6;face++)for(int v=0;v<6;v++){
            int c=v==0||v==3?0:v==1?1:v==5?3:2;
            float a=c==0||c==3?-s:s,b=c<2?-s:s,xx,yy,zz;
            if(face<2){xx=a;yy=b;zz=face==0?-s:s;}else if(face<4){xx=face==2?-s:s;yy=b;zz=a;}else{xx=a;yy=face==4?-s:s;zz=b;}
            float tx=xx*cs-zz*sn,tz=xx*sn+zz*cs,ty=yy*ct-tz*st;tz=yy*st+tz*ct;
            mesh.point(x+tx,y+ty,z+tz,KillOptions.mix(color,0xFFFFFF,face*.09F),alpha*(.45F+face*.1F));
        }
    }
    private static float jitter(int seed,int segment,int epoch){int h=seed^segment*0x68E31DA4^epoch*0x1B56C4E9;h^=h>>>13;h*=0x5BD1E995;h^=h>>>15;return(h&65535)/32767.5F-1;}
    private void beam(float x,float y,float z,float height,float width,int color,float alpha){
        // Soft cross-section and fading caps: no opaque rectangular beam slabs.
        int n=12;
        for(int axis=0;axis<2;axis++)for(int strip=0;strip<n;strip++)for(int v=0;v<6;v++){
            float a=(strip/(float)n*2-1)*width,b=((strip+1F)/n*2-1)*width;
            float low=v/6F,high=(v+1F)/6;
            float wa=(float)Math.exp(-a*a/(width*width)*5),wb=(float)Math.exp(-b*b/(width*width)*5);
            float aa=alpha*wa*(1-low*low),ab=alpha*wb*(1-low*low),ba=alpha*wa*(1-high*high),bb=alpha*wb*(1-high*high);
            float ax=axis==0?x+a:x,az=axis==0?z:z+a,bx=axis==0?x+b:x,bz=axis==0?z:z+b;
            mesh.point(ax,y+low*height,az,color,aa);mesh.point(bx,y+low*height,bz,color,ab);mesh.point(bx,y+high*height,bz,color,bb);
            mesh.point(ax,y+low*height,az,color,aa);mesh.point(bx,y+high*height,bz,color,bb);mesh.point(ax,y+high*height,az,color,ba);
        }
    }
    public void draw(KillPool pool,KillOptions opt,double now,double cx,double cy,double cz,float rx,float rz,float ux,float uy,float uz){
        if(opt.intensity<=0||!pool.any())return;this.rx=rx;this.rz=rz;this.ux=ux;this.uy=uy;this.uz=uz;
        state.begin();try{
            mesh.begin();
            // Black surfaces need ordinary alpha blending; all emissive geometry is batched separately.
            for(int e=0;e<KillPool.SLOTS;e++)if(pool.active[e]&&pool.style[e]==8){
                float u=(float)((now-pool.born[e])/pool.life[e]);if(u<0||u>=.82F)continue;
                float radius=.34F*pool.size[e]*Math.min(1,u*7)*KillOptions.clamp((.82F-u)*7,0,1);
                sphere((float)(pool.x[e]-cx),(float)(pool.y[e]-cy)+.9F*pool.size[e],(float)(pool.z[e]-cz),radius,0x05020C,opt.intensity*.8F,opt.low,false);
            }
            mesh.flush(false);
            for(int e=0;e<KillPool.SLOTS;e++)if(pool.active[e]){
                float age=(float)(now-pool.born[e]),u=age/pool.life[e];if(u<0||u>=1)continue;
                float x=(float)(pool.x[e]-cx),y=(float)(pool.y[e]-cy),z=(float)(pool.z[e]-cz),s=pool.size[e];
                float fade=KillOptions.clamp(age*14,0,1)*(1-u)*(1-u),a=fade*opt.intensity*pool.gain[e];
                int style=pool.style[e],base=KillOptions.tint(pool.mode[e],pool.color[e],pool.second[e],.3F,u,now);
                if(style==0){
                    float bolt=KillOptions.clamp((.5F-age)*6,0,1),epoch=(float)(age/LIGHTNING_STEP_SECONDS),blend=epoch-(int)epoch;blend=blend*blend*(3-2*blend);
                    float strike=KillOptions.clamp(age*28,0,1)*(1-u)*(1-u)*opt.intensity*pool.gain[e]*1.35F;
                    for(int branch=0;branch<4;branch++){
                        float height=branch==0?LIGHTNING_HEIGHT:4.2F;
                        float px=x,py=y+height*s,pz=z;
                        for(int k=1;k<=12;k++){
                            float t=k/12F,jx=jitter(pool.salt[e]+branch,k,(int)epoch)*(1-blend)+jitter(pool.salt[e]+branch,k,(int)epoch+1)*blend,jz=jitter(pool.salt[e]-branch,k+77,(int)epoch)*(1-blend)+jitter(pool.salt[e]-branch,k+77,(int)epoch+1)*blend;
                            float spread=branch==0?0:(branch%2==0?-1:1)*(branch==3?.75F:.46F);
                            float nx=x+jx*.5F*s+spread*t*s,ny=y+(1-t)*height*s,nz=z+jz*.5F*s;
                            line(px,py,pz,nx,ny,nz,LIGHTNING_WIDTH*s,base,strike*bolt*(branch==0?1:.5F),opt.low);px=nx;py=ny;pz=nz;
                        }
                    }
                    sprite(x,y+.5F*s,z,1.8F*s,1.8F*s,0,1,KillOptions.mix(base,0xFFFFFF,.35F),strike*KillPool.flash(age)*1.7F);
                    ring(x,y+.04F,z,(.25F+u*1.2F)*s,.075F*s,base,a*.55F,opt.low,0);
                }else if(style==1){
                    for(int k=0;k<3;k++)ring(x,y+.035F+k*.025F,z,(.2F+u*(2.8F-k*.3F))*s,.04F*s,base,a*(1-k*.22F),opt.low,0);
                    sphere(x,y,z,(.3F+u*2.2F)*s,base,a*.06F,opt.low,true);
                }else if(style==2){
                    for(int k=10;k>=0;k--){float t=Math.max(0,u-k*.028F),p=t*9,rad=(.15F+t*.35F)*s;float sx=x+cos(p)*rad,sy=y+(.4F+t*3.8F)*s,sz=z+sin(p)*rad;
                        sprite(sx,sy,sz,(.22F+k*.006F)*s,(.32F+k*.009F)*s,0,1,base,a*(1-k*.075F)*.65F);}
                }else if(style==7){ring(x,y+.035F,z,(.2F+u*2)*s,.045F*s,base,a,opt.low,0);}
                else if(style==8){ring(x,y+.9F*s,z,(.52F+sin(u*7)*.07F)*s,.055F*s,base,a,opt.low,.25F);}
                else if(style==9){
                    float h=6*s,w=(.18F+sin(u*Math.PI)*.12F)*s;
                    beam(x,y,z,h,w*2,base,a*.25F);beam(x,y,z,h,w*.32F,KillOptions.mix(base,0xFFFFFF,.85F),a*.55F);
                    ring(x,y+.04F,z,(.35F+u*.8F)*s,.045F*s,base,a,opt.low,0);
                }
                int count=Math.min(pool.count[e],opt.low?24:60);
                for(int j=0;j<count;j++){
                    int p=e*KillPool.PER_SLOT+j;float seed=pool.seed[p],vx=pool.vx[p],vy=pool.vy[p],vz=pool.vz[p];
                    float px=x,py=y,pz=z,part=a*(.5F+seed*.5F),radius=(.035F+seed*.035F)*s;
                    int color=KillOptions.tint(pool.mode[e],pool.color[e],pool.second[e],seed,u,now);
                    if(style==0||style==1){px+=vx*age*s*.5F;py+=(.1F+Math.abs(vy)*age)*s;pz+=vz*age*s*.5F;spark(px,py,pz,radius*.65F,color,part*.5F,opt.low);}
                    else if(style==2){float t=(u+seed*.4F),angle=seed*20+t*7;px+=cos(angle)*(.2F+t*.3F)*s;py+=(.2F+t*3)*s;pz+=sin(angle)*(.2F+t*.3F)*s;spark(px,py,pz,radius*.5F,color,part*.4F,opt.low);}
                    else if(style==3){
                        px+=((j%4-1.5F)*.15F+vx*age*.65F)*s;py+=Math.max(.05F,(j/12)*.32F+.15F+(1+vy*.6F)*age-age*age*1.6F)*s;pz+=((j/4%3-1)*.12F+vz*age*.65F)*s;
                        cube(px,py,pz,(.06F+seed*.035F)*s,seed*8+age*(2+seed*4),color,part*.55F);
                    }else if(style==4){
                        px+=(vx*.55F*age+sin(age*4+seed*20)*.12F)*s;py+=Math.max(.05F,.6F+(1.5F+Math.abs(vy)*.5F)*age-age*age*.8F)*s;pz+=vz*.55F*age*s;
                        float flap=.2F+.8F*Math.abs(cos(age*8+seed*20));sprite(px,py,pz,radius*flap,radius*.45F,seed*8+age*2,0,color,part*.7F);
                    }else if(style==5){
                        float drag=(1-(float)Math.exp(-age*.9F))/.9F;px+=vx*drag*s;py+=(1+vy*drag-age*age*.24F)*s;pz+=vz*drag*s;
                        for(int trail=opt.low?1:3;trail>0;trail--){float past=Math.max(0,age-trail*.045F),d=(1-(float)Math.exp(-past*.9F))/.9F;sprite(x+vx*d*s,y+(1+vy*d-past*past*.24F)*s,z+vz*d*s,radius,radius,0,1,color,part*.12F);}
                        spark(px,py,pz,radius,color,part,opt.low);
                    }else if(style==6){
                        float t=u,angle=seed*35+t*8,r=(1-t)*(.2F+seed*.65F)*s;px+=cos(angle)*r;py+=(.1F+t*(1.5F+seed*1.5F))*s;pz+=sin(angle)*r;
                        sprite(px,py,pz,(.12F+seed*.09F)*(1-u*.6F)*s,(.25F+seed*.16F)*(1-u*.4F)*s,sin(angle)*.2F,2,color,part*.55F);if(j%3==0)spark(px,py+.25F*s,pz,radius*.5F,0xFFE0A0,part,opt.low);
                    }else if(style==7){
                        px+=vx*age*s*.7F;py+=Math.max(.04F,.7F+(Math.abs(vy)+.4F)*age-age*age*1.7F)*s;pz+=vz*age*s*.7F;
                        sprite(px,py,pz,radius*.7F,radius*2.5F,seed*10+age*3,3,color,part);if(!opt.low)sprite(px,py,pz,radius*3,radius*3,0,1,color,part*.12F);
                    }else if(style==8){
                        float r=u<.75F?(1-u/.75F)*(1+seed)*s:(u-.75F)*8*s,angle=seed*30+u*12;px+=cos(angle)*r;py+=(.9F+sin(angle*.7)*r*.25F)*s;pz+=sin(angle)*r;
                        float brightness=u>.75F?opt.intensity*.8F*sin((u-.75F)*Math.PI*4):part;
                        spark(px,py,pz,radius,color,brightness,opt.low);
                    }else{
                        px+=cos(seed*35+u*5)*(.12F+seed*.2F)*s;py+=(seed+u)*4*s;pz+=sin(seed*35+u*5)*(.12F+seed*.2F)*s;spark(px,py,pz,radius*.6F,color,part*.8F,opt.low);
                    }
                }
            }
            mesh.flush(true);
        }finally{state.end();}
    }
    public void dispose(){mesh.dispose();}
}
