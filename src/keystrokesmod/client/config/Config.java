package keystrokesmod.client.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.ByteBuffer;
import java.nio.file.*;

/** Raven-compatible profile object; reads are side-effect free and writes are atomic UTF-8. */
public class Config {
    public final File file;
    public final long creationDate;
    public Config(File path){
        file=path;JsonObject data=getData();long created=0;
        try{if(data!=null&&data.has("creationTime"))created=data.get("creationTime").getAsLong();}catch(RuntimeException ignored){}
        creationDate=created>0?created:file.exists()&&file.lastModified()>0?file.lastModified():System.currentTimeMillis();
    }
    public String getName(){String name=file.getName();return name.toLowerCase(java.util.Locale.ROOT).endsWith(".bplus")?name.substring(0,name.length()-6):name;}
    public JsonObject getData(){
        if(!file.isFile()||file.length()==0||file.length()>4*1024*1024)return null;
        try{
            byte[] bytes=Files.readAllBytes(file.toPath());String text;
            if(bytes.length>=2&&(bytes[0]&255)==255&&(bytes[1]&255)==254)text=new String(bytes,StandardCharsets.UTF_16LE);
            else if(bytes.length>=2&&(bytes[0]&255)==254&&(bytes[1]&255)==255)text=new String(bytes,StandardCharsets.UTF_16BE);
            else try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
            catch(CharacterCodingException legacy){text=new String(bytes,Charset.forName("windows-1252"));}
            if(!text.isEmpty()&&text.charAt(0)==0xFEFF)text=text.substring(1);
            JsonElement data=new JsonParser().parse(text);return data.isJsonObject()?data.getAsJsonObject():null;
        }catch(IOException|RuntimeException ignored){return null;}
    }
    public void save(JsonObject data){
        if(data==null)throw new IllegalArgumentException("There are no settings to save.");
        data.addProperty("creationTime",creationDate);Path target=file.toPath().toAbsolutePath().normalize(),temp=null;
        try{
            Files.createDirectories(target.getParent());temp=Files.createTempFile(target.getParent(),".phantom-config-",".tmp");
            try(Writer writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){new GsonBuilder().setPrettyPrinting().create().toJson(data,writer);}
            if(Files.isRegularFile(target))Files.copy(target,target.resolveSibling(target.getFileName()+".previous"),StandardCopyOption.REPLACE_EXISTING);
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
            temp=null;
        }catch(IOException error){throw new IllegalStateException("Couldn't write the config. Check folder permissions and disk space.",error);}
        finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
    }
}
