package keystrokesmod.client.module.modules.render.island;

import java.awt.Font;import java.nio.IntBuffer;import java.util.*;
import keystrokesmod.client.module.modules.render.airvisuals.*;
import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;import org.lwjgl.opengl.*;

/** One retained renderer for every event/provider. Private framebuffer provides rounded clipping. */
public final class IslandRenderer {
    private final AuroraGLState state=new AuroraGLState();private final IslandCanvas canvas=new IslandCanvas();
    private final IslandFont title=new IslandFont(10,Font.BOLD),body=new IslandFont(9,Font.PLAIN);
    private final IslandSpring width=new IslandSpring(120),height=new IslandSpring(28),radius=new IslandSpring(14),health=new IslandSpring(1),toggle=new IslandSpring(0);
    private final PlaybackClock playback=new PlaybackClock();private final LyricMotion lyricMotion=new LyricMotion();private double framePosition;
    private final Map<String,Float> widths=new LinkedHashMap<String,Float>(128,.75F,true){protected boolean removeEldestEntry(Map.Entry<String,Float> e){return size()>256;}};
    private final IntBuffer artPixels=BufferUtils.createIntBuffer(4096);
    private int artwork;private int[] uploaded;private long previous,changed;
    private View current,old;private LyricTimeline timeline=LyricTimeline.EMPTY;private Layout[] layouts=new Layout[0];
    private long displayedSecond=-1;private String timeLabel="";
    private int compactLine=-1,compactFirst,compactLast,compactOldLine=-1,compactOldFirst,compactOldLast;private long compactChanged,compactTransport=-1;
    private static final class View {
        final IslandEvent event;final MediaSnapshot media;final boolean music,lyrics,compact;final String key;
        View(IslandEvent event,MediaSnapshot media,boolean lyrics,boolean compact){this.event=event;this.media=media;this.compact=compact;music=event==null&&media.present();this.lyrics=music&&lyrics;key=event!=null?event.key:music?media.song():"idle";}
    }
    private static final class Layout {final String text;final float[] advances;final int[] colors;final float width;Layout[][] sung;Layout(String text,IslandFont font){this.text=text;colors=new int[text.length()];advances=new float[text.length()+1];for(int i=0;i<text.length();){int count=Character.charCount(text.codePointAt(i));float w=font.width(text.substring(i,i+count));for(int j=1;j<=count;j++)advances[i+j]=advances[i]+w*j/count;i+=count;}width=advances[text.length()];}}
    private float textWidth(String s){Float w=widths.get(s);if(w==null){w=title.width(s);widths.put(s,w);}return w;}
    private static float smooth(double t){t=Math.max(0,Math.min(1,t));return(float)(t*t*(3-2*t));}
    private static int alpha(int color,float opacity){return AirTheme.alpha(color,opacity);}
    public static float marquee(double seconds,float overflow,float speed){if(overflow<=0)return 0;double travel=overflow/Math.max(1,speed),cycle=travel*2+3,t=seconds%cycle;if(t<1.5)return 0;t-=1.5;if(t<travel)return(float)(t*speed);t-=travel;if(t<1.5)return overflow;return(float)Math.max(0,overflow-(t-1.5)*speed);}
    public void draw(Minecraft mc,ScaledResolution screen,IslandOptions o,IslandEvent event,MediaSnapshot media,LyricsManager.Result lyrics,String idle,double healthValue,boolean showMusic){
        state.begin();try{drawScoped(mc,screen,o,event,media,lyrics,idle,healthValue,showMusic);}finally{state.end();GL11.glColor4f(1,1,1,1);net.minecraft.client.renderer.GlStateManager.color(1,1,1,1);}
    }
    private void drawScoped(Minecraft mc,ScaledResolution screen,IslandOptions o,IslandEvent event,MediaSnapshot media,LyricsManager.Result lyrics,String idle,double healthValue,boolean showMusic){
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glMatrixMode(GL11.GL_MODELVIEW);
        long now=System.nanoTime();double dt=previous==0?0:Math.max(0,(now-previous)/1e9);previous=now;
        MediaSnapshot visible=showMusic?media:MediaSnapshot.EMPTY;boolean lyricView=event==null&&visible.present()&&o.lyrics&&lyrics.song.equals(media.song())&&lyrics.timeline.size()>0;
        String key=event!=null?event.key:visible.present()?visible.song():"idle";
        if(current==null||!current.key.equals(key)||current.event!=event||current.compact!=o.lyricsOnly||current.lyrics!=lyricView&&visible.present()){
            old=current;current=new View(event,visible,lyricView,o.lyricsOnly);changed=now;
        }
        framePosition=playback.update(media,now);
        if(timeline!=lyrics.timeline){timeline=lyrics.timeline;layouts=new Layout[timeline.size()];lyricMotion.reset();compactLine=compactOldLine=-1;}
        if(lyrics.song.equals(media.song()))lyricMotion.update(timeline,framePosition+lyrics.offset,playback.discontinuity,dt,o.lyricsDuration);
        float targetW=120,targetH=28;
        if(current.event!=null){targetW=Math.max(190,textWidth(current.event.title)*o.textSize+88);targetH=current.event.kind==IslandEvent.Kind.Toggle?38:48;}
        else if(current.music&&!current.compact){targetW=Math.max(250,Math.min(o.maxWidth,textWidth(media.title)*o.textSize+95));targetH=current.lyrics?(o.lines==3?130:90):54;}
        float available=Math.max(100,screen.getScaledWidth()/o.scale-16);targetW=Math.min(Math.min(o.maxWidth,available),targetW);
        float w=(float)width.update(targetW,dt,o.speed,o.overshoot),h=(float)height.update(targetH,dt,o.speed,o.overshoot),r=(float)radius.update(targetH<=40?targetH/2:22,dt,o.speed,o.overshoot);
        w=Math.max(40,Math.min(available,w));h=Math.max(20,Math.min(160,h));float x=(screen.getScaledWidth()-w*o.scale)/2,y=Math.min(o.y,Math.max(0,screen.getScaledHeight()-h*o.scale));float density=screen.getScaleFactor()*o.scale;
        float transition=smooth((now-changed)/(.22e9/Math.max(.2,o.speed)));
        int accent=o.matchArt&&media.artwork!=null?media.accent:o.accent;
        boolean contentOpen=false;
        try{
            GL20.glUseProgram(0);GL11.glDisable(GL11.GL_STENCIL_TEST);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glColorMask(true,true,true,true);
            GL11.glTranslatef(x,y,0);GL11.glScalef(o.scale,o.scale,1);
            if(o.shadow)AirDraw.card(-1,0,w+2,h+2,r,0x00000000,1);
            if(o.glow)for(int i=5;i>0;i--)AirDraw.card(-i,-i,w+i*2,h+i*2,r+i,alpha(accent,.012F*(1-.1F*i)),0);
            canvas.begin(600,180,density);contentOpen=true;
            if(!o.art&&uploaded!=null)upload(null);else if(o.art&&media.artwork!=uploaded)upload(media.artwork);
            if(old!=null&&transition<1)content(old,mc,o,w,h,1-transition,now,accent,lyrics,idle,healthValue,density,media,dt);
            content(current,mc,o,w,h,transition,now,accent,lyrics,idle,healthValue,density,media,dt);
            canvas.finish();contentOpen=false;canvas.composite(0,0,w,h,r,density);
        }finally{if(contentOpen)canvas.finish();}
        if(transition>=1)old=null;
    }
    private void upload(int[] pixels){
        if(artwork!=0)GL11.glDeleteTextures(artwork);artwork=0;uploaded=pixels;if(pixels==null)return;
        artwork=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,artwork);artPixels.clear();artPixels.put(pixels).flip();GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try{GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,64,64,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,artPixels);}finally{GL11.glPopClientAttrib();}
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
    }
    private void text(IslandFont font,String text,float x,float y,int color,float scale){GL11.glPushMatrix();try{GL11.glTranslatef(x,y,0);GL11.glScalef(scale,scale,1);font.drawHud(text,0,0,color);}finally{GL11.glPopMatrix();}}
    private void content(View view,Minecraft mc,IslandOptions o,float w,float h,float opacity,long now,int accent,LyricsManager.Result lyrics,String idle,double hp,float density,MediaSnapshot live,double dt){
        if(opacity<=.001F)return;GL11.glPushMatrix();
        try{float scale=.96F+.04F*opacity;GL11.glTranslatef(w/2,3*(1-opacity),0);GL11.glScalef(scale,scale,1);GL11.glTranslatef(-w/2,0,0);
            int white=alpha(0xFFF5F5F7,opacity),dim=alpha(0xFF93939E,opacity);
            if(view.event!=null){IslandEvent e=view.event;
                text(title,e.icon.isEmpty()?"*":e.icon,14,11,alpha(e.color,opacity),o.textSize);
                canvas.clip(34,0,Math.max(1,w-97),h);text(title,e.title,35,e.kind==IslandEvent.Kind.Toggle?13:8,white,o.textSize);if(e.kind!=IslandEvent.Kind.Toggle)text(body,e.message,35,25,dim,o.textSize);canvas.unclip();
                if(e.kind==IslandEvent.Kind.Toggle){float t=(float)toggle.update(e.enabled?1:0,dt,o.speed,0);int track=AirTheme.mix(0xFF63333C,0xFF258756,t);AirDraw.card(w-48,10,32,18,9,alpha(track,opacity),0);AirDraw.card(w-46+t*14,12,14,14,7,alpha(0xFFFFFFFF,opacity),0);}
                if(e.kind==IslandEvent.Kind.Warning){float pulse=.55F+.2F*(float)Math.sin(now/1e9*3);AirDraw.card(12,h-5,w-24,1.5F,1,alpha(e.color,pulse*opacity),0);}
                if(e.kind==IslandEvent.Kind.Combat){float value=(float)health.update(hp,dt,1,0);AirDraw.card(35,h-7,w-55,2,1,alpha(0xFF303037,opacity),0);AirDraw.card(35,h-7,Math.max(.01F,(w-55)*value),2,1,alpha(e.color,opacity),0);}
                return;
            }
            if(view.compact){if(view.lyrics&&view.key.equals(live.song())&&lyrics.song.equals(live.song()))drawCompactLyrics(o,w,h,framePosition+lyrics.offset,now,opacity,accent);
                else if(view.music){String status=!lyrics.ready?"Loading lyrics…":lyrics.instrumental?"Instrumental track":lyrics.unavailable?"Lyrics service busy":o.online?"Lyrics unavailable":"Online lyrics off";float s=Math.min(1,(w-22)/Math.max(1,body.width(status)));text(body,status,(w-body.width(status)*s)/2,(h-11)/2,dim,s);}return;}
            if(!view.music){float tw=textWidth(idle)*o.textSize;text(title,idle,(w-tw)/2,(h-10*o.textSize)/2,white,o.textSize);return;}
            MediaSnapshot m=view.key.equals(live.song())?live:view.media;
            if(o.art&&artwork!=0&&uploaded==m.artwork)canvas.artwork(artwork,10,10,30,density,opacity);else{text(title,"~",17,18,alpha(accent,opacity),1.3F);}
            float left=49,textArea=Math.max(10,w-left-42),overflow=Math.max(0,textWidth(m.title)*o.textSize-textArea),offset=marquee((now-changed)/1e9,overflow,o.marquee);
            canvas.clip(left,4,textArea,33);text(title,m.title,left-offset,10,white,o.textSize);text(body,m.artist.isEmpty()?m.provider:m.artist,left,26,dim,o.textSize);canvas.unclip();
            if(m.playing&&!m.buffering)for(int i=0;i<4;i++){float bar=3+(float)(.5+.5*Math.sin(now/1e9*(4+i*.6)+i*1.7))*12;AirDraw.card(w-29+i*4,28-bar,2.3F,bar,1,alpha(accent,opacity),0);}else text(body,"II",w-26,17,dim,1);
            double position=m==live?framePosition:m.at(now);
            if(o.progress){float fraction=m.duration>0?(float)(position/m.duration):0;AirDraw.card(14,h-7,w-28,1.5F,1,alpha(0xFF31313B,opacity),0);if(fraction>0)AirDraw.card(14,h-7,(w-28)*fraction,1.5F,1,alpha(accent,opacity*.85F),0);
                long second=(long)position;if(second!=displayedSecond){displayedSecond=second;timeLabel=time(second)+(m.duration>0?" / "+time((long)m.duration):"");}text(body,timeLabel,h<70?49:14,h-17,dim,.65F);
            }
            if(view.lyrics&&lyrics.song.equals(m.song())){if(!lyrics.source.isEmpty())text(body,lyrics.label,49,39,dim,.65F);drawLyrics(o,w,h,position+lyrics.offset,now,opacity*(m.playing?1:.6F),accent,dt);}
        }finally{GL11.glPopMatrix();}
    }
    private static String time(long s){return s/60+":"+(s%60<10?"0":"")+s%60;}
    private Layout layout(int line){Layout result=layouts[line];if(result==null)layouts[line]=result=new Layout(timeline.line(line).text,body);return result;}
    private Layout word(Layout full,LyricWords words,int index,int variant){
        if(full.sung==null)full.sung=new Layout[words.start.length][5];
        Layout result=full.sung[index][variant];if(result==null)full.sung[index][variant]=result=new Layout(words.display[index][variant],body);return result;
    }
    private float wordWidth(Layout full,LyricWords words,int index,double seconds,double end){
        float held=words.held(index,seconds,end);int lower=(int)held,upper=Math.min(4,lower+1);float f=held-lower;
        return word(full,words,index,lower).width*(1-f)+word(full,words,index,upper).width*f;
    }
    private float phraseWidth(int line,int first,int last,double seconds){
        LyricTimeline.Line lyric=timeline.line(line);Layout full=layout(line);LyricWords words=lyric.words;double end=line+1<timeline.size()?timeline.line(line+1).time:lyric.time+5;
        float total=0;for(int i=first;i<last;i++){total+=wordWidth(full,words,i,seconds,end);if(i+1<last)total+=full.advances[words.start[i+1]]-full.advances[words.end[i]];}return total;
    }
    private void sung(Layout part,float x,float y,float size,double progress,float master){
        double chars=progress*part.text.length();
        for(int c=0;c<part.colors.length;c++){float fill=progress>=1?1:progress<=0?0:smooth(chars-c);part.colors[c]=alpha(AirTheme.mix(0xFF79747F,0xFFFFFFFF,fill),master);}
        GL11.glPushMatrix();try{GL11.glTranslatef(x,y,0);GL11.glScalef(size,size,1);
            body.draw(part.text,0,0,0xFFFFFFFF,part.colors);
        }finally{GL11.glPopMatrix();}
    }
    private void phrase(int line,int first,int last,double seconds,float center,float y,float limit,float textSize,float master,boolean karaoke){
        LyricTimeline.Line lyric=timeline.line(line);if(lyric==null||first>=last)return;Layout full=layout(line);LyricWords words=lyric.words;
        double end=line+1<timeline.size()?timeline.line(line+1).time:lyric.time+5;
        float span=phraseWidth(line,first,last,seconds),size=Math.min(textSize,limit/Math.max(1,span)),x=center-span*size/2;
        for(int i=first;i<last;i++){
            float held=words.held(i,seconds,end);int lower=(int)held,upper=Math.min(4,lower+1);float f=held-lower;
            double progress=karaoke?words.progress(i,seconds,end):1;
            // Keep the unchanged prefix fully bright as a new held-vowel letter fades in.
            sung(word(full,words,i,lower),x,y,size,progress,master);
            if(f>.001)sung(word(full,words,i,upper),x,y,size,progress,master*f);
            x+=wordWidth(full,words,i,seconds,end)*size;if(i+1<last)x+=(full.advances[words.start[i+1]]-full.advances[words.end[i]])*size;
        }
    }
    private void drawCompactLyrics(IslandOptions o,float w,float h,double seconds,long now,float opacity,int accent){
        if(timeline.approximate)text(body,"~",8,(h-11)/2,alpha(0xFFE3BA73,opacity),.9F);
        double dots=o.dots?lyricMotion.dots:0;int line=timeline.index(seconds);
        if(line<0||timeline.line(line).words.start.length==0){
            if(o.dots)dots=1;
            else{String label=line<0?"Intro":"Instrumental";text(body,label,(w-body.width(label))/2,(h-11)/2,alpha(0xFF93939E,opacity),1);}
        }
        if(compactTransport!=playback.discontinuity){compactLine=compactOldLine=-1;compactTransport=playback.discontinuity;}
        if(line>=0&&timeline.line(line).words.start.length>0){
            LyricTimeline.Line lyric=timeline.line(line);double end=line+1<timeline.size()?timeline.line(line+1).time:lyric.time+5;
            int active=lyric.words.active(seconds,end),first=active/3*3,last=Math.min(first+3,lyric.words.start.length);
            // At most three words. Extremely long phrases may use one/two rather than clip the active word.
            if(phraseWidth(line,first,last,seconds)*.65F>w-24){first=active;last=Math.min(first+2,lyric.words.start.length);if(phraseWidth(line,first,last,seconds)*.65F>w-24)last=first+1;}
            if(compactLine!=line||compactFirst!=first||compactLast!=last){
                compactOldLine=compactLine;compactOldFirst=compactFirst;compactOldLast=compactLast;
                compactLine=line;compactFirst=first;compactLast=last;compactChanged=now;
            }
            // Compact timing follows the transport directly, never the expanded scrolling spring.
            // Keep a new phrase readable immediately; a crossfade must not blank rapid phrases.
            float t=smooth((now-compactChanged)/85000000D),master=opacity*(float)(1-dots);
            canvas.clip(10,(h-16)/2,w-20,16);
            try{
                if(compactOldLine>=0&&t<1)phrase(compactOldLine,compactOldFirst,compactOldLast,seconds,w/2,(h-9)/2-3*t,w-(timeline.approximate?34:24),Math.min(1,o.textSize),master*(1-t),o.karaoke);
                phrase(line,first,last,seconds,w/2,(h-11)/2+1.5F*(1-t),w-(timeline.approximate?34:24),Math.min(1,o.textSize),master*(.7F+.3F*t),o.karaoke);
            }finally{canvas.unclip();}
        }
        if(dots>.001)for(int i=0;i<3;i++)AirDraw.card(w/2-10+i*8,h/2-1.5F,3,3,1.5F,alpha(0xFFFFFFFF,opacity*(float)dots*(.5F+.25F*(float)Math.sin(now/1e9*2-i*.6))),0);
    }
    private void drawLyrics(IslandOptions o,float w,float h,double position,long now,float opacity,int accent,double dt){
        double dots=o.dots?lyricMotion.dots:0;
        if(dots>.001)for(int i=0;i<3;i++){float pulse=.3F+.4F*(float)(.5+.5*Math.sin(now/1e9*2-i*.6));AirDraw.card(w/2-10+i*8,o.lines==1?64:81,3,3,1.5F,alpha(accent,pulse*opacity*(float)dots),0);}
        canvas.clip(14,o.lines==1?50:47,w-28,o.lines==1?24:Math.max(1,h-58));
        try{int center=(int)Math.floor(lyricMotion.position),from=Math.max(0,center-3),to=Math.min(timeline.size()-1,center+4);
            for(int i=from;i<=to;i++){
                double distance=i-lyricMotion.position;
                float focus=1-smooth(Math.abs(distance)),fade=o.lines==1?1-smooth(Math.abs(distance)):(float)Math.max(0,1-Math.abs(distance)*.42),lineY=(o.lines==1?58:79)+(float)distance*21;
                if(fade<=0)continue;LyricTimeline.Line line=timeline.line(i);Layout layout=layouts[i];if(layout==null)layouts[i]=layout=new Layout(line.text,body);
                float s=Math.min(o.textSize,Math.max(.65F,(w-32)/Math.max(1,layout.width))),x=(w-layout.width*s)/2;
                float master=opacity*fade*(float)(lyricMotion.opacity*(1-dots));
                if(focus>.001&&o.karaoke)phrase(i,0,line.words.start.length,position,w/2,lineY,w-32,o.textSize,master,true);
                else text(body,layout.text,x,lineY,alpha(AirTheme.mix(0xFF94949F,0xFFF5F5F7,focus),master),s);
            }
        }finally{canvas.unclip();}
    }
    public void dispose(){canvas.dispose();title.dispose();body.dispose();widths.clear();if(artwork!=0)GL11.glDeleteTextures(artwork);artwork=0;uploaded=null;previous=0;current=old=null;layouts=new Layout[0];timeline=LyricTimeline.EMPTY;playback.reset();lyricMotion.reset();}
}
