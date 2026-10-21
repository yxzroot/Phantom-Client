package keystrokesmod.client.module.modules.render.worldparticles;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

/** Original vector-drawn sprites, rasterized once with 2x antialiasing. No borrowed assets. */
public final class ParticleAssets {
    public static final int CELL=128,CELLS=5;
    private static void polygon(Graphics2D g,int gray,double... p){
        Path2D.Double path=new Path2D.Double();path.moveTo(p[0],p[1]);
        for(int i=2;i<p.length;i+=2)path.lineTo(p[i],p[i+1]);path.closePath();
        g.setColor(new Color(gray,gray,gray));g.fill(path);
    }
    public static ByteBuffer pixels(){
        BufferedImage image=new BufferedImage(CELL*CELLS,CELL,BufferedImage.TYPE_INT_ARGB);
        for(int style=0;style<CELLS;style++){
            BufferedImage hi=new BufferedImage(CELL*2,CELL*2,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=hi.createGraphics();g.scale(2,2);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            if(style==0){
                Path2D.Double p=new Path2D.Double();p.moveTo(64,10);
                p.curveTo(70,48,76,55,114,64);p.curveTo(76,73,70,80,64,118);
                p.curveTo(58,80,52,73,14,64);p.curveTo(52,55,58,48,64,10);g.fill(p);
                g.setColor(new Color(255,255,255,145));g.setStroke(new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.drawLine(35,35,93,93);g.drawLine(93,35,35,93);
            }else if(style==1){
                g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                for(int arm=0;arm<6;arm++){
                    double a=arm*Math.PI/3;g.rotate(a,64,64);g.draw(new Line2D.Double(64,64,64,15));
                    g.drawLine(64,34,52,25);g.drawLine(64,34,76,25);
                    g.drawLine(64,48,54,39);g.drawLine(64,48,74,39);g.rotate(-a,64,64);
                }
            }else if(style==2){
                Path2D.Double p=new Path2D.Double();p.moveTo(64,8);
                p.curveTo(60,39,43,74,43,91);p.curveTo(43,121,85,121,85,91);
                p.curveTo(85,74,68,39,64,8);g.fill(p);
            }else if(style==3){
                // Ethereum silhouette, faceted rather than an opaque rectangular icon.
                polygon(g,255,64,8,31,65,64,49);
                polygon(g,185,64,8,64,49,97,65);
                polygon(g,210,31,65,64,49,64,83);
                polygon(g,150,64,49,97,65,64,83);
                polygon(g,245,31,72,64,91,64,120);
                polygon(g,180,64,91,97,72,64,120);
            }else{
                g.setPaint(new RadialGradientPaint(64,64,58,new float[]{0,.2F,.55F,1},
                        new Color[]{new Color(255,255,255,150),new Color(255,255,255,120),new Color(255,255,255,25),new Color(255,255,255,0)}));
                g.fillOval(6,6,116,116);
            }
            g.dispose();Graphics2D out=image.createGraphics();out.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            out.drawImage(hi,style*CELL,0,CELL,CELL,null);out.dispose();
        }
        ByteBuffer bytes=BufferUtils.createByteBuffer(CELL*CELL*CELLS*4);
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL*CELLS;x++){
            int p=image.getRGB(x,y);bytes.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p).put((byte)(p>>>24));
        }
        bytes.flip();return bytes;
    }
}
