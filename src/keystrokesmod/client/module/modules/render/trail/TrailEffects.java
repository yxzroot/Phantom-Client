package keystrokesmod.client.module.modules.render.trail;

/** A single 20-spark pool shared by all players, analytical delta-time motion. */
public final class TrailEffects {
    public static final int CAP=20;
    public final double[] x=new double[CAP],y=new double[CAP],z=new double[CAP],born=new double[CAP];
    public final float[] vx=new float[CAP],vy=new float[CAP],vz=new float[CAP],life=new float[CAP];
    public final int[] owner=new int[CAP],color=new int[CAP];
    private int seed=0x1AA45E1;
    private float random(){seed^=seed<<13;seed^=seed>>>17;seed^=seed<<5;return(seed&0x7FFFFFFF)/2147483648F;}
    public void clear(){for(int i=0;i<CAP;i++)life[i]=0;}
    public void clearOwner(int id){for(int i=0;i<CAP;i++)if(owner[i]==id)life[i]=0;}
    public void expire(double now){for(int i=0;i<CAP;i++)if(now-born[i]>life[i])life[i]=0;}
    public int active(){int count=0;for(int i=0;i<CAP;i++)if(life[i]>0)count++;return count;}
    public void spawn(int id,double px,double py,double pz,int tint,double now){
        for(int i=0;i<CAP;i++)if(life[i]<=0){
            x[i]=px;y[i]=py;z[i]=pz;born[i]=now;owner[i]=id;color[i]=tint;
            vx[i]=(random()-.5F)*.28F;vy[i]=.12F+random()*.22F;vz[i]=(random()-.5F)*.28F;life[i]=.35F+random()*.35F;return;
        }
    }
}
