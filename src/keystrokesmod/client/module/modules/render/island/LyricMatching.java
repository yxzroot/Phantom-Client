package keystrokesmod.client.module.modules.render.island;

import com.google.gson.*;
import java.util.*;
import java.text.Normalizer;
import static keystrokesmod.client.module.modules.render.island.LyricsLookup.searchTitle;
import static keystrokesmod.client.module.modules.render.island.LyricsLookup.searchArtist;

/** Worker-only metadata hypotheses/ranking. No song or artist allowlists. */
public final class LyricMatching {
    private LyricMatching(){}
    public static String field(JsonObject o,String key){try{return o.get(key).isJsonNull()?"":o.get(key).getAsString();}catch(RuntimeException ignored){return "";}}
    private static final String[] CYR={"a","b","v","g","d","e","zh","z","i","i","k","l","m","n","o","p","r","s","t","u","f","kh","ts","ch","sh","shch","","y","","e","yu","ya"};
    public static String canonical(String text){
        String s=Normalizer.normalize(MediaSnapshot.normalize(text),Normalizer.Form.NFKD).replaceAll("\\p{M}","");StringBuilder out=new StringBuilder();
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c>='а'&&c<='я')out.append(CYR[c-'а']);else if(c=='і'||c=='ї')out.append('i');else if(c=='є')out.append("ye");else if(c=='ґ')out.append('g');else if(c!='\''&&c!='’')out.append(c);}
        return out.toString().replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
    }
    public static String core(String s){return searchTitle(s).replaceAll("(?iu)\\s*[\\[(](?:slowed|sped[ -]?up|nightcore|reverb|remix|live|acoustic|remaster(?:ed)?)[^\\])]*[\\])]","").replaceAll("(?iu)\\s+[-–—]\\s+(?:slowed|sped[ -]?up|nightcore|reverb|remix|live|acoustic|remaster(?:ed)?).*","").trim();}
    private static String version(String s){String lower=searchTitle(s),base=core(s);return lower.equals(base)?"":canonical(lower.replace(base,""));}
    public static double similarity(String a,String b){
        if(a.length()>512||b.length()>512)return 0;
        a=canonical(a);b=canonical(b);if(a.equals(b))return 1;if(a.isEmpty()||b.isEmpty()||Math.min(a.length(),b.length())<6)return 0;
        if(!a.replaceAll("\\D","").equals(b.replaceAll("\\D","")))return 0;
        int[] row=new int[b.length()+1];for(int j=0;j<row.length;j++)row[j]=j;
        for(int i=1;i<=a.length();i++){int diagonal=row[0];row[0]=i;for(int j=1;j<=b.length();j++){int old=row[j];row[j]=Math.min(Math.min(row[j]+1,row[j-1]+1),diagonal+(a.charAt(i-1)==b.charAt(j-1)?0:1));diagonal=old;}}
        return 1-row[b.length()]/(double)Math.max(a.length(),b.length());
    }
    public static final class Query {public final String title,artist;Query(String t,String a){title=t;artist=a;}}
    private static void add(List<Query> out,String title,String artist){title=searchTitle(title);artist=searchArtist(artist);if(title.isEmpty()||artist.isEmpty()||title.length()>250||artist.length()>160)return;for(Query q:out)if(canonical(q.title).equals(canonical(title))&&canonical(q.artist).equals(canonical(artist)))return;out.add(new Query(title,artist));}
    public static List<Query> queries(MediaSnapshot song){
        List<Query> out=new ArrayList<>();String[] parts=searchTitle(song.title).split("\\s+[-–—]\\s+",2);
        boolean embedded=parts.length==2&&parts[0].length()<=80&&!parts[1].matches("(?iu)(?:live|remix|slowed|sped up|acoustic|remaster(?:ed)?)(?:\\s.*)?");
        if(embedded)add(out,parts[1],parts[0]);add(out,song.title,song.artist);if(embedded)add(out,parts[0],parts[1]);
        java.util.regex.Matcher plus=java.util.regex.Pattern.compile("^\\+([^\\s+]+)\\s+(.+)$").matcher(song.title);if(plus.matches())add(out,plus.group(2),plus.group(1));return out;
    }
    public static String queryTitle(MediaSnapshot song){List<Query> q=queries(song);return q.isEmpty()?searchTitle(song.title):q.get(0).title;}
    public static String queryArtist(MediaSnapshot song){List<Query> q=queries(song);return q.isEmpty()?searchArtist(song.artist):q.get(0).artist;}
    private static double artist(String a,String b){
        if(canonical(a).equals(canonical(b)))return 1;double best=0;
        for(String x:a.split("(?iu)\\s+(?:feat\\.?|ft\\.?|featuring|&|x|and|и)\\s+|\\s*[,;/]\\s*"))for(String y:b.split("(?iu)\\s+(?:feat\\.?|ft\\.?|featuring|&|x|and|и)\\s+|\\s*[,;/]\\s*")){double s=similarity(searchArtist(x),searchArtist(y));if(s>=.92)best=Math.max(best,s);}return best;
    }
    public static double score(JsonObject item,MediaSnapshot song,boolean timed){
        String title=field(item,"trackName"),name=field(item,"artistName");double match=Double.POSITIVE_INFINITY;
        if(title.length()>512||name.length()>512)return Double.POSITIVE_INFINITY;
        for(Query q:queries(song)){double t=similarity(timed?searchTitle(title):core(title),timed?searchTitle(q.title):core(q.title)),a=artist(name,q.artist);
            if(t<.88||a<.92||timed&&!version(title).equals(version(q.title)))continue;match=Math.min(match,(1-t)*50+(1-a)*35);}
        if(!Double.isFinite(match))return match;double duration=0;try{duration=item.get("duration").getAsDouble();}catch(RuntimeException ignored){}
        double difference=song.duration>0&&Double.isFinite(duration)&&duration>0?Math.abs(duration-song.duration):0;
        if(difference>(timed?Math.max(4,song.duration*.04):Math.max(20,song.duration*.30)))return Double.POSITIVE_INFINITY;
        return match+difference*.25+(canonical(song.album).isEmpty()||canonical(field(item,"albumName")).equals(canonical(song.album))?0:1);
    }
    public static JsonObject select(JsonElement data,MediaSnapshot song,boolean timed){
        if(data==null||data.isJsonNull())return null;JsonArray list;if(data.isJsonArray())list=data.getAsJsonArray();else{list=new JsonArray();list.add(data);}JsonObject best=null;double score=Double.POSITIVE_INFINITY;
        for(JsonElement e:list){if(!e.isJsonObject())continue;JsonObject item=e.getAsJsonObject();String text=field(item,timed?"syncedLyrics":"plainLyrics");if(timed?LyricTimeline.parse(text).size()==0:text.trim().isEmpty())continue;double value=score(item,song,timed);if(value<score){best=item;score=value;}}return best;
    }
}
