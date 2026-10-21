package keystrokesmod.client.module.modules.render.aspect;

/** Real-time, allocation-free projection ratio envelope. */
public final class AspectAnimator {
    private double value=16D/9,from=value,target=value,elapsed=.2;
    public void reset(float ratio){value=from=target=safe(ratio);elapsed=.2;}
    public float update(float next,double dt,boolean smooth){
        next=safe(next);if(next!=target){from=value;target=next;elapsed=0;}
        elapsed=Math.min(.2,elapsed+(Double.isFinite(dt)?Math.max(0,dt):0));
        double t=smooth?elapsed/.2:1;t=t*t*(3-2*t);value=from+(target-from)*t;
        return(float)value;
    }
    public static float safe(float value){return Float.isFinite(value)&&value>0?Math.max(.1F,Math.min(10,value)):16F/9;}
    public static float fov(float vertical,float nativeRatio,float ratio,boolean keepVertical){
        if(keepVertical||ratio==nativeRatio)return vertical;
        if(!Float.isFinite(vertical)||vertical<=0||vertical>=180)return vertical;
        return(float)Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(vertical)*.5)*safe(nativeRatio)/safe(ratio)));
    }
}
