package keystrokesmod.client.module.modules.render.swordglow;

/** Allocation-free, bounded Hermite ribbon. Disconnected strokes never form a sheet. */
public final class SwordTrail {
    public static final int CAP=49;
    public final float[] x=new float[CAP],y=new float[CAP],z=new float[CAP];
    public final float[] nx=new float[CAP],ny=new float[CAP],alpha=new float[CAP];
    public final boolean[] start=new boolean[CAP];
    private final float[] cx=new float[13],cy=new float[13],cz=new float[13],cp=new float[13];
    private final double[] ct=new double[13];
    private final int[] chain=new int[13];
    public int count;

    public void prepare(SwordMotion motion,double now,float lifetime,boolean lowQuality){
        count=0;int controls=0;
        if(motion.count==0)return;
        int newest=(motion.head+11)%12;
        // Include the live tip between 60Hz samples, eliminating a visible 16ms lag.
        if(motion.isMoving()&&motion.observed>motion.stamp[newest]+.00001){
            cx[0]=motion.tipX;cy[0]=motion.tipY;cz[0]=motion.tipZ;cp[0]=motion.currentPower;ct[0]=motion.observed;chain[0]=motion.chain();controls++;
        }
        for(int j=0;j<motion.count;j++){
            int index=(motion.head-1-j+12)%12;
            cx[controls]=motion.x[index];cy[controls]=motion.y[index];cz[controls]=motion.z[index];
            cp[controls]=motion.power[index];ct[controls]=motion.stamp[index];chain[controls]=motion.stroke[index];controls++;
        }
        int steps=lowQuality?2:4;
        for(int j=0;j<controls-1;j++){
            if(chain[j]!=chain[j+1]||now-ct[j]>lifetime)continue;
            int a=j>0&&chain[j-1]==chain[j]?j-1:j;
            int b=j+2<controls&&chain[j+2]==chain[j]?j+2:j+1;
            boolean begin=j==0||chain[j-1]!=chain[j];
            for(int k=0;k<=steps;k++){
                if(k==0&&!begin)continue;
                float t=k/(float)steps,age=(float)(now-(ct[j]+(ct[j+1]-ct[j])*t));
                if(age<0||age>=lifetime)continue;
                // Bounded tangents cannot overshoot a sharp direction change or join
                // a new stroke. Width is at most 3.6% of the actual blade length.
                x[count]=curve(cx,a,j,j+1,b,t);y[count]=curve(cy,a,j,j+1,b,t);z[count]=curve(cz,a,j,j+1,b,t);
                float tx=derivative(cx,a,j,j+1,b,t),ty=derivative(cy,a,j,j+1,b,t),len=(float)Math.sqrt(tx*tx+ty*ty);
                float p=cp[j]+(cp[j+1]-cp[j])*t;
                float fade=1-age/lifetime;float width=motion.bladeLength*(.012F+.024F*p)*fade;
                nx[count]=len<.000001?0:-ty/len*width;ny[count]=len<.000001?0:tx/len*width;
                alpha[count]=p*fade*fade*(.35F+.65F*Math.min(1,(j+t)*.8F));
                start[count]=begin&&k==0;count++;
            }
        }
    }
    private static float curve(float[] v,int a,int b,int c,int d,float t){
        float delta=v[c]-v[b];
        float m0=tangent((v[c]-v[a])*.5F,delta),m1=tangent((v[d]-v[b])*.5F,delta);
        float t2=t*t,t3=t2*t;
        return (2*t3-3*t2+1)*v[b]+(t3-2*t2+t)*m0+(-2*t3+3*t2)*v[c]+(t3-t2)*m1;
    }
    private static float tangent(float value,float delta){return delta==0||value*delta<=0?0:Math.copySign(Math.min(Math.abs(value),Math.abs(delta)*2),delta);}
    private static float derivative(float[] v,int a,int b,int c,int d,float t){
        float delta=v[c]-v[b],m0=tangent((v[c]-v[a])*.5F,delta),m1=tangent((v[d]-v[b])*.5F,delta),t2=t*t;
        return (6*t2-6*t)*v[b]+(3*t2-4*t+1)*m0+(-6*t2+6*t)*v[c]+(3*t2-2*t)*m1;
    }
}
