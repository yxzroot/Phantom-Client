package keystrokesmod.client.module.modules.movement.airstrafe;

/** Tick-based additive air acceleration. No vertical state, vectors, RNG, or frame clock. */
public final class AirSteering {
    private double airTime,blend;
    public double x,z;
    public void reset(){airTime=blend=0;}
    public double fade(){double t=Math.min(1,airTime/.1);return t*t*(3-2*t);}
    public double strength(){return blend;}
    public void step(double vx,double vz,double baseX,double baseZ,double wishX,double wishZ,AirOptions o){
        airTime=Math.min(.1,airTime+.05);
        boolean input=wishX*wishX+wishZ*wishZ>.00001;
        double target=input?o.strength*fade():0;
        double before=Math.hypot(vx,vz),wishLength=Math.hypot(wishX,wishZ);
        // Direction already follows input immediately; the old sluggish feeling
        // came from weak acceleration fighting retained momentum for many ticks.
        // Give sideways/reversing input extra bounded authority, not a velocity
        // replacement. Straight-line acceleration keeps its original strength.
        double turn=input&&before>1E-6?Math.max(0,Math.min(1,(1-(vx*wishX+vz*wishZ)/(before*wishLength))*.5)):0;
        double response=o.smoothing<=0?1:1-Math.exp(-.05/(.025+o.smoothing*.2*(1-.85*turn)));
        blend+=(target-blend)*response;
        double acceleration=.06*blend*(1+2*turn);
        x=vx+baseX+wishX*acceleration;z=vz+baseZ+wishZ*acceleration;
        // Project ONLY the newly accelerated velocity onto the allowed disk.
        // Projection is non-expansive: the correction can never turn a small
        // acceleration into a large direction snap. External momentum is retained.
        double limit=Math.max(o.cap,before),speed=Math.hypot(x,z);
        if(speed>limit&&speed>0){double scale=Math.nextDown(limit)/speed;x*=scale;z*=scale;}
    }
}
