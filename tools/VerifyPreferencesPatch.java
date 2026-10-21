import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.util.CheckClassAdapter;
public final class VerifyPreferencesPatch {
    static byte[] read(ZipFile z,ZipEntry e)throws Exception{try(java.io.InputStream in=z.getInputStream(e)){return in.readAllBytes();}}
    public static void main(String[] args)throws Exception{
        Set<String> allowed=new HashSet<>();for(String name:new String[]{"GuiRoot","HeaderBar","GuiPreferences","PreferencesMenu"})allowed.add("keystrokesmod/client/clickgui/apple/rebuild/"+name+".class");int count=0;
        try(ZipFile a=new ZipFile(args[0]);ZipFile b=new ZipFile(args[1])){for(String name:allowed){byte[] data=read(b,b.getEntry(name));ClassReader r=new ClassReader(data);if(r.readUnsignedShort(6)!=52)throw new IllegalStateException("Not Java 8");r.accept(new CheckClassAdapter(new ClassWriter(0),true),0);}
            for(Enumeration<? extends ZipEntry> it=a.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(allowed.contains(e.getName()))continue;if(!Arrays.equals(read(a,e),read(b,b.getEntry(e.getName()))))throw new IllegalStateException("Unrelated change "+e.getName());count++;}
        }System.out.println("PASS: only four GUI preferences/header classes changed; Java 8/dataflow; "+count+" unrelated entries byte-identical (color picker, lyrics and all modules preserved)");
    }
}
