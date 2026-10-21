package keystrokesmod.client.module.modules.render.island;
/** Mutable render-thread snapshot of the normal Phantom settings. */
public final class IslandOptions {
    public enum Idle { Name,FPS,Clock,Nothing }
    public enum MusicMode { Always,SongChange,KeyPress }
    public Idle idle=Idle.Name;public MusicMode musicMode=MusicMode.Always;
    public float scale=1,y=8,maxWidth=360,textSize=1,speed=1,overshoot=.3F,eventSeconds=2.5F,lyricsDuration=.6F,marquee=20;
    public int accent=0xFF9B8CFF,lines=3;
    public boolean shadow=true,glow=false,matchArt=true,art=true,lyrics=true,karaoke=true,online=false,dots=true,progress=true,lyricsOnly;
}
