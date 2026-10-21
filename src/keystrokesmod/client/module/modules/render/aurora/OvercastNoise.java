package keystrokesmod.client.module.modules.render.aurora;

/** Seamless, directionally stretched and domain-warped noise; generated once per renderer. */
public final class OvercastNoise {
    public static final int W=512,H=256;
    private static float hash(int x,int y){int v=x*374761393+y*668265263;v=(v^(v>>>13))*1274126177;return((v^(v>>>16))&0xFFFFFF)/16777216F;}
    private static float noise(double u,double v,int px,int py){
        double x=u*px,y=v*py;int ix=(int)Math.floor(x),iy=(int)Math.floor(y);float fx=(float)(x-ix),fy=(float)(y-iy);fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);
        int a=(ix%px+px)%px,b=(iy%py+py)%py,c=(a+1)%px,d=(b+1)%py;
        float lo=hash(a,b)*(1-fx)+hash(c,b)*fx,hi=hash(a,d)*(1-fx)+hash(c,d)*fx;return lo*(1-fy)+hi*fy;
    }
    public static float value(double u,double v){
        // Periodic warp in BOTH axes keeps the tile seamless while bending long bands
        // into broad curls. Large octaves dominate; fine noise never becomes speckle.
        double tau=Math.PI*2;
        double wu=u+.19*Math.sin(v*tau)+.12*(noise(u,v,3,3)-.5);
        double wv=v+.13*Math.sin(u*tau)+.055*Math.sin(u*tau*2+Math.sin(v*tau))+.12*(noise(u+.3,v,3,3)-.5);
        float sum=0,weight=1,total=0;
        for(int octave=0;octave<5;octave++){sum+=noise(wu,wv,2<<octave,8<<octave)*weight;total+=weight;weight*=.55F;}
        float value=(sum/total-.12F)/.76F;
        value=Math.max(0,Math.min(1,value));return value*value*(3-2*value);
    }
    public static int[] pixels(){int[] out=new int[W*H];for(int y=0;y<H;y++)for(int x=0;x<W;x++){int gray=Math.round(value(x/(double)W,y/(double)H)*255);out[y*W+x]=0xFF000000|gray<<16|gray<<8|gray;}return out;}
    public static float horizon(AuroraOptions opt,int channel){
        int shift=(2-channel)*8;float a=(opt.duskColor>>shift&255)/255F,b=(opt.duskSecond>>shift&255)/255F;
        float base=channel==0?.055F:channel==1?.071F:.105F;
        return(base+a*.10F+b*.13F+AuroraSimulation.clamp(opt.duskGlow,0,2)*(.018F+.035F*(a+b)))*(1-AuroraSimulation.clamp(opt.duskDarkness,0,1)*.40F);
    }
}
