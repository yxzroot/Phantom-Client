import keystrokesmod.client.module.modules.render.island.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic catalog/transport tests, not an artist allowlist or hand-fixed real lyrics. */
public final class LyricsPipelineTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static MediaSnapshot song(String title,String artist,double duration){return new MediaSnapshot("fixture","Riff",title,artist,"","",duration,0,true,1,System.nanoTime(),null,0);}
    static JsonObject item(String title,String artist,double duration,String timed,String plain){JsonObject o=new JsonObject();o.addProperty("trackName",title);o.addProperty("artistName",artist);o.addProperty("duration",duration);if(timed!=null)o.addProperty("syncedLyrics",timed);if(plain!=null)o.addProperty("plainLyrics",plain);return o;}
    static void await(LyricsManager manager)throws Exception{long until=System.nanoTime()+3000000000L;while(!manager.result().ready&&System.nanoTime()<until)Thread.sleep(5);check(manager.result().ready,"Worker never completed");}
    static void cpu()throws Exception{
        JsonObject sample=item("Northern Signal","Lunar Example",120,"[00:01]Synthetic fixture one\n[00:08]Synthetic fixture two","Synthetic fixture one\nSynthetic fixture two");
        for(String title:new String[]{"Northern Signal","Northern Signal (Official Video)","Lunar Example - Northern Signal","Northern Signal - Lunar Example"}){
            MediaSnapshot query=song(title,title.contains(" - ")?"UploaderChannel":"Lunar Example",120);check(LyricsLookup.select(sample,query)==sample,"Generic metadata layout failed: "+title);
        }
        check(LyricsLookup.select(sample,song("Northern Signl","Lunar Example",120))==sample,"Small title typo rejected");check(LyricsLookup.select(sample,song("Northern Signal","Wrong Performer",120))==null,"Wrong artist accepted");
        check(LyricsLookup.select(sample,song("Northern Signal 2","Lunar Example",120))==null,"Numbered sequel confused");
        JsonObject russian=item("Письмо домой","Тестовая группа",120,"[00:01]Тестовая строка",null);check(LyricsLookup.select(russian,song("Pismo domoi","Testovaya gruppa",120))==russian,"Generic Cyrillic/Latin normalization failed");
        JsonObject plain=item("Northern Signal","Lunar Example",120,null,"First synthetic fixture\n\n[Chorus]\nSecond synthetic fixture");
        AtomicInteger requests=new AtomicInteger();LyricsResolver resolver=new LyricsResolver(url->{requests.incrementAndGet();return plain;});
        LyricsResolver.Outcome result=resolver.resolve(song("Northern Signal","Lunar Example",120),()->true,ignored->{});check(result.record!=null&&"true".equals(result.record.get("phantomEstimated").getAsString()),"Plain lyrics discarded");
        LyricTimeline estimated=LyricTimeline.estimate(result.record.get("plainLyrics").getAsString(),120);check(estimated.approximate&&estimated.size()==2&&estimated.line(0).text.equals("First synthetic fixture"),"Plain text lost/faked timing flag");check(estimated.line(1).time>0&&estimated.line(1).time<120,"Estimated timing unbounded");
        AtomicInteger primary=new AtomicInteger(),secondary=new AtomicInteger();LyricsResolver fallback=new LyricsResolver(url->{if(url.contains("lrclib.net")){primary.incrementAndGet();throw new LyricsResolver.ServiceFailure("busy",30000);}secondary.incrementAndGet();JsonObject o=new JsonObject();o.addProperty("lyrics","Independent synthetic fixture\nSecond fixture");return o;});
        result=fallback.resolve(song("Northern Signal","Lunar Example",120),()->true,ignored->{});check(result.record!=null&&result.record.get("phantomSource").getAsString().equals("lyrics.ovh"),"Independent provider not used");check(primary.get()==1&&secondary.get()==1,"Rate-limited provider hammered");
        JsonObject instrumental=item("Quiet Orbit","Lunar Example",120,null,null);instrumental.addProperty("instrumental",true);result=new LyricsResolver(url->instrumental).resolve(song("Quiet Orbit","Lunar Example",120),()->true,ignored->{});check(result.record!=null&&result.record.get("instrumental").getAsBoolean(),"Explicit instrumental mistaken for missing lyrics");
        AtomicInteger cancellation=new AtomicInteger();try{new LyricsResolver(url->{cancellation.incrementAndGet();return JsonNull.INSTANCE;}).resolve(song("A","B",100),()->false,ignored->{});throw new AssertionError("Cancelled search ran");}catch(InterruptedIOException expected){}check(cancellation.get()==0,"Cancelled request sent");
        AtomicInteger ordering=new AtomicInteger();result=new LyricsResolver(url->ordering.getAndIncrement()==0?plain:sample).resolve(song("Northern Signal","Lunar Example",120),()->true,ignored->{});check(!result.record.get("phantomEstimated").getAsBoolean(),"Plain result blocks later synced upgrade");
        Path root=Files.createTempDirectory("phantom-lyrics-pipeline-");LyricsManager manager=new LyricsManager(root,new LyricsResolver(url->plain));MediaSnapshot original=song("Lunar Example - Northern Signal","UploadChannel",120);try{manager.request(original,true);await(manager);check(manager.result().timeline.approximate&&manager.result().timeline.hasWords(),"Manager dropped plain result");}finally{manager.close();}
        AtomicInteger unexpected=new AtomicInteger();manager=new LyricsManager(root,new LyricsResolver(url->{unexpected.incrementAndGet();throw new IOException("network must not be needed");}));try{MediaSnapshot direct=song("Northern Signal","Lunar Example",120);manager.request(direct,false);await(manager);check(manager.result().timeline.hasWords()&&unexpected.get()==0,"Validated shared offline cache not reused");}finally{manager.close();}
        // Stale epochs cannot publish an old song after rapid skips, even if a transport ignores interruption.
        CountDownLatch started=new CountDownLatch(1);LyricsResolver slow=new LyricsResolver(url->{if(url.contains("old")){started.countDown();try{Thread.sleep(80);}catch(InterruptedException ignored){}return item("Old","Artist",120,"[00:01]Old synthetic fixture",null);}return item("New","Artist",120,"[00:01]New synthetic fixture",null);});
        manager=new LyricsManager(Files.createTempDirectory("phantom-lyrics-epoch-"),slow);try{manager.request(song("Old","Artist",120),true);check(started.await(1,TimeUnit.SECONDS),"Old query never started");MediaSnapshot next=song("New","Artist",120);manager.request(next,true);await(manager);check(manager.result().song.equals(next.song())&&manager.result().timeline.line(0).text.equals("New synthetic fixture"),"Stale song replaced active lyrics");}finally{manager.close();}
        System.out.println("PASS pipeline: generic/reversed/uploader metadata, fuzzy safeguards, Cyrillic transliteration, plain fallback labeling, independent provider, Retry-After circuit, cancellation, synced upgrade, offline shared cache and stale epochs");
    }
    static void live()throws Exception{
        // Coverage audit only. These records are test input, never compiled into the client lookup.
        MediaSnapshot[] songs={song("Кино - Хочу перемен","Alexander Pintz",293),song("Спокойная ночь","Кино",383),song("+emoslut666 specialsadism","voidvoice717",102),song("Hello","Adele",295),song("Get Lucky","Daft Punk",369),song("Blinding Lights","The Weeknd",200)};
        int synced=0,plain=0,missing=0,busy=0;LyricsResolver resolver=new LyricsResolver();
        for(MediaSnapshot song:songs){LyricsResolver.Outcome result=resolver.resolve(song,()->true,ignored->{});String state;if(result.record==null){if(result.unavailable){busy++;state="service unavailable";}else{missing++;state="no match";}}else if(result.record.get("phantomEstimated").getAsBoolean()){plain++;state="plain/estimated";}else{synced++;state="synced";}System.out.println("AUDIT "+song.artist+" / "+song.title+": "+state);}
        System.out.println("AUDIT TOTAL synced="+synced+" plain="+plain+" missing="+missing+" service-errors="+busy+"; no lyric text logged");
    }
    public static void main(String[] args)throws Exception{if(args.length>0&&args[0].equals("live"))live();else cpu();}
}
