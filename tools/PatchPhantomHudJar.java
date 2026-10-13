import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Patch only Module.getHudSuffix and the standalone HUD classes; preserve every other JAR entry. */
public final class PatchPhantomHudJar implements Opcodes {
    private static final String MODULE = "keystrokesmod/client/module/Module.class";
    private static final String PREFIX = "keystrokesmod/client/module/modules/render/";
    private static final String FONT = "keystrokesmod/client/utils/font/";
    private static final String HOST = "keystrokesmod/client/module/modules/HUD.class";
    private static final String EVENT = "keystrokesmod/client/event/impl/Render2DEvent";

    private static byte[] api(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        for (MethodNode method : node.methods)
            if (method.name.equals("getHudSuffix")) throw new IllegalStateException("Already patched");
        MethodNode method = new MethodNode(ACC_PUBLIC, "getHudSuffix", "()Ljava/lang/String;", null, null);
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC, PREFIX + "HudSuffixes", "resolve",
                "(Lkeystrokesmod/client/module/Module;)Ljava/lang/String;", false));
        method.instructions.add(new InsnNode(ARETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        node.methods.add(method);
        ClassWriter out = new ClassWriter(0);
        node.accept(out);
        return out.toByteArray();
    }

    /** Route the stock HUD's proven Raven event subscription to Phantom before it draws itself. */
    private static byte[] host(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        int count = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("onRender2D") || !method.desc.equals("(L" + EVENT + ";)V")) continue;
            for (org.objectweb.asm.tree.AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals(PREFIX + "PhantomHUD") && call.name.equals("renderHosted")
                            && call.desc.equals("(L" + EVENT + ";)Z")) return original;
                }
            }
            InsnList guard = new InsnList();
            LabelNode continueRaven = new LabelNode();
            guard.add(new VarInsnNode(ALOAD, 1));
            guard.add(new MethodInsnNode(INVOKESTATIC, PREFIX + "PhantomHUD", "renderHosted",
                    "(L" + EVENT + ";)Z", false));
            guard.add(new JumpInsnNode(IFEQ, continueRaven));
            guard.add(new InsnNode(RETURN));
            guard.add(continueRaven);
            method.instructions.insert(guard);
            method.maxStack = Math.max(1, method.maxStack);
            count++;
        }
        if (count != 1) throw new IllegalStateException("Stock HUD Render2D method not found: " + count);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 5)
            throw new IllegalArgumentException("prepare input.jar output.jar | assemble prepared.jar classes mappings output.jar");
        boolean prepare = args[0].equals("prepare");
        if (!prepare && !args[0].equals("assemble")) throw new IllegalArgumentException("Invalid mode");
        if (prepare && args.length != 3 || !prepare && args.length != 5)
            throw new IllegalArgumentException("Wrong argument count");
        Path input = Path.of(args[1]);
        Path output = Path.of(args[prepare ? 2 : 4]);
        if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);
        Map<String, byte[]> replacements = new HashMap<>();
        if (!prepare) {
            Path classes = Path.of(args[2]).resolve(PREFIX);
            BuildLunarModuleJar.SrgRemapper remapper = new BuildLunarModuleJar.SrgRemapper(Path.of(args[3]));
            try (java.util.stream.Stream<Path> files = Files.list(classes)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .filter(path -> path.getFileName().toString().startsWith("PhantomHUD")
                                || path.getFileName().toString().startsWith("HudSuffixes"))
                        .forEach(path -> {
                            try {
                                String name = PREFIX + path.getFileName();
                                ClassWriter writer = new ClassWriter(0);
                                new ClassReader(Files.readAllBytes(path)).accept(new ClassRemapper(writer, remapper), 0);
                                replacements.put(name, writer.toByteArray());
                            } catch (Exception e) { throw new IllegalStateException(e); }
                        });
            }
            try (java.util.stream.Stream<Path> files = Files.list(Path.of(args[2]).resolve(FONT))) {
                files.filter(path -> (path.getFileName().toString().startsWith("PhantomTTF")
                                || path.getFileName().toString().startsWith("PhantomDiagnostics"))
                                && path.getFileName().toString().endsWith(".class"))
                        .forEach(path -> {
                            try {
                                String name = FONT + path.getFileName();
                                ClassWriter writer = new ClassWriter(0);
                                new ClassReader(Files.readAllBytes(path)).accept(new ClassRemapper(writer, remapper), 0);
                                replacements.put(name, writer.toByteArray());
                            } catch (Exception e) { throw new IllegalStateException(e); }
                        });
            }
            if (!replacements.containsKey(PREFIX + "PhantomHUD.class")
                    || !replacements.containsKey(PREFIX + "HudSuffixes.class")
                    || !replacements.containsKey(FONT + "PhantomTTF.class")
                    || !replacements.containsKey(FONT + "PhantomDiagnostics.class"))
                throw new IllegalStateException("Missing HUD classes");
        }
        int moduleCount = 0;
        int hostCount = 0;
        try (ZipFile source = new ZipFile(input.toFile());
             ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(output))) {
            for (java.util.Enumeration<? extends ZipEntry> entries = source.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                byte[] data;
                try (InputStream in = source.getInputStream(entry)) { data = in.readAllBytes(); }
                if (name.equals(MODULE) && prepare) { data = api(data); moduleCount++; }
                if (name.equals(HOST) && !prepare) { data = host(data); hostCount++; }
                if (replacements.containsKey(name)) data = replacements.remove(name);
                ZipEntry copy = new ZipEntry(name);
                copy.setTime(entry.getTime());
                target.putNextEntry(copy);
                target.write(data);
                target.closeEntry();
            }
            for (Map.Entry<String, byte[]> addition : replacements.entrySet()) {
                target.putNextEntry(new ZipEntry(addition.getKey()));
                target.write(addition.getValue());
                target.closeEntry();
            }
        }
        if (prepare && moduleCount != 1) throw new IllegalStateException("Module.class not found");
        if (!prepare && hostCount != 1) throw new IllegalStateException("HUD.class not found");
    }
}
