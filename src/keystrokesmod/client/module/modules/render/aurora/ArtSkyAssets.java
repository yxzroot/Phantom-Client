package keystrokesmod.client.module.modules.render.aurora;

/** All art is original code-generated data. Session-cached CPU assets, never regenerated per frame. */
public final class ArtSkyAssets {
    public static final int STAR_CAP=2000;
    private static boolean warming;
    private ArtSkyAssets(){}
    /** Start CPU art at module registration, not while a player is moving. GL upload stays on render. */
    public static synchronized void prewarm(){if(warming)return;warming=true;Thread worker=new Thread(()->{try{noise(false);noise(true);starMesh();skylinePixels();petalPixels();treePixels();}catch(RuntimeException|LinkageError failure){java.util.logging.Logger.getLogger("Phantom/SkyChanger").log(java.util.logging.Level.WARNING,"Procedural sky art unavailable; vanilla fallback remains available",failure);}},"Phantom-sky-art");worker.setDaemon(true);worker.start();}
    private static float hash(int x,int y){int n=x*374761393+y*668265263;n=(n^(n>>>13))*1274126177;return((n^(n>>>16))&0xFFFFFF)/16777216F;}
    public static float noise(double u,double v,int nx,int ny){
        double x=u*nx,y=v*ny;int ix=(int)Math.floor(x),iy=(int)Math.floor(y);float a=AuroraSimulation.smooth((float)(x-ix)),b=AuroraSimulation.smooth((float)(y-iy));
        int x0=(ix%nx+nx)%nx,y0=(iy%ny+ny)%ny,x1=(x0+1)%nx,y1=(y0+1)%ny;
        return(hash(x0,y0)*(1-a)+hash(x1,y0)*a)*(1-b)+(hash(x0,y1)*(1-a)+hash(x1,y1)*a)*b;
    }
    public static float field(double u,double v,boolean streak){
        double wu=u+.13*Math.sin(v*Math.PI*2)+.14*(noise(u,v,4,4)-.5),wv=v+.10*Math.sin(u*Math.PI*2)+.16*(noise(u+.2,v,4,4)-.5);
        float sum=0,total=0,w=1;for(int i=0;i<5;i++){sum+=noise(wu,wv,(streak?2:4)<<i,(streak?12:4)<<i)*w;total+=w;w*=.53F;}
        return AuroraSimulation.smooth((sum/total-.18F)/.64F);
    }
    private static int[] noisePixels(int size){int[] out=new int[size*size];for(int y=0;y<size;y++)for(int x=0;x<size;x++){double u=x/(double)size,v=y/(double)size;int r=Math.round(field(u,v,false)*255),g=Math.round(field(u+.37,v+.13,true)*255),b=Math.round(noise(u,v,32,32)*255);out[y*size+x]=0xFF000000|r<<16|g<<8|b;}return out;}
    private static int[] sprite(int size,int kind){
        int[] out=new int[size*size];for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            double px=(x+.5)/size*2-1,py=(y+.5)/size*2-1,r=Math.sqrt(px*px+py*py);float alpha;int rgb;
            if(kind==0){alpha=(float)Math.max(0,(Math.exp(-r*r*9)-Math.exp(-9))/.9999);rgb=0xFFFFFF;}
            else if(kind==1){
                double angle=.36,rx=px*Math.cos(angle)+py*Math.sin(angle),ry=-px*Math.sin(angle)+py*Math.cos(angle),ring=Math.sqrt(rx*rx+ry*ry/.0784);
                double ra=AuroraSimulation.smooth((float)((ring-.56)/.025))*AuroraSimulation.smooth((float)((.98-ring)/.025));if(ring>.76&&ring<.80)ra=0;
                int ringColor=mix(0xCFB5D8,0x607EAF,(float)(ring-.56)/.42F);ra*=.52+.25*Math.sin(ring*170)*Math.sin(ring*170);if(rx<-.2&&ry>.01&&Math.abs(ry)<.13)ra*=.22;
                float sphere=AuroraSimulation.smooth((float)((.435-r)/.012));double z=Math.sqrt(Math.max(0,1-r*r/(.43*.43))),light=Math.max(.08,(-px*.52+py*.66)/.43+z*.6);
                int body=mix(0x607DAB,0xDAB3D2,(float)(.5+.23*Math.sin(py*85+Math.sin(px*5))));body=scale(body,(float)(.25+.65*Math.min(1,light)));
                float ringAlpha=(float)ra;if(ry<0){alpha=ringAlpha+sphere*(1-ringAlpha);rgb=alpha>0?mix(body,ringColor,ringAlpha/Math.max(.001F,alpha)):0;}
                else{alpha=sphere+ringAlpha*(1-sphere);rgb=mix(ringColor,body,sphere);}
            }else{float z=(float)Math.sqrt(Math.max(0,1-r*r));alpha=AuroraSimulation.smooth((float)((.86-r)/.025));rgb=scale(mix(0xA7BDD8,0xF2E8E7,(float)(.5+.12*Math.sin(px*16)*Math.cos(py*12))),.35F+.6F*z);}
            out[y*size+x]=Math.round(AuroraSimulation.clamp(alpha,0,1)*255)<<24|rgb;
        }return out;
    }
    static int mix(int a,int b,float t){return ArtSkyPalette.mix(a,b,t);}static int scale(int rgb,float v){return Math.min(255,Math.round((rgb>>16&255)*v))<<16|Math.min(255,Math.round((rgb>>8&255)*v))<<8|Math.min(255,Math.round((rgb&255)*v));}
    private static int[] skyline(){int[] out=new int[512*512];for(int y=0;y<512;y++)for(int x=0;x<512;x++){double u=x/512D,v=y/511D;float height=.12F+.17F*noise(u,0,16,1)+.19F*noise(u,0,43,1)+.10F*noise(u,0,89,1);float a=AuroraSimulation.smooth((height-(float)v)*170);int c=mix(0x160C2D,0xC25CBB,(float)Math.exp(-Math.abs(height-v)*230));out[y*512+x]=Math.round(a*255)<<24|c;}return out;}
    private static final class Cache {static final int[] HIGH=noisePixels(512),LOW=noisePixels(256),STAR=sprite(64,0),PLANET=sprite(384,1),MOON=sprite(128,2);static final float[] DOME=dome(),STARS=stars();}
    private static final class SunsetCache {static final int[] SKYLINE=skyline();}
    public static int[] skylinePixels(){return SunsetCache.SKYLINE;}
    private static int[] petal(){int[] out=new int[64*64];for(int y=0;y<64;y++)for(int x=0;x<64;x++){double px=(x+.5)/32-1,py=(y+.5)/32-1;double width=.66*Math.sqrt(Math.max(0,1-Math.pow((py+.05)/.92,2)))*(.8+.2*py);double edge=width-Math.abs(px);float a=AuroraSimulation.smooth((float)(edge/.07));double notch=Math.exp(-px*px*120)*.24;if(py>.57-notch)a*=AuroraSimulation.smooth((float)((.84-notch-py)/.05));a*=AuroraSimulation.smooth((float)((py+.95)/.06));int color=mix(0xFFF8FB,0xF29AC5,(float)((py+1)/2));out[y*64+x]=Math.round(a*255)<<24|color;}return out;}
    private static final class CherryCache {static final int[] PETAL=petal();}
    public static int[] petalPixels(){return CherryCache.PETAL;}public static int[] treePixels(){return CherryTreeArt.pixels(false);}
    public static int[] noise(boolean low){return low?Cache.LOW:Cache.HIGH;}public static int[] star(){return Cache.STAR;}public static int[] planet(){return Cache.PLANET;}public static int[] moon(){return Cache.MOON;}
    public static float[] domeMesh(){return Cache.DOME;}public static float[] starMesh(){return Cache.STARS;}
    private static float[] dome(){float[] out=new float[96*40*4*3];int p=0;for(int y=0;y<40;y++)for(int x=0;x<96;x++)for(int c=0;c<4;c++){double az=(x+(c==1||c==2?1:0))*Math.PI*2/96,el=-Math.PI/2+(y+(c>=2?1:0))*Math.PI/40;out[p++]=(float)(Math.cos(az)*Math.cos(el)*46);out[p++]=(float)(Math.sin(el)*46);out[p++]=(float)(Math.sin(az)*Math.cos(el)*46);}return out;}
    private static float[] stars(){
        java.util.Random random=new java.util.Random(0xC05A1C);float[] out=new float[STAR_CAP*4*9];int p=0;
        for(int i=0;i<STAR_CAP;i++){
            double az=random.nextDouble()*Math.PI*2,up=.02+random.nextDouble()*.98;
            if(i%3==0){az=random.nextDouble()*Math.PI*2;up=Math.min(.98,Math.max(.03,.46+.30*Math.sin(az+.6)+(random.nextDouble()-.5)*.12));}
            double horizontal=Math.sqrt(1-up*up),nx=Math.cos(az)*horizontal,nz=Math.sin(az)*horizontal;
            float rank=random.nextFloat(),size=rank>.986?.13F:rank>.82?.050F:.017F+random.nextFloat()*.016F,phase=random.nextFloat();int color=rank>.95?0xFFE4BB:i%4==0?0xB7D1FF:0xE7EDFF;
            for(int c=0;c<4;c++){float a=c==0||c==3?-size:size,b=c<2?-size:size;out[p++]=(float)(nx*45-Math.sin(az)*a-nx*up/horizontal*b);out[p++]=(float)(up*45+horizontal*b);out[p++]=(float)(nz*45+Math.cos(az)*a-nz*up/horizontal*b);out[p++]=(color>>16&255)/255F;out[p++]=(color>>8&255)/255F;out[p++]=(color&255)/255F;out[p++]=phase;out[p++]=c==0||c==3?0:1;out[p++]=c<2?0:1;}
        }return out;
    }
}
