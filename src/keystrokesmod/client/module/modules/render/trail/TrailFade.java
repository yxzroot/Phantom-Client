package keystrokesmod.client.module.modules.render.trail;

/** Real-time 200ms enable/disable envelope, reversible without popping. */
public final class TrailFade {
    private double value;
    public void clear(){value=0;}
    public void advance(boolean enabled,double seconds){
        if(!Double.isFinite(seconds)||seconds<0)return;
        value=Math.max(0,Math.min(1,value+(enabled?1:-1)*seconds/.2));
    }
    public float gain(){return(float)(value*value*(3-2*value));}
}
