package keystrokesmod.client.module.modules.render.swordglow;

/** Twelve fixed-rate samples: trail duration does not shrink at 240 FPS. View-space tip coordinates. */
public final class SwordMotion {
    public final float[] x=new float[12],y=new float[12],z=new float[12];
    public final double[] stamp=new double[12];
    public final float[] power=new float[12];
    public final int[] stroke=new int[12];
    public final SwordTrail trail=new SwordTrail();
    public float tipX,tipY,tipZ,hiltX,hiltY,hiltZ,emit;
    public int count,head;
    public double observed=Double.NaN,nextSample,lastStrike=-1;
    public boolean hitArmed=true;
    public long frame=-1;
    public float bladeLength=1,currentPower;
    public int releaseId=-1;
    private int chain,lastStroke;
    private boolean moving;
    public void reset(){count=head=0;observed=Double.NaN;frame=-1;emit=0;hitArmed=true;lastStrike=-1;releaseId=-1;chain=lastStroke=0;moving=false;currentPower=0;trail.count=0;}
    public float observe(float nx,float ny,float nz,double now,boolean swinging){
        return observe(nx,ny,nz,now,swinging,0,1);
    }
    public float observe(float nx,float ny,float nz,double now,boolean swinging,int swingId,float energy){
        if(!Double.isFinite(now)||!Float.isFinite(nx)||!Float.isFinite(ny)||!Float.isFinite(nz))return 0;
        float elapsed=Double.isNaN(observed)?0:(float)Math.max(0,Math.min(.25,now-observed));
        float dx=nx-tipX,dy=ny-tipY,dz=nz-tipZ;
        boolean discontinuity=Double.isNaN(observed)||now<observed||now-observed>.25||dx*dx+dy*dy+dz*dz>bladeLength*bladeLength;
        if(discontinuity){count=head=0;nextSample=now;tipX=nx;tipY=ny;tipZ=nz;observed=now;chain++;}
        if(swinging&&(!moving||swingId!=lastStroke)){chain++;nextSample=now;}
        float speed=elapsed>0&&!discontinuity?(float)Math.sqrt(dx*dx+dy*dy+dz*dz)/elapsed:0;
        currentPower=Math.min(1,speed/Math.max(.01F,bladeLength*4))*SwordAnimator.finiteClamp(energy,0,1,0);
        if(swinging){
            while(nextSample<=now){float t=now==observed?1:(float)((nextSample-observed)/(now-observed));t=Math.max(0,Math.min(1,t));
                x[head]=tipX+(nx-tipX)*t;y[head]=tipY+(ny-tipY)*t;z[head]=tipZ+(nz-tipZ)*t;stamp[head]=nextSample;
                power[head]=currentPower;stroke[head]=chain;
                head=(head+1)%12;count=Math.min(12,count+1);nextSample+=1.0/60;
            }
        }else nextSample=now;
        moving=swinging;lastStroke=swingId;
        tipX=nx;tipY=ny;tipZ=nz;observed=now;return elapsed;
    }
    public boolean isMoving(){return moving;}
    public int chain(){return chain;}
}
