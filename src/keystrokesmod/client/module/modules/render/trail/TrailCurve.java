package keystrokesmod.client.module.modules.render.trail;

/** Bounded cubic interpolation, distance resampling and camera-facing 3-D frames. */
public final class TrailCurve {
    public static final int CAP=768;
    public final float[] x=new float[CAP],y=new float[CAP],z=new float[CAP],nx=new float[CAP],ny=new float[CAP],nz=new float[CAP];
    public final float[] footX=new float[CAP],footZ=new float[CAP],alpha=new float[CAP],width=new float[CAP],age=new float[CAP];
    public final double[] distance=new double[CAP];
    public final int[] color=new int[CAP];
    public final float[] topX=new float[CAP],topY=new float[CAP],topZ=new float[CAP],hemX=new float[CAP],hemY=new float[CAP],hemZ=new float[CAP];
    public int count;
    private static double tangent(double value,double delta){return value*delta<=0?0:Math.copySign(Math.min(Math.abs(value),Math.abs(delta)*2),delta);}
    private static double curve(double[] v,int a,int b,int c,int d,double t){
        double delta=v[c]-v[b],m0=tangent((v[c]-v[a])*.5,delta),m1=tangent((v[d]-v[b])*.5,delta),t2=t*t,t3=t2*t;
        return v[b]+(t3-2*t2+t)*m0+(-2*t3+3*t2)*delta+(t3-t2)*m1;
    }
    private void append(TrailBuffer b,TrailOptions opt,double now,double px,double py,double pz,double time,float boost,float life,double dist,boolean first,int s,int e,float t){
        int i=count++;x[i]=(float)px;y[i]=(float)py;z[i]=(float)pz;distance[i]=dist;
        age[i]=TrailOptions.clamp((float)((now-time)/(opt.length*life)),0,1);
        float f=1-age[i];alpha[i]=f*f*boost;
        if(first){float near=(float)Math.sqrt(px*px+py*py+pz*pz);alpha[i]*=smooth((near-.7F)/.7F);}
        width[i]=opt.width*.5F*(float)Math.sqrt(f)*boost;
        color[i]=opt.tint(age[i],now,b.totalDistance-dist);
        float bx=b.backX[s]+(b.backX[e]-b.backX[s])*t,bz=b.backZ[s]+(b.backZ[e]-b.backZ[s])*t;
        float norm=(float)Math.sqrt(bx*bx+bz*bz);if(norm>.0001F){bx/=norm;bz/=norm;}else{bx=0;bz=1;}
        float crouch=b.crouch[s]+(b.crouch[e]-b.crouch[s])*t,lean=crouch*.5F;
        if(opt.style==TrailOptions.Style.SolidBand){
            float sin=(float)Math.sin(lean),cos=(float)Math.cos(lean);
            float reach=.28F*cos-(opt.heightOffset-.75F)*sin;
            float centerY=.75F+(opt.heightOffset-.75F)*cos+.28F*sin-crouch*.125F;
            float half=opt.bandHeight*.5F;
            topX[i]=bx*(reach-half*sin);topZ[i]=bz*(reach-half*sin);topY[i]=centerY+half*cos;
            hemX[i]=bx*(reach+half*sin);hemZ[i]=bz*(reach+half*sin);hemY[i]=centerY-half*cos;
            return;
        }
        float reach=.205F*(float)Math.cos(lean)-(opt.top-.75F)*(float)Math.sin(lean);
        topX[i]=bx*reach;topZ[i]=bz*reach;topY[i]=.75F+(opt.top-.75F)*(float)Math.cos(lean)+.205F*(float)Math.sin(lean)-crouch*.125F;
        hemX[i]=bx*.205F;hemZ[i]=bz*.205F;hemY[i]=opt.hem;
    }
    private static float smooth(float value){float t=TrailOptions.clamp(value,0,1);return t*t*(3-2*t);}
    public void prepare(TrailBuffer b,TrailOptions opt,double now,double cx,double cy,double cz,double eyeX,double eyeY,double eyeZ,boolean first){
        count=0;if(b.count<2)return;
        int newest=b.index(0);double px=b.x[newest]-cx,py=b.y[newest]-cy,pz=b.z[newest]-cz;
        double pt=b.stamp[newest],pd=b.distance[newest];float pb=b.boost[newest],pl=b.life[newest];
        append(b,opt,now,px,py,pz,pt,pb,pl,pd,first,newest,newest,0);
        double spacing=opt.low?.09:.035,remaining=spacing;int limit=opt.low?256:CAP;
        for(int j=0;j<b.count-1&&count<limit;j++){
            int a=b.index(Math.max(0,j-1)),s=b.index(j),e=b.index(j+1),d=b.index(Math.min(b.count-1,j+2));
            for(int part=1;part<=3&&count<limit;part++){
                double t=part/3D,qx=curve(b.x,a,s,e,d,t)-cx,qy=curve(b.y,a,s,e,d,t)-cy,qz=curve(b.z,a,s,e,d,t)-cz;
                double qt=b.stamp[s]+(b.stamp[e]-b.stamp[s])*t,qd=b.distance[s]+(b.distance[e]-b.distance[s])*t;
                float qb=b.boost[s]+(b.boost[e]-b.boost[s])*(float)t,ql=b.life[s]+(b.life[e]-b.life[s])*(float)t;
                double dx=qx-px,dy=qy-py,dz=qz-pz,length=Math.sqrt(dx*dx+dy*dy+dz*dz);
                double consumed=0;
                while(length-consumed>=remaining&&count<limit){
                    consumed+=remaining;double f=consumed/length;
                    append(b,opt,now,px+dx*f,py+dy*f,pz+dz*f,pt+(qt-pt)*f,pb+(qb-pb)*(float)f,pl+(ql-pl)*(float)f,pd+(qd-pd)*f,first,s,e,(float)((part-1+f)/3));
                    remaining=spacing;
                }
                remaining-=length-consumed;px=qx;py=qy;pz=qz;pt=qt;pd=qd;pb=qb;pl=ql;
            }
        }
        if(count>0){alpha[count-1]=0;width[count-1]=0;}
        for(int i=0;i<count;i++){
            if(first){double ex=x[i]-eyeX,ey=y[i]-eyeY,ez=z[i]-eyeZ;alpha[i]*=smooth((float)(Math.sqrt(ex*ex+ey*ey+ez*ez)-.55)/.75F);}
            int a=Math.max(0,i-1),d=Math.min(count-1,i+1);
            float dx=x[d]-x[a],dy=y[d]-y[a],dz=z[d]-z[a];
            double vx=eyeX-x[i],vy=eyeY-y[i],vz=eyeZ-z[i];
            double ax=dy*vz-dz*vy,ay=dz*vx-dx*vz,az=dx*vy-dy*vx,n=Math.sqrt(ax*ax+ay*ay+az*az);
            if(n<1e-7){if(i>0){ax=nx[i-1];ay=ny[i-1];az=nz[i-1];n=1;}else{ax=-dz;ay=0;az=dx;n=Math.sqrt(ax*ax+az*az);if(n<1e-7){ax=1;ay=az=0;n=1;}}}
            if(i>0&&ax*nx[i-1]+ay*ny[i-1]+az*nz[i-1]<0)n=-n;
            nx[i]=(float)(ax/n);ny[i]=(float)(ay/n);nz[i]=(float)(az/n);
            float horizontal=(float)Math.sqrt(dx*dx+dz*dz);
            footX[i]=horizontal>1e-6?-dz/horizontal:i>0?footX[i-1]:1;
            footZ[i]=horizontal>1e-6?dx/horizontal:i>0?footZ[i-1]:0;
        }
        if(opt.style==TrailOptions.Style.SolidBand)TrailBand.prepare(this,opt,now);
    }
}
