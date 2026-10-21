package keystrokesmod.client.module.modules.render.island;

import com.google.gson.*;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
import java.awt.image.BufferedImage;import java.awt.Graphics2D;import java.awt.RenderingHints;import javax.imageio.*;import javax.imageio.stream.ImageInputStream;

/** OS adapters share arbitration, clock resync and bounded artwork decoding. Never called by render. */
public final class MediaSessionManager implements AutoCloseable {
    private volatile MediaSnapshot snapshot=MediaSnapshot.EMPTY;private volatile boolean running;
    private volatile Process process;private Thread worker;private long selectedAt;
    private final Map<String,MediaSnapshot> artCache=new LinkedHashMap<String,MediaSnapshot>(16,.75F,true){protected boolean removeEldestEntry(Map.Entry<String,MediaSnapshot> e){return size()>16;}};
    private final Map<String,MediaSnapshot> sessions=new HashMap<>();private final Map<String,Long> activity=new HashMap<>();
    private final RiffBridge riff;
    public MediaSessionManager(){this(null);}
    public MediaSessionManager(Path root){riff=root==null?null:new RiffBridge(root);}
    public MediaSnapshot snapshot(){MediaSnapshot current=snapshot;if(current.present()&&System.nanoTime()-current.updated>10000000000L)current=MediaSnapshot.EMPTY;MediaSnapshot direct=riff==null?MediaSnapshot.EMPTY:riff.snapshot();return direct.present()&&(direct.playing||!current.playing)?direct:current;}
    private static ProcessBuilder.Redirect discard(){return ProcessBuilder.Redirect.to(new File(System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")?"NUL":"/dev/null"));}
    public synchronized void start(){if(running)return;running=true;if(riff!=null)riff.start();worker=new Thread(this::run,"Phantom-media");worker.setDaemon(true);worker.start();}
    public synchronized void close(){running=false;if(riff!=null)riff.close();Process p=process;if(p!=null)p.destroy();if(worker!=null)worker.interrupt();snapshot=MediaSnapshot.EMPTY;}
    private void run(){
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);
        while(running)try{if(os.contains("win"))windows();else if(os.contains("linux"))linux();else{IslandLog.once("Media platform unsupported",null);return;}}
        catch(Exception failure){if(running){IslandLog.once("OS media adapter unavailable; notifications still work",failure);snapshot=MediaSnapshot.EMPTY;try{Thread.sleep(5000);}catch(InterruptedException ignored){return;}}}
    }
    private void windows()throws Exception{
        Path script=Files.createTempFile("phantom-media-",".ps1");
        try(InputStream resource=MediaSessionManager.class.getResourceAsStream("/assets/phantom/island/windows-media.ps1")){
            if(resource==null)throw new IOException("Bundled media helper missing");Files.copy(resource,script,StandardCopyOption.REPLACE_EXISTING);
        }
        try{
            Process p=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-ExecutionPolicy","Bypass","-File",script.toString()).redirectError(discard()).start();process=p;
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream(),StandardCharsets.UTF_8))){String line;while(running&&(line=reader.readLine())!=null){if(line.length()>2000000)continue;try{JsonObject root=new JsonParser().parse(line).getAsJsonObject();if(root.has("error"))IslandLog.once(root.get("error").getAsString(),null);accept(root.getAsJsonArray("sessions"));}catch(RuntimeException invalid){IslandLog.once("Invalid media helper response",null);}}}
            if(running)throw new IOException("Media helper exited");
        }finally{Process p=process;if(p!=null)p.destroy();process=null;Files.deleteIfExists(script);}
    }
    private String command(String... args)throws Exception{
        Process p=new ProcessBuilder(args).redirectError(discard()).start();process=p;
        try{if(!p.waitFor(1200,TimeUnit.MILLISECONDS)){p.destroyForcibly();throw new IOException("playerctl timeout");}if(p.exitValue()!=0)return "";
            byte[] bytes=new byte[65536];int n=p.getInputStream().read(bytes);return n<=0?"":new String(bytes,0,n,StandardCharsets.UTF_8).trim();
        }finally{p.destroy();process=null;}
    }
    private void linux()throws Exception{
        while(running){JsonArray sessions=new JsonArray();String players=command("playerctl","--list-all");int count=0;
            for(String player:players.split("\\r?\\n")){if(player.isEmpty()||count++>=12)continue;
                String data=command("playerctl","--player="+player,"metadata","--format","{{xesam:title}}\u001f{{xesam:artist}}\u001f{{xesam:album}}\u001f{{mpris:length}}\u001f{{position}}\u001f{{status}}\u001f{{mpris:artUrl}}\u001f{{mpris:trackid}}");String[] parts=data.split("\u001f",-1);if(parts.length<8||parts[0].isEmpty()||parts[5].equalsIgnoreCase("Stopped"))continue;
                JsonObject o=new JsonObject();o.addProperty("session",player);o.addProperty("source",player);o.addProperty("title",parts[0]);o.addProperty("artist",parts[1]);o.addProperty("album",parts[2]);o.addProperty("duration",number(parts[3])/1e6);o.addProperty("position",number(parts[4])/1e6);o.addProperty("playing",parts[5].equalsIgnoreCase("Playing"));o.addProperty("artworkUrl",parts[6]);o.addProperty("id",parts[7]);sessions.add(o);
            }accept(sessions);Thread.sleep(1000);
        }
    }
    private static double number(String v){try{return Double.parseDouble(v);}catch(Exception ignored){return 0;}}
    private static String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    private static double num(JsonObject o,String key,double fallback){try{double n=o.get(key).getAsDouble();return Double.isFinite(n)?n:fallback;}catch(Exception ignored){return fallback;}}
    public static String provider(String source){String s=source.toLowerCase(Locale.ROOT);if(s.contains("riff"))return "Riff";if(s.contains("spotify"))return "Spotify";if(s.contains("soundcloud"))return "SoundCloud";if(s.contains("youtube music")||s.contains("ytmusic"))return "YouTube Music";if(s.contains("youtube"))return "YouTube";return source;}
    public void accept(JsonArray array){
        long now=System.nanoTime();MediaSnapshot old=snapshot,best=null,current=null;Set<String> seen=new HashSet<>();
        if(array!=null)for(JsonElement element:array)try{
            JsonObject o=element.getAsJsonObject();String title=str(o,"title"),source=str(o,"source"),id=str(o,"id");if(title.isEmpty())continue;
            String artKey=str(o,"session")+"|"+str(o,"artist")+"|"+title;
            MediaSnapshot cached=artCache.get(artKey);int[] pixels=cached==null?null:cached.artwork;int accent=cached==null?0xFF9B8CFF:cached.accent;
            if(cached==null||pixels==null&&o.has("artworkBase64")){try{BufferedImage art=decode(o);if(art!=null){pixels=art.getRGB(0,0,64,64,null,0,64);long r=0,g=0,b=0;for(int pixel:pixels){r+=pixel>>16&255;g+=pixel>>8&255;b+=pixel&255;}accent=0xFF000000|(int)(r/4096)<<16|(int)(g/4096)<<8|(int)(b/4096);}}catch(Exception badArt){IslandLog.once("Album art unavailable; retaining track metadata",null);}}
            boolean playing=o.has("playing")&&o.get("playing").getAsBoolean();double position=num(o,"position",0);
            // Preserve the actual report: render-thread PlaybackClock smooths noise without
            // losing seek/pause/buffering information by replacing it with an old estimate.
            boolean buffering=o.has("buffering")&&o.get("buffering").getAsBoolean();
            MediaSnapshot item=new MediaSnapshot(str(o,"session"),provider(source),title,str(o,"artist"),str(o,"album"),id,num(o,"duration",0),position,playing,num(o,"rate",1),now,pixels,accent,(long)num(o,"revision",0),buffering);
            MediaSnapshot previous=sessions.put(item.session,item);seen.add(item.session);
            if(previous==null||!previous.playing&&item.playing||!previous.id.equals(item.id)||!previous.title.equals(item.title))activity.put(item.session,now);
            artCache.put(artKey,item);if(item.session.equals(old.session))current=item;
            if(best==null||item.playing&&!best.playing||item.playing==best.playing&&activity.getOrDefault(item.session,0L)>activity.getOrDefault(best.session,0L))best=item;
        }catch(Exception failure){IslandLog.once("Media metadata/artwork rejected",null);}
        sessions.keySet().retainAll(seen);activity.keySet().retainAll(seen);
        if(current!=null&&(best==null||!best.playing||current.playing&&(now-selectedAt<1500000000L||activity.getOrDefault(current.session,0L)>=activity.getOrDefault(best.session,0L))))best=current;
        if(best==null){snapshot=MediaSnapshot.EMPTY;return;}if(!best.session.equals(old.session))selectedAt=now;snapshot=best;
    }
    private static BufferedImage decode(JsonObject o)throws Exception{
        byte[] bytes=null;String b64=str(o,"artworkBase64"),url=str(o,"artworkUrl");
        if(!b64.isEmpty()&&b64.length()<1500000)bytes=Base64.getDecoder().decode(b64);
        else if(!url.isEmpty())bytes=IslandIO.artwork(url);
        if(bytes==null)return null;
        try(ImageInputStream input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){Iterator<ImageReader> readers=ImageIO.getImageReaders(input);if(!readers.hasNext())return null;ImageReader reader=readers.next();try{reader.setInput(input);if((long)reader.getWidth(0)*reader.getHeight(0)>16000000)return null;BufferedImage image=reader.read(0),small=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=small.createGraphics();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,0,0,64,64,null);}finally{g.dispose();}return small;}finally{reader.dispose();}}
    }
}
