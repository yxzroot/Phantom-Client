package keystrokesmod.client.module.modules.render.aurora;

/** Fixed, recycled world-space petals. Roof samples are served by the controller's bounded cache. */
public final class PetalSimulation {
    public static final int CAP=250;
    public final double[] x=new double[CAP],y=new double[CAP],z=new double[CAP];
    public final float[] size=new float[CAP],alpha=new float[CAP],rotation=new float[CAP];
    private final float[] fall=new float[CAP],seed=new float[CAP];
    private boolean initialized;private int random=0x5A4B715;private double cx,cy,cz;
    public float fade;public int count;private int budget=90;
    private float next(){random^=random<<13;random^=random>>>17;random^=random<<5;return(random&0x7FFFFFFF)/2147483648F;}
    public void reset(){initialized=false;fade=0;count=0;java.util.Arrays.fill(alpha,0);}
    private void spawn(int i,double px,double py,double pz,boolean top){x[i]=px+(next()-.5)*26;y[i]=py+(top?7+next()*4:(next()-.5)*20);z[i]=pz+(next()-.5)*26;alpha[i]=0;}
    public void advance(double seconds,double time,boolean active,ArtSkyOptions o,boolean low,double px,double py,double pz,AuroraSimulation.Roof roof){
        double dt=Double.isFinite(seconds)?Math.max(0,Math.min(.25,seconds)):0;
        if(initialized&&(Math.abs(px-cx)>20||Math.abs(py-cy)>20||Math.abs(pz-cz)>20))reset();cx=px;cy=py;cz=pz;
        if(!initialized){for(int i=0;i<CAP;i++){seed[i]=next()*100;size[i]=.035F+next()*.055F;fall[i]=.35F+next()*.6F;spawn(i,px,py,pz,false);}initialized=true;}
        boolean outside=roof.height((int)Math.floor(px),(int)Math.floor(pz))<=py;
        float target=active&&outside&&o.petals>0?o.cherryMix:0;fade+=(target-fade)*(float)(1-Math.exp(-dt*12));
        if(o.petals>0)budget=o.petals==1?90:CAP;count=low?(budget+1)/2:budget;
        double gust=.5+.5*Math.sin(time*.18)*Math.sin(time*.07),wind=o.wind*(.28+gust*.7);
        for(int i=0;i<CAP;i++){
            x[i]+=dt*(wind+Math.sin(time*.8+seed[i])*.22);z[i]+=dt*(wind*.23+Math.cos(time*.6+seed[i])*.19);y[i]-=dt*fall[i];rotation[i]=(float)(time*(.3+fall[i])+seed[i]);
            if(y[i]<py-9||Math.abs(x[i]-px)>15||Math.abs(z[i]-pz)>15)spawn(i,px,py,pz,true);
            boolean exposed=outside&&y[i]>=roof.height((int)Math.floor(x[i]),(int)Math.floor(z[i]));float visible=exposed&&i<count?1:0;alpha[i]+=(visible-alpha[i])*(float)(1-Math.exp(-dt*12));
        }
    }
}
