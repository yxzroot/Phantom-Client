import keystrokesmod.client.module.modules.render.island.*;import com.google.gson.*;
import java.nio.*;import org.lwjgl.BufferUtils;import org.lwjgl.opengl.*;

public final class IslandTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static IslandEvent event(String key,int priority,boolean on){return new IslandEvent(IslandEvent.Kind.Toggle,key,key,"","*",0xFF9B8CFF,priority,1000,on);}
    private static void cpu(){
        transport();
        double reference=0;for(int fps:new int[]{60,144,240}){IslandSpring s=new IslandSpring(120);for(int i=0;i<fps;i++)s.update(300,1D/fps,1,.3);if(reference==0)reference=s.value;else check(Math.abs(s.value-reference)<1e-9,"Frame-dependent spring");check(Math.abs(s.value-300)<.001,"Spring not settled");s.update(180,.03,1,.3);check(Double.isFinite(s.value),"Retarget invalid");}
        IslandQueue q=new IslandQueue();q.post(event("a",1,true),0);check(q.current(0).enabled,"Event not displayed");q.post(event("a",1,false),1);check(!q.current(1).enabled&&q.size()==0,"Duplicate toggle not merged");for(int i=0;i<20;i++)q.post(event("n"+i,1,true),2);check(q.size()==5,"Queue not bounded");q.post(event("warning",5,true),3);check(q.current(3).key.equals("warning"),"Warning not prioritized");check(q.current(2000000000L)!=null,"Next event lost");
        LyricTimeline l=LyricTimeline.parse("[ar:Artist]\n[offset:100]\n[00:01.00][00:04.500]Hello\n[00:02.0]<00:02.0>One <00:02.5>two\n[bad]ignore\n[00:99.00]bad");check(l.size()==3&&l.index(1.2)==0&&l.index(4.7)==2,"LRC timestamp/order/multiple tags");check(l.line(1).fill(2.35,4.5)>0&&l.line(1).fill(2.35,4.5)<1,"Enhanced word timing");check(LyricTimeline.parse("garbage").size()==0,"Invalid LRC not ignored");
        String name=IslandIO.safeName("../../Artist\\Title:<bad>");check(!name.contains("/")&&!name.contains("\\")&&!name.contains(".."),"Unsafe filename");
        MediaSnapshot playing=new MediaSnapshot("a","Riff","Track","Artist","Album","1",60,10,true,1,1000000000L,null,0);check(playing.at(2000000000L)==11&&playing.at(90000000000L)==60,"Playback position interpolation");MediaSnapshot paused=new MediaSnapshot("a","Riff","Track","Artist","Album","1",60,10,false,1,1000000000L,null,0);check(paused.at(2000000000L)==10,"Pause advances clock");
        MediaSessionManager manager=new MediaSessionManager();JsonArray sessions=new JsonParser().parse("[{\"session\":\"riff\",\"source\":\"riff\",\"title\":\"Test fixture\",\"id\":\"1\",\"position\":5,\"playing\":true}]").getAsJsonArray();manager.accept(sessions);check(manager.snapshot().provider.equals("Riff")&&manager.snapshot().position==5,"Unified OS normalization");manager.accept(new JsonArray());check(!manager.snapshot().present(),"Closed player retained");manager.close();
        for(String p:new String[]{"Spotify","SoundCloud","YouTube","YouTube Music","Riff"})check(MediaSessionManager.provider(p).equals(p),"Provider missing "+p);
        check(MediaSnapshot.normalize("Song (Official Video)").equals("song")&&MediaSnapshot.normalize("Lyrics of Life").equals("lyrics of life"),"Unsafe lyric title normalization");
        MediaSnapshot match=new MediaSnapshot("fixture","Riff","Song","Artist","Album","",113,0,true,1,0,null,0);
        JsonArray candidates=new JsonParser().parse("[{\"trackName\":\"Song\",\"artistName\":\"Other artist\",\"duration\":113,\"syncedLyrics\":\"[00:01]Wrong fixture\"},{\"trackName\":\"Song\",\"artistName\":\"Artist\",\"duration\":900,\"syncedLyrics\":\"[00:01]Wrong version\"},{\"trackName\":\"song\",\"artistName\":\"artist\",\"duration\":114,\"syncedLyrics\":\"[00:01]Matched fixture\"}]").getAsJsonArray();
        check(LyricsLookup.select(candidates,match)==candidates.get(2),"Search picked unrelated artist/version instead of matched synced lyrics");
        check(LyricsLookup.select(new JsonArray(),match)==null,"Empty search fabricated lyrics");
        MediaSnapshot ru=new MediaSnapshot("fixture","Riff","Рассвет (Текст песни)","emoslut666 - Topic","","",120,0,true,1,0,null,0);
        JsonArray russian=new JsonParser().parse("[{\"trackName\":\"Рассвет\",\"artistName\":\"emoslut666\",\"duration\":120,\"syncedLyrics\":\"[00:01]Привет мир\"},{\"trackName\":\"Рассвет (slowed)\",\"artistName\":\"emoslut666\",\"duration\":120,\"syncedLyrics\":\"[00:01]Wrong version\"}]").getAsJsonArray();
        check(LyricsLookup.select(russian,ru)==russian.get(0),"Cyrillic title/Topic metadata not matched safely");check(LyricTimeline.parse("[00:01]Привет мир\n[00:01]\n[00:04]Пока").line(0).text.equals("Привет мир"),"Blank duplicate timestamp erases lyrics");
        MediaSnapshot upload=new MediaSnapshot("fixture","Riff","+emoslut666 specialsadism","voidvoice717","","",102,0,true,1,0,null,0);
        JsonArray uploads=new JsonParser().parse("[{\"trackName\":\"specialsadism\",\"artistName\":\"voidvoice717\",\"duration\":114,\"syncedLyrics\":\"[00:01]Wrong version\"},{\"trackName\":\"specialsadism\",\"artistName\":\"voidvoice717\",\"duration\":103,\"syncedLyrics\":\"[00:01]Matched fixture\"},{\"trackName\":\"specialsadism\",\"artistName\":\"Unrelated\",\"duration\":102,\"syncedLyrics\":\"[00:01]Wrong artist\"}]").getAsJsonArray();
        check(LyricsLookup.select(uploads,upload)==uploads.get(1),"Plus-credit upload title misses lyrics or picks wrong duration/artist");check(LyricsLookup.searchTitle("+44").equals("+44"),"Literal plus title stripped");
        MediaSnapshot kinoUpload=new MediaSnapshot("fixture","Riff","Кино - Хочу перемен","Alexander Pintz","","",293,0,true,1,0,null,0);
        JsonArray kino=new JsonParser().parse("[{\"trackName\":\"Хочу перемен\",\"artistName\":\"Кино\",\"duration\":293,\"syncedLyrics\":\"[00:01]Matched fixture\"},{\"trackName\":\"Хочу перемен\",\"artistName\":\"Different cover artist\",\"duration\":293,\"syncedLyrics\":\"[00:01]Wrong artist\"}]").getAsJsonArray();
        check(LyricsLookup.select(kino,kinoUpload)==kino.get(0),"Embedded artist uploader case fails");check(LyricsLookup.queryArtist(kinoUpload).equalsIgnoreCase("Кино")&&LyricsLookup.queryTitle(kinoUpload).equalsIgnoreCase("Хочу перемен"),"Queries still use uploader");
        MediaSnapshot liveVersion=new MediaSnapshot("fixture","Riff","Song - Live","Artist","","",100,0,true,1,0,null,0);check(LyricsLookup.queryTitle(liveVersion).equals("song - live"),"Version suffix mistaken for artist credit");
        check(IslandRenderer.marquee(1,100,20)==0&&IslandRenderer.marquee(4,100,20)>0,"Marquee pause/scroll");try{lyrics();riff();}catch(Exception e){throw new AssertionError(e);}System.out.println("PASS: 60/144/240Hz analytic springs, direct retarget, bounded/merged/prioritized events, LRC/enhanced timing, traversal prevention, play/pause/clamp, provider normalization, session close and marquee");
    }
    private static MediaSnapshot sample(double position,boolean playing,boolean buffering,long time,long revision){return new MediaSnapshot("riff","Riff","Fixture","Artist","","id",120,position,playing,1,time,null,0,revision,buffering);}
    private static void transport(){
        LyricTimeline wordFixture=LyricTimeline.parse("[00:01]<00:01>hello <00:02>hello <00:03>i <00:04>love <00:07>you\n[00:10]Next fixture");
        LyricWords words=wordFixture.line(0).words;check(words.start.length==5&&words.active(4.5,10)==3,"Explicit word timing/group boundaries wrong");
        check(words.held(3,5.5,10)>0&&words.display[3][4].equals("loveeeee"),"Held vowel does not extend from enhanced timing");
        LyricWords plain=LyricTimeline.parse("[00:01]hello i love you\n[00:10]Next fixture").line(0).words;check(plain.held(2,7,10)==0,"Line-only lyrics fabricated held-vocal detection");
        for(int fps:new int[]{60,144,240}){
            PlaybackClock clock=new PlaybackClock();long start=1000000000L;MediaSnapshot current=sample(10,true,false,start,0);
            double previous=10;
            for(int i=0;i<=fps*2;i++){
                long now=start+Math.round(i*1e9/fps);
                if(i>0&&i%Math.max(1,fps/4)==0)current=sample(10+i/(double)fps+(i%2==0?-.04:.04),true,false,now,0);
                double value=clock.update(current,now);check(value>=previous-1e-6,"Metadata jitter reversed lyrics");previous=value;
            }
            long t=start+3000000000L;clock.update(sample(12,false,false,t,0),t);check(clock.update(sample(12,false,false,t+1000000000L,0),t+1000000000L)==12,"Paused lyrics drift");
            clock.update(sample(12,true,true,t+2000000000L,0),t+2000000000L);check(clock.update(sample(12,true,true,t+3000000000L,0),t+3000000000L)==12,"Buffering lyrics drift");
            clock.update(sample(12,true,false,t+4000000000L,0),t+4000000000L);
            check(Math.abs(clock.update(sample(11.8,true,false,t+4100000000L,1),t+4100000000L)-11.8)<1e-6,"Explicit small backward seek swallowed");
            LyricTimeline lines=LyricTimeline.parse("[00:01]Fixture one\n[00:02]Fixture two\n[00:24]Fixture three");LyricMotion motion=new LyricMotion();
            motion.update(lines,1.9,0,.1,.6F);motion.update(lines,2.01,0,1D/fps,.6F);
            check(motion.position>0&&motion.position<1,"Lyric line switches without a smooth transition");
            for(int i=0;i<fps;i++)motion.update(lines,3+i/(double)fps,0,1D/fps,.6F);
            check(Math.abs(motion.position-1)<.02,"Lyric motion never settles");
            motion.update(lines,1.1,1,1D/fps,.6F);check(motion.position==0&&motion.opacity<.2,"Seek did not reset lyric visual position");
            check(lines.line(1).fill(10,24)>0&&lines.line(1).fill(10,24)<1,"Plain LRC invented an early vocal cutoff");
            for(int i=0;i<fps*5;i++)motion.update(lines,10+i/(double)fps,2,1D/fps,.6F);
            check(motion.active==1&&motion.dots<.001,"Long valid lyric interval replaced by instrumental dots");
            LyricTimeline breakFixture=LyricTimeline.parse("[00:01]Fixture lyric\n[00:03]\n[00:08]Back again");
            for(int i=0;i<fps;i++)motion.update(breakFixture,4+i/(double)fps,3,1D/fps,.6F);check(motion.dots>.99,"Explicit instrumental break lost");
            for(int i=0;i<fps;i++)motion.update(breakFixture,8+i/(double)fps,3,1D/fps,.6F);check(motion.active==2&&motion.dots<.001,"Dots stuck after next vocal cue");
            LyricTimeline malformed=LyricTimeline.parse("[00:01]<00:99>Invalid <00:02>valid <00:01>backwards");check(Double.isFinite(malformed.line(0).fill(2.5,5)),"Malformed enhanced timestamp poisoned lyric fill");
        }
        System.out.println("PASS: 60/144/240Hz lyric motion, jitter smoothing, pause/buffering freeze, explicit small backward seeks and instrumental fill");
    }
    private static void riff()throws Exception{
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-riff-fixture-");String token="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        java.nio.file.Files.write(root.resolve("island-riff-bridge.json"),("{\"port\":0,\"token\":\""+token+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        RiffBridge bridge=new RiffBridge(root);bridge.start();try{
            long until=System.nanoTime()+3000000000L;while(bridge.port()==0&&System.nanoTime()<until)Thread.sleep(5);check(bridge.port()!=0,"Riff loopback server did not start");
            String song="[{\"title\":\"Bridge fixture\",\"artist\":\"Test artist\",\"position\":12,\"duration\":100,\"playing\":true,\"artworkBase64\":\"not an image\"}]";
            check(post(bridge.port(),"bad",song)==403&&!bridge.snapshot().present(),"Unauthenticated Riff request accepted");
            check(post(bridge.port(),token,song)==204&&bridge.snapshot().provider.equals("Riff")&&bridge.snapshot().title.equals("Bridge fixture"),"Riff plugin metadata not normalized");
            check(bridge.snapshot().playing&&bridge.snapshot().position==12,"Riff real playback fields discarded");
            check(post(bridge.port(),token,"[]")==204&&!bridge.snapshot().present(),"Riff stopped session retained");
            System.out.println("PASS: authenticated loopback Riff bridge, rejected invalid token, real metadata/playback normalization, bad artwork isolation and session clear");
        }finally{bridge.close();}
    }
    private static int post(int port,String token,String sessions)throws Exception{
        java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL("http://127.0.0.1:"+port+"/phantom-media").openConnection();connection.setConnectTimeout(2000);connection.setReadTimeout(2000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","text/plain;charset=UTF-8");try{try(java.io.OutputStream out=connection.getOutputStream()){out.write(("{\"token\":\""+token+"\",\"sessions\":"+sessions+"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));}return connection.getResponseCode();}finally{connection.disconnect();}
    }
    private static void lyrics()throws Exception{
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-lyrics-fixture-"),local=root.resolve("lyrics");java.nio.file.Files.createDirectory(local);java.nio.file.Files.write(local.resolve("Artist - Fixture.lrc"),"[00:01.00]First test line\n[00:02.00]Second test line".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MediaSnapshot song=new MediaSnapshot("fixture","Riff","Fixture","Artist","","a",60,0,true,1,0,null,0);LyricsManager manager=new LyricsManager(root);manager.request(song,false);long until=System.nanoTime()+3000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(5);check(manager.result().ready&&manager.result().timeline.size()==2,"Async local LRC unavailable");manager.offset(song.song(),2.5);until=System.nanoTime()+3000000000L;while(!java.nio.file.Files.exists(root.resolve("island-song-offsets.properties"))&&System.nanoTime()<until)Thread.sleep(5);manager.close();
        manager=new LyricsManager(root);manager.request(song,false);until=System.nanoTime()+3000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(5);check(manager.result().offset==2.5,"Per-song offset not persisted");MediaSnapshot other=new MediaSnapshot("other","Spotify","Unknown fixture","Artist","","b",60,0,true,1,0,null,0);manager.request(other,false);check(manager.result().timeline.size()==0,"Previous track lyrics retained");manager.request(song,false);until=System.nanoTime()+3000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(5);check(manager.result().song.equals(song.song())&&manager.result().timeline.size()==2,"Rapid skip replaced active lyrics with stale results");manager.close();System.out.println("PASS: worker-only local LRC priority, offline mode, persisted per-song offsets, rapid track changes and no stale lyrics");
    }
    private static void gl()throws Exception{
        net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
        Pbuffer p=new Pbuffer(256,128,new PixelFormat().withDepthBits(24),null);p.makeCurrent();IslandCanvas canvas=new IslandCanvas();try{
            GL11.glViewport(0,0,256,128);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,256,128,0,-100,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glClearColor(1,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            canvas.begin(200,80,1);GL11.glClearColor(0,1,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);canvas.finish();canvas.composite(30,30,120,28,14,1);
            ByteBuffer pixels=BufferUtils.createByteBuffer(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);
            int corner=((128-31)*256+31)*4,center=((128-44)*256+90)*4;check((pixels.get(corner)&255)>240&&(pixels.get(corner+1)&255)<10,"Content leaked outside rounded corner");check((pixels.get(center+1)&255)>240,"Island content not composited");check(GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT)==0,"Framebuffer leaked");
            IntBuffer viewport=BufferUtils.createIntBuffer(16);GL11.glGetInteger(GL11.GL_VIEWPORT,viewport);check(viewport.get(2)==256&&viewport.get(3)==128,"Viewport leaked");
            for(int i=0;i<240;i++){canvas.begin(200,80,1);canvas.finish();canvas.composite(30,30,120+i%10,28,14,1);}check(GL11.glGetError()==0,"Island GL errors");System.out.println("PASS: real GL private framebuffer/SDF rounded clipping, visible content, viewport/FBO recovery and 240 passes");
            production();
        }finally{canvas.dispose();p.destroy();}
    }
    private static void production()throws Exception{
        net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
        java.lang.reflect.Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        net.minecraft.client.Minecraft mc=(net.minecraft.client.Minecraft)unsafe.allocateInstance(net.minecraft.client.Minecraft.class);java.lang.reflect.Field singleton=net.minecraft.client.Minecraft.class.getDeclaredField("theMinecraft");singleton.setAccessible(true);Object original=singleton.get(null);singleton.set(null,mc);
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-island-fixture-");IslandRenderer renderer=new IslandRenderer();LyricsManager lyrics=new LyricsManager(root);
        try{
            mc.displayWidth=256;mc.displayHeight=128;mc.gameSettings=new net.minecraft.client.settings.GameSettings();mc.gameSettings.guiScale=1;java.lang.reflect.Field dataDir=net.minecraft.client.Minecraft.class.getField("mcDataDir");dataDir.setAccessible(true);dataDir.set(mc,root.toFile());
            java.lang.reflect.Field language=net.minecraft.client.Minecraft.class.getDeclaredField("mcLanguageManager");language.setAccessible(true);language.set(mc,new net.minecraft.client.resources.LanguageManager(new net.minecraft.client.resources.data.IMetadataSerializer(),"en_US"));
            net.minecraft.client.gui.ScaledResolution screen=new net.minecraft.client.gui.ScaledResolution(mc);
            int tex0=GL11.glGenTextures(),tex1=GL11.glGenTextures();GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex0);GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex1);
            GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_LIGHTING);GL11.glDepthMask(true);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(0,0,256,128);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glColor4f(.2F,.3F,.4F,.7F);
            IslandOptions o=new IslandOptions();o.maxWidth=220;o.scale=.8F;IslandEvent toggle=event("Module fixture",1,true);
            for(int i=0;i<12;i++)renderer.draw(mc,screen,o,toggle,MediaSnapshot.EMPTY,lyrics.result(),"Phantom",.7,false);
            check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==GL13.GL_TEXTURE1&&GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==tex1,"Production renderer leaked lightmap texture");GL13.glActiveTexture(GL13.GL_TEXTURE0);check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==tex0,"Production renderer leaked atlas texture");GL13.glActiveTexture(GL13.GL_TEXTURE1);
            check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),"Production renderer leaked world/HUD state");check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_PROJECTION,"Production renderer leaked matrix mode");
            check(GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT)==0&&GL11.glGetError()==0,"Production renderer FBO/GL failure");
            int[] art=new int[4096];java.util.Arrays.fill(art,0xFF8866CC);MediaSnapshot song=new MediaSnapshot("fixture","Riff","Rendering fixture","Fixture Artist","","track",60,12,true,1,System.nanoTime(),art,0xFF8866CC);
            for(int i=0;i<240;i++)renderer.draw(mc,screen,o,i<60?toggle:null,song,lyrics.result(),"Phantom",.7,true);check(GL11.glGetError()==0,"Production music/equalizer/artwork GL failure");
            settle(renderer,mc,screen,o,song,lyrics.result());
            GL11.glClearColor(.12F,.15F,.19F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);renderer.draw(mc,screen,o,null,song,lyrics.result(),"Phantom",.7,true);
            ByteBuffer preview=BufferUtils.createByteBuffer(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,preview);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(256,128,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int py=0;py<128;py++)for(int px=0;px<256;px++){int i=(py*256+px)*4;image.setRGB(px,127-py,(preview.get(i)&255)<<16|(preview.get(i+1)&255)<<8|(preview.get(i+2)&255));}javax.imageio.ImageIO.write(image,"png",root.resolve("island-preview.png").toFile());System.out.println("Island preview: "+root.resolve("island-preview.png"));
            java.lang.reflect.Constructor<LyricsManager.Result> resultConstructor=LyricsManager.Result.class.getDeclaredConstructor(String.class,LyricTimeline.class,double.class);resultConstructor.setAccessible(true);LyricsManager.Result timed=resultConstructor.newInstance(song.song(),LyricTimeline.parse("[00:04]Previous test lyric\n[00:12]<00:12>Current <00:12.50>test lyric\n[00:18]Next test lyric"),0D);settle(renderer,mc,screen,o,song,timed);renderer.draw(mc,screen,o,null,song,timed,"Phantom",.7,true);check(GL11.glGetError()==0,"Expanded lyric/karaoke GL failure");
            o.lyricsOnly=true;o.matchArt=false;o.accent=0xFFFFFFFF;settle(renderer,mc,screen,o,song,timed);
            java.lang.reflect.Field wf=IslandRenderer.class.getDeclaredField("width"),hf=IslandRenderer.class.getDeclaredField("height");wf.setAccessible(true);hf.setAccessible(true);
            check(Math.abs(((IslandSpring)wf.get(renderer)).value-120)<.01&&Math.abs(((IslandSpring)hf.get(renderer)).value-28)<.01,"Lyrics-only expands instead of staying a small pill");
            java.lang.reflect.Field first=IslandRenderer.class.getDeclaredField("compactFirst"),last=IslandRenderer.class.getDeclaredField("compactLast");first.setAccessible(true);last.setAccessible(true);check(last.getInt(renderer)-first.getInt(renderer)<=3,"Compact pill shows more than three words");
            for(int i=0;i<240;i++)renderer.draw(mc,screen,o,null,song,timed,"Phantom",.7,true);
            check(GL11.glGetError()==0,"Compact lyrics GL failure");o.lyricsOnly=false;
            MediaSnapshot phraseSong=new MediaSnapshot("fixture","Riff","Phrase fixture","Fixture artist","","phrases",60,5.5,false,1,System.nanoTime(),null,0);
            LyricsManager.Result phrases=resultConstructor.newInstance(phraseSong.song(),LyricTimeline.parse("[00:01]<00:01>hello <00:02>hello <00:03>i <00:04>love <00:07>you\n[00:10]Next fixture"),0D);
            o.lyricsOnly=true;o.accent=0xFF754431;settle(renderer,mc,screen,o,phraseSong,phrases);
            java.lang.reflect.Field phraseChanged=IslandRenderer.class.getDeclaredField("compactChanged");phraseChanged.setAccessible(true);phraseChanged.setLong(renderer,System.nanoTime()-1000000000L);
            GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);renderer.draw(mc,screen,o,null,phraseSong,phrases,"Phantom",.7,true);
            ByteBuffer whitePixels=BufferUtils.createByteBuffer(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,whitePixels);int whites=0;for(int i=0;i<256*128;i++)if((whitePixels.get(i*4)&255)>180&&(whitePixels.get(i*4+1)&255)>180&&(whitePixels.get(i*4+2)&255)>180)whites++;
            check(whites>2,"Compact sung text is theme-colored instead of white");check(first.getInt(renderer)==3&&last.getInt(renderer)==5,"Compact three-word phrase does not advance with song timing");o.lyricsOnly=false;
            MediaSnapshot russian=new MediaSnapshot("fixture","Riff","Рассвет","emoslut666","","ru",60,5.5,false,1,System.nanoTime(),null,0);
            LyricsManager.Result ru=resultConstructor.newInstance(russian.song(),LyricTimeline.parse("[00:01]<00:01>Добрый <00:02>вечер <00:03>мой <00:04>друг <00:07>привет\n[00:10]Следующая строка"),0D);
            o.lyricsOnly=true;settle(renderer,mc,screen,o,russian,ru);check(first.getInt(renderer)==3,"Cyrillic compact phrase behind explicit timing");GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);renderer.draw(mc,screen,o,null,russian,ru,"Phantom",.7,true);check(GL11.glGetError()==0,"Cyrillic font GL failure");
            IslandFont font=new IslandFont(9,java.awt.Font.PLAIN);check(font.supports('Я')&&font.supports('ё'),"Cyrillic font missing");float ratio=font.width("Привет")/font.width("Hello!");check(ratio>.6&&ratio<2,"Cyrillic font shrunk or uses unrelated metrics");font.dispose();
            ByteBuffer ruPixels=BufferUtils.createByteBuffer(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,ruPixels);int ruBright=0;java.awt.image.BufferedImage ruImage=new java.awt.image.BufferedImage(256,128,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int py=0;py<128;py++)for(int px=0;px<256;px++){int i=(py*256+px)*4,red=ruPixels.get(i)&255,green=ruPixels.get(i+1)&255,blue=ruPixels.get(i+2)&255;if(red>180&&green>180&&blue>180)ruBright++;ruImage.setRGB(px,127-py,red<<16|green<<8|blue);}check(ruBright>2,"Cyrillic sung text invisible");javax.imageio.ImageIO.write(ruImage,"png",root.resolve("island-cyrillic-preview.png").toFile());System.out.println("Cyrillic preview: "+root.resolve("island-cyrillic-preview.png"));o.lyricsOnly=false;
            MediaSnapshot longGap=new MediaSnapshot("fixture","Riff","Long gap fixture","Fixture","","long-gap",391,40,false,1,System.nanoTime(),null,0);
            LyricsManager.Result longLyrics=resultConstructor.newInstance(longGap.song(),LyricTimeline.parse("[00:04.60]Добрый вечер мой дорогой друг сегодня\n[00:55.16]Следующая строка\n[01:10]"),0D);
            o.lyricsOnly=true;settle(renderer,mc,screen,o,longGap,longLyrics,2);java.lang.reflect.Field motionField=IslandRenderer.class.getDeclaredField("lyricMotion");motionField.setAccessible(true);check(((LyricMotion)motionField.get(renderer)).dots<.001,"Production compact valid long line hidden by dots");check(first.getInt(renderer)<last.getInt(renderer),"Long interval has no visible compact phrase");o.lyricsOnly=false;
            LyricTimeline estimated=LyricTimeline.estimate("First synthetic fixture\nSecond synthetic fixture\nThird synthetic fixture",60);
            MediaSnapshot estimateSong=new MediaSnapshot("fixture","Riff","Estimate fixture","Fixture","","estimate",60,15,false,1,System.nanoTime(),null,0);
            java.lang.reflect.Constructor<LyricsManager.Result> sourceCtor=LyricsManager.Result.class.getDeclaredConstructor(String.class,LyricTimeline.class,double.class,boolean.class,boolean.class,String.class);sourceCtor.setAccessible(true);LyricsManager.Result approximate=sourceCtor.newInstance(estimateSong.song(),estimated,0D,true,false,"LRCLIB");
            o.lyricsOnly=true;settle(renderer,mc,screen,o,estimateSong,approximate,2);check(approximate.timeline.approximate&&approximate.label.contains("estimated"),"Approximate timing not labeled");
            GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);renderer.draw(mc,screen,o,null,estimateSong,approximate,"Phantom",.7,true);ByteBuffer approxPixels=BufferUtils.createByteBuffer(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,approxPixels);int amber=0;for(int i=0;i<256*128;i++){int red=approxPixels.get(i*4)&255,green=approxPixels.get(i*4+1)&255,blue=approxPixels.get(i*4+2)&255;if(red>100&&red>green*1.1&&green>blue*1.2)amber++;}check(amber>0,"Compact approximate marker invisible");o.lyricsOnly=false;settle(renderer,mc,screen,o,estimateSong,approximate,2);check(GL11.glGetError()==0,"Expanded estimated source footer leaks GL");
            for(IslandEvent.Kind kind:IslandEvent.Kind.values()){IslandEvent e=new IslandEvent(kind,"event fixture",kind.name(),"Fixture notification","!",0xFF8866CC,2,1000,true);renderer.draw(mc,screen,o,e,song,timed,"Phantom",.7,true);}check(GL11.glGetError()==0,"Island event-style GL failure");
            GL11.glDeleteTextures(tex0);GL11.glDeleteTextures(tex1);System.out.println("PASS: production island renderer with real Phantom font atlas, notifications, artwork and music; compact 120x28 lyrics-only pill across 240 frames; lighting/fog/depth/scissor/matrix/FBO and both texture bindings restored");
        }finally{renderer.dispose();lyrics.close();singleton.set(null,original);}
    }
    private static void settle(IslandRenderer renderer,net.minecraft.client.Minecraft mc,net.minecraft.client.gui.ScaledResolution screen,IslandOptions o,MediaSnapshot song,LyricsManager.Result result)throws Exception{
        settle(renderer,mc,screen,o,song,result,10);
    }
    private static void settle(IslandRenderer renderer,net.minecraft.client.Minecraft mc,net.minecraft.client.gui.ScaledResolution screen,IslandOptions o,MediaSnapshot song,LyricsManager.Result result,int minBright)throws Exception{
        java.lang.reflect.Field previous=IslandRenderer.class.getDeclaredField("previous"),changed=IslandRenderer.class.getDeclaredField("changed");previous.setAccessible(true);changed.setAccessible(true);
        for(int i=0;i<120;i++){previous.setLong(renderer,System.nanoTime()-16666667L);changed.setLong(renderer,System.nanoTime()-1000000000L);renderer.draw(mc,screen,o,null,song,result,"Phantom",.7,true);}
        ByteBuffer data=BufferUtils.createByteBuffer(256*128*4);GL11.glReadPixels(0,0,256,128,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,data);int bright=0;for(int i=0;i<256*128;i++)if((data.get(i*4)&255)>150&&(data.get(i*4+1)&255)>150&&(data.get(i*4+2)&255)>150)bright++;
        if(bright<=minBright){java.awt.image.BufferedImage failure=new java.awt.image.BufferedImage(256,128,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int py=0;py<128;py++)for(int px=0;px<256;px++){int k=(py*256+px)*4;failure.setRGB(px,127-py,(data.get(k)&255)<<16|(data.get(k+1)&255)<<8|(data.get(k+2)&255));}javax.imageio.ImageIO.write(failure,"png",mc.mcDataDir.toPath().resolve("failed-frame.png").toFile());java.lang.reflect.Field motion=IslandRenderer.class.getDeclaredField("lyricMotion");motion.setAccessible(true);LyricMotion m=(LyricMotion)motion.get(renderer);System.out.println("Failed frame: "+mc.mcDataDir.toPath().resolve("failed-frame.png")+" bright="+bright+" active="+m.active+" dots="+m.dots+" opacity="+m.opacity);}
        check(bright>minBright,"Production title/artist text invisible");
    }
    private static void liveLyrics()throws Exception{
        java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-live-lyrics-check-");MediaSnapshot song=new MediaSnapshot("live-check","Riff","dancing alone","requiem for you","","",113,0,true,1,System.nanoTime(),null,0);
        java.nio.file.Path cache=root.resolve("lyrics-cache");java.nio.file.Files.createDirectories(cache);java.nio.file.Files.write(cache.resolve(IslandIO.sha(song.song())+".json"),"{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LyricsManager manager=new LyricsManager(root);try{manager.request(song,true);long until=System.nanoTime()+22000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(20);check(manager.result().ready&&manager.result().timeline.size()>0,"Live lyrics lookup failed or kept stale legacy not-found entry");System.out.println("PASS: production LRCLIB lookup recovered synchronized lyrics for requested song despite legacy negative cache; "+manager.result().timeline.size()+" timed lines; no lyric text logged");}finally{manager.close();}
    }
    private static void liveArtist()throws Exception{
        java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL("https://lrclib.net/api/search?q=emoslut666").openConnection();connection.setConnectTimeout(3000);connection.setReadTimeout(5000);JsonArray data;try(java.io.InputStream in=connection.getInputStream()){data=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();}finally{connection.disconnect();}
        JsonObject chosen=null;for(JsonElement value:data){JsonObject item=value.getAsJsonObject();if(item.get("artistName").getAsString().equalsIgnoreCase("emoslut666")&&item.has("syncedLyrics")&&!item.get("syncedLyrics").isJsonNull()){chosen=item;break;}}check(chosen!=null,"No artist fixture available");
        MediaSnapshot song=new MediaSnapshot("live-check","Riff",chosen.get("trackName").getAsString()+" (Текст песни)","emoslut666 - Topic","","",chosen.get("duration").getAsDouble(),0,true,1,System.nanoTime(),null,0);java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-artist-check-");LyricsManager manager=new LyricsManager(root);
        try{manager.request(song,true);long until=System.nanoTime()+35000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(20);check(manager.result().ready&&manager.result().timeline.size()>0,"Production Cyrillic artist lookup failed");System.out.println("PASS: live emoslut666 metadata + Cyrillic video suffix + Topic artist recovered synced lyrics; no lyric text logged");}finally{manager.close();}
    }
    private static void liveUpload()throws Exception{
        MediaSnapshot song=new MediaSnapshot("live-check","Riff","+emoslut666 specialsadism","voidvoice717","","",102,0,true,1,System.nanoTime(),null,0);java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-upload-check-");LyricsManager manager=new LyricsManager(root);
        try{manager.request(song,true);long until=System.nanoTime()+35000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(20);check(manager.result().ready&&!manager.result().unavailable&&manager.result().timeline.size()>0,"Exact screenshot upload lookup failed (or service unavailable)");System.out.println("PASS: exact +emoslut666 specialsadism / voidvoice717 / 102-second upload recovered synchronized lyrics; no lyric text logged");}finally{manager.close();}
    }
    private static void liveKino()throws Exception{
        MediaSnapshot song=new MediaSnapshot("live-check","Riff","Кино - Хочу перемен","Alexander Pintz","","",293,0,true,1,System.nanoTime(),null,0);java.nio.file.Path root=java.nio.file.Files.createTempDirectory("phantom-kino-check-");LyricsManager manager=new LyricsManager(root);
        try{manager.request(song,true);long until=System.nanoTime()+35000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(20);check(manager.result().ready&&!manager.result().unavailable&&manager.result().timeline.size()>0,"Embedded Kino / uploader lookup failed (or service unavailable)");System.out.println("PASS: exact Кино - Хочу перемен / Alexander Pintz upload recovered synchronized lyrics; no lyric text logged");}finally{manager.close();}
    }
    public static void main(String[] args)throws Exception{if(args.length==0)cpu();else if(args[0].equals("lyrics-live"))liveLyrics();else if(args[0].equals("artist-live"))liveArtist();else if(args[0].equals("upload-live"))liveUpload();else if(args[0].equals("kino-live"))liveKino();else gl();}
}
