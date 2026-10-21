package keystrokesmod.client.module.modules.combat.aim;

/** 500 ms at up to 256 samples/sec; samples are interpolated without temporary vectors. */
public final class PositionHistory {
    private final double[] time=new double[132],xs=new double[132],ys=new double[132],zs=new double[132];
    private int start,count;
    public double x,y,z;
    public void clear(){start=count=0;}
    public int size(){return count;}
    public void add(double now,double px,double py,double pz){
        if(count>0){int last=(start+count-1)%time.length;
            double dx=px-xs[last],dy=py-ys[last],dz=pz-zs[last];
            if(now<time[last]||now-time[last]>.6||dx*dx+dy*dy+dz*dz>16)clear();
            else if(now-time[last]<1D/256){xs[last]=px;ys[last]=py;zs[last]=pz;return;}
        }
        while(count>1&&now-time[(start+1)%time.length]>.5){start=(start+1)%time.length;count--;}
        if(count==time.length){start=(start+1)%time.length;count--;}
        int i=(start+count++)%time.length;time[i]=now;xs[i]=px;ys[i]=py;zs[i]=pz;
    }
    public void sample(double when){
        if(count==0){x=y=z=0;return;}
        int a=start;
        for(int n=1;n<count;n++){int b=(start+n)%time.length;if(time[b]>=when){double f=clamp((when-time[a])/Math.max(.000001,time[b]-time[a]),0,1);x=xs[a]+(xs[b]-xs[a])*f;y=ys[a]+(ys[b]-ys[a])*f;z=zs[a]+(zs[b]-zs[a])*f;return;}a=b;}
        x=xs[a];y=ys[a];z=zs[a];
    }
    public static double clamp(double v,double lo,double hi){return Double.isNaN(v)||Double.isInfinite(v)?lo:Math.max(lo,Math.min(hi,v));}
}
