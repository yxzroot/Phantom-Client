import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

/** Adds only SwordGlow, its helpers/hooks, one registry entry and three mixin names. */
public final class BuildSwordGlowJar implements Opcodes {
    private static final String MODULE = "keystrokesmod/client/module/modules/render/SwordGlow";
    private static final String MANAGER = "keystrokesmod/client/module/ModuleManager";
    private static final String MIXINS = "mixins.raindots.json";
    private static final String[] HOOKS = {"MixinSwordGlowFrame", "MixinSwordGlowItem", "MixinSwordGlowHand"};

    private static byte[] register(byte[] bytes) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        int inserted = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof TypeInsnNode && ((TypeInsnNode) instruction).desc.equals(MODULE))
                    throw new IllegalStateException("SwordGlow is already registered");
                if (!(instruction instanceof FieldInsnNode)) continue;
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.getOpcode() != PUTSTATIC || !field.owner.equals(MANAGER) || !field.name.equals("initialized")) continue;
                AbstractInsnNode before = field.getPrevious();
                if (before.getOpcode() != ICONST_1) throw new IllegalStateException("Unexpected registry layout");
                InsnList addition = new InsnList();
                addition.add(new VarInsnNode(ALOAD, 0)); addition.add(new TypeInsnNode(NEW, MODULE));
                addition.add(new InsnNode(DUP));
                addition.add(new MethodInsnNode(INVOKESPECIAL, MODULE, "<init>", "()V", false));
                addition.add(new MethodInsnNode(INVOKEVIRTUAL, MANAGER, "addModule", "(Lkeystrokesmod/client/module/Module;)V", false));
                method.instructions.insertBefore(before, addition);
                method.maxStack = Math.max(method.maxStack, 3);
                inserted++;
            }
        }
        if (inserted != 1) throw new IllegalStateException("Expected one registry insertion, got " + inserted);
        ClassWriter writer = new ClassWriter(0); node.accept(writer); return writer.toByteArray();
    }

    private static byte[] mixins(byte[] bytes) {
        JsonObject config = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray client = config.has("client") ? config.getAsJsonArray("client") : new JsonArray();
        for (String hook : HOOKS) client.add(new com.google.gson.JsonPrimitive(hook));
        config.add("client", client);
        return config.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream in = zip.getInputStream(entry)) { return in.readAllBytes(); }
    }

    /** Check every injected selector and invocation against the actual local Forge class files. */
    private static void checkHookTargets(Path forge, BuildLunarModuleJar.SrgRemapper mapper) throws Exception {
        String renderer = "net/minecraft/client/renderer/";
        String stack = "Lnet/minecraft/item/ItemStack;", model = "Lnet/minecraft/client/resources/model/IBakedModel;";
        String transforms = "Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;";
        String[][] targets = {
            {renderer + "EntityRenderer", "func_181560_a", "(FJ)V"},
            {renderer + "entity/RenderItem", "func_175049_a", "(" + stack + "Lnet/minecraft/entity/EntityLivingBase;" + transforms + ")V"},
            {renderer + "entity/RenderItem", "func_180454_a", "(" + stack + model + ")V", renderer + "GlStateManager", "func_179121_F"},
            {renderer + "entity/RenderItem", "func_180454_a", "(" + stack + model + ")V", "net/minecraft/item/ItemStack", "func_77962_s"},
            {renderer + "ItemRenderer", "func_178099_a", "(Lnet/minecraft/entity/EntityLivingBase;" + stack + transforms + ")V"},
            {renderer + "ItemRenderer", "func_78440_a", "(F)V"}
        };
        try (ZipFile zip = new ZipFile(forge.toFile())) {
            for (String[] target : targets) {
                ClassNode node = new ClassNode();
                new ClassReader(read(zip, zip.getEntry(target[0] + ".class"))).accept(node, 0);
                MethodNode found = null;
                for (MethodNode method : node.methods)
                    if (mapper.mapMethodName(node.name, method.name, method.desc).equals(target[1]) && method.desc.equals(target[2])) found = method;
                if (found == null) throw new IllegalStateException("Missing mixin target: " + java.util.Arrays.toString(target));
                if (target.length > 3) {
                    int invokes = 0;
                    for (AbstractInsnNode instruction : found.instructions.toArray()) {
                        if (!(instruction instanceof MethodInsnNode)) continue;
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        if (call.owner.equals(target[3]) && mapper.mapMethodName(call.owner, call.name, call.desc).equals(target[4])) invokes++;
                    }
                    if (invokes < 1) throw new IllegalStateException("Missing injection point: " + target[4]);
                }
            }
        }
        String[][] nativeGlintCalls = {
            {renderer + "entity/RenderItem", "func_180451_a", "(" + model + ")V",
                    renderer + "entity/RenderItem", "func_175035_a", "2"},
            {renderer + "entity/RenderItem", "func_180451_a", "(" + model + ")V",
                    renderer + "GlStateManager", "func_179152_a", "2"},
            {renderer + "entity/RenderItem", "func_180451_a", "(" + model + ")V",
                    renderer + "GlStateManager", "func_179114_b", "2"},
            {renderer + "entity/RenderItem", "func_180451_a", "(" + model + ")V",
                    renderer + "GlStateManager", "func_179109_b", "2"}
        };
        try (ZipFile zip = new ZipFile(forge.toFile())) {
            for (String[] target : nativeGlintCalls) {
                ClassNode node = new ClassNode();
                new ClassReader(read(zip, zip.getEntry(target[0] + ".class"))).accept(node, 0);
                MethodNode found = null;
                for (MethodNode method : node.methods)
                    if (mapper.mapMethodName(node.name, method.name, method.desc).equals(target[1]) && method.desc.equals(target[2])) found = method;
                if (found == null) throw new IllegalStateException("Missing native glint method: " + target[1]);
                int invokes = 0;
                for (AbstractInsnNode instruction : found.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals(target[3]) && mapper.mapMethodName(call.owner, call.name, call.desc).equals(target[4])) invokes++;
                }
                if (invokes != Integer.parseInt(target[5]))
                    throw new IllegalStateException("Unexpected native glint invocation count for " + target[4] + ": " + invokes);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("base.jar classes mappings forge.jar output.jar");
        Path base = Path.of(args[0]), classes = Path.of(args[1]), output = Path.of(args[4]);
        if (Files.exists(output)) throw new IllegalArgumentException("Output already exists: " + output);
        BuildLunarModuleJar.SrgRemapper mapper = new BuildLunarModuleJar.SrgRemapper(Path.of(args[2]));
        checkHookTargets(Path.of(args[3]), mapper);
        Map<String, byte[]> additions = new HashMap<>();
        try (java.util.stream.Stream<Path> paths = Files.walk(classes)) {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".class"))::iterator) {
                ClassReader reader = new ClassReader(Files.readAllBytes(path));
                String name = reader.getClassName();
                boolean allowed = name.equals(MODULE) || name.startsWith(MODULE + "$")
                        || name.startsWith("keystrokesmod/client/module/modules/render/swordglow/");
                for (String hook : HOOKS) allowed |= name.equals("keystrokesmod/client/mixin/mixins/" + hook);
                if (!allowed) continue;
                String constants = new String(Files.readAllBytes(path), StandardCharsets.ISO_8859_1);
                if (constants.contains("SwordGlowDebug") || constants.contains("drawDebugRed"))
                    throw new IllegalStateException("Temporary red diagnostics left in " + name);
                if (name.equals(MODULE)) {
                    ClassNode module = new ClassNode(); reader.accept(module, 0);
                    for (MethodNode method : module.methods) {
                        if (!method.name.equals("<init>")) continue;
                        for (AbstractInsnNode instruction : method.instructions.toArray()) {
                            if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).owner.equals("keystrokesmod/client/main/Raven"))
                                throw new IllegalStateException("Module constructor reads Raven during static initialization");
                        }
                    }
                }
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, mapper), 0);
                byte[] mapped = writer.toByteArray();
                new ClassReader(mapped).accept(new CheckClassAdapter(new ClassWriter(0), true), 0);
                if (new ClassReader(mapped).readUnsignedShort(6) != 52) throw new IllegalStateException("Not Java 8: " + name);
                additions.put(name + ".class", mapped);
            }
        }
        if (!additions.containsKey(MODULE + ".class")) throw new IllegalStateException("Module missing");
        Set<String> expected = new HashSet<>(additions.keySet());
        expected.add(MANAGER + ".class"); expected.add(MIXINS);
        int patched = 0, added = additions.size();
        try (ZipFile source = new ZipFile(base.toFile()); ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement(); byte[] bytes = read(source, entry);
                if (additions.containsKey(entry.getName())) throw new IllegalStateException("Already contains " + entry.getName());
                if (entry.getName().equals(MANAGER + ".class")) { bytes = register(bytes); patched++; }
                if (entry.getName().equals(MIXINS)) { bytes = mixins(bytes); patched++; }
                ZipEntry copy = new ZipEntry(entry.getName()); copy.setTime(entry.getTime());
                out.putNextEntry(copy); out.write(bytes); out.closeEntry();
            }
            if (patched != 2) throw new IllegalStateException("Registry or mixin config missing");
            for (Map.Entry<String, byte[]> entry : additions.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey())); out.write(entry.getValue()); out.closeEntry();
            }
        }
        try (ZipFile original = new ZipFile(base.toFile()); ZipFile result = new ZipFile(output.toFile())) {
            Enumeration<? extends ZipEntry> entries = original.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!expected.contains(entry.getName()) && !java.util.Arrays.equals(read(original, entry), read(result, result.getEntry(entry.getName()))))
                    throw new IllegalStateException("Unrelated entry modified: " + entry.getName());
            }
        }
        System.out.println("PASS: Java 8 bytecode, held-item hooks and vanilla-glint redirect targets, no red diagnostics, " + added + " new classes, one Render registry entry; unrelated JAR entries preserved");
        System.out.println(output.toAbsolutePath());
    }
}
