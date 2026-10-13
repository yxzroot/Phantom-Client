package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.CpsPositionSetting;
import keystrokesmod.client.utils.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import com.google.common.eventbus.Subscribe;

/** Rolling one-second count of the left/right autoclickers' generated key events. */
public final class AutoClickCps extends Module {
    private static final int CAPACITY = 256;
    private static final long SECOND = 1_000_000_000L;
    private static final long[] left = new long[CAPACITY], right = new long[CAPACITY];
    private static int leftIndex, rightIndex;
    private static AutoClickCps instance;
    public static final int CARD_WIDTH = 100, CARD_HEIGHT = 19;
    private final CpsPositionSetting position = new CpsPositionSetting();

    public AutoClickCps() {
        super("CPS Counter", ModuleCategory.render);
        withDescription("Counts generated left/right autoclicker clicks during the last second.");
        registerSettings(new DescriptionSetting(getDescription()), position);
        instance = this;
    }

    @Override public void onEnable() { clear(); }
    @Override public void onDisable() { clear(); }

    private static void clear() {
        java.util.Arrays.fill(left, 0);
        java.util.Arrays.fill(right, 0);
        leftIndex = rightIndex = 0;
    }

    public static void recordLeft() {
        if (instance != null && instance.isEnabled()) left[leftIndex++ & (CAPACITY - 1)] = System.nanoTime();
    }

    public static void recordRight() {
        if (instance != null && instance.isEnabled()) right[rightIndex++ & (CAPACITY - 1)] = System.nanoTime();
    }

    private static int count(long[] times, long now) {
        int total = 0;
        for (long time : times) if (time > 0 && now - time >= 0 && now - time < SECOND) total++;
        return total;
    }

    public CpsPositionSetting getPosition() { return position; }

    public static void drawCard(int x, int y) {
        drawCardValues(x, y, count(left, System.nanoTime()), count(right, System.nanoTime()));
    }

    public static void drawPreview(int x, int y) {
        drawCardValues(x, y, 0, 0);
    }

    private static void drawCardValues(int x, int y, int l, int r) {
        RenderUtils.drawRoundedRect(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 4, 0xD641464E);
        RenderUtils.drawRoundedRect(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1,
                3, 0xE523272D);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        String label = "[" + l + " : " + r + "] CPS";
        mc.fontRendererObj.drawStringWithShadow(label,
                x + (CARD_WIDTH - mc.fontRendererObj.getStringWidth(label)) / 2,
                y + (CARD_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2, 0xFFE8EAEE);
    }

    @Subscribe public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.showDebugInfo
                || mc.currentScreen != null) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        drawCard(position.x(resolution.getScaledWidth(), CARD_WIDTH),
                position.y(resolution.getScaledHeight(), CARD_HEIGHT));
    }
}
