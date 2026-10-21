import java.nio.file.*;import java.util.*;import java.util.zip.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.commons.ClassRemapper;import org.objectweb.asm.util.CheckClassAdapter;

/** Narrow, repeatable registration/event bridge patch. No GUI, world or combat rewrites. */
public final class PatchIslandJar implements Opcodes {
    private static final String MODULE="keystrokesmod/client/module/modules/render/DynamicIsland",MANAGER="keystrokesmod/client/module/ModuleManager.class",NOTIFY="keystrokesmod/client/notifications/NotificationRenderer.class",QUEUE="keystrokesmod/client/notifications/NotificationManager.class";
    private static byte[] read(ZipFile z,ZipEntry e)throws Exception{try(java.io.InputStream in=z.getInputStream(e)){return in.readAllBytes();}}
    private static byte[] patch(byte[] bytes,String path){
        ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);
        if(path.equals(MANAGER)){
            for(MethodNode m:n.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof TypeInsnNode&&i.getOpcode()==NEW&&((TypeInsnNode)i).desc.equals(MODULE))return bytes;
            int found=0;for(MethodNode m:n.methods)if(m.name.equals("<init>"))for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof FieldInsnNode&&i.getOpcode()==PUTSTATIC&&((FieldInsnNode)i).name.equals("initialized")){
                AbstractInsnNode before=i.getPrevious();if(before.getOpcode()!=ICONST_1)throw new IllegalStateException("Module registry changed");InsnList list=new InsnList();list.add(new VarInsnNode(ALOAD,0));list.add(new TypeInsnNode(NEW,MODULE));list.add(new InsnNode(DUP));list.add(new MethodInsnNode(INVOKESPECIAL,MODULE,"<init>","()V",false));list.add(new MethodInsnNode(INVOKEVIRTUAL,"keystrokesmod/client/module/ModuleManager","addModule","(Lkeystrokesmod/client/module/Module;)V",false));m.instructions.insertBefore(before,list);found++;
            }if(found!=1)throw new IllegalStateException("Registry insertion count "+found);
        }else{
            String method=path.equals(NOTIFY)?"moduleStateChanged":"show",bridge=path.equals(NOTIFY)?"moduleChanged":"notification",desc=path.equals(NOTIFY)?"(Lkeystrokesmod/client/module/Module;)Z":"(Ljava/lang/Object;)Z";
            int found=0;for(MethodNode m:n.methods)if(m.name.equals(method)){
                for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals(MODULE)&&((MethodInsnNode)i).name.equals(bridge))return bytes;
                LabelNode fallback=new LabelNode();InsnList list=new InsnList();list.add(new VarInsnNode(ALOAD,0));list.add(new MethodInsnNode(INVOKESTATIC,MODULE,bridge,desc,false));list.add(new JumpInsnNode(IFEQ,fallback));list.add(new InsnNode(RETURN));list.add(fallback);list.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(list);found++;
            }if(found!=1)throw new IllegalStateException("Missing notification bridge "+path);
        }
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);n.accept(writer);byte[] result=writer.toByteArray();new ClassReader(result).accept(new CheckClassAdapter(new ClassWriter(0),true),0);return result;
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalStateException("Output exists");Map<String,byte[]> additions=new TreeMap<>();BuildLunarModuleJar.SrgRemapper remap=new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));
        try(java.util.stream.Stream<Path> paths=Files.walk(Path.of(args[1]))){for(Path p:(Iterable<Path>)paths.filter(f->f.toString().endsWith(".class"))::iterator){ClassReader r=new ClassReader(Files.readAllBytes(p));String name=r.getClassName();if(!(name.equals(MODULE)||name.startsWith(MODULE+"$")||name.startsWith("keystrokesmod/client/module/modules/render/island/")))throw new IllegalStateException("Unexpected class "+name);if(r.readUnsignedShort(6)!=52)throw new IllegalStateException("Java 8 required");ClassWriter w=new ClassWriter(0);r.accept(new ClassRemapper(w,remap),0);byte[] result=w.toByteArray();new ClassReader(result).accept(new CheckClassAdapter(new ClassWriter(0),true),0);additions.put(name+".class",result);}}
        try(java.util.stream.Stream<Path> paths=Files.walk(Path.of(args[4]))){for(Path p:(Iterable<Path>)paths.filter(Files::isRegularFile)::iterator)additions.put("assets/phantom/island/"+Path.of(args[4]).relativize(p).toString().replace('\\','/'),Files.readAllBytes(p));}
        Set<String> changed=new HashSet<>(additions.keySet());changed.addAll(Arrays.asList(MANAGER,NOTIFY,QUEUE));
        try(ZipFile base=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){for(Enumeration<? extends ZipEntry> es=base.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();byte[] data=additions.remove(e.getName());if(data==null)data=read(base,e);if(e.getName().equals(MANAGER)||e.getName().equals(NOTIFY)||e.getName().equals(QUEUE))data=patch(data,e.getName());ZipEntry copy=new ZipEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);out.write(data);out.closeEntry();}for(Map.Entry<String,byte[]> e:additions.entrySet()){out.putNextEntry(new ZipEntry(e.getKey()));out.write(e.getValue());out.closeEntry();}}
        int same=0;try(ZipFile base=new ZipFile(args[0]);ZipFile result=new ZipFile(output.toFile())){for(Enumeration<? extends ZipEntry> es=base.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();if(!changed.contains(e.getName())){if(!Arrays.equals(read(base,e),read(result,result.getEntry(e.getName()))))throw new IllegalStateException("Unrelated entry changed "+e.getName());same++;}}}
        System.out.println("PASS: Java 8/dataflow, one DynamicIsland registration, existing module/notification hooks bridged with vanilla fallback; "+same+" unrelated entries byte-identical");System.out.println(output);
    }
}
