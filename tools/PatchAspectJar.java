import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.nio.charset.StandardCharsets;
import com.google.gson.*;import org.objectweb.asm.*;import org.objectweb.asm.commons.ClassRemapper;import org.objectweb.asm.tree.*;import org.objectweb.asm.util.CheckClassAdapter;

/** Isolated AspectRatio registration and projection-only hook. */
public final class PatchAspectJar implements Opcodes {
    private static final String MODULE="keystrokesmod/client/module/modules/render/AspectRatio",MANAGER="keystrokesmod/client/module/ModuleManager.class",HOOK="keystrokesmod/client/mixin/mixins/MixinAspectRatio",CONFIG="mixins.raindots.json";
    private static byte[] read(ZipFile zip,ZipEntry e)throws Exception{try(java.io.InputStream in=zip.getInputStream(e)){return in.readAllBytes();}}
    private static byte[] register(byte[] bytes){
        ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);int count=0,inserted=0;
        for(MethodNode m:n.methods)if(m.name.equals("<init>"))for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof TypeInsnNode&&i.getOpcode()==NEW&&((TypeInsnNode)i).desc.equals(MODULE))count++;
        if(count==1)return bytes;if(count!=0)throw new IllegalStateException("Duplicate AspectRatio");
        for(MethodNode m:n.methods)if(m.name.equals("<init>"))for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof FieldInsnNode&&i.getOpcode()==PUTSTATIC&&((FieldInsnNode)i).name.equals("initialized")){
            AbstractInsnNode before=i.getPrevious();if(before.getOpcode()!=ICONST_1)throw new IllegalStateException("Registry changed");InsnList add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new TypeInsnNode(NEW,MODULE));add.add(new InsnNode(DUP));add.add(new MethodInsnNode(INVOKESPECIAL,MODULE,"<init>","()V",false));add.add(new MethodInsnNode(INVOKEVIRTUAL,"keystrokesmod/client/module/ModuleManager","addModule","(Lkeystrokesmod/client/module/Module;)V",false));m.instructions.insertBefore(before,add);m.maxStack=Math.max(3,m.maxStack);inserted++;
        }
        if(inserted!=1)throw new IllegalStateException("Expected one registry insertion");ClassWriter out=new ClassWriter(0);n.accept(out);return out.toByteArray();
    }
    private static void validate(ZipFile forge,BuildLunarModuleJar.SrgRemapper remap,byte[] hook){
        try{
            ClassWriter out=new ClassWriter(0);new ClassReader(read(forge,forge.getEntry("net/minecraft/client/renderer/EntityRenderer.class"))).accept(new ClassRemapper(out,remap),0);
            ClassNode target=new ClassNode();new ClassReader(out.toByteArray()).accept(target,0);Map<String,MethodNode> methods=new HashMap<>();for(MethodNode m:target.methods)methods.put(m.name+m.desc,m);
            ClassNode node=new ClassNode();new ClassReader(hook).accept(node,0);int core=0,optional=0;
            for(MethodNode m:node.methods)if(m.visibleAnnotations!=null)for(AnnotationNode a:m.visibleAnnotations){
                if(a.desc.endsWith("/ModifyArg;"))throw new IllegalStateException("Fragile perspective argument hook returned");
                if(!a.desc.endsWith("/Inject;"))continue;
                int require=-1;AnnotationNode at=null;
                for(int i=0;i<a.values.size();i+=2){if(a.values.get(i).equals("require"))require=(Integer)a.values.get(i+1);if(a.values.get(i).equals("at")){Object v=a.values.get(i+1);at=(AnnotationNode)(v instanceof List?((List<?>)v).get(0):v);}}
                String site="";if(at!=null)for(int i=0;i<at.values.size();i+=2)if(at.values.get(i).equals("value"))site=(String)at.values.get(i+1);
                if(site.equals("INVOKE")){if(require!=0)throw new IllegalStateException("Lunar-specific call must be optional: "+m.name);optional++;}else{if(require!=1)throw new IllegalStateException("Missing required boundary hook");core++;}
                for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals("method"))for(Object value:a.values.get(i+1) instanceof List?(List<?>)a.values.get(i+1):Collections.singletonList(a.values.get(i+1))){
                    MethodNode actual=methods.get(value);if(actual==null)throw new IllegalStateException("Missing injection target "+value);
                }
            }
            if(core!=7||optional!=7)throw new IllegalStateException("Matrix/boundary projection hooks incomplete");
            System.out.println("PASS: stable camera/hand/world boundaries verified; no required Project/GLU calls (including Lunar's absent hand call); no GUI or picking hook");
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalStateException("Output exists");BuildLunarModuleJar.SrgRemapper remap=new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));Map<String,byte[]> additions=new TreeMap<>();
        try(java.util.stream.Stream<Path> paths=Files.walk(Path.of(args[1]))){for(Path p:(Iterable<Path>)paths.filter(f->f.toString().endsWith(".class"))::iterator){ClassReader r=new ClassReader(Files.readAllBytes(p));String n=r.getClassName();if(!(n.equals(MODULE)||n.startsWith(MODULE+"$")||n.startsWith("keystrokesmod/client/module/modules/render/aspect/")||n.equals(HOOK)||n.equals(HOOK+"World")))throw new IllegalStateException("Unexpected class "+n);if(r.readUnsignedShort(6)!=52)throw new IllegalStateException("Java 8 required");ClassWriter w=new ClassWriter(0);r.accept(new ClassRemapper(w,remap),0);byte[] bytes=w.toByteArray();new ClassReader(bytes).accept(new CheckClassAdapter(new ClassWriter(0),true),0);additions.put(n+".class",bytes);}}
        try(ZipFile forge=new ZipFile(args[4])){validate(forge,remap,additions.get(HOOK+".class"));
            ClassWriter targetOut=new ClassWriter(0);new ClassReader(read(forge,forge.getEntry("net/minecraft/client/renderer/RenderGlobal.class"))).accept(new ClassRemapper(targetOut,remap),0);
            ClassNode target=new ClassNode(),hook=new ClassNode();new ClassReader(targetOut.toByteArray()).accept(target,0);new ClassReader(additions.get(HOOK+"World.class")).accept(hook,0);
            Set<String> sites=new HashSet<>();for(MethodNode m:target.methods)sites.add(m.name+m.desc);int verified=0;
            for(MethodNode m:hook.methods)if(m.visibleAnnotations!=null)for(AnnotationNode a:m.visibleAnnotations)if(a.desc.endsWith("/Inject;"))for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals("method")){if(!sites.contains(a.values.get(i+1)))throw new IllegalStateException("Missing final world boundary "+a.values.get(i+1));verified++;}
            if(verified!=5)throw new IllegalStateException("Final draw boundaries incomplete");System.out.println("PASS: final sky/terrain/block/entity/selection draw boundaries verified");}
        Set<String> changed=new HashSet<>(additions.keySet());changed.add(MANAGER);changed.add(CONFIG);
        try(ZipFile base=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
            for(Enumeration<? extends ZipEntry> es=base.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();byte[] bytes=additions.remove(e.getName());if(bytes==null)bytes=read(base,e);
                if(e.getName().equals(MANAGER))bytes=register(bytes);
                if(e.getName().equals(CONFIG)){JsonObject json=new JsonParser().parse(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();JsonArray client=json.getAsJsonArray("client");for(String name:new String[]{"MixinAspectRatio","MixinAspectRatioWorld"}){boolean exists=false;for(JsonElement v:client)exists|=v.getAsString().equals(name);if(!exists)client.add(new JsonPrimitive(name));}bytes=json.toString().getBytes(StandardCharsets.UTF_8);}
                ZipEntry copy=new ZipEntry(e.getName());copy.setTime(e.getTime());out.putNextEntry(copy);out.write(bytes);out.closeEntry();
            }
            for(Map.Entry<String,byte[]> e:additions.entrySet()){out.putNextEntry(new ZipEntry(e.getKey()));out.write(e.getValue());out.closeEntry();}
        }
        int untouched=0;try(ZipFile base=new ZipFile(args[0]);ZipFile result=new ZipFile(output.toFile())){for(Enumeration<? extends ZipEntry> es=base.entries();es.hasMoreElements();){ZipEntry e=es.nextElement();if(changed.contains(e.getName()))continue;if(!Arrays.equals(read(base,e),read(result,result.getEntry(e.getName()))))throw new IllegalStateException("Unrelated entry changed "+e.getName());untouched++;}}
        System.out.println("PASS: Java 8/dataflow, AspectRatio registered once; "+untouched+" unrelated entries preserved");System.out.println(output.toAbsolutePath());
    }
}
