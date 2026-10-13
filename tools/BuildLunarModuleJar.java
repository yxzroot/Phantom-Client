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
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Reobfuscate only the new module and insert its constructor into the installed JAR's registry. */
public final class BuildLunarModuleJar implements Opcodes {
    private static final String MANAGER = "keystrokesmod/client/module/ModuleManager";
    private static final String MODULE = "keystrokesmod/client/module/Module";
    private static String moduleName;

    static final class SrgRemapper extends Remapper {
        final Map<String, String> fields = new HashMap<>();
        final Map<String, String> methods = new HashMap<>();

        SrgRemapper(Path path) throws Exception {
            for (String line : Files.readAllLines(path)) {
                String[] parts = line.split(" ");
                if (parts.length == 3 && parts[0].equals("FD:")) {
                    fields.put(parts[1], parts[2].substring(parts[2].lastIndexOf('/') + 1));
                } else if (parts.length == 5 && parts[0].equals("MD:")) {
                    methods.put(parts[1] + " " + parts[2], parts[3].substring(parts[3].lastIndexOf('/') + 1));
                }
            }
            if (fields.size() < 100 || methods.size() < 100)
                throw new IllegalStateException("Incomplete MCP-to-SRG mappings");
        }

        @Override public String mapFieldName(String owner, String name, String desc) {
            return lookup(fields, owner, name, "");
        }

        @Override public String mapMethodName(String owner, String name, String desc) {
            return lookup(methods, owner, name, " " + desc);
        }

        private String lookup(Map<String, String> table, String owner, String name, String suffix) {
            String direct = table.get(owner + "/" + name + suffix);
            if (direct != null) return direct;
            // javac emits subclass owners for inherited Minecraft members;
            // ForgeGradle's hierarchy-aware reobfuscation would resolve these.
            if (owner.equals("net/minecraft/client/entity/EntityPlayerSP")
                    || owner.equals("net/minecraft/entity/player/EntityPlayer")) {
                for (String ancestor : new String[] {"net/minecraft/client/entity/AbstractClientPlayer",
                        "net/minecraft/entity/player/EntityPlayer", "net/minecraft/entity/EntityLivingBase",
                        "net/minecraft/entity/EntityLiving", "net/minecraft/entity/Entity"}) {
                    String mapped = table.get(ancestor + "/" + name + suffix);
                    if (mapped != null) return mapped;
                }
            }
            if (owner.equals("net/minecraft/client/multiplayer/WorldClient")) {
                String mapped = table.get("net/minecraft/world/World/" + name + suffix);
                if (mapped != null) return mapped;
            }
            if (owner.equals("keystrokesmod/client/clickgui/raven/components/ColorPickerScreen")
                    || owner.equals("keystrokesmod/client/clickgui/apple/CpsDragScreen")) {
                for (String ancestor : new String[] {"net/minecraft/client/gui/GuiScreen",
                        "net/minecraft/client/gui/Gui"}) {
                    String mapped = table.get(ancestor + "/" + name + suffix);
                    if (mapped != null) return mapped;
                }
            }
            if (owner.equals("net/minecraft/util/BlockPos")) {
                String mapped = table.get("net/minecraft/util/Vec3i/" + name + suffix);
                if (mapped != null) return mapped;
            }
            return name;
        }
    }

    private static byte[] register(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        int count = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>")) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) continue;
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.getOpcode() != PUTSTATIC || !field.owner.equals(MANAGER)
                        || !field.name.equals("initialized")) continue;
                InsnList addition = new InsnList();
                addition.add(new org.objectweb.asm.tree.VarInsnNode(ALOAD, 0));
                addition.add(new TypeInsnNode(NEW, moduleName));
                addition.add(new InsnNode(DUP));
                addition.add(new MethodInsnNode(INVOKESPECIAL, moduleName, "<init>", "()V", false));
                addition.add(new MethodInsnNode(INVOKEVIRTUAL, MANAGER, "addModule", "(L" + MODULE + ";)V", false));
                // Insert before ICONST_1; the stack is empty before that instruction.
                AbstractInsnNode one = insn.getPrevious();
                if (one == null || one.getOpcode() != ICONST_1)
                    throw new IllegalStateException("Unexpected registry constructor");
                method.instructions.insertBefore(one, addition);
                method.maxStack = Math.max(3, method.maxStack);
                count++;
            }
        }
        if (count != 1) throw new IllegalStateException("Expected exactly one registry insertion: " + count);
        ClassWriter out = new ClassWriter(0);
        node.accept(out);
        return out.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("working.jar module.class mcp-srg.srg new.jar");
        Path input = Path.of(args[0]), helper = Path.of(args[1]), mappings = Path.of(args[2]), output = Path.of(args[3]);
        if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);
        SrgRemapper remapper = new SrgRemapper(mappings);
        moduleName = new ClassReader(Files.readAllBytes(helper)).getClassName();
        if (!moduleName.startsWith("keystrokesmod/client/module/modules/movement/")
                && !moduleName.startsWith("keystrokesmod/client/module/modules/render/"))
            throw new IllegalArgumentException("Unexpected module package: " + moduleName);
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(Files.readAllBytes(helper)).accept(new ClassRemapper(writer, remapper), 0);
        byte[] newClass = writer.toByteArray();
        int registryCount = 0;
        try (ZipFile source = new ZipFile(input.toFile());
             ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(output))) {
            if (source.getEntry(moduleName + ".class") != null) throw new IllegalStateException("Module already installed");
            for (java.util.Enumeration<? extends ZipEntry> entries = source.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                byte[] data;
                try (InputStream in = source.getInputStream(entry)) { data = in.readAllBytes(); }
                if (entry.getName().equals(MANAGER + ".class")) { data = register(data); registryCount++; }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(entry.getTime());
                target.putNextEntry(copy);
                target.write(data);
                target.closeEntry();
            }
            ZipEntry added = new ZipEntry(moduleName + ".class");
            target.putNextEntry(added);
            target.write(newClass);
            target.closeEntry();
        }
        if (registryCount != 1) {
            Files.delete(output);
            throw new IllegalStateException("ModuleManager not found");
        }
    }
}
