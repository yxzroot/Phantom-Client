package keystrokesmod.client.module.modules.render.island;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Island-only Unicode pages. Poppins where supported, same-size system fallback otherwise.
 * No Minecraft bitmap fallback, no per-glyph draw calls or per-frame rasterization. */
public final class IslandFont {
    private static final int RASTER=4,PAD=8;
    private static Font poppins;
    private final Font primary,fallback;
    private final LinkedHashMap<Integer,Page> pages=new LinkedHashMap<Integer,Page>(8,.75F,true);
    private static final class Page {int texture,side,line;final float[] advance=new float[256];final int[] x=new int[256],y=new int[256],w=new int[256];}
    public IslandFont(float size,int style){
        if(poppins==null)try(InputStream in=IslandFont.class.getResourceAsStream("/assets/raindots/fonts/Poppins.ttf")){if(in!=null)poppins=Font.createFont(Font.TRUETYPE_FONT,in);}catch(Exception ignored){}
        primary=(poppins==null?new Font(Font.SANS_SERIF,style,12):poppins).deriveFont(style,size*RASTER);fallback=new Font(Font.SANS_SERIF,style,12).deriveFont(size*RASTER);
    }
    private Font face(int cp){return primary.canDisplay(cp)?primary:fallback;}
    public boolean supports(int cp){return primary.canDisplay(cp)||fallback.canDisplay(cp);}
    private static void hints(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);}
    private Page page(int cp){int key=cp>>>8;Page p=pages.get(key);if(p!=null)return p;p=new Page();BufferedImage probe=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);Graphics2D g=probe.createGraphics();hints(g);int ascent=0,line=0;
        for(int i=0;i<256;i++){int c=key*256+i;Font font=face(c);g.setFont(font);FontMetrics m=g.getFontMetrics();ascent=Math.max(ascent,m.getAscent());line=Math.max(line,m.getHeight());String glyph=new String(Character.toChars(c));p.advance[i]=(float)font.getStringBounds(glyph,g.getFontRenderContext()).getWidth()/RASTER;p.w[i]=Math.max(1,(int)Math.ceil(p.advance[i]*RASTER))+PAD*2;}
        p.line=line+PAD*2;p.side=512;while(true){int x=0,y=0;for(int i=0;i<256;i++){if(x+p.w[i]>p.side){x=0;y+=p.line;}p.x[i]=x;p.y[i]=y;x+=p.w[i];}if(y+p.line<=p.side)break;p.side*=2;if(p.side>4096){g.dispose();throw new IllegalStateException("Unicode atlas too large");}}
        BufferedImage image=new BufferedImage(p.side,p.side,BufferedImage.TYPE_INT_ARGB);Graphics2D draw=image.createGraphics();hints(draw);draw.setColor(Color.WHITE);for(int i=0;i<256;i++){int c=key*256+i;draw.setFont(face(c));draw.drawString(new String(Character.toChars(c)),p.x[i]+PAD,p.y[i]+PAD+ascent);}draw.dispose();g.dispose();
        int binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);try{p.texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,p.texture);GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);
            IntBuffer data=BufferUtils.createIntBuffer(p.side*p.side);data.put(image.getRGB(0,0,p.side,p.side,null,0,p.side)).flip();GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,p.side,p.side,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        }finally{GL11.glBindTexture(GL11.GL_TEXTURE_2D,binding);GL11.glPopClientAttrib();}
        pages.put(key,p);if(pages.size()>8){Map.Entry<Integer,Page> oldest=pages.entrySet().iterator().next();GL11.glDeleteTextures(oldest.getValue().texture);pages.remove(oldest.getKey());}return p;
    }
    public float width(String text){float result=0;if(text!=null)for(int i=0;i<text.length();){int cp=text.codePointAt(i);result+=page(cp).advance[cp&255];i+=Character.charCount(cp);}return result;}
    public void drawHud(String text,float x,float y,int color){draw(text,x,y,color,null);}
    public void draw(String text,float x,float y,int color,int[] colors){
        if(text==null||text.isEmpty())return;
        // Prepare before glBegin: a newly encountered page may need its one-time upload.
        for(int i=0;i<text.length();){int cp=text.codePointAt(i);page(cp);i+=Character.charCount(cp);}
        Page replacement=page(63);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_ALPHA_TEST);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDepthMask(false);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
        int bound=-1;boolean open=false;try{for(int i=0;i<text.length();){int cp=text.codePointAt(i);Page p=pages.get(cp>>>8);int c=cp&255;if(p==null){p=replacement;c=63;}if(p.texture!=bound){if(open)GL11.glEnd();GL11.glBindTexture(GL11.GL_TEXTURE_2D,p.texture);GL11.glBegin(GL11.GL_QUADS);open=true;bound=p.texture;}
            int argb=colors==null?color:colors[Math.min(i,colors.length-1)];GL11.glColor4f((argb>>16&255)/255F,(argb>>8&255)/255F,(argb&255)/255F,(argb>>>24)/255F);
            float x0=x-PAD/(float)RASTER,y0=y-PAD/(float)RASTER,x1=x0+p.w[c]/(float)RASTER,y1=y0+p.line/(float)RASTER,u=p.x[c]/(float)p.side,v=p.y[c]/(float)p.side,uu=(p.x[c]+p.w[c])/(float)p.side,vv=(p.y[c]+p.line)/(float)p.side;
            GL11.glTexCoord2f(u,v);GL11.glVertex2f(x0,y0);GL11.glTexCoord2f(uu,v);GL11.glVertex2f(x1,y0);GL11.glTexCoord2f(uu,vv);GL11.glVertex2f(x1,y1);GL11.glTexCoord2f(u,vv);GL11.glVertex2f(x0,y1);x+=p.advance[c];i+=Character.charCount(cp);
        }}finally{if(open)GL11.glEnd();}
    }
    public void dispose(){for(Page p:pages.values())GL11.glDeleteTextures(p.texture);pages.clear();}
}
