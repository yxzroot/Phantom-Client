import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.nio.charset.StandardCharsets;
import com.google.gson.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Emergency Lunar compatibility rollback: remove only AspectRatio registration and mixin. */
public final class DisableAspectJar implements Opcodes {
    static byte[] read(ZipFile z,ZipEntry e)throws Exception{try(java.io.InputStream in=z.getInputStream(e)){return in.readAllBytes();}}
    public static void main(String[] a)throws Exception{
        Path output=Path.of(a[1]);if(Files.exists(output))throw new IllegalStateException("Output exists");int hooks=0,modules=0;
        try(ZipFile base=new ZipFile(a[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
            for(Enumeration<? extends ZipEntry> es=base.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();byte[] data=read(base,e);
                if(e.getName().equals("mixins.raindots.json")){JsonObject json=new JsonParser().parse(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();JsonArray old=json.getAsJsonArray("client"),clean=new JsonArray();for(JsonElement v:old){if(v.getAsString().equals("MixinAspectRatio"))hooks++;else clean.add(v);}json.add("client",clean);data=json.toString().getBytes(StandardCharsets.UTF_8);}
                if(e.getName().equals("keystrokesmod/client/module/ModuleManager.class")){
                    ClassNode n=new ClassNode();new ClassReader(data).accept(n,0);
                    for(MethodNode m:n.methods)if(m.name.equals("<init>"))for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof TypeInsnNode&&i.getOpcode()==NEW&&((TypeInsnNode)i).desc.equals("keystrokesmod/client/module/modules/render/AspectRatio")){
                        AbstractInsnNode start=i.getPrevious(),dup=i.getNext(),init=dup.getNext(),add=init.getNext();
                        if(start.getOpcode()!=ALOAD||dup.getOpcode()!=DUP||init.getOpcode()!=INVOKESPECIAL||!(add instanceof MethodInsnNode)||!((MethodInsnNode)add).name.equals("addModule"))throw new IllegalStateException("Unexpected registry");
                        m.instructions.remove(start);m.instructions.remove(i);m.instructions.remove(dup);m.instructions.remove(init);m.instructions.remove(add);modules++;
                    }
                    ClassWriter w=new ClassWriter(0);n.accept(w);data=w.toByteArray();
                }
                ZipEntry copy=new ZipEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);out.write(data);out.closeEntry();
            }
        }
        if(hooks!=1||modules!=1)throw new IllegalStateException("Expected one hook and one module: "+hooks+"/"+modules);
        try(ZipFile base=new ZipFile(a[0]);ZipFile fixed=new ZipFile(output.toFile())){for(Enumeration<? extends ZipEntry> es=base.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();if(e.getName().equals("mixins.raindots.json")||e.getName().equals("keystrokesmod/client/module/ModuleManager.class"))continue;if(!Arrays.equals(read(base,e),read(fixed,fixed.getEntry(e.getName()))))throw new IllegalStateException("Unrelated change");}}
        System.out.println("PASS: incompatible AspectRatio hook and module registration removed; all other entries identical");
    }
}
