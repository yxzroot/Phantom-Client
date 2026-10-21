package keystrokesmod.client.module.modules.render.aurora;

/** Reversible real-time blend between aurora and the monochrome sky. */
public final class SkyStyleBlend {
    private double mix;
    public void reset(boolean constellation){mix=constellation?1:0;}
    public void advance(boolean constellation,double seconds,float duration){
        if(!Double.isFinite(seconds)||seconds<0)return;
        mix=Math.max(0,Math.min(1,mix+(constellation?1:-1)*seconds/AuroraSimulation.clamp(duration,.05F,2)));
    }
    public float amount(){return(float)(mix*mix*(3-2*mix));}
}
