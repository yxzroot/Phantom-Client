package keystrokesmod.client.module.modules.render.island;
import java.io.*;import java.net.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;
/** Bounded, timeout-protected worker-only I/O. */
public final class IslandIO {
    private IslandIO(){}
    public static byte[] read(InputStream in,int maximum)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>maximum)throw new IOException("Response exceeds limit");out.write(buffer,0,n);}return out.toByteArray();}
    public static byte[] artwork(String uri)throws IOException{
        URI u;try{u=URI.create(uri);}catch(IllegalArgumentException invalid){return null;}
        if("file".equalsIgnoreCase(u.getScheme())){Path p=Paths.get(u);if(!Files.isRegularFile(p)||Files.size(p)>1048576)return null;return Files.readAllBytes(p);}
        if(!"https".equalsIgnoreCase(u.getScheme())&&!"http".equalsIgnoreCase(u.getScheme()))return null;
        HttpURLConnection connection=(HttpURLConnection)u.toURL().openConnection();connection.setConnectTimeout(2000);connection.setReadTimeout(2500);connection.setRequestProperty("User-Agent","Phantom-DynamicIsland/1.0");
        try{if(connection.getResponseCode()!=200)return null;try(InputStream in=connection.getInputStream()){return read(in,1048576);}}finally{connection.disconnect();}
    }
    public static String safeName(String s){String value=s==null?"":s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_").replace("..","_").trim();return value.substring(0,Math.min(150,value.length()));}
    public static String sha(String s){try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder result=new StringBuilder();for(byte b:digest)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return result.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public static void atomic(Path path,byte[] bytes)throws IOException{Files.createDirectories(path.getParent());Path temp=Files.createTempFile(path.getParent(),"island-",".tmp");try{Files.write(temp,bytes);try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}
}
