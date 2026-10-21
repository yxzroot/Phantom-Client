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
import org.objectweb.asm.commons.ClassRemapper;

/** Reobfuscates every compiled class below a directory and replaces or adds it to an installed JAR. */
public final class PatchMappedClassesJar {
    private PatchMappedClassesJar() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4)
            throw new IllegalArgumentException("input.jar compiled-classes mcp-srg.srg output.jar");
        Path input = Path.of(args[0]);
        Path classes = Path.of(args[1]);
        Path output = Path.of(args[3]);
        if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);

        BuildLunarModuleJar.SrgRemapper remapper = new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));
        Map<String, byte[]> replacements = new HashMap<>();
        try (java.util.stream.Stream<Path> paths = Files.walk(classes)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".class")).forEach(path -> {
                try {
                    ClassReader reader = new ClassReader(Files.readAllBytes(path));
                    ClassWriter writer = new ClassWriter(0);
                    reader.accept(new ClassRemapper(writer, remapper), 0);
                    replacements.put(reader.getClassName() + ".class", writer.toByteArray());
                } catch (Exception error) {
                    throw new IllegalStateException("Could not map " + path, error);
                }
            });
        }
        if (replacements.isEmpty()) throw new IllegalArgumentException("No class files under " + classes);

        try (ZipFile source = new ZipFile(input.toFile());
             ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(output))) {
            for (java.util.Enumeration<? extends ZipEntry> entries = source.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                byte[] data;
                try (InputStream stream = source.getInputStream(entry)) { data = stream.readAllBytes(); }
                byte[] replacement = replacements.remove(entry.getName());
                if (replacement != null) data = replacement;
                ZipEntry copy = new ZipEntry(entry.getName());
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
    }
}
