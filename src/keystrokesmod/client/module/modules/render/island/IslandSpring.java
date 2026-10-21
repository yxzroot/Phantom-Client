package keystrokesmod.client.module.modules.render.island;

/** Analytic damped spring: retargets without losing velocity; no frame-size dependence. */
public final class IslandSpring {
    public double value,velocity;
    public IslandSpring(double value){this.value=value;}
    public double update(double target,double dt,double speed,double overshoot){
        if(!Double.isFinite(target))return value;
        dt=Double.isFinite(dt)?Math.max(0,Math.min(1,dt)):0;
        double omega=22*Math.max(.2,Math.min(3,speed));
        double damping=.99-Math.max(0,Math.min(1,overshoot))*.35;
        double decay=Math.exp(-damping*omega*dt),freq=omega*Math.sqrt(1-damping*damping);
        double x=value-target,b=(velocity+damping*omega*x)/freq,c=Math.cos(freq*dt),s=Math.sin(freq*dt);
        value=target+decay*(x*c+b*s);
        velocity=decay*((b*freq-damping*omega*x)*c-(x*freq+damping*omega*b)*s);
        if(Math.abs(value-target)<.0001&&Math.abs(velocity)<.001){value=target;velocity=0;}
        return value;
    }
}
