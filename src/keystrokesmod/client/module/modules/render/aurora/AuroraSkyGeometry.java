package keystrokesmod.client.module.modules.render.aurora;

/** Original sky geometry/texture generated once; no world access and no frame allocations. */
public final class AuroraSkyGeometry {
    public static final int COLUMNS=160, BANDS=12, MAX_CURTAINS=6, MILKY_W=1024, MILKY_H=128;
    private static final double[] ANGLES={-2.8,-1.2,.35,1.9,2.9,4.2};
    private AuroraSkyGeometry(){}
    public static float[] curtain(int index){
        float[] data=new float[COLUMNS*BANDS*4*7];int at=0;
        double angle=ANGLES[index],distance=23+index*2.5,width=12+(index%3)*2;
        for(int column=0;column<COLUMNS;column++)for(int band=0;band<BANDS;band++)for(int corner=0;corner<4;corner++){
            float u=(column+(corner==1||corner==2?1:0))/(float)COLUMNS;
            float v=(band+(corner>=2?1:0))/(float)BANDS;
            // Non-circular path. Different column depths and wavy lower edges break the cylinder/ring.
            double along=(u-.5)*width*2+Math.sin(u*9+index)*1.4;
            double depth=distance+Math.sin(u*6+index*1.7)*3.8+Math.sin(u*17-index)*1.1;
            data[at++]=(float)(along*Math.cos(angle)-depth*Math.sin(angle));
            data[at++]=(float)(3+index*.65+Math.sin(u*5+index)*1.8+Math.sin(u*14+index)*.45+v*(14+(index%3)*4));
            data[at++]=(float)(-depth*Math.cos(angle)-along*Math.sin(angle));
            data[at++]=u;data[at++]=v;data[at++]=0;
            data[at++]=AuroraSimulation.smooth(u*7)*AuroraSimulation.smooth((1-u)*7)*(index%3==2?.58F:1);
        }
        return data;
    }
    /** Nine floats per vertex: xyz, rgba, uv. Tiny/medium/rare bright star sprites. */
    public static float[] stars(){
        java.util.Random random=new java.util.Random(0xA1707A);float[] data=new float[AuroraSimulation.MAX_STARS*4*9];int at=0;
        for(int i=0;i<AuroraSimulation.MAX_STARS;i++){
            double az=random.nextDouble()*Math.PI*2,up=.004+random.nextDouble()*.996,radial=Math.sqrt(1-up*up);
            float layer=random.nextFloat(),s=layer<.80?.017F+random.nextFloat()*.018F:layer<.995?.035F+random.nextFloat()*.03F:.16F;
            float warm=random.nextFloat(),alpha=layer<.80?.3F+random.nextFloat()*.35F:layer<.995?.6F+random.nextFloat()*.35F:1;
            for(int corner=0;corner<4;corner++){
                float r=corner==0||corner==3?-s:s,v=corner<2?-s:s;
                data[at++]=(float)(Math.sin(az)*radial*45+Math.cos(az)*r-Math.sin(az)*up*v);
                data[at++]=(float)(up*45+radial*v);
                data[at++]=(float)(Math.cos(az)*radial*45-Math.sin(az)*r-Math.cos(az)*up*v);
                data[at++]=.76F+warm*.24F;data[at++]=.84F+warm*.13F;data[at++]=1-warm*.20F;data[at++]=alpha;
                data[at++]=corner==0||corner==3?0:1;data[at++]=corner<2?0:1;
            }
        }
        return data;
    }
    /** A tilted great-circle band, used for cached Milky Way and procedural high wisps. */
    public static float[] band(boolean wisps){
        int columns=96,rows=8;float[] data=new float[columns*rows*4*9];int at=0;
        double tilt=wisps?.35:.72;
        for(int column=0;column<columns;column++)for(int row=0;row<rows;row++)for(int corner=0;corner<4;corner++){
            float u=(column+(corner==1||corner==2?1:0))/(float)columns;
            float v=(row+(corner>=2?1:0))/(float)rows;
            double az=u*Math.PI*2,el=(v-.5)*(wisps?.56:.72)+(wisps?.55:.36);
            double x=Math.sin(az)*Math.cos(el)*44,y=Math.sin(el)*44,z=Math.cos(az)*Math.cos(el)*44;
            data[at++]=(float)(x*Math.cos(tilt)-y*Math.sin(tilt));
            data[at++]=(float)(x*Math.sin(tilt)+y*Math.cos(tilt));data[at++]=(float)z;
            data[at++]=1;data[at++]=1;data[at++]=1;data[at++]=1;data[at++]=u;data[at++]=v;
        }
        return data;
    }
    public static float[] moon(){
        float[] data=new float[4*9];int at=0;double az=-.7,up=.6,radial=.8;
        for(int corner=0;corner<4;corner++){
            double r=(corner==0||corner==3?-1:1)*4.8,v=(corner<2?-1:1)*4.8;
            data[at++]=(float)(Math.sin(az)*radial*43+Math.cos(az)*r-Math.sin(az)*up*v);
            data[at++]=(float)(up*43+radial*v);
            data[at++]=(float)(-Math.cos(az)*radial*43+Math.sin(az)*r+Math.cos(az)*up*v);
            data[at++]=1;data[at++]=1;data[at++]=1;data[at++]=1;data[at++]=corner==0||corner==3?0:1;data[at++]=corner<2?0:1;
        }
        return data;
    }
    private static float hash(int x,int y){int n=x*374761393+y*668265263;n=(n^(n>>>13))*1274126177;return (n&0x7fffffff)/2147483648F;}
    private static float noise(float x,float y){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);float fx=AuroraSimulation.smooth(x-ix),fy=AuroraSimulation.smooth(y-iy);
        float a=hash(ix,iy),b=hash(ix+1,iy),c=hash(ix,iy+1),d=hash(ix+1,iy+1);
        return (a+(b-a)*fx)*(1-fy)+(c+(d-c)*fx)*fy;
    }
    public static int[] milkyPixels(){
        int[] pixels=new int[MILKY_W*MILKY_H];
        for(int y=0;y<MILKY_H;y++)for(int x=0;x<MILKY_W;x++){
            float u=x/(float)MILKY_W,v=y/(float)MILKY_H;
            float lane=(v-.5F)+(float)Math.sin(u*19)*.045F;
            float cloud=noise(u*30,v*8)*.55F+noise(u*65,v*19)*.3F+noise(u*160,v*40)*.15F;
            float body=(float)Math.exp(-lane*lane*18)*( .35F+cloud*.65F);
            body*=1-.65F*(float)Math.exp(-Math.pow((lane+.03)*24,2));
            float star=hash(x,y)>.996?hash(x+31,y)*.8F*body:0;
            int r=Math.min(255,(int)(body*25+star*200)),g=Math.min(255,(int)(body*31+star*210)),b=Math.min(255,(int)(body*62+star*240));
            pixels[y*MILKY_W+x]=0xFF000000|r<<16|g<<8|b;
        }
        return pixels;
    }
}
