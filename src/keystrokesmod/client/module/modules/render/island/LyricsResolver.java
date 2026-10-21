package keystrokesmod.client.module.modules.render.island;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;

/** Bounded, cancellable provider pipeline. JSON/plain content is never logged. */
public final class LyricsResolver {
    public interface Transport {JsonElement get(String address)throws IOException;}
    public static final class ServiceFailure extends IOException {public final long retryMillis;public ServiceFailure(String message,long retry){super(message);retryMillis=retry;}}
    public static final class Outcome {public final JsonObject record;public final boolean unavailable;Outcome(JsonObject r,boolean u){record=r;unavailable=u;}}
    private final Transport transport;private final long[] blockedUntil=new long[2];
    public LyricsResolver(){this(LyricsResolver::network);}
    public LyricsResolver(Transport transport){this.transport=transport;}
    private static String enc(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(UnsupportedEncodingException e){throw new AssertionError(e);}}
    private JsonElement get(String address,int provider)throws IOException{
        if(System.nanoTime()<blockedUntil[provider])throw new ServiceFailure("Lyrics provider cooling down",1000);
        try{return transport.get(address);}catch(ServiceFailure e){blockedUntil[provider]=System.nanoTime()+Math.max(1000,Math.min(120000,e.retryMillis))*1000000L;throw e;}
        catch(IOException e){blockedUntil[provider]=System.nanoTime()+5000000000L;throw e;}
    }
    private static JsonObject copy(JsonObject item,String source){JsonObject result=new JsonParser().parse(item.toString()).getAsJsonObject();result.addProperty("phantomSource",source);return result;}
    public static String plain(JsonObject item){
        String text=LyricMatching.field(item,"plainLyrics");if(!text.trim().isEmpty())return text;
        LyricTimeline timeline=LyricTimeline.parse(LyricMatching.field(item,"syncedLyrics"));StringBuilder out=new StringBuilder();
        for(int i=0;i<timeline.size();i++){if(i>0)out.append('\n');out.append(timeline.line(i).text);}return out.toString();
    }
    private static JsonArray array(JsonElement data){if(data==null||data.isJsonNull())return new JsonArray();if(data.isJsonArray())return data.getAsJsonArray();JsonArray out=new JsonArray();if(data.isJsonObject())out.add(data);return out;}
    private static boolean timed(JsonObject item,MediaSnapshot song){return Double.isFinite(LyricMatching.score(item,song,true))&&LyricTimeline.parse(LyricMatching.field(item,"syncedLyrics")).hasWords();}
    public Outcome resolve(MediaSnapshot song,BooleanSupplier active,Consumer<JsonObject> progress)throws IOException{
        List<LyricMatching.Query> hypotheses=LyricMatching.queries(song);if(hypotheses.isEmpty())return new Outcome(null,false);
        long deadline=System.nanoTime()+18000000000L;List<String> urls=new ArrayList<>();Set<String> unique=new LinkedHashSet<>();
        LyricMatching.Query first=hypotheses.get(0);String primary="track_name="+enc(first.title)+"&artist_name="+enc(first.artist);
        String exact=primary+(song.album.isEmpty()?"":"&album_name="+enc(song.album))+(song.duration>0?"&duration="+Math.round(song.duration):"");
        unique.add("https://lrclib.net/api/get?"+exact);unique.add("https://lrclib.net/api/search?"+primary);unique.add("https://lrclib.net/api/search?q="+enc(first.title+" "+first.artist));
        for(int i=1;i<hypotheses.size()&&unique.size()<5;i++){LyricMatching.Query q=hypotheses.get(i);unique.add("https://lrclib.net/api/search?track_name="+enc(q.title)+"&artist_name="+enc(q.artist));}
        unique.add("https://lrclib.net/api/search?track_name="+enc(LyricMatching.core(first.title)));urls.addAll(unique);
        JsonObject bestPlain=null;double bestPlainScore=Double.POSITIVE_INFINITY;boolean unavailable=false;Set<String> published=new HashSet<>();
        for(String url:urls){
            if(!active.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Stale lyric search");if(System.nanoTime()>deadline)break;
            JsonElement data;try{data=get(url,0);}catch(IOException e){unavailable=true;continue;}
            JsonObject bestTimed=null,instrumental=null;double bestTimedScore=Double.POSITIVE_INFINITY;
            for(JsonElement value:array(data)){if(!value.isJsonObject())continue;JsonObject item=value.getAsJsonObject();
                double score=LyricMatching.score(item,song,true);if(timed(item,song)&&score<bestTimedScore){bestTimed=item;bestTimedScore=score;}
                if("true".equals(LyricMatching.field(item,"instrumental"))&&score<=1)instrumental=item;
                String text=plain(item);double p=LyricMatching.score(item,song,false);if(!text.trim().isEmpty()&&p<bestPlainScore){bestPlain=copy(item,"LRCLIB");bestPlain.addProperty("plainLyrics",text);bestPlain.addProperty("phantomEstimated",true);bestPlainScore=p;}
            }
            if(bestTimed!=null){JsonObject result=copy(bestTimed,"LRCLIB");result.addProperty("phantomEstimated",false);return new Outcome(result,false);}
            if(instrumental!=null&&bestPlain==null)return new Outcome(copy(instrumental,"LRCLIB"),false);
            if(bestPlain!=null&&published.add(bestPlain.toString()))progress.accept(bestPlain);
        }
        if(bestPlain!=null)return new Outcome(bestPlain,false);
        // Independent documented plain-lyrics API. No scraping, API keys or platform tokens.
        for(int i=0;i<Math.min(2,hypotheses.size());i++){
            if(!active.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Stale lyric search");
            LyricMatching.Query q=hypotheses.get(i);String title=LyricMatching.core(q.title);
            try{JsonElement response=get("https://api.lyrics.ovh/v1/"+enc(q.artist).replace("+","%20")+"/"+enc(title).replace("+","%20"),1);
                if(response!=null&&response.isJsonObject()){String text=LyricMatching.field(response.getAsJsonObject(),"lyrics");if(text.trim().length()>8){JsonObject item=new JsonObject();item.addProperty("trackName",title);item.addProperty("artistName",q.artist);item.addProperty("plainLyrics",text);item.addProperty("phantomEstimated",true);item.addProperty("phantomSource","lyrics.ovh");return new Outcome(item,false);}}
            }catch(IOException e){unavailable=true;}
        }
        return new Outcome(null,unavailable);
    }
    private static JsonElement network(String address)throws IOException{
        HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();connection.setConnectTimeout(2000);connection.setReadTimeout(3000);connection.setRequestProperty("User-Agent","Phantom-DynamicIsland/2.0 (personal cosmetic lyric display)");connection.setRequestProperty("Lrclib-Client","Phantom-DynamicIsland/2.0");
        try{int status=connection.getResponseCode();if(status==404)return JsonNull.INSTANCE;
            if(status==429||status==503){long delay=1000;try{delay=Math.max(1,Long.parseLong(connection.getHeaderField("Retry-After")))*1000;}catch(Exception ignored){}throw new ServiceFailure("Lyrics HTTP "+status,delay);}
            if(status!=200)throw new IOException("Lyrics HTTP "+status);
            try(InputStream in=connection.getInputStream()){try{return new JsonParser().parse(new String(IslandIO.read(in,2000000),StandardCharsets.UTF_8));}catch(RuntimeException bad){throw new IOException("Invalid lyrics response",bad);}}
        }finally{connection.disconnect();}
    }
}
