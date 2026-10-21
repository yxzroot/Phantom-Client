import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.util.CheckClassAdapter;

/** Patches only SwordGlow ownership and its hooks; verifies all other original JAR entries. */
public final class PatchHeldGlintJar {
    private static boolean allowed(String name) {
        String root = "keystrokesmod/client/";
        return name.equals(root + "module/modules/render/SwordGlow")
                || name.startsWith(root + "module/modules/render/SwordGlow$")
                || name.startsWith(root + "module/modules/render/swordglow/")
                || name.equals(root + "mixin/mixins/MixinSwordGlowHand")
                || name.equals(root + "mixin/mixins/MixinSwordGlowItem");
    }
    private static byte[] read(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream input = zip.getInputStream(entry)) { return input.readAllBytes(); }
    }
    public static void main(String[] args) throws Exception {
        Path base = Path.of(args[0]), classes = Path.of(args[1]), output = Path.of(args[3]);
        if (Files.exists(output)) throw new IllegalStateException("Output already exists");
        BuildLunarModuleJar.SrgRemapper remapper = new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));
        Map<String, byte[]> changes = new TreeMap<>();
        try (java.util.stream.Stream<Path> paths = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".class"))::iterator) {
                ClassReader reader = new ClassReader(Files.readAllBytes(file));
                if (!allowed(reader.getClassName())) throw new IllegalStateException("Unrelated class " + reader.getClassName());
                if (reader.readUnsignedShort(6) != 52) throw new IllegalStateException("Expected Java 8");
                ClassWriter writer = new ClassWriter(0); reader.accept(new ClassRemapper(writer, remapper), 0);
                byte[] bytes = writer.toByteArray();
                new ClassReader(bytes).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
                changes.put(reader.getClassName() + ".class", bytes);
            }
        }
        if (!changes.containsKey("keystrokesmod/client/module/modules/render/swordglow/SwordRenderContext.class"))
            throw new IllegalStateException("Missing held-render fix");
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
        int untouched = 0;
        try (ZipFile original = new ZipFile(base.toFile()); ZipFile result = new ZipFile(output.toFile())) {
            for (Enumeration<? extends ZipEntry> entries = original.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (changed.contains(entry.getName())) continue;
                if (!Arrays.equals(read(original, entry), read(result, result.getEntry(entry.getName()))))
                    throw new IllegalStateException("Unrelated entry changed: " + entry.getName());
                untouched++;
            }
        }
        System.out.println("PASS: Java 8/dataflow; only held SwordGlow classes changed; " + untouched + " other JAR entries preserved");
        System.out.println(output.toAbsolutePath());
    }
}
