package keystrokesmod.client.module.modules.render.island;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** One worker, epoch cancellation, validated positive caches and independent provider fallback. */
public final class LyricsManager implements AutoCloseable {
    public static final class Result {
        public final String song,source,label;public final LyricTimeline timeline;public final double offset;public final boolean ready,unavailable,instrumental;
        Result(String s,LyricTimeline t,double o){this(s,t,o,true,false,"");}
        Result(String s,LyricTimeline t,double o,boolean ready){this(s,t,o,ready,false,"");}
        Result(String s,LyricTimeline t,double o,boolean ready,boolean unavailable){this(s,t,o,ready,unavailable,"");}
        Result(String s,LyricTimeline t,double o,boolean ready,boolean unavailable,String source){this(s,t,o,ready,unavailable,source,false);}
        Result(String s,LyricTimeline t,double o,boolean ready,boolean unavailable,String source,boolean instrumental){song=s;timeline=t;offset=o;this.ready=ready;this.unavailable=unavailable;this.source=source;this.instrumental=instrumental;label=source+(t.approximate?" · estimated":"");}
    }
    private final Path root;private final ExecutorService worker;private final LyricsResolver resolver;
    private volatile Result result=new Result("",LyricTimeline.EMPTY,0);private volatile String requested="";private volatile boolean closed;private volatile long epoch;
    private Future<?> pending;
    private final Map<String,JsonObject> cache=new LinkedHashMap<String,JsonObject>(32,.75F,true){protected boolean removeEldestEntry(Map.Entry<String,JsonObject> e){return size()>64;}};
    private final Properties offsets=new Properties();private boolean offsetsLoaded;private final ConcurrentMap<String,Double> overrides=new ConcurrentHashMap<>();
    private volatile long retryAt;private int failures;
    public LyricsManager(Path root){this(root,new LyricsResolver());}
    public LyricsManager(Path root,LyricsResolver resolver){this.root=root;this.resolver=resolver;worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Phantom-lyrics");t.setDaemon(true);return t;});}
    public Result result(){return result;}
    public synchronized void request(MediaSnapshot song,boolean online){
        String identity=song.present()?song.song():"",key=identity+"|"+online;
        if(closed||requested.equals(key)&&(retryAt==0||System.nanoTime()<retryAt))return;
        boolean same=requested.equals(key);requested=key;retryAt=0;long search=++epoch;if(!same)failures=0;if(pending!=null)pending.cancel(true);
        Result prior=result;result=same&&prior.timeline.hasWords()?new Result(identity,prior.timeline,prior.offset,false,false,prior.source):new Result(identity,LyricTimeline.EMPTY,0,false);
        if(identity.isEmpty()){result=new Result("",LyricTimeline.EMPTY,0);return;}
        pending=worker.submit(()->run(song,online,key,search));
    }
    private boolean active(String key,long search){return !closed&&epoch==search&&requested.equals(key)&&!Thread.currentThread().isInterrupted();}
    private static boolean usable(JsonObject item,MediaSnapshot song){return Double.isFinite(LyricMatching.score(item,song,false))&&(!LyricsResolver.plain(item).trim().isEmpty()||"true".equals(LyricMatching.field(item,"instrumental"))&&Double.isFinite(LyricMatching.score(item,song,true)));}
    private static LyricTimeline timeline(JsonObject item,MediaSnapshot song){
        LyricTimeline timed=LyricTimeline.parse(LyricMatching.field(item,"syncedLyrics"));
        boolean estimated="true".equals(LyricMatching.field(item,"phantomEstimated"))||!Double.isFinite(LyricMatching.score(item,song,true));
        return !estimated&&timed.hasWords()?timed:LyricTimeline.estimate(LyricsResolver.plain(item),song.duration);
    }
    private Result doc(MediaSnapshot song,JsonObject item,double offset){String source=LyricMatching.field(item,"phantomSource");return new Result(song.song(),timeline(item,song),offset,true,false,source.isEmpty()?"LRCLIB":source,"true".equals(LyricMatching.field(item,"instrumental")));}
    private void run(MediaSnapshot song,boolean online,String key,long search){
        if(!active(key,search))return;
        try{
            loadOffsets();double stored=0;try{stored=Double.parseDouble(offsets.getProperty(IslandIO.sha(song.song()),"0"));}catch(NumberFormatException ignored){}
            final double offset=Math.max(-30,Math.min(30,overrides.getOrDefault(song.song(),stored)));
            Path lyrics=root.resolve("lyrics"),local=lyrics.resolve(IslandIO.safeName(song.artist+" - "+song.title)+".lrc").normalize();
            if(local.getParent().equals(lyrics.normalize())&&Files.isRegularFile(local)&&Files.size(local)<=2000000){LyricTimeline parsed=LyricTimeline.parse(new String(Files.readAllBytes(local),StandardCharsets.UTF_8));if(parsed.hasWords()){publish(key,search,new Result(song.song(),parsed,offset,true,false,"Local"));return;}}
            JsonObject hit=cache.get(key);if(hit==null)hit=readCache(song);
            if(hit!=null){cache.put(key,hit);Result cached=doc(song,hit,offset);publish(key,search,cached);if(!online||!cached.timeline.approximate)return;}
            if(!online){publish(key,search,new Result(song.song(),LyricTimeline.EMPTY,offset));return;}
            LyricsResolver.Outcome found=resolver.resolve(song,()->active(key,search),item->{if(active(key,search)){cache.put(key,item);writeCache(song,item);publish(key,search,doc(song,item,offset));}});
            if(!active(key,search))return;
            if(found.record!=null){cache.put(key,found.record);writeCache(song,found.record);Result resolved=doc(song,found.record,offset);publish(key,search,resolved);failures=0;if(resolved.timeline.approximate)retryAt=System.nanoTime()+300000000000L;}
            else if(result.song.equals(song.song())&&result.timeline.hasWords()){retryAt=System.nanoTime()+60000000000L;}
            else{publish(key,search,new Result(song.song(),LyricTimeline.EMPTY,offset,true,found.unavailable));retryAt=System.nanoTime()+(long)((found.unavailable?Math.min(60,5*Math.pow(2,Math.min(4,failures++))):60)*1000000000L);}
        }catch(InterruptedIOException|CancellationException stale){/* A skip is not a provider error. */}
        catch(Exception failure){if(active(key,search)){Result prior=result;if(!prior.song.equals(song.song())||!prior.timeline.hasWords())result=new Result(song.song(),LyricTimeline.EMPTY,0,true,true);retryAt=System.nanoTime()+10000000000L;IslandLog.once("Lyrics lookup failed; retrying without clearing available lyrics",failure);}}
    }
    private synchronized void publish(String key,long search,Result value){if(active(key,search)){Double override=overrides.get(value.song);result=override==null?value:new Result(value.song,value.timeline,override,value.ready,value.unavailable,value.source,value.instrumental);}}
    private JsonObject read(Path path,MediaSnapshot song)throws IOException{
        if(!Files.isRegularFile(path)||Files.size(path)>2000000)return null;try{JsonObject item=new JsonParser().parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject();return usable(item,song)?item:null;}catch(RuntimeException invalid){return null;}
    }
    private String sharedKey(LyricMatching.Query q,MediaSnapshot song){return IslandIO.sha(LyricMatching.canonical(LyricMatching.core(q.title))+"|"+LyricMatching.canonical(q.artist)+"|"+Math.round(song.duration/2)*2);}
    private JsonObject readCache(MediaSnapshot song)throws IOException{
        Path folder=root.resolve("lyrics-cache");JsonObject item=read(folder.resolve(IslandIO.sha(song.song())+".json"),song);if(item!=null)return item;
        for(LyricMatching.Query q:LyricMatching.queries(song)){item=read(folder.resolve("shared-"+sharedKey(q,song)+".json"),song);if(item!=null)return item;}return null;
    }
    private void writeCache(MediaSnapshot song,JsonObject item){
        byte[] bytes=item.toString().getBytes(StandardCharsets.UTF_8);try{Path folder=root.resolve("lyrics-cache");IslandIO.atomic(folder.resolve(IslandIO.sha(song.song())+".json"),bytes);
            for(LyricMatching.Query q:LyricMatching.queries(song)){MediaSnapshot hypothesis=new MediaSnapshot("","",q.title,q.artist,"","",song.duration,0,false,1,0,null,0);if(Double.isFinite(LyricMatching.score(item,hypothesis,false)))IslandIO.atomic(folder.resolve("shared-"+sharedKey(q,song)+".json"),bytes);}
        }catch(IOException failure){IslandLog.once("Lyrics available but cache could not be saved",failure);}
    }
    public void offset(String song,double seconds){
        if(closed||song.isEmpty())return;double safe=Math.max(-30,Math.min(30,seconds));overrides.put(song,safe);Result old=result;if(old.song.equals(song))result=new Result(song,old.timeline,safe,old.ready,old.unavailable,old.source,old.instrumental);
        worker.execute(()->{try{loadOffsets();offsets.setProperty(IslandIO.sha(song),Double.toString(safe));ByteArrayOutputStream bytes=new ByteArrayOutputStream();offsets.store(bytes,"Phantom per-song lyric offsets (seconds)");IslandIO.atomic(root.resolve("island-song-offsets.properties"),bytes.toByteArray());}catch(IOException failure){IslandLog.once("Cannot save lyric offsets",failure);}});
    }
    private void loadOffsets()throws IOException{if(offsetsLoaded)return;Path path=root.resolve("island-song-offsets.properties");if(Files.isRegularFile(path))try(InputStream in=Files.newInputStream(path)){offsets.load(in);}offsetsLoaded=true;}
    public synchronized void close(){closed=true;requested="";epoch++;if(pending!=null)pending.cancel(true);worker.shutdownNow();result=new Result("",LyricTimeline.EMPTY,0);}
}
