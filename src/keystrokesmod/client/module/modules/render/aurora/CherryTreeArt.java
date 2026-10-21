package keystrokesmod.client.module.modules.render.aurora;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Original seamless angular panorama. Shapes are drawn in DEGREES, not square texture UVs.
 * This keeps a five-degree tree five degrees tall and prevents flattened mushroom canopies. */
public final class CherryTreeArt {
    // Match texels per ANGULAR degree on both axes so mip selection cannot blur the
    // horizon horizontally just because its vertical texture coordinates are compressed.
    public static final int WIDTH=8192,HEIGHT=256,LOW_DIVISOR=4;
    public static final float MIN_ELEVATION=-.035F,SPAN=.175F;
    private CherryTreeArt(){}
    private static final class Cache {
        static final BufferedImage IMAGE=generate();
        static final int[] HIGH=pixels(IMAGE),LOW=downsample(IMAGE);
    }
    public static int[] pixels(boolean low){return low?Cache.LOW:Cache.HIGH;}
    private static int[] pixels(BufferedImage image){
        int w=image.getWidth(),h=image.getHeight();int[] out=new int[w*h];
        // OpenGL v=0 is the lowest elevation; Java2D row 0 is the highest.
        for(int y=0;y<h;y++)image.getRGB(0,h-1-y,w,1,out,y*w,w);return out;
    }
    private static int[] downsample(BufferedImage source){BufferedImage low=new BufferedImage(WIDTH/LOW_DIVISOR,HEIGHT/LOW_DIVISOR,BufferedImage.TYPE_INT_ARGB);Graphics2D g=low.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,low.getWidth(),low.getHeight(),null);g.dispose();return pixels(low);}
    private static BufferedImage generate(){
        BufferedImage image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
            g.translate(0,HEIGHT);g.scale(WIDTH/360D,-HEIGHT/Math.toDegrees(SPAN));g.translate(0,-Math.toDegrees(MIN_ELEVATION));
            // Two irregular populations: smaller misty trees behind taller branching silhouettes.
            for(int layer=0;layer<2;layer++){
                Random random=new Random(0x53414B555241L+layer*43);int count=layer==0?47:36;
                for(int i=0;i<count;i++){
                    double az=(i+.18+random.nextDouble()*.68)*360/count;
                    double height=(layer==0?1.8:3.1)+random.nextDouble()*(layer==0?1.7:2.5);
                    double width=height*(.68+random.nextDouble()*.30),lean=(random.nextDouble()-.5)*.5;
                    long seed=random.nextLong();
                    for(int wrap=-1;wrap<=1;wrap++)tree(g,az+wrap*360,height,width,lean,layer,seed);
                }
            }
        }finally{g.dispose();}return image;
    }
    private static void tree(Graphics2D g,double x,double height,double width,double lean,int layer,long seed){
        Random random=new Random(seed);double base=-1.3,split=height*.40;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,layer==0?.48F:.84F));
        Color bark=layer==0?new Color(112,87,113):new Color(65,48,72);g.setColor(bark);
        Path2D.Double trunk=new Path2D.Double();trunk.moveTo(x-width*.025,base);trunk.curveTo(x-width*.024,split*.1,x+lean*.3-width*.016,split*.6,x+lean,split+height*.17);trunk.lineTo(x+lean+width*.018,split*.82);trunk.curveTo(x+width*.02,split*.4,x+width*.03,base*.5,x+width*.035,base);trunk.closePath();g.fill(trunk);
        for(int branch=0;branch<9;branch++){
            double side=(branch-4)/4D,targetX=x+lean+side*width*.35,targetY=height*(.58+.18*random.nextDouble())-.14*Math.abs(side);
            Path2D.Double limb=new Path2D.Double();limb.moveTo(x+lean*.15,height*.13);limb.curveTo(x+side*width*.06,split*.6,x+side*width*.23,targetY-height*.27,targetX,targetY);
            g.setStroke(new BasicStroke((float)(width*(.010+.008*(1-Math.abs(side)))),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(limb);
            g.setStroke(new BasicStroke((float)(width*.006),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Double(targetX-side*width*.10,targetY-height*.14,targetX+side*width*.10,targetY+height*.10));
        }
        // Rounded asymmetric blossom clusters, not one ellipse stamped on a rectangular trunk.
        for(int clump=0;clump<12;clump++){
            double t=(clump%6)/5D,side=t*2-1,cx=x+lean+side*width*.31+(random.nextDouble()-.5)*width*.13;
            double cy=height*(.65+.18*(1-side*side))+(clump/6)*height*.12+(random.nextDouble()-.5)*height*.12;
            double rx=width*(.16+random.nextDouble()*.09),ry=height*(.15+random.nextDouble()*.09),phase=random.nextDouble()*6.28;
            Path2D.Double crown=new Path2D.Double();
            for(int n=0;n<=64;n++){double a=n*Math.PI*2/64,ruffle=1+.07*Math.sin(a*7+phase)+.045*Math.sin(a*13+phase*2);double px=cx+Math.cos(a)*rx*ruffle,py=cy+Math.sin(a)*ry*ruffle;if(n==0)crown.moveTo(px,py);else crown.lineTo(px,py);}crown.closePath();
            Color lower=layer==0?new Color(119,88,120):new Color(98,63,94),upper=layer==0?new Color(161,119,151):new Color(198,143,172);
            g.setPaint(new GradientPaint((float)cx,(float)(cy-ry),lower,(float)(cx-rx*.25),(float)(cy+ry),upper));g.fill(crown);
            // A few small blossom groups break the smooth crown surface; all cached at startup.
            g.setColor(new Color(233,175,195,layer==0?28:42));
            for(int blossom=0;blossom<13;blossom++){double a=random.nextDouble()*Math.PI*2,r=Math.sqrt(random.nextDouble())*.80,bx=cx+Math.cos(a)*rx*r,by=cy+Math.sin(a)*ry*r,s=height*(.010+random.nextDouble()*.008);g.fill(new Ellipse2D.Double(bx-s,by-s,s*2,s*2));}
        }
        g.setComposite(AlphaComposite.SrcOver);
    }
}
