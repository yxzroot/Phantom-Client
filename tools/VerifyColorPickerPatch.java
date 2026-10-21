import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

/** Rejects any unrelated JAR change and verifies the mapped screen callbacks. */
public final class VerifyColorPickerPatch {
    public static void main(String[] args) throws Exception {
        String picker = "keystrokesmod/client/clickgui/raven/components/ColorPickerScreen.class";
        try (ZipFile base = new ZipFile(args[0]); ZipFile result = new ZipFile(args[1])) {
            if (base.size() != result.size()) throw new AssertionError("Unexpected new JAR entries");
            int untouched = 0;
            for (Enumeration<? extends ZipEntry> entries = base.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                byte[] original = base.getInputStream(entry).readAllBytes();
                byte[] patched = result.getInputStream(result.getEntry(entry.getName())).readAllBytes();
                if (entry.getName().equals(picker)) {
                    if (Arrays.equals(original, patched)) throw new AssertionError("Picker was not patched");
                    ClassReader reader = new ClassReader(patched);
                    if (reader.readUnsignedShort(6) != 52) throw new AssertionError("Expected Java 8 bytecode");
                    reader.accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
                    ClassNode node = new ClassNode(); reader.accept(node, 0);
                    Set<String> methods = new HashSet<>();
                    for (MethodNode method : node.methods) methods.add(method.name + method.desc);
                    for (String callback : new String[] {"func_73866_w_()V", "func_73863_a(IIF)V", "func_73864_a(III)V", "func_146273_a(IIIJ)V", "func_146286_b(III)V", "func_73869_a(CI)V", "func_146281_b()V", "func_73868_f()Z"})
                        if (!methods.contains(callback)) throw new AssertionError("Missing SRG callback: " + callback);
                } else {
                    if (!Arrays.equals(original, patched)) throw new AssertionError("Unrelated change: " + entry.getName());
                    untouched++;
                }
            }
            System.out.println("PASS: Java 8/dataflow, mapped GUI callbacks; only ColorPickerScreen changed; " + untouched + " JAR entries preserved (including SwordGlow)");
        }
    }
}
