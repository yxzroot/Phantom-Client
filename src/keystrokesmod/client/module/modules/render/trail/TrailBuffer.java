package keystrokesmod.client.module.modules.render.trail;

/** World-space frame samples. Nothing is allocated while recording/expiring. */
public final class TrailBuffer {
    public static final int CAP=1024;
    public final double[] x=new double[CAP],y=new double[CAP],z=new double[CAP],stamp=new double[CAP],distance=new double[CAP];
    public final float[] boost=new float[CAP],life=new float[CAP];
    public final float[] backX=new float[CAP],backZ=new float[CAP],crouch=new float[CAP];
    private float poseX,poseZ=1,poseCrouch;
    public void pose(float yaw,boolean sneaking){double angle=yaw*Math.PI/180;poseX=(float)Math.sin(angle);poseZ=-(float)Math.cos(angle);poseCrouch=sneaking?1:0;}
    public int head,count;
    public boolean visible,initialized,ground,landed;
    public double lastX,lastY,lastZ,lastTime,totalDistance,landingAt=-100,landingX,landingY,landingZ;
    public float emit;
    private double observed=-1;
    public void clear(){head=count=0;initialized=landed=visible=false;totalDistance=emit=0;landingAt=-100;observed=-1;}
    public int index(int age){return(head-1-age+CAP)%CAP;}
    public void expire(double now,float length){
        while(count>0){int index=index(count-1);if(now-stamp[index]<=length*life[index])break;count--;}
    }
    private void add(double px,double py,double pz,double now,boolean airborne,boolean sprint){
        x[head]=px;y[head]=py;z[head]=pz;stamp[head]=now;distance[head]=totalDistance;
        boost[head]=airborne?1.30F:sprint?1.20F:1;life[head]=sprint?1.35F:1;
        backX[head]=poseX;backZ[head]=poseZ;crouch[head]=poseCrouch;
        head=(head+1)%CAP;count=Math.min(CAP,count+1);
    }
    /** Returns distance moved this frame (also drives distance-based sparkle spawning). */
    public double record(double px,double py,double pz,double now,boolean onGround,boolean sprint,TrailOptions opt){
        landed=false;
        if(!Double.isFinite(px)||!Double.isFinite(py)||!Double.isFinite(pz)||!Double.isFinite(now)){clear();return 0;}
        if(initialized&&(now<observed||now-observed>.5)){clear();}
        double dx=px-lastX,dy=py-lastY,dz=pz-lastZ,ds=dx*dx+dy*dy+dz*dz;
        if(initialized&&ds>64)clear();
        observed=now;
        if(!initialized){initialized=true;lastX=px;lastY=py;lastZ=pz;lastTime=now;ground=onGround;add(px,py,pz,now,!onGround,sprint);return 0;}
        if(onGround&&!ground){landed=true;landingAt=now;landingX=px;landingY=py;landingZ=pz;}
        ground=onGround;expire(now,opt.length);
        double moved=Math.sqrt(ds);
        if(moved<(opt.low?.025:.0015)||opt.low&&now-lastTime<1.0/60)return 0;
        if(count==0)add(lastX,lastY,lastZ,Math.max(lastTime,now-.05),!onGround,sprint);
        totalDistance+=moved;add(px,py,pz,now,!onGround,sprint);
        lastX=px;lastY=py;lastZ=pz;lastTime=now;
        return moved;
    }
}
