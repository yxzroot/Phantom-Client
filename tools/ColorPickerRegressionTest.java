import java.lang.reflect.Field;
import java.lang.reflect.Method;
import keystrokesmod.client.clickgui.raven.components.ColorPickerScreen;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;

/** Exercises the real picker without launching Minecraft or changing user configs. */
public final class ColorPickerRegressionTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void field(ColorPickerScreen screen, String name, Object value) throws Exception {
        Field field = ColorPickerScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(screen, value);
    }
    private static void invoke(ColorPickerScreen screen, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = ColorPickerScreen.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(screen, args);
    }
    private static void rgb(ColorPickerScreen screen, int rgb) throws Exception {
        invoke(screen, "applyRGB", new Class<?>[] {int.class}, rgb);
    }
    private static void drag(ColorPickerScreen screen, int mode, int x, int y) throws Exception {
        field(screen, "squareX", 0); field(screen, "squareY", 0);
        field(screen, "squareW", 101); field(screen, "squareH", 101);
        field(screen, "dragging", mode);
        invoke(screen, "updateDrag", new Class<?>[] {int.class, int.class}, x, y);
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("baseline")) {
            try {
                new ColorPickerScreen(null, new PickerRGBSetting("Gradient start", 140, 80, 255, null));
                throw new AssertionError("Expected the installed version's null-opacity crash");
            } catch (NullPointerException expected) {
                System.out.println("REPRODUCED: original gradient picker crashes on missing opacity");
                return;
            }
        }
        for (String name : new String[] {"Gradient start", "Gradient end", "Static color"}) {
            PickerRGBSetting setting = new PickerRGBSetting(name, 140, 80, 255, null);
            ColorPickerScreen screen = new ColorPickerScreen(null, setting);
            rgb(screen, 0x12ABEF);
            check((setting.getRGB() & 0xFFFFFF) == 0x12ABEF, "RGB edits not applied");
            drag(screen, 1, 100, 0);
            check((setting.getRGB() & 0xFFFFFF) != 0x12ABEF, "Palette drag not applied");
            drag(screen, 2, 100, 50);
            drag(screen, 3, 0, 50); // Even stale opacity drag cannot dereference null.
            screen.onGuiClosed();
            check((setting.getRGB() & 0xFFFFFF) == 0x8C50FF, "Cancel did not restore RGB");
            screen = new ColorPickerScreen(null, setting);
            rgb(screen, 0x22DD88);
            field(screen, "accepted", true);
            screen.onGuiClosed();
            check((setting.getRGB() & 0xFFFFFF) == 0x22DD88, "Apply did not retain RGB");
            PickerRGBSetting restored = new PickerRGBSetting(name, 0, 0, 0, null);
            restored.applyConfigFromJson(setting.getConfigAsJson());
            check(restored.getRGB() == setting.getRGB(), "Config round trip changed RGB");
            check(restored.getOpacity() == null, "Unexpected opacity added to RGB-only setting");
        }
        SliderSetting opacity = new SliderSetting("Opacity", 65, 0, 100, 1);
        PickerRGBSetting setting = new PickerRGBSetting("Outline color", 20, 30, 40, opacity);
        ColorPickerScreen screen = new ColorPickerScreen(null, setting);
        rgb(screen, 0xFFFFFF);
        drag(screen, 3, 0, 75);
        check(opacity.getInput() == 25, "Existing opacity slider stopped working");
        screen.onGuiClosed();
        check(opacity.getInput() == 65 && (setting.getRGB() & 0xFFFFFF) == 0x141E28,
                "Cancel did not restore RGB plus opacity");
        screen = new ColorPickerScreen(null, setting);
        drag(screen, 3, 0, 20);
        field(screen, "accepted", true);
        screen.onGuiClosed();
        check(opacity.getInput() == 80, "Apply did not retain opacity");
        System.out.println("PASS: real gradient/static picker construction, RGB/HSV edits, null-opacity safety, Apply/Cancel, config round trip, existing opacity controls");
    }
}
