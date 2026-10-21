package keystrokesmod.client.module.modules.combat.aim;

/** Pure time-based controller: critically damped velocity, dead zone and sensitivity accumulator. */
public final class TrailingPull {
    public static final double DEAD_ZONE=.7;
    private double vy,vp,dy,dp,ry,rp,yieldYawUntil,yieldPitchUntil;
    public double yawStep,pitchStep,fade;
    public void reset(){vy=vp=dy=dp=ry=rp=fade=yawStep=pitchStep=yieldYawUntil=yieldPitchUntil=0;}
    public void clearMotion(){vy=vp=dy=dp=ry=rp=yawStep=pitchStep=0;}
    public void step(double ey,double ep,double realYaw,double realPitch,double inputYaw,double inputPitch,
                     double dt,double now,boolean active,double quantum,AssistOptions o){
        dt=PositionHistory.clamp(dt,0,.05);yawStep=pitchStep=0;
        fade+=PositionHistory.clamp((active?o.activation:0)-fade,-dt/Math.max(.01,o.fade),dt/Math.max(.01,o.fade));
        if(!active||!Double.isFinite(ey)||!Double.isFinite(ep)){clearMotion();return;}
        double error=Math.hypot(ey,ep),realError=Math.hypot(realYaw,realPitch);
        if(error<=DEAD_ZONE||realError<=DEAD_ZONE){clearMotion();return;}
        if(inputYaw*ey<-.000001){yieldYawUntil=now+.1;vy=dy=ry=0;}
        if(inputPitch*ep<-.000001){yieldPitchUntil=now+.1;vp=dp=rp=0;}
        double gain=o.speed*(.5+Math.min(error,25)*.08)*fade;
        gain*=1+o.humanization*(.10*noise(now*.37,31)+.06*noise(now*.71,79));
        double distance=error-DEAD_ZONE,cap=gain*Math.min(1,distance/2);
        double ty=now<yieldYawUntil?0:cap*ey/error*o.yawStrength;
        double tp=now<yieldPitchUntil?0:cap*ep/error*o.pitchStrength;
        double omega=18,ex=Math.exp(-omega*dt),a=vy-ty,b=dy+omega*a;
        double sy=ty*dt+a*(1-ex)/omega+b*(1-ex*(1+omega*dt))/(omega*omega);
        vy=ty+(a+b*dt)*ex;dy=(dy-omega*b*dt)*ex;
        a=vp-tp;b=dp+omega*a;
        double sp=tp*dt+a*(1-ex)/omega+b*(1-ex*(1+omega*dt))/(omega*omega);
        vp=tp+(a+b*dt)*ex;dp=(dp-omega*b*dt)*ex;
        if(now<yieldYawUntil||sy*ey<=0){sy=0;ry=vy=dy=0;}
        if(now<yieldPitchUntil||sp*ep<=0){sp=0;rp=vp=dp=0;}
        if(sy*ry<0)ry=0;if(sp*rp<0)rp=0;
        double q=Math.max(.000001,quantum);ry+=sy;rp+=sp;
        yawStep=Math.copySign(Math.floor(Math.abs(ry)/q)*q,ry);
        pitchStep=Math.copySign(Math.floor(Math.abs(rp)/q)*q,rp);
        // Quantizing axes separately can rotate the step slightly. Check both the
        // actual chest and delayed point AFTER quantization, including the segment.
        if(crosses(ey,ep,yawStep,pitchStep)||crosses(realYaw,realPitch,yawStep,pitchStep)){
            yawStep=pitchStep=ry=rp=0;return;
        }
        ry-=yawStep;rp-=pitchStep;
    }
    private static boolean crosses(double ey,double ep,double sy,double sp){
        double len=sy*sy+sp*sp;if(len==0)return false;
        double t=PositionHistory.clamp((ey*sy+ep*sp)/len,0,1);
        return Math.hypot(ey-t*sy,ep-t*sp)<DEAD_ZONE;
    }
    public static double wrap(double angle){angle%=360;return angle>=180?angle-360:angle< -180?angle+360:angle;}
    public static double quantum(float sensitivity){double f=PositionHistory.clamp(sensitivity,0,1)*.6+.2;return f*f*f*8*.15;}
    /** Smooth hash/value noise: deterministic in time, no RNG calls or per-frame jitter. */
    public static double noise(double time,long seed){long i=(long)Math.floor(time);double f=time-i;f=f*f*(3-2*f);return hash(i+seed)*(1-f)+hash(i+1+seed)*f;}
    private static double hash(long value){value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;value=(value^(value>>>27))*0x94D049BB133111EBL;return ((value^(value>>>31))>>>11)*0x1.0p-52-1;}
}
