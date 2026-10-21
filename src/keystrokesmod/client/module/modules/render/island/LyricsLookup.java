package keystrokesmod.client.module.modules.render.island;

import com.google.gson.*;

/** Selects synced results by actual artist/title/duration, never just the first search hit. */
public final class LyricsLookup {
    private LyricsLookup(){}
    public static String searchTitle(String text){return MediaSnapshot.normalize(text).replaceFirst("^\\+[^\\s+]+\\s+","").replaceAll("(?iu)\\s*[\\[(](?:feat\\.?|ft\\.?)\\s+[^\\])]+[\\])]","").replaceAll("(?iu)\\s*[\\[(](?:official(?:\\s+(?:music|lyric))?\\s+(?:video|audio)|lyrics?|visuali[sz]er|текст(?: песни)?|клип|официальное видео|official)\\s*[\\])]","").trim();}
    public static String searchArtist(String text){return text.replaceAll("(?iu)\\s*[-–]\\s*topic$","").split("(?iu)\\s+(?:feat\\.?|ft\\.?|featuring|&|x)\\s+|\\s*[,;]\\s*",2)[0].trim();}
    public static String queryTitle(MediaSnapshot song){return LyricMatching.queryTitle(song);}
    public static String queryArtist(MediaSnapshot song){return LyricMatching.queryArtist(song);}
    public static JsonObject select(JsonElement data,MediaSnapshot song){
        return LyricMatching.select(data,song,true);
    }
}
