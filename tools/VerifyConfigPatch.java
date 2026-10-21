import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.tree.*;import org.objectweb.asm.commons.ClassRemapper;
public final class VerifyConfigPatch {
    static byte[] read(ZipFile z,ZipEntry e)throws Exception{if(e==null)throw new IllegalStateException("Missing JAR entry");try(java.io.InputStream in=z.getInputStream(e)){return in.readAllBytes();}}
    static boolean allowed(String n){return n.equals("keystrokesmod/client/config/Config.class")||n.equals("keystrokesmod/client/config/ConfigManager.class")||n.startsWith("keystrokesmod/client/config/ConfigManager$")||n.equals("keystrokesmod/client/clickgui/apple/rebuild/GuiRoot.class")||n.equals("keystrokesmod/client/clickgui/apple/rebuild/ConfigTab.class")||n.startsWith("keystrokesmod/client/clickgui/apple/rebuild/ConfigTab$");}
    static ClassNode node(String owner,ZipFile jar,BuildLunarModuleJar.SrgRemapper remap,Map<String,ClassNode> cache)throws Exception{
        if(cache.containsKey(owner))return cache.get(owner);ZipEntry entry=jar.getEntry(owner+".class");if(entry==null)return null;
        byte[] bytes=read(jar,entry);if(remap!=null){ClassWriter out=new ClassWriter(0);new ClassReader(bytes).accept(new ClassRemapper(out,remap),0);bytes=out.toByteArray();}
        ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);cache.put(owner,n);return n;
    }
    static boolean resolves(String owner,String name,String desc,boolean field,ZipFile jar,BuildLunarModuleJar.SrgRemapper remap,Map<String,ClassNode> cache)throws Exception{
        if(owner==null||owner.startsWith("java/"))return false;ClassNode n=node(owner,jar,remap,cache);if(n==null)return false;
        if(field){for(FieldNode f:n.fields)if(f.name.equals(name)&&f.desc.equals(desc))return true;}
        else for(MethodNode m:n.methods)if(m.name.equals(name)&&m.desc.equals(desc))return true;
        if(!name.equals("<init>")&&resolves(n.superName,name,desc,field,jar,remap,cache))return true;
        for(String parent:n.interfaces)if(resolves(parent,name,desc,field,jar,remap,cache))return true;return false;
    }
    static void references(ZipFile result,String forgePath,String mappings)throws Exception{
        BuildLunarModuleJar.SrgRemapper remap=new BuildLunarModuleJar.SrgRemapper(java.nio.file.Path.of(mappings));
        Map<String,ClassNode> mcCache=new HashMap<>(),clientCache=new HashMap<>();int count=0;
        try(ZipFile forge=new ZipFile(forgePath)){
            for(Enumeration<? extends ZipEntry> it=result.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(!allowed(e.getName()))continue;
                ClassNode n=node(e.getName().replace(".class",""),result,null,clientCache);
                for(MethodNode m:n.methods)for(AbstractInsnNode i:m.instructions.toArray()){
                    String owner=null,name=null,desc=null;boolean field=i instanceof FieldInsnNode;
                    if(field){FieldInsnNode f=(FieldInsnNode)i;owner=f.owner;name=f.name;desc=f.desc;}
                    else if(i instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)i;owner=c.owner;name=c.name;desc=c.desc;}
                    if(owner==null)continue;
                    if(owner.startsWith("net/minecraft/")){if(!resolves(owner,name,desc,field,forge,remap,mcCache))throw new IllegalStateException("Missing SRG member "+owner+"."+name+desc);count++;}
                    else if(owner.startsWith("keystrokesmod/")){if(!resolves(owner,name,desc,field,result,null,clientCache))throw new IllegalStateException("Missing client member "+owner+"."+name+desc);count++;}
                }
            }
        }
        System.out.println("PASS: "+count+" packaged Minecraft/SRG and client member references resolve, including Config-tab drag/release routing");
    }
    public static void main(String[] args)throws Exception{
        int preserved=0,checked=0;List<String> changed=new ArrayList<>();
        try(ZipFile before=new ZipFile(args[0]);ZipFile after=new ZipFile(args[1])){
            for(Enumeration<? extends ZipEntry> it=after.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(allowed(e.getName())){byte[] b=read(after,e);ClassReader reader=new ClassReader(b);if(reader.readUnsignedShort(6)!=52)throw new IllegalStateException("Not Java 8");reader.accept(new CheckClassAdapter(new ClassWriter(0),true),0);checked++;if(!Arrays.equals(read(before,before.getEntry(e.getName())),b))changed.add(e.getName());}else{if(!Arrays.equals(read(before,before.getEntry(e.getName())),read(after,e)))throw new IllegalStateException("Unrelated change: "+e.getName());preserved++;}}
            for(Enumeration<? extends ZipEntry> it=before.entries();it.hasMoreElements();)if(after.getEntry(it.nextElement().getName())==null)throw new IllegalStateException("Removed entry");
            if(args.length==4)references(after,args[2],args[3]);
        }
        if(checked!=8)throw new IllegalStateException("Expected eight scoped config/input classes, got "+checked);
        System.out.println("PASS: scoped config/input classes only; Java 8/dataflow; "+preserved+" unrelated JAR entries byte-identical; changed entries: "+changed);
    }
}
