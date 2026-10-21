import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.commons.ClassRemapper;import org.objectweb.asm.tree.*;import org.objectweb.asm.util.CheckClassAdapter;
/** Scoped picker replacement; module/config payloads are never rebuilt. */
public final class PatchSharedPickerJar {
    static final String OLD="keystrokesmod/client/clickgui/raven/components/ColorPickerScreen";
    static boolean remove(String n){return n.equals(OLD+".class")||n.equals("keystrokesmod/client/clickgui/raven/components/RGBComponent$Helping.class")||n.equals("keystrokesmod/client/clickgui/kv/components/KvRgbComponent$Helping.class");}
    static byte[] read(ZipFile z,ZipEntry e)throws Exception{try(java.io.InputStream in=z.getInputStream(e)){return in.readAllBytes();}}
    static boolean allowed(String n){return n.startsWith("keystrokesmod/client/clickgui/color/")||n.startsWith("keystrokesmod/client/module/setting/impl/RGBSetting")||n.equals("keystrokesmod/client/module/setting/impl/PickerRGBSetting.class")||n.matches("keystrokesmod/client/clickgui/(raven/components/(RGBComponent|PickerRGBComponent)|kv/components/KvRgbComponent|apple/settings/(ARgb|ApplePickerRGB)|apple/rebuild/(ColorRow|SettingsDrawer))\\.class");}
    public static void main(String[] a)throws Exception{
        Path output=Path.of(a[3]);if(Files.exists(output))throw new IllegalStateException("Output already exists");
        BuildLunarModuleJar.SrgRemapper remap=new BuildLunarModuleJar.SrgRemapper(Path.of(a[2]));Map<String,byte[]> replace=new TreeMap<>();
        try(java.util.stream.Stream<Path> files=Files.walk(Path.of(a[1]))){for(Path p:(Iterable<Path>)files.filter(p->p.toString().endsWith(".class"))::iterator){ClassReader reader=new ClassReader(Files.readAllBytes(p));String name=reader.getClassName()+".class";if(!allowed(name))throw new IllegalStateException("Unexpected class "+name);if(reader.readUnsignedShort(6)!=52)throw new IllegalStateException("Not Java 8");ClassWriter w=new ClassWriter(0);reader.accept(new ClassRemapper(w,remap),0);byte[] b=w.toByteArray();new ClassReader(b).accept(new CheckClassAdapter(new ClassWriter(0),true),0);replace.put(name,b);}}
        Set<String> changed=new HashSet<>(replace.keySet());int colors=0;
        try(ZipFile z=new ZipFile(a[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
            for(Enumeration<? extends ZipEntry> it=z.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(remove(e.getName()))continue;byte[] b=replace.remove(e.getName());if(b==null)b=read(z,e);out.putNextEntry(new ZipEntry(e.getName()));out.write(b);out.closeEntry();}
            for(Map.Entry<String,byte[]> e:replace.entrySet()){out.putNextEntry(new ZipEntry(e.getKey()));out.write(e.getValue());out.closeEntry();}
        }
        int untouched=0;try(ZipFile before=new ZipFile(a[0]);ZipFile after=new ZipFile(output.toFile())){
            for(Enumeration<? extends ZipEntry> it=after.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(!e.getName().endsWith(".class"))continue;ClassNode n=new ClassNode();new ClassReader(read(after,e)).accept(n,0);
                for(FieldNode f:n.fields)if(f.desc.equals("Lkeystrokesmod/client/module/setting/impl/RGBSetting;")||f.desc.equals("Lkeystrokesmod/client/module/setting/impl/PickerRGBSetting;")){if(n.name.contains("/module/modules/")){System.out.println("COLOR: "+n.name+"."+f.name);colors++;}}
                for(MethodNode m:n.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals(OLD))throw new IllegalStateException("Old picker still referenced: "+n.name);
            }
            if(after.getEntry(OLD+".class")!=null)throw new IllegalStateException("Old picker retained");
            for(Enumeration<? extends ZipEntry> it=before.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(changed.contains(e.getName())||remove(e.getName()))continue;if(!Arrays.equals(read(before,e),read(after,after.getEntry(e.getName()))))throw new IllegalStateException("Unrelated entry changed "+e.getName());untouched++;}
        }
        System.out.println("PASS: Java 8/dataflow; old picker and channel draggers removed; "+colors+" module color fields covered; "+untouched+" unrelated entries byte-identical");System.out.println(output.toAbsolutePath());
    }
}
