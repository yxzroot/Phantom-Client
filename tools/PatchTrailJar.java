import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

/** Scoped render module + one registry entry. Default remains Trail for existing builds. */
public final class PatchTrailJar implements Opcodes {
    private static String MODULE="keystrokesmod/client/module/modules/render/Trail";
    private static String PACKAGE="keystrokesmod/client/module/modules/render/trail/";
    private static String LABEL="Trail";
    private static final String MANAGER="keystrokesmod/client/module/ModuleManager.class";
    private static byte[] read(ZipFile zip,ZipEntry entry)throws Exception{try(java.io.InputStream in=zip.getInputStream(entry)){return in.readAllBytes();}}
    private static int count(byte[] bytes){
        ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);int count=0;
        for(MethodNode m:n.methods)if(m.name.equals("<init>"))for(AbstractInsnNode i:m.instructions.toArray())
            if(i instanceof TypeInsnNode&&i.getOpcode()==NEW&&((TypeInsnNode)i).desc.equals(MODULE))count++;
        return count;
    }
    private static byte[] register(byte[] bytes){
        int count=count(bytes);if(count==1)return bytes;if(count!=0)throw new IllegalStateException("Duplicate "+LABEL);
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int inserted=0;
        for(MethodNode method:node.methods)if(method.name.equals("<init>"))for(AbstractInsnNode instruction:method.instructions.toArray()){
            if(!(instruction instanceof FieldInsnNode)||instruction.getOpcode()!=PUTSTATIC||!((FieldInsnNode)instruction).name.equals("initialized"))continue;
            AbstractInsnNode before=instruction.getPrevious();if(before.getOpcode()!=ICONST_1)throw new IllegalStateException("Registry constructor changed");
            InsnList add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new TypeInsnNode(NEW,MODULE));add.add(new InsnNode(DUP));
            add.add(new MethodInsnNode(INVOKESPECIAL,MODULE,"<init>","()V",false));
            add.add(new MethodInsnNode(INVOKEVIRTUAL,"keystrokesmod/client/module/ModuleManager","addModule","(Lkeystrokesmod/client/module/Module;)V",false));
            method.instructions.insertBefore(before,add);method.maxStack=Math.max(3,method.maxStack);inserted++;
        }
        if(inserted!=1)throw new IllegalStateException("Expected one "+LABEL+" insertion");
        ClassWriter writer=new ClassWriter(0);node.accept(writer);byte[] result=writer.toByteArray();
        if(count(result)!=1)throw new IllegalStateException("Registration failed");return result;
    }
    private static ClassNode target(String owner,ZipFile forge,BuildLunarModuleJar.SrgRemapper remap,Map<String,ClassNode> cache)throws Exception{
        if(cache.containsKey(owner))return cache.get(owner);ZipEntry entry=forge.getEntry(owner+".class");if(entry==null)return null;
        ClassWriter out=new ClassWriter(0);new ClassReader(read(forge,entry)).accept(new ClassRemapper(out,remap),0);
        ClassNode node=new ClassNode();new ClassReader(out.toByteArray()).accept(node,0);cache.put(owner,node);return node;
    }
    private static boolean resolves(String owner,String name,String desc,boolean field,ZipFile forge,BuildLunarModuleJar.SrgRemapper remap,Map<String,ClassNode> cache)throws Exception{
        if(owner==null||owner.startsWith("java/"))return false;ClassNode node=target(owner,forge,remap,cache);if(node==null)return false;
        if(field){for(FieldNode f:node.fields)if(f.name.equals(name)&&f.desc.equals(desc))return true;}
        else for(MethodNode m:node.methods)if(m.name.equals(name)&&m.desc.equals(desc))return true;
        if(!name.equals("<init>")&&resolves(node.superName,name,desc,field,forge,remap,cache))return true;
        for(String parent:node.interfaces)if(resolves(parent,name,desc,field,forge,remap,cache))return true;
        return false;
    }
    private static void validate(Map<String,byte[]> additions,String forgePath,BuildLunarModuleJar.SrgRemapper remap)throws Exception{
        Map<String,ClassNode> cache=new HashMap<>();int checked=0;
        try(ZipFile forge=new ZipFile(forgePath)){
            for(byte[] bytes:additions.values()){
                ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);
                for(MethodNode m:n.methods)for(AbstractInsnNode insn:m.instructions.toArray()){
                    String owner=null,name=null,desc=null;boolean field=insn instanceof FieldInsnNode;
                    if(field){FieldInsnNode f=(FieldInsnNode)insn;owner=f.owner;name=f.name;desc=f.desc;}
                    else if(insn instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)insn;owner=c.owner;name=c.name;desc=c.desc;}
                    if(owner!=null&&owner.startsWith("net/minecraft/")){
                        if(!resolves(owner,name,desc,field,forge,remap,cache))throw new IllegalStateException("Missing mapped Minecraft member: "+owner+"."+name+desc);checked++;
                        if(LABEL.equals("Airstrafe")&&field&&insn.getOpcode()==PUTFIELD&&!(name.equals("field_70159_w")||name.equals("field_70179_y")))
                            throw new IllegalStateException("Airstrafe writes a non-horizontal Minecraft field: "+owner+"."+name);
                    }
                    boolean startup=LABEL.equals("WorldParticles")&&(n.name.endsWith("/ParticleAssets")||m.name.equals("initialize")||m.name.equals("shader"));
                    if(LABEL.equals("Airstrafe"))startup=m.name.equals("findWeb")||n.name.endsWith("/AirDebug");
                    if(LABEL.equals("KillEffect"))startup=m.name.equals("initialize")||m.name.equals("shader")||m.name.equals("pixels")||m.name.equals("celebrate")||m.name.equals("increment")||m.name.equals("applyConfigFromJson");
                    if(!startup&&!m.name.equals("<init>")&&!m.name.equals("<clinit>")&&!m.name.equals("fail")&&n.name.startsWith(PACKAGE)&&
                            (insn.getOpcode()==NEW||insn.getOpcode()==NEWARRAY||insn.getOpcode()==ANEWARRAY))throw new IllegalStateException("Frame-path allocation in "+n.name+"."+m.name);
                }
            }
        }
        System.out.println("PASS: "+checked+" exact Forge/SRG member references resolved; no explicit frame-path object/array allocations");
    }
    public static void main(String[] args)throws Exception{
        if(args.length==7){
            if(!(args[5].equals("WorldParticles")&&args[6].equals("worldparticles"))&&!(args[5].equals("KillEffect")&&args[6].equals("killeffect"))&&!(args[5].equals("Airstrafe")&&args[6].equals("airstrafe")))throw new IllegalArgumentException("Unsupported module scope");
            LABEL=args[5];String category=LABEL.equals("Airstrafe")?"movement":"render";
            MODULE="keystrokesmod/client/module/modules/"+category+"/"+LABEL;
            PACKAGE="keystrokesmod/client/module/modules/"+category+"/"+args[6]+"/";
        }
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalStateException("Output already exists");
        BuildLunarModuleJar.SrgRemapper remap=new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));Map<String,byte[]> additions=new TreeMap<>();
        try(java.util.stream.Stream<Path> paths=Files.walk(Path.of(args[1]))){
            for(Path file:(Iterable<Path>)paths.filter(p->p.toString().endsWith(".class"))::iterator){
                ClassReader reader=new ClassReader(Files.readAllBytes(file));String name=reader.getClassName();
                if(!(name.equals(MODULE)||name.startsWith(MODULE+"$")||name.startsWith(PACKAGE)))throw new IllegalStateException("Unexpected class "+name);
                if(reader.readUnsignedShort(6)!=52)throw new IllegalStateException("Not Java 8");
                ClassWriter writer=new ClassWriter(0);reader.accept(new ClassRemapper(writer,remap),0);byte[] bytes=writer.toByteArray();
                new ClassReader(bytes).accept(new CheckClassAdapter(new ClassWriter(0),true),0);additions.put(name+".class",bytes);
            }
        }
        validate(additions,args[4],remap);
        Set<String> changed=new HashSet<>(additions.keySet());changed.add(MANAGER);
        try(ZipFile original=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
            if(original.getEntry(MANAGER)==null)throw new IllegalStateException("Module manager missing");
            for(Enumeration<? extends ZipEntry> entries=original.entries();entries.hasMoreElements();){
                ZipEntry entry=entries.nextElement();byte[] bytes=additions.remove(entry.getName());if(bytes==null)bytes=read(original,entry);
                if(entry.getName().equals(MANAGER))bytes=register(bytes);
                ZipEntry copy=new ZipEntry(entry.getName());copy.setTime(entry.getTime());out.putNextEntry(copy);out.write(bytes);out.closeEntry();
            }
            for(Map.Entry<String,byte[]> e:additions.entrySet()){out.putNextEntry(new ZipEntry(e.getKey()));out.write(e.getValue());out.closeEntry();}
        }
        int untouched=0;
        try(ZipFile original=new ZipFile(args[0]);ZipFile result=new ZipFile(output.toFile())){
            if(count(read(result,result.getEntry(MANAGER)))!=1)throw new IllegalStateException("Duplicate/missing registration");
            for(Enumeration<? extends ZipEntry> entries=original.entries();entries.hasMoreElements();){ZipEntry entry=entries.nextElement();if(changed.contains(entry.getName()))continue;
                if(!Arrays.equals(read(original,entry),read(result,result.getEntry(entry.getName()))))throw new IllegalStateException("Unrelated entry changed: "+entry.getName());untouched++;}
        }
        System.out.println("PASS: Java 8/dataflow, exactly one "+LABEL+" registration; "+untouched+" unrelated entries byte-identical (GUI, config, sky and sword included)");
        System.out.println(output.toAbsolutePath());
    }
}
