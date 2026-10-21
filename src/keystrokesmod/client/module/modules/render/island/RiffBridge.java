package keystrokesmod.client.module.modules.render.island;

import com.google.gson.*;import com.sun.net.httpserver.*;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.MessageDigest;import java.util.concurrent.*;

/** Optional authenticated loopback adapter for Riff's documented plugin API.
 * No Riff server credentials, browser storage, app patching or Discord dependency. */
public final class RiffBridge implements AutoCloseable {
    private final Path root;private final MediaSessionManager parser=new MediaSessionManager();
    private volatile boolean running;private volatile HttpServer server;private ExecutorService executor;private String token;
    public RiffBridge(Path root){this.root=root;}
    public MediaSnapshot snapshot(){return parser.snapshot();}
    public int port(){HttpServer s=server;return s==null?0:s.getAddress().getPort();}
    public synchronized void start(){
        if(running)return;running=true;Thread bootstrap=new Thread(this::open,"Phantom-Riff-bootstrap");bootstrap.setDaemon(true);bootstrap.start();
    }
    private void open(){
        try{
            Path path=root.resolve("island-riff-bridge.json");if(!Files.isRegularFile(path)){return;}
            if(Files.size(path)>4096)throw new IOException("Invalid Riff bridge configuration");
            JsonObject config=new JsonParser().parse(new String(Files.readAllBytes(path),StandardCharsets.UTF_8)).getAsJsonObject();String secret=config.get("token").getAsString();int port=config.get("port").getAsInt();
            if(!secret.matches("[a-fA-F0-9]{64}")||port<0||port>65535||port>0&&port<1024)throw new IOException("Invalid Riff bridge pairing");
            HttpServer created=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port),4);
            ExecutorService service=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Phantom-Riff-media");t.setDaemon(true);return t;});
            synchronized(this){if(!running){created.stop(0);service.shutdownNow();return;}token=secret;executor=service;created.setExecutor(service);created.createContext("/phantom-media",this::receive);created.start();server=created;}
        }catch(Exception failure){if(running)IslandLog.once("Riff companion unavailable; OS media reader remains active",failure);}
    }
    private void receive(HttpExchange request)throws IOException{
        int status=400;
        try{
            String origin=request.getRequestHeaders().getFirst("Origin");
            if(!request.getRemoteAddress().getAddress().isLoopbackAddress()||!request.getRequestURI().getPath().equals("/phantom-media")){status=403;return;}
            if(origin!=null&&!origin.equals("null")&&!origin.matches("https?://(127\\.0\\.0\\.1|localhost)(:[0-9]{1,5})?")){status=403;return;}
            if(origin!=null){request.getResponseHeaders().set("Access-Control-Allow-Origin",origin);request.getResponseHeaders().set("Vary","Origin");}
            request.getResponseHeaders().set("Cache-Control","no-store");
            if(request.getRequestMethod().equals("OPTIONS")){request.getResponseHeaders().set("Access-Control-Allow-Methods","POST, OPTIONS");request.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");request.getResponseHeaders().set("Access-Control-Allow-Private-Network","true");status=204;return;}
            if(!request.getRequestMethod().equals("POST")){status=405;return;}
            JsonObject body=new JsonParser().parse(new String(IslandIO.read(request.getRequestBody(),1500000),StandardCharsets.UTF_8)).getAsJsonObject();
            String supplied=body.has("token")?body.get("token").getAsString():"";
            if(supplied.length()!=64||!MessageDigest.isEqual(token.getBytes(StandardCharsets.US_ASCII),supplied.getBytes(StandardCharsets.US_ASCII))){status=403;return;}
            JsonArray sessions=body.getAsJsonArray("sessions");if(sessions==null||sessions.size()>1){status=400;return;}
            if(sessions.size()==1){JsonObject song=sessions.get(0).getAsJsonObject();song.addProperty("session","phantom-riff-companion");song.addProperty("source","Riff");}
            parser.accept(sessions);status=204;
        }catch(RuntimeException|IOException invalid){status=400;}
        finally{try{request.sendResponseHeaders(status,-1);}finally{request.close();}}
    }
    public synchronized void close(){running=false;HttpServer s=server;server=null;if(s!=null)s.stop(0);if(executor!=null)executor.shutdownNow();parser.close();}
}
