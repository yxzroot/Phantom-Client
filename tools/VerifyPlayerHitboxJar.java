import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;

public final class VerifyPlayerHitboxJar {
    public static void main(String[] args) throws Exception {
        try (ZipFile jar = new ZipFile(args[0])) {
            String[] names = {"keystrokesmod/client/module/ModuleManager.class",
                    "keystrokesmod/client/module/modules/render/PlayerHitboxESP.class",
                    "keystrokesmod/client/module/modules/render/AutoClickCps.class",
                    "keystrokesmod/client/module/modules/HUD.class",
                    "keystrokesmod/client/module/modules/HudAnimation.class",
                    "keystrokesmod/client/module/setting/impl/CpsPositionSetting.class",
                    "keystrokesmod/client/module/setting/impl/PickerRGBSetting.class",
                    "keystrokesmod/client/clickgui/apple/AppleModuleRow.class",
                    "keystrokesmod/client/clickgui/apple/settings/ApplePickerRGB.class",
                    "keystrokesmod/client/clickgui/apple/settings/AppleCpsMoveRow.class",
                    "keystrokesmod/client/clickgui/apple/CpsDragScreen.class",
                    "keystrokesmod/client/clickgui/raven/components/PickerRGBComponent.class",
                    "keystrokesmod/client/clickgui/raven/components/ColorPickerScreen.class"};
            for (String name : names) {
                ClassNode node = new ClassNode();
                new ClassReader(jar.getInputStream(jar.getEntry(name)).readAllBytes()).accept(node, 0);
                for (MethodNode method : node.methods) {
                    if (method.instructions.size() > 0)
                        new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, method);
                    if (name.endsWith("ModuleManager.class")) continue;
                    for (AbstractInsnNode insn : method.instructions) {
                        if (!(insn instanceof MethodInsnNode)) continue;
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if (call.owner.startsWith("net/minecraft/network/")
                                || call.name.toLowerCase().contains("sendpacket"))
                            throw new IllegalStateException("Nonvisual network call: " + call.name);
                    }
                }
                System.out.println("Verified " + name);
            }
        }
    }
}
