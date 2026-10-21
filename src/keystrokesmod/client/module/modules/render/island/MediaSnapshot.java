package keystrokesmod.client.module.modules.render.island;

/** Published by a worker. Artwork pixels are exclusively owned, never modified after publication. */
public final class MediaSnapshot {
    public static final MediaSnapshot EMPTY=new MediaSnapshot("","","","","","",0,0,false,1,0,null,0xFF9B8CFF);
    public final String session,provider,title,artist,album,id;
    public final double duration,position,rate;public final boolean playing;public final long updated;
    public final int[] artwork;public final int accent;
    public final long revision;public final boolean buffering;
    private final String identity;
    public MediaSnapshot(String session,String provider,String title,String artist,String album,String id,double duration,double position,boolean playing,double rate,long updated,int[] artwork,int accent){
        this(session,provider,title,artist,album,id,duration,position,playing,rate,updated,artwork,accent,0,false);
    }
    public MediaSnapshot(String session,String provider,String title,String artist,String album,String id,double duration,double position,boolean playing,double rate,long updated,int[] artwork,int accent,long revision,boolean buffering){
        this.session=clean(session);this.provider=clean(provider);this.title=clean(title);this.artist=clean(artist);this.album=clean(album);this.id=clean(id);
        this.duration=finite(duration);this.position=finite(position);this.playing=playing;this.rate=Double.isFinite(rate)&&rate>0?Math.min(4,rate):1;this.updated=updated;this.artwork=artwork;this.accent=accent;
        identity=normalize(this.artist)+"|"+normalize(this.title)+"|"+Math.round(this.duration/2)*2;
        this.revision=revision;this.buffering=buffering;
    }
    public boolean present(){return !title.isEmpty();}
    public double at(long now){double p=position+(playing&&!buffering?Math.max(0,now-updated)/1e9*rate:0);return duration>0?Math.min(duration,p):p;}
    public String song(){return identity;}
    public static String normalize(String text){return clean(text).toLowerCase(java.util.Locale.ROOT).replaceAll("\\s*[\\[(](official\\s+(video|audio)|lyrics?|visualizer|hd|4k)[\\])]\\s*$","").replaceAll("\\s+(?:[-–|]\\s*)?(official\\s+(video|audio)|lyrics?|visualizer|hd|4k)\\s*$","").replaceAll("\\s+"," ").trim();}
    private static double finite(double d){return Double.isFinite(d)?Math.max(0,d):0;}
    private static String clean(String s){return s==null?"":s.replace('\u0000',' ').trim();}
}
