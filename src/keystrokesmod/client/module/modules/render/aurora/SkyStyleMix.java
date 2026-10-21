package keystrokesmod.client.module.modules.render.aurora;

/** Convex crossfade; rapid changes never flash an unrelated style. Legacy indices stay stable. */
public final class SkyStyleMix {
    private final double[] weights={1,0,0,0,0,0},from={1,0,0,0,0,0};private int target;private double elapsed=1;
    public void reset(int style){target=style;elapsed=1;for(int i=0;i<weights.length;i++)weights[i]=from[i]=i==style?1:0;}
    public void advance(int style,double dt,float duration){
        if(style!=target){target=style;elapsed=0;System.arraycopy(weights,0,from,0,weights.length);}
        elapsed=Math.min(1,elapsed+(Double.isFinite(dt)?Math.max(0,dt):0)/AuroraSimulation.clamp(duration,.05F,2));
        double t=elapsed*elapsed*(3-2*elapsed);for(int i=0;i<weights.length;i++)weights[i]=from[i]+((i==target?1:0)-from[i])*t;
    }
    public float amount(int style){return(float)weights[style];}
}
