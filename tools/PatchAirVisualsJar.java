import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

/** Only the in-game HUD, its palette/helpers and SwordGlow's color bridge may change. */
public final class PatchAirVisualsJar {
    private static final String RENDER = "keystrokesmod/client/module/modules/render/";
    private static boolean allowed(String name) {
        return name.equals("keystrokesmod/client/mixin/mixins/MixinSwordGlowItem")
                || name.equals("keystrokesmod/client/mixin/mixins/MixinSwordGlowHand")
                || name.startsWith(RENDER + "swordglow/")
                || name.startsWith(RENDER + "airvisuals/") || name.equals(RENDER + "AirHudRenderer")
                || name.startsWith(RENDER + "AirHudRenderer$") || name.equals(RENDER + "PhantomHUD")
                || name.startsWith(RENDER + "PhantomHUD$") || name.equals(RENDER + "SwordGlow")
                || name.startsWith(RENDER + "SwordGlow$");
    }
    private static byte[] read(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream input = zip.getInputStream(entry)) { return input.readAllBytes(); }
    }
    public static void main(String[] args) throws Exception {
        Path base = Path.of(args[0]), classes = Path.of(args[1]), project = Path.of(args[3]), output = Path.of(args[4]);
        if (Files.exists(output)) throw new IllegalStateException("Output exists");
        BuildLunarModuleJar.SrgRemapper remapper = new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));
        Map<String, byte[]> changes = new TreeMap<>();
        try (java.util.stream.Stream<Path> files = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                ClassReader reader = new ClassReader(Files.readAllBytes(file));
                String name = reader.getClassName();
                if (name.equals("AirVisualsTest")) continue;
                if (!allowed(name)) throw new IllegalStateException("Out-of-scope class " + name);
                ClassNode node = new ClassNode(); reader.accept(node, 0);
                if (node.version != Opcodes.V1_8) throw new IllegalStateException("Expected Java 8 " + name);
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode insn : method.instructions.toArray()) {
                        if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.startsWith("net/ccbluex/"))
                            throw new IllegalStateException("LiquidBounce runtime dependency leaked into " + name);
                        if (insn instanceof FieldInsnNode && ((FieldInsnNode) insn).owner.startsWith("net/ccbluex/"))
                            throw new IllegalStateException("LiquidBounce runtime field leaked into " + name);
                    }
                }
                ClassWriter writer = new ClassWriter(0); reader.accept(new ClassRemapper(writer, remapper), 0);
                byte[] bytes = writer.toByteArray();
                new ClassReader(bytes).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
                changes.put(name + ".class", bytes);
            }
        }
        for (String required : new String[]{"PhantomHUD", "AirHudRenderer", "SwordGlow", "airvisuals/AirPalette", "airvisuals/AirTheme", "airvisuals/AirDraw"})
            if (!changes.containsKey(RENDER + required + ".class")) throw new IllegalStateException("Missing " + required);
        for (String resource : new String[]{"THIRD_PARTY_NOTICES", "licenses/AirClient-LICENSE", "LICENSE"})
            changes.put(resource, Files.readAllBytes(project.resolve(resource)));
        Set<String> changed = new HashSet<>(changes.keySet());
        try (ZipFile original = new ZipFile(base.toFile()); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
            for (Enumeration<? extends ZipEntry> entries = original.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement(); byte[] bytes = changes.remove(entry.getName());
                if (bytes == null) bytes = read(original, entry);
                ZipEntry copy = new ZipEntry(entry.getName()); copy.setTime(entry.getTime());
                out.putNextEntry(copy); out.write(bytes); out.closeEntry();
            }
            for (Map.Entry<String, byte[]> entry : changes.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey())); out.write(entry.getValue()); out.closeEntry();
            }
        }
        int preservedGui = 0, preservedGameplay = 0;
        try (ZipFile original = new ZipFile(base.toFile()); ZipFile patched = new ZipFile(output.toFile())) {
            for (Enumeration<? extends ZipEntry> entries = original.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (!changed.contains(entry.getName()) && !Arrays.equals(read(original, entry), read(patched, patched.getEntry(entry.getName()))))
                    throw new IllegalStateException("Unrelated entry modified " + entry.getName());
                if (entry.getName().startsWith("keystrokesmod/client/clickgui/")) preservedGui++;
                if (entry.getName().startsWith("keystrokesmod/client/module/modules/") && !entry.getName().startsWith(RENDER)) preservedGameplay++;
            }
        }
        System.out.println("PASS: Java 8/dataflow; no LiquidBounce runtime; " + preservedGui + " GUI entries and "
                + preservedGameplay + " gameplay/client module entries preserved byte-for-byte; licenses included");
        System.out.println(output.toAbsolutePath());
    }
}
