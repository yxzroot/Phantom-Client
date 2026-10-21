package keystrokesmod.client.module.modules.render.aurora;

/** Original bounded connected graph. Each new node joins its two closest earlier nodes. */
public final class ConstellationGeometry {
    public static final int NODES=300,SEGMENTS=12,STRIDE=11;
    public final float[] points=new float[NODES*3],seed=new float[NODES];
    public final int[] from=new int[NODES*2-3],to=new int[NODES*2-3];
    public ConstellationGeometry(){
        java.util.Random random=new java.util.Random(0xC05A1C);
        int edge=0;
        for(int i=0;i<NODES;i++){
            double yaw=random.nextDouble()*Math.PI*2,up=.025+random.nextDouble()*.975,radial=Math.sqrt(1-up*up);
            points[i*3]=(float)(Math.cos(yaw)*radial);points[i*3+1]=(float)up;points[i*3+2]=(float)(Math.sin(yaw)*radial);seed[i]=random.nextFloat();
            int nearest=-1,second=-1;double best=100,next=100;
            for(int j=0;j<i;j++){
                double dx=points[i*3]-points[j*3],dy=points[i*3+1]-points[j*3+1],dz=points[i*3+2]-points[j*3+2],d=dx*dx+dy*dy+dz*dz;
                if(d<best){second=nearest;next=best;nearest=j;best=d;}else if(d<next){second=j;next=d;}
            }
            if(nearest>=0){from[edge]=i;to[edge++]=nearest;}
            if(second>=0){from[edge]=i;to[edge++]=second;}
        }
    }
    public static int edgeCount(int nodes){return nodes<2?0:nodes*2-3;}
    private int vertex(float[] out,int at,int a,int b,float t,float right,float up){
        for(int i=0;i<3;i++)out[at++]=points[a*3+i];
        for(int i=0;i<3;i++)out[at++]=points[b*3+i]*.5F+.5F;
        out[at++]=seed[b];out[at++]=t;out[at++]=seed[a];out[at++]=right;out[at++]=up;return at;
    }
    public float[] lines(){
        float[] out=new float[from.length*SEGMENTS*2*STRIDE];int at=0;
        for(int edge=0;edge<from.length;edge++)for(int j=0;j<SEGMENTS;j++)for(int side=0;side<2;side++)
            at=vertex(out,at,from[edge],to[edge],(j+side)/(float)SEGMENTS,0,0);
        return out;
    }
    public float[] nodes(){
        float[] out=new float[NODES*4*STRIDE];int at=0;
        for(int i=0;i<NODES;i++)for(int j=0;j<4;j++)at=vertex(out,at,i,i,0,j==0||j==3?-1:1,j<2?-1:1);
        return out;
    }
}
