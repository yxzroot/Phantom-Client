import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import org.objectweb.asm.util.CheckClassAdapter;

public final class VerifyAimPatch {
    static final String BASE="keystrokesmod/client/module/modules/combat/",MIX="keystrokesmod/client/mixin/mixins/";
    static boolean allowed(String name){return name.equals(BASE+"AimAssist.class")||name.startsWith(BASE+"aim/")||name.equals(MIX+"MixinFreelookInput.class")||name.equals(MIX+"MixinFreelookRenderer.class");}
    static String methodText(MethodNode m){org.objectweb.asm.util.Textifier t=new org.objectweb.asm.util.Textifier();m.accept(new org.objectweb.asm.util.TraceMethodVisitor(t));java.io.StringWriter out=new java.io.StringWriter();t.print(new java.io.PrintWriter(out));return out.toString();}
    static ClassNode noDebug(ZipFile jar,String name)throws Exception{ClassNode n=new ClassNode();new ClassReader(VerifyConfigPatch.read(jar,jar.getEntry(name))).accept(n,ClassReader.SKIP_DEBUG);return n;}
    static void stripObservers(ClassNode node){
        for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals(BASE+"AimAssist")){
            String call=((MethodInsnNode)i).name;int count=call.equals("observeMouseTurn")?5:call.equals("cameraFrame")?2:0;if(count==0)continue;
            AbstractInsnNode first=i;for(int k=1;k<count;k++)first=first.getPrevious();for(int k=0;k<count;k++){AbstractInsnNode next=first.getNext();m.instructions.remove(first);first=next;}
        }
    }
    static void mixins(ZipFile before,ZipFile after)throws Exception{
        for(String path:new String[]{MIX+"MixinFreelookInput.class",MIX+"MixinFreelookRenderer.class"}){
            ClassNode old=noDebug(before,path),fresh=noDebug(after,path);
            stripObservers(old);stripObservers(fresh);
            for(MethodNode m:old.methods){MethodNode match=null;for(MethodNode f:fresh.methods)if(f.name.equals(m.name)&&f.desc.equals(m.desc))match=f;
                if(match==null||!methodText(m).equals(methodText(match)))throw new IllegalStateException("Unrelated Freelook/Scaffold hook changed: "+path+"."+m.name);
            }
            if(path.endsWith("Renderer.class")){
                boolean found=false;for(MethodNode m:fresh.methods)if(m.name.equals("phantom$assistFrame")){
                    if(m.visibleAnnotations==null)throw new IllegalStateException("Render hook annotations missing");
                    for(AnnotationNode a:m.visibleAnnotations)if(a.desc.endsWith("/Inject;")&&a.values.toString().contains("func_78471_a(FJ)V"))found=true;
                }if(!found)throw new IllegalStateException("Render-world injection target annotation missing");
            }
        }
        System.out.println("PASS: existing Freelook/Scaffold hook method bodies unchanged apart from the added AimAssist mouse observer; render-world injection annotation retained");
    }
    public static void main(String[] args)throws Exception{
        int untouched=0,checked=0,refs=0,registry=0;
        BuildLunarModuleJar.SrgRemapper remap=new BuildLunarModuleJar.SrgRemapper(java.nio.file.Path.of(args[3]));
        Map<String,ClassNode> mc=new HashMap<>(),client=new HashMap<>();
        try(ZipFile before=new ZipFile(args[0]);ZipFile after=new ZipFile(args[1]);ZipFile forge=new ZipFile(args[2])){
            mixins(before,after);
            for(Enumeration<? extends ZipEntry> it=after.entries();it.hasMoreElements();){ZipEntry entry=it.nextElement();String path=entry.getName();byte[] bytes=VerifyConfigPatch.read(after,entry);
                if(!allowed(path)){if(!Arrays.equals(bytes,VerifyConfigPatch.read(before,before.getEntry(path))))throw new IllegalStateException("Unrelated entry changed: "+path);untouched++;continue;}
                ClassReader reader=new ClassReader(bytes);if(reader.readUnsignedShort(6)!=52)throw new IllegalStateException("Not Java 8");reader.accept(new CheckClassAdapter(new ClassWriter(0),true),0);checked++;
                ClassNode node=new ClassNode();reader.accept(node,0);
                for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray()){
                    String owner=null,name=null,desc=null;boolean field=i instanceof FieldInsnNode;
                    if(field){FieldInsnNode f=(FieldInsnNode)i;owner=f.owner;name=f.name;desc=f.desc;}
                    else if(i instanceof MethodInsnNode){MethodInsnNode f=(MethodInsnNode)i;owner=f.owner;name=f.name;desc=f.desc;}
                    if(owner!=null&&(owner.startsWith("net/minecraft/")||owner.startsWith("keystrokesmod/"))){boolean minecraft=owner.startsWith("net/minecraft/");if(!VerifyConfigPatch.resolves(owner,name,desc,field,minecraft?forge:after,minecraft?remap:null,minecraft?mc:client))throw new IllegalStateException("Unresolved member "+owner+"."+name+desc);refs++;}
                    if(path.startsWith(BASE+"aim/")&&!m.name.equals("<init>")&&!m.name.equals("<clinit>")&&!path.endsWith("AssistDebug.class")&& !m.name.equals("setValue") && (i.getOpcode()==Opcodes.NEW||i.getOpcode()==Opcodes.NEWARRAY||i.getOpcode()==Opcodes.ANEWARRAY))throw new IllegalStateException("Frame allocation "+path+"."+m.name);
                }
            }
            ClassNode manager=VerifyConfigPatch.node("keystrokesmod/client/module/ModuleManager",after,null,client);
            for(MethodNode m:manager.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof TypeInsnNode&&i.getOpcode()==Opcodes.NEW&&((TypeInsnNode)i).desc.equals(BASE+"AimAssist"))registry++;
            if(registry!=1)throw new IllegalStateException("AimAssist registry count "+registry);
            if(!VerifyConfigPatch.resolves("net/minecraft/client/renderer/EntityRenderer","func_78471_a","(FJ)V",false,forge,remap,mc))throw new IllegalStateException("Render hook target missing");
            for(Enumeration<? extends ZipEntry> it=before.entries();it.hasMoreElements();)if(after.getEntry(it.nextElement().getName())==null)throw new IllegalStateException("Removed JAR entry");
            // Existing third-party friends calls rely on these exact public descriptors.
            ClassNode old=VerifyConfigPatch.node(BASE+"AimAssist",before,null,new HashMap<>()),fresh=VerifyConfigPatch.node(BASE+"AimAssist",after,null,client);
            for(FieldNode f:old.fields)if((f.access&Opcodes.ACC_PUBLIC)!=0&&!VerifyConfigPatch.resolves(fresh.name,f.name,f.desc,true,after,null,client))throw new IllegalStateException("Removed field ABI "+f.name);
            for(MethodNode m:old.methods)if((m.access&Opcodes.ACC_PUBLIC)!=0&&!VerifyConfigPatch.resolves(fresh.name,m.name,m.desc,false,after,null,client))throw new IllegalStateException("Removed method ABI "+m.name);
        }
        if(checked<12)throw new IllegalStateException("Missing scoped classes: "+checked);
        System.out.println("PASS: "+checked+" Java 8/dataflow classes, "+refs+" exact SRG/client references, preserved legacy AimAssist ABI, one existing registry entry, no explicit math/controller frame allocations; "+untouched+" unrelated entries byte-identical");
    }
}
