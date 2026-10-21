package keystrokesmod.client.module.modules.render.killeffect;

/** Five effect slots, sixty particles per slot. Replacing a slot also replaces its particles. */
public final class KillPool {
    public static final int SLOTS=5,PER_SLOT=60,CAP=SLOTS*PER_SLOT;
    public final boolean[] active=new boolean[SLOTS];
    public final double[] x=new double[SLOTS],y=new double[SLOTS],z=new double[SLOTS],born=new double[SLOTS];
    public final float[] life=new float[SLOTS],size=new float[SLOTS],gain=new float[SLOTS];
    public final int[] style=new int[SLOTS],color=new int[SLOTS],second=new int[SLOTS],mode=new int[SLOTS],count=new int[SLOTS],salt=new int[SLOTS];
    public final float[] vx=new float[CAP],vy=new float[CAP],vz=new float[CAP],seed=new float[CAP];
    private final int[] bag={0,1,2,3,4,5,6,7,8,9};
    private int cursor,random=0x6A22CC9,bagAt=10;
    public int streak;
    public double lastKill=-100;
    private float random(){random^=random<<13;random^=random>>>17;random^=random<<5;return(random>>>8)*0x1.0p-24F;}
    public void clear(){java.util.Arrays.fill(active,false);java.util.Arrays.fill(count,0);streak=0;lastKill=-100;cursor=0;}
    public void expire(double now){for(int i=0;i<SLOTS;i++)if(active[i]&&now-born[i]>=life[i]){active[i]=false;count[i]=0;}if(now-lastKill>10)streak=0;}
    private int randomStyle(){
        if(bagAt==10){for(int i=9;i>0;i--){int at=(int)(random()*(i+1)),swap=bag[i];bag[i]=bag[at];bag[at]=swap;}bagAt=0;}
        return bag[bagAt++];
    }
    public int spawn(KillOptions o,double px,double py,double pz,double now,boolean mine){
        if(!Double.isFinite(px)||!Double.isFinite(py)||!Double.isFinite(pz))return -1;
        int slot=cursor;cursor=(cursor+1)%SLOTS;
        if(mine){streak=now-lastKill<=10?streak+1:1;streak=Math.min(9999,streak);lastKill=now;}
        float boost=o.streak&&mine?1+Math.min(8,streak-1)*.055F:1;
        active[slot]=true;x[slot]=px;y[slot]=py;z[slot]=pz;born[slot]=now;
        style[slot]=o.effect==KillOptions.Effect.Random?randomStyle():o.effect.ordinal();
        size[slot]=KillOptions.clamp(o.size,.3F,3)*boost;gain[slot]=boost;life[slot]=KillOptions.clamp(o.duration,.4F,4);
        color[slot]=o.byEffect&&o.mode==KillOptions.ColorMode.Static?KillOptions.primary(style[slot]):o.color;
        second[slot]=o.byEffect&&o.mode==KillOptions.ColorMode.Static?KillOptions.secondary(style[slot]):o.second;
        mode[slot]=o.byEffect&&o.mode==KillOptions.ColorMode.Static?KillOptions.ColorMode.Gradient.ordinal():o.mode.ordinal();
        if(o.byEffect&&o.mode==KillOptions.ColorMode.Static&&style[slot]==4)mode[slot]=KillOptions.ColorMode.Rainbow.ordinal();
        count[slot]=o.low?24:PER_SLOT;salt[slot]=random;
        for(int j=0;j<count[slot];j++){
            int p=slot*PER_SLOT+j;double a=random()*Math.PI*2;float u=random()*2-1,r=(float)Math.sqrt(Math.max(0,1-u*u)),speed=.6F+random()*1.9F;
            vx[p]=(float)Math.cos(a)*r*speed;vz[p]=(float)Math.sin(a)*r*speed;vy[p]=u*speed;seed[p]=random();
        }
        return slot;
    }
    public int particles(boolean low){int sum=0;for(int i=0;i<SLOTS;i++)if(active[i])sum+=Math.min(low?24:PER_SLOT,count[i]);return sum;}
    public boolean any(){for(int i=0;i<SLOTS;i++)if(active[i])return true;return false;}
    public static float flash(double age){if(age<0||age>.24)return 0;float t=(float)(age/.24);return(1-t)*(1-t);}
    public static float pulse(double age){if(age<0||age>=.15)return 0;float s=(float)Math.sin(age/.15*Math.PI);return s*s;}
}
