import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

/** Replace one already-installed class while retaining all other JAR entries. */
public final class ReplaceMappedClassJar {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("installed.jar replacement.class mappings.srg output.jar");
        Path output = Path.of(args[3]);
        if (Files.exists(output)) throw new IllegalStateException("Output exists: " + output);
        ClassReader reader = new ClassReader(Files.readAllBytes(Path.of(args[1])));
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]))), 0);
        String name = reader.getClassName() + ".class";
        int replaced = 0;
        try (ZipFile source = new ZipFile(Path.of(args[0]).toFile());
             ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(output))) {
            if (source.getEntry(name) == null) throw new IllegalStateException("Missing installed class: " + name);
            for (Enumeration<? extends ZipEntry> entries = source.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                byte[] data;
                try (InputStream in = source.getInputStream(entry)) { data = in.readAllBytes(); }
                if (entry.getName().equals(name)) { data = writer.toByteArray(); replaced++; }
                ZipEntry copy = new ZipEntry(entry.getName());
                copy.setTime(entry.getTime());
                target.putNextEntry(copy);
                target.write(data);
                target.closeEntry();
            }
            if (replaced != 1) throw new IllegalStateException("Unexpected replacement count: " + replaced);
        }
    }
}
