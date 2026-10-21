import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

/** Narrow patch: SkyChanger classes, render-only mixins and ONE module registry entry only. */
public final class PatchAuroraJar implements Opcodes {
    private static final String MODULE="keystrokesmod/client/module/modules/render/SkyChanger";
    private static final String MANAGER="keystrokesmod/client/module/ModuleManager.class";
    private static final String CONFIG="mixins.raindots.json";
    private static final String[] HOOKS={"MixinSkyChangerWorld","MixinSkyChangerLightmap","MixinSkyChangerBorder"};
    private static final class SkyRemapper extends Remapper {
        private final BuildLunarModuleJar.SrgRemapper base;
        SkyRemapper(Path file)throws Exception{base=new BuildLunarModuleJar.SrgRemapper(file);}
        @Override public String mapFieldName(String owner,String name,String desc){return base.mapFieldName(owner,name,desc);}
        @Override public String mapMethodName(String owner,String name,String desc){
            if(owner.equals("keystrokesmod/client/module/modules/render/aurora/AuroraWind")) {
                for(String ancestor:new String[]{"net/minecraft/client/audio/ISound","net/minecraft/client/audio/ITickableSound","net/minecraft/util/ITickable"}) {
                    String mapped=base.mapMethodName(ancestor,name,desc);if(!mapped.equals(name))return mapped;
                }
            }
            return base.mapMethodName(owner,name,desc);
        }
    }
    private static byte[] registry(byte[] bytes) {
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int inserted=0;
        int existing=0;
        for(MethodNode method:node.methods) if(method.name.equals("<init>"))
            for(AbstractInsnNode instruction:method.instructions.toArray())
                if(instruction instanceof TypeInsnNode&&((TypeInsnNode)instruction).desc.equals(MODULE))existing++;
        if(existing==1)return bytes;
        if(existing>1)throw new IllegalStateException("Duplicate SkyChanger registration");
        for(MethodNode method:node.methods) if(method.name.equals("<init>")) {
            for(AbstractInsnNode instruction:method.instructions.toArray()) {
                if(instruction instanceof TypeInsnNode&&((TypeInsnNode)instruction).desc.equals(MODULE))throw new IllegalStateException("SkyChanger already registered");
                if(!(instruction instanceof FieldInsnNode))continue;
                FieldInsnNode field=(FieldInsnNode)instruction;
                if(field.getOpcode()!=PUTSTATIC||!field.name.equals("initialized"))continue;
                AbstractInsnNode before=field.getPrevious();if(before.getOpcode()!=ICONST_1)throw new IllegalStateException("Registry shape changed");
                InsnList add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new TypeInsnNode(NEW,MODULE));add.add(new InsnNode(DUP));
                add.add(new MethodInsnNode(INVOKESPECIAL,MODULE,"<init>","()V",false));
                add.add(new MethodInsnNode(INVOKEVIRTUAL,"keystrokesmod/client/module/ModuleManager","addModule","(Lkeystrokesmod/client/module/Module;)V",false));
                method.instructions.insertBefore(before,add);method.maxStack=Math.max(3,method.maxStack);inserted++;
            }
        }
        if(inserted!=1)throw new IllegalStateException("Expected exactly one module registration");
        ClassWriter out=new ClassWriter(0);node.accept(out);return out.toByteArray();
    }
    private static byte[] read(ZipFile zip,ZipEntry entry)throws Exception{try(java.io.InputStream in=zip.getInputStream(entry)){return in.readAllBytes();}}
    private static void validateBorder(Map<String,byte[]> additions){
        ClassNode hook=new ClassNode();new ClassReader(additions.get("keystrokesmod/client/mixin/mixins/MixinSkyChangerBorder.class")).accept(hook,0);
        int gates=0,cancels=0,head=0,cancellable=0;
        for(MethodNode method:hook.methods){
            if(method.name.equals("<init>"))continue;
            if(method.visibleAnnotations!=null)for(AnnotationNode annotation:method.visibleAnnotations){
                if(!annotation.desc.endsWith("/Inject;"))continue;
                for(int k=0;k<annotation.values.size();k+=2){
                    String key=(String)annotation.values.get(k);Object value=annotation.values.get(k+1);
                    if(key.equals("cancellable")&&Boolean.TRUE.equals(value))cancellable++;
                    if(key.equals("at"))for(Object at:(List<?>)value){
                        AnnotationNode location=(AnnotationNode)at;
                        for(int j=0;j<location.values.size();j+=2)if(location.values.get(j).equals("value")&&location.values.get(j+1).equals("HEAD"))head++;
                    }
                }
            }
            for(AbstractInsnNode insn:method.instructions.toArray()){
                if(insn instanceof FieldInsnNode)throw new IllegalStateException("Border hook must not mutate/read world state");
                if(!(insn instanceof MethodInsnNode))continue;MethodInsnNode call=(MethodInsnNode)insn;
                if(call.owner.equals(MODULE)&&call.name.equals("hidesWorldBorder")&&call.desc.equals("()Z"))gates++;
                else if(call.owner.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfo")&&call.name.equals("cancel"))cancels++;
                else throw new IllegalStateException("Non-render operation in border hook: "+call.owner+"."+call.name);
            }
        }
        if(gates!=1||cancels!=1||head!=1||cancellable!=1)throw new IllegalStateException("Border hook must cancel once at HEAD through module gate");
        System.out.println("PASS: border hook only gates/cancels the visual render at HEAD, before GL changes; no world/border/packet mutations");
    }
    private static void validateHooks(Map<String,byte[]> additions,ZipFile forge,SkyRemapper remapper)throws Exception {
        for(String hook:HOOKS){
            ClassNode node=new ClassNode();new ClassReader(additions.get("keystrokesmod/client/mixin/mixins/"+hook+".class")).accept(node,0);
            String owner=hook.endsWith("World")?"net/minecraft/world/World":hook.endsWith("Border")?"net/minecraft/client/renderer/RenderGlobal":"net/minecraft/client/renderer/EntityRenderer";
            ClassWriter mapped=new ClassWriter(0);new ClassReader(read(forge,forge.getEntry(owner+".class"))).accept(new ClassRemapper(mapped,remapper),0);
            ClassNode target=new ClassNode();new ClassReader(mapped.toByteArray()).accept(target,0);
            Set<String> available=new HashSet<>();for(MethodNode m:target.methods)available.add(m.name+m.desc);
            for(MethodNode method:node.methods) if(method.visibleAnnotations!=null) for(AnnotationNode annotation:method.visibleAnnotations){
                if(!annotation.desc.endsWith("/Inject;"))continue;
                for(int i=0;i<annotation.values.size();i+=2)if(annotation.values.get(i).equals("method")) {
                    Object selectors=annotation.values.get(i+1);
                    for(Object selector:selectors instanceof List?(List<?>)selectors:java.util.Collections.singletonList(selectors))
                        if(!available.contains(selector))throw new IllegalStateException("Missing target "+owner+" "+selector);
                }
            }
            for(FieldNode field:node.fields)if(field.name.startsWith("field_")){
                boolean found=false;for(FieldNode actual:target.fields)found|=actual.name.equals(field.name)&&actual.desc.equals(field.desc);
                if(!found)throw new IllegalStateException("Missing shadow "+field.name);
            }
        }
    }
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[4]);if(Files.exists(output))throw new IllegalStateException("Output exists");
        SkyRemapper remapper=new SkyRemapper(Path.of(args[2]));Map<String,byte[]> additions=new TreeMap<>();
        try(java.util.stream.Stream<Path> paths=Files.walk(Path.of(args[1]))){
            for(Path file:(Iterable<Path>)paths.filter(p->p.toString().endsWith(".class"))::iterator){
                ClassReader reader=new ClassReader(Files.readAllBytes(file));String name=reader.getClassName();
                if(!(name.equals(MODULE)||name.startsWith(MODULE+"$")||name.startsWith("keystrokesmod/client/module/modules/render/aurora/")||name.startsWith("keystrokesmod/client/mixin/mixins/MixinSkyChanger")))throw new IllegalStateException("Unrelated class "+name);
                if(reader.readUnsignedShort(6)!=52)throw new IllegalStateException("Expected Java 8");
                ClassWriter mapped=new ClassWriter(0);reader.accept(new ClassRemapper(mapped,remapper),0);
                byte[] data=mapped.toByteArray();new ClassReader(data).accept(new CheckClassAdapter(new ClassWriter(0),true),0);additions.put(name+".class",data);
            }
        }
        try(ZipFile forge=new ZipFile(args[3])){validateHooks(additions,forge,remapper);}validateBorder(additions);
        Set<String> changed=new HashSet<>(additions.keySet());changed.add(MANAGER);changed.add(CONFIG);
        try(ZipFile original=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){
            for(Enumeration<? extends ZipEntry> entries=original.entries();entries.hasMoreElements();){
                ZipEntry entry=entries.nextElement();byte[] data=additions.remove(entry.getName());
                if(data==null)data=read(original,entry);
                if(entry.getName().equals(MANAGER))data=registry(data);
                if(entry.getName().equals(CONFIG)){
                    JsonObject config=new JsonParser().parse(new String(data,StandardCharsets.UTF_8)).getAsJsonObject();
                    JsonArray client=config.has("client")?config.getAsJsonArray("client"):new JsonArray();
                    for(String hook:HOOKS){boolean exists=false;for(JsonElement value:client)exists|=value.getAsString().equals(hook);if(!exists)client.add(new JsonPrimitive(hook));}
                    config.add("client",client);data=config.toString().getBytes(StandardCharsets.UTF_8);
                }
                ZipEntry copy=new ZipEntry(entry.getName());copy.setTime(entry.getTime());out.putNextEntry(copy);out.write(data);out.closeEntry();
            }
            for(Map.Entry<String,byte[]> entry:additions.entrySet()){out.putNextEntry(new ZipEntry(entry.getKey()));out.write(entry.getValue());out.closeEntry();}
        }
        int untouched=0;
        try(ZipFile original=new ZipFile(args[0]);ZipFile result=new ZipFile(output.toFile())){
            for(Enumeration<? extends ZipEntry> entries=original.entries();entries.hasMoreElements();){ZipEntry entry=entries.nextElement();if(changed.contains(entry.getName()))continue;
                if(!Arrays.equals(read(original,entry),read(result,result.getEntry(entry.getName()))))throw new IllegalStateException("Unrelated entry changed: "+entry.getName());untouched++;}
        }
        System.out.println("PASS: Java 8/dataflow, exact Forge hook/shadow mappings, one registry entry; "+untouched+" unrelated entries preserved (SwordGlow and GUI included)");
        System.out.println(output.toAbsolutePath());
    }
}
