package keystrokesmod.client.module.modules.render.aurora;

/** One deterministic chance roll per real-time interval, never one roll per frame. */
public final class AuroraShootingStar {
    public float progress,alpha,yaw,elevation,direction;
    private static float random(long slot,int salt){
        long value=slot+0x9E3779B97F4A7C15L*salt;
        value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;
        value=(value^(value>>>27))*0x94D049BB133111EBL;value^=value>>>31;
        return (value>>>40)/16777216F;
    }
    public static boolean occurs(long slot,float chance){return random(slot,1)<AuroraSimulation.clamp(chance,0,1);}
    public static double startsAt(long slot,float interval){
        interval=AuroraSimulation.clamp(interval,3,60);
        return slot*(double)interval+.65+random(slot,2)*interval*.45;
    }
    public boolean sample(double time,float chance,float interval){
        alpha=0;
        if(!Double.isFinite(time)||time<0)return false;
        interval=AuroraSimulation.clamp(interval,3,60);long slot=(long)Math.floor(time/interval);
        if(!occurs(slot,chance))return false;
        double age=time-startsAt(slot,interval),duration=.65+random(slot,3)*.30;
        if(age<=0||age>=duration)return false;
        progress=(float)(age/duration);alpha=(float)Math.sin(progress*Math.PI);
        yaw=random(slot,4)*360;elevation=18+random(slot,5)*13;direction=random(slot,6)<.5F?-1:1;
        return true;
    }
}
