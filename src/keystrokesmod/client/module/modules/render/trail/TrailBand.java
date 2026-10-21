package keystrokesmod.client.module.modules.render.trail;

/** Dedicated Solid Band proportions. Other styles never use these operations. */
public final class TrailBand {
    private TrailBand(){}
    private static float lerp(float[] a,int i,float t){return a[i-1]+(a[i]-a[i-1])*t;}
    public static void prepare(TrailCurve c,TrailOptions o,double now){
        if(c.count<2)return;
        double head=c.distance[0];
        for(int i=1;!o.matchLength&&i<c.count;i++)if(head-c.distance[i]>=o.bandLength){
            double stop=head-o.bandLength;
            float t=(float)((c.distance[i-1]-stop)/Math.max(1e-8,c.distance[i-1]-c.distance[i]));
            c.x[i]=lerp(c.x,i,t);c.y[i]=lerp(c.y,i,t);c.z[i]=lerp(c.z,i,t);
            c.topX[i]=lerp(c.topX,i,t);c.topY[i]=lerp(c.topY,i,t);c.topZ[i]=lerp(c.topZ,i,t);
            c.hemX[i]=lerp(c.hemX,i,t);c.hemY[i]=lerp(c.hemY,i,t);c.hemZ[i]=lerp(c.hemZ,i,t);
            c.age[i]=lerp(c.age,i,t);c.distance[i]=stop;c.count=i+1;break;
        }
        double span=Math.max(.001,head-c.distance[c.count-1]);
        for(int i=0;i<c.count;i++){
            double along=head-c.distance[i],u=along/span;
            // A short elliptical nose, parallel body, then a long smoothly closed tail.
            double nose=Math.min(1,along/Math.min(.10,span*.2));
            double round=Math.sqrt(Math.max(0,1-(1-nose)*(1-nose)));
            double start=1-(.18+.62*o.tailTaper),tail=Math.max(0,Math.min(1,(u-start)/(1-start)));
            c.width[i]=(float)(round*Math.sqrt(Math.max(0,1-tail*tail)));
            c.color[i]=o.tint((float)u,now,along);
            c.alpha[i]*=1-o.shimmer*.5F+o.shimmer*.5F*(float)Math.sin(now*2.5-along*3);
        }
        c.width[0]=c.width[c.count-1]=0;c.alpha[c.count-1]=0;
    }
}
