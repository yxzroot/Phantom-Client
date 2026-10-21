import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Change only Module's default description constant, preserving its Lunar keybind implementation. */
public final class PatchModuleDescriptionDefault {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input.jar output.jar");
        String name = "keystrokesmod/client/module/Module.class";
        boolean patched = false;
        try (ZipFile input = new ZipFile(args[0]); ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            for (Enumeration<? extends ZipEntry> e = input.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                byte[] bytes;
                try (InputStream stream = input.getInputStream(entry)) { bytes = stream.readAllBytes(); }
                if (name.equals(entry.getName())) {
                    ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
                    for (MethodNode method : node.methods) if ("<init>".equals(method.name)) {
                        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                            if (!(instruction instanceof FieldInsnNode)) continue;
                            FieldInsnNode field = (FieldInsnNode) instruction;
                            if (field.getOpcode() != Opcodes.PUTFIELD || !"description".equals(field.name) || !node.name.equals(field.owner)) continue;
                            AbstractInsnNode previous = instruction.getPrevious();
                            if (!(previous instanceof LdcInsnNode) || !"".equals(((LdcInsnNode) previous).cst) || patched)
                                throw new IllegalStateException("Unexpected Module description initializer");
                            ((LdcInsnNode) previous).cst = "No description available.";
                            patched = true;
                        }
                    }
                    ClassWriter writer = new ClassWriter(0); node.accept(writer); bytes = writer.toByteArray();
                }
                ZipEntry copy = new ZipEntry(entry.getName()); copy.setTime(entry.getTime());
                output.putNextEntry(copy); output.write(bytes); output.closeEntry();
            }
        }
        if (!patched) throw new IllegalStateException("Module default description not found");
    }
}
