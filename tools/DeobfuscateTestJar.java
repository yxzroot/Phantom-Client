import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.commons.*;

/** Test runtime only: let real installed client classes run against the MCP Forge test JAR. */
public final class DeobfuscateTestJar {
    public static void main(String[] args)throws Exception{
        Map<String,String> names=new HashMap<>();for(String line:Files.readAllLines(Path.of(args[1]))){String[] p=line.split(" ");if(p.length==3&&p[0].equals("FD:"))names.put(p[2].substring(p[2].lastIndexOf('/')+1),p[1].substring(p[1].lastIndexOf('/')+1));else if(p.length==5&&p[0].equals("MD:"))names.put(p[3].substring(p[3].lastIndexOf('/')+1),p[1].substring(p[1].lastIndexOf('/')+1));}
        Remapper remap=new Remapper(){public String mapFieldName(String owner,String name,String desc){return names.getOrDefault(name,name);}public String mapMethodName(String owner,String name,String desc){return names.getOrDefault(name,name);}};
        Path output=Path.of(args[2]);if(Files.exists(output))throw new IllegalStateException("Test output already exists");
        try(ZipFile input=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){for(Enumeration<? extends ZipEntry> it=input.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();byte[] bytes;try(java.io.InputStream in=input.getInputStream(e)){bytes=in.readAllBytes();}if(e.getName().endsWith(".class")){ClassWriter writer=new ClassWriter(0);new ClassReader(bytes).accept(new ClassRemapper(writer,remap),0);bytes=writer.toByteArray();}out.putNextEntry(new ZipEntry(e.getName()));out.write(bytes);out.closeEntry();}}
    }
}
