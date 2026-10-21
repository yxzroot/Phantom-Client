package keystrokesmod.client.module.modules.render.worldparticles;

/** Fixed pool, world-space positions, analytic drift integrals (not frame-count motion). */
public final class ParticleSimulation {
    public interface Exposure {
        int height(int x,int z);
        boolean open(double x,double y,double z);
    }
    public static final int CAP=300;
    public final double[] x=new double[CAP],y=new double[CAP],z=new double[CAP];
    public final float[] phase=new float[CAP],size=new float[CAP],alpha=new float[CAP],angle=new float[CAP];
    private final double[] checkAt=new double[CAP];
    private final double[] spin=new double[CAP];
    private final boolean[] exposed=new boolean[CAP];
    private final float[] born=new float[CAP];
    public int count,currentShape,previousShape;
    public double time;
    public float fade,shapeMix=1;
    private double cx,cy,cz;
    private float fadePosition;
    private double motionTime;
    private boolean initialized;
    private int random=0x2F6E2B1;
    public void reset(){count=0;initialized=false;fade=fadePosition=0;time=motionTime=0;shapeMix=1;}
    private float random(){random^=random<<13;random^=random>>>17;random^=random<<5;return (random>>>8)*0x1.0p-24F;}
    private boolean spawn(int i,ParticleOptions o,double px,double py,double pz,Exposure roof){
        float r=ParticleOptions.clamp(o.radius,4,24),vertical=r*.65F;
        // Bounded attempts; no chunk loads and no indefinite search in a cave.
        for(int attempt=0;attempt<4;attempt++){
            x[i]=px+(random()*2-1)*r;y[i]=py+(random()*2-1)*vertical;z[i]=pz+(random()*2-1)*r;
            int h=roof.height((int)Math.floor(x[i]),(int)Math.floor(z[i]));
            if(roof.open(x[i],y[i],z[i])&&(!o.outdoors||y[i]>h+.2)){
                phase[i]=random();size[i]=.65F+random()*.7F;alpha[i]=0;born[i]=0;
                spin[i]=angle[i]=random()*6.2831853F;checkAt[i]=time+.05+random()*.2;exposed[i]=true;return true;
            }
        }
        alpha[i]=0;born[i]=0;exposed[i]=false;checkAt[i]=time+.35+random()*.3;return false;
    }
    public void advance(double dt,boolean active,boolean paused,ParticleOptions o,double px,double py,double pz,Exposure roof){
        if(!Double.isFinite(px)||!Double.isFinite(py)||!Double.isFinite(pz)){reset();return;}
        dt=Double.isFinite(dt)?Math.max(0,Math.min(.25,dt)):0;
        double dx=px-cx,dy=py-cy,dz=pz-cz;
        if(!initialized||dx*dx+dy*dy+dz*dz>256){
            reset();initialized=true;cx=px;cy=py;cz=pz;currentShape=previousShape=o.shape.ordinal();
        }
        cx=px;cy=py;cz=pz;
        fadePosition=ParticleOptions.clamp(fadePosition+(active?1:-1)*(float)(dt/.2),0,1);
        fade=fadePosition*fadePosition*(3-2*fadePosition);
        if(o.shape.ordinal()!=currentShape){previousShape=currentShape;currentShape=o.shape.ordinal();shapeMix=0;}
        shapeMix=ParticleOptions.clamp(shapeMix+(float)(dt/.18),0,1);
        if(paused)return;
        double before=motionTime;time+=dt;motionTime+=dt*ParticleOptions.clamp(o.speed,0,3);int target=active?o.limit():count;
        for(int i=count;i<target;i++)spawn(i,o,px,py,pz,roof);
        count=target;
        float r=ParticleOptions.clamp(o.radius,4,24),vertical=r*.65F;
        for(int i=0;i<count;i++){
            if(o.motion==ParticleOptions.Motion.Drift&&exposed[i]){
                double a=before,b=motionTime,p=phase[i]*6.2831853;
                // Exact integrals of layered velocity waves make 60/240Hz agree.
                x[i]+=.16*(Math.cos(a*.7+p)-Math.cos(b*.7+p))/.7+o.wind*.28*(b-a);
                z[i]+=.12*(Math.sin(b*.53+p)-Math.sin(a*.53+p))/.53+o.wind*.09*(b-a);
                if(o.shape==ParticleOptions.Shape.Snow)y[i]-=(b-a)*(.2+size[i]*.12);
                else if(o.shape==ParticleOptions.Shape.Rain)y[i]-=(b-a)*(1.6+size[i]);
                else y[i]+=.09*(Math.cos(a*.65+p)-Math.cos(b*.65+p))/.65;
                if(o.rotate&&o.shape!=ParticleOptions.Shape.Rain){spin[i]+=(b-a)*(.12+phase[i]*.3);angle[i]=(float)spin[i];}
            }
            boolean outside=Math.abs(x[i]-px)>r||Math.abs(z[i]-pz)>r||Math.abs(y[i]-py)>vertical;
            if(outside&&active){spawn(i,o,px,py,pz,roof);continue;}
            if(time>=checkAt[i]){
                boolean clear=roof.open(x[i],y[i],z[i]);
                exposed[i]=clear&&(!o.outdoors||y[i]>roof.height((int)Math.floor(x[i]),(int)Math.floor(z[i]))+.2);
                checkAt[i]=time+.2;
                if(!exposed[i]&&active){spawn(i,o,px,py,pz,roof);continue;}
            }
            born[i]=Math.min(1,born[i]+(float)(dt/.3));
            alpha[i]=exposed[i]?born[i]*born[i]*(3-2*born[i]):0;
        }
    }
}
