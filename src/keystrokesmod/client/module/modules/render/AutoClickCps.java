package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.CpsPositionSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.font.PhantomTTF;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.Font;
import java.util.Arrays;

/** Minimal Phantom CPS pill. The rolling click tracker remains separate from its renderer. */
public final class AutoClickCps extends Module {
    public enum DisplayMode { Left, Right, Both }
    public enum ColorMode { Phantom, Rainbow, Aurora, Sunset, Ice, Candy }

    private static final int CAPACITY = 256;
    private static final long SECOND = 1_000_000_000L;
    private static final long[] LEFT_TIMES = new long[CAPACITY], RIGHT_TIMES = new long[CAPACITY];
    private static final PhantomTTF NUMBER_FONT = new PhantomTTF(10, Font.PLAIN);
    private static final PhantomTTF LABEL_FONT = new PhantomTTF(8, Font.PLAIN);
    private static final String LABEL = "CPS";
    public static final int CARD_HEIGHT = 18, CARD_WIDTH = 86;
    private static AutoClickCps instance;
    private static int leftIndex, rightIndex;

    private final CpsPositionSetting position = new CpsPositionSetting();
    private final ComboSetting<DisplayMode> display = new ComboSetting<>("Display", DisplayMode.Both);
    private final TickSetting decimals = new TickSetting("Show Decimals", false);
    private final ComboSetting<ColorMode> colorMode = new ComboSetting<>("Color Mode", ColorMode.Phantom);
    private final SliderSetting colorSpeed = new SliderSetting("Color Speed", 1, .25, 3, .05);
    private final TickSetting graph = new TickSetting("Graph", false);
    private final TickSetting glow = new TickSetting("Ghost Glow", false);
    private final TickSetting particles = new TickSetting("Ghost Particles", false);

    private float visualLeft, visualRight, opacity, slide;
    private long lastFrame;
    private int currentWidth = CARD_WIDTH;
    private final int[] numberColors = new int[4];

    public AutoClickCps() {
        super("CPS Counter", ModuleCategory.render);
        withDescription("Minimal Phantom CPS pill. Drag it to override its default Phantom HUD position.");
        registerSettings(new DescriptionSetting(getDescription()), position, display, decimals, colorMode,
                colorSpeed, graph, glow, particles);
        instance = this;
        showInHud = false;
    }

    @Override public boolean showInHud() { return false; }
    @Override public void onEnable() { clear(); opacity = 0; slide = -6; lastFrame = 0; }
    @Override public void onDisable() { clear(); lastFrame = 0; }

    private static void clear() {
        Arrays.fill(LEFT_TIMES, 0); Arrays.fill(RIGHT_TIMES, 0);
        leftIndex = rightIndex = 0;
    }

    public static void recordLeft() {
        if (instance != null && instance.isEnabled()) LEFT_TIMES[leftIndex++ & (CAPACITY - 1)] = System.nanoTime();
    }

    public static void recordRight() {
        if (instance != null && instance.isEnabled()) RIGHT_TIMES[rightIndex++ & (CAPACITY - 1)] = System.nanoTime();
    }

    private static int count(long[] times, long now) {
        int result = 0;
        for (long time : times) if (time > 0 && now - time >= 0 && now - time < SECOND) result++;
        return result;
    }

    public CpsPositionSetting getPosition() { return position; }
    public static int getCardWidth() { return instance == null ? CARD_WIDTH : instance.currentWidth; }
    public static int getCardHeight() { return CARD_HEIGHT; }

    public static void drawCard(int x, int y) {
        if (instance != null) instance.render(x, y, count(LEFT_TIMES, System.nanoTime()),
                count(RIGHT_TIMES, System.nanoTime()), 1F, 0F, System.nanoTime());
    }

    public static void drawPreview(int x, int y) {
        if (instance != null) instance.render(x, y, 7, 4, 1F, 0F, System.nanoTime());
    }

    @Subscribe public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.showDebugInfo || mc.currentScreen != null) {
            lastFrame = 0;
            return;
        }
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 1F / 60 : Math.min(.05F, (now - lastFrame) / 1_000_000_000F);
        lastFrame = now;
        visualLeft = approach(visualLeft, count(LEFT_TIMES, now), dt, 11);
        visualRight = approach(visualRight, count(RIGHT_TIMES, now), dt, 11);
        opacity = approach(opacity, 1, dt, 16);
        slide = approach(slide, 0, dt, 16);
        updateWidth(visualLeft, visualRight);
        ScaledResolution screen = new ScaledResolution(mc);
        int x, y;
        if (position.hasPlaced()) {
            x = position.x(screen.getScaledWidth(), currentWidth);
            y = position.y(screen.getScaledHeight(), CARD_HEIGHT);
        } else {
            x = PhantomHUD.defaultCpsX(screen.getScaledWidth(), currentWidth);
            y = PhantomHUD.defaultCpsY(screen.getScaledHeight(), CARD_HEIGHT);
        }
        render(x, y, visualLeft, visualRight, opacity, slide, now);
    }

    private void updateWidth(float left, float right) {
        float content;
        if (display.getMode() == DisplayMode.Left) content = fixedWidth(format(left));
        else if (display.getMode() == DisplayMode.Right) content = fixedWidth(format(right));
        else content = fixedWidth(format(left)) + 3 + separatorWidth() + 3 + fixedWidth(format(right));
        // Tabular glyph cells keep each digit stable. The pill grows only if the displayed
        // number genuinely gains a digit, instead of leaving awkward blank digit slots.
        currentWidth = Math.max(58, (int) Math.ceil(10 + content + 3 + LABEL_FONT.width(LABEL) + 8));
    }

    private void render(float x, float y, float left, float right, float alpha, float offset, long now) {
        updateWidth(left, right);
        x += offset;
        int start = startColor(now), end = endColor(now);
        drawPill(x, y, start, end, alpha, now);
        String leftText = format(left), rightText = format(right);
        float cursor = x + 12;
        if (display.getMode() == DisplayMode.Left) {
            float cell = fixedWidth(leftText);
            drawNumber(leftText, cursor, y + 2, cell, start, end, alpha, now, 0);
            cursor += cell;
        } else if (display.getMode() == DisplayMode.Right) {
            float cell = fixedWidth(rightText);
            drawNumber(rightText, cursor, y + 2, cell, start, end, alpha, now, 0);
            cursor += cell;
        } else {
            float leftWidth = fixedWidth(leftText);
            drawNumber(leftText, cursor, y + 2, leftWidth, start, end, alpha, now, 0);
            cursor += leftWidth + 3;
            LABEL_FONT.draw("|", cursor, y + 5, rgba(0xFF8A8F98, alpha * .40F), null);
            cursor += separatorWidth() + 3;
            float rightWidth = fixedWidth(rightText);
            drawNumber(rightText, cursor, y + 2, rightWidth, start, end, alpha, now, 2);
            cursor += rightWidth;
        }
        LABEL_FONT.draw(LABEL, cursor + 3, y + 5, rgba(0xFF8A8F98, alpha), null);
    }

    private void drawNumber(String text, float x, float y, float cellWidth, int start, int end,
                            float alpha, long now, int offset) {
        float digit = digitWidth();
        float cursor = x + cellWidth - fixedWidth(text);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int color = character == '.' ? 0xFF8A8F98
                    : gradient(start, end, now, i + offset);
            numberColors[0] = color;
            NUMBER_FONT.draw(String.valueOf(character), cursor, y, rgba(0xFFFFFFFF, alpha), numberColors);
            cursor += character == '.' ? digit * .55F : digit;
        }
    }

    private void drawPill(float x, float y, int start, int end, float alpha, long now) {
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableTexture2D(); GlStateManager.disableDepth(); GlStateManager.disableCull();
        GlStateManager.disableLighting();
        try {
            if (glow.isToggled()) {
                float pulse = .7F + .3F * (float) Math.sin(now * (2 * Math.PI / 3_000_000_000D));
                for (int layer = 3; layer >= 1; layer--)
                    rounded(x - layer, y - layer, currentWidth + layer * 2, CARD_HEIGHT + layer * 2,
                            7 + layer, rgba(start, alpha * pulse * .15F / layer));
            }
            // A stable dark base prevents bright worlds from making one half of the pill look
            // heavy while the other half disappears. The subtle fade sits on top of that base.
            rounded(x, y, currentWidth, CARD_HEIGHT, 7, rgba(0xFF0A0B0E, alpha * .72F));
            roundedFade(x, y, currentWidth, CARD_HEIGHT, 7,
                    rgba(0xFF0E1013, alpha * .16F), rgba(0xFF0E1013, alpha * .04F));
            outline(x, y, currentWidth, CARD_HEIGHT, 7, rgba(0xFFFFFFFF, alpha * .06F));
            vertical(x + 2, y + 3, x + 4, y + CARD_HEIGHT - 3, rgba(start, alpha * .82F), rgba(end, 0));
            if (graph.isToggled()) {
                float graphY = y + CARD_HEIGHT - 4;
                horizontal(x + 8, graphY, x + currentWidth - 7, graphY + 1,
                        rgba(start, alpha * .12F), rgba(end, 0));
            }
            if (particles.isToggled()) for (int i = 0; i < 3; i++) {
                float phase = (float) ((now / 1_000_000_000D * (.20 + i * .05) + i * .31) % 1);
                solid(x + 5 + i * 2, y + CARD_HEIGHT - 4 - phase * 8, 1, 1,
                        rgba(i % 2 == 0 ? start : end, alpha * .18F * (1 - phase)));
            }
        } finally {
            GlStateManager.tryBlendFuncSeparate(src, dst, srcA, dstA);
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (!blend) GlStateManager.disableBlend();
            GlStateManager.resetColor();
        }
    }

    private String format(float value) {
        if (!decimals.isToggled()) return Integer.toString(Math.round(value));
        int tenth = Math.round(value * 10);
        return (tenth / 10) + "." + Math.abs(tenth % 10);
    }

    private float digitWidth() { return Math.max(NUMBER_FONT.width("0"), NUMBER_FONT.width("8")); }
    private float fixedWidth(String text) {
        float digit = digitWidth(), width = 0;
        for (int i = 0; i < text.length(); i++) width += text.charAt(i) == '.' ? digit * .55F : digit;
        return width;
    }
    private float separatorWidth() { return LABEL_FONT.width("|"); }

    private int startColor(long now) {
        switch (colorMode.getMode()) {
            case Aurora: return 0xFF00E7A0;
            case Sunset: return 0xFFFF5F8F;
            case Ice: return 0xFF4FC3F7;
            case Candy: return 0xFFFF6EDC;
            case Rainbow: return rainbow(now, 0);
            default: return 0xFF7B5CFF;
        }
    }

    private int endColor(long now) {
        switch (colorMode.getMode()) {
            case Aurora: return 0xFF7B5CFF;
            case Sunset: return 0xFFFFC857;
            case Ice: return 0xFFD8F8FF;
            case Candy: return 0xFF8B7CFF;
            case Rainbow: return rainbow(now, .35F);
            default: return 0xFF4FC3F7;
        }
    }

    private int gradient(int start, int end, long now, int character) {
        if (colorMode.getMode() == ColorMode.Rainbow) return rainbow(now, character * .12F);
        float phase = (float) ((now / 4_000_000_000D * colorSpeed.getInput() - character * .17F) % 1D);
        return mix(start, end, .5F + .5F * (float) Math.sin(phase * 2 * Math.PI));
    }

    private int rainbow(long now, float offset) {
        float phase = (float) ((now / 4_000_000_000D * colorSpeed.getInput() + offset) % 1D);
        return 0xFF000000 | java.awt.Color.HSBtoRGB(phase, .66F, 1F) & 0xFFFFFF;
    }

    private static float approach(float current, float target, float dt, float speed) {
        return current + (target - current) * (1 - (float) Math.exp(-speed * dt));
    }
    private static int rgba(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255)
                * Math.max(0, Math.min(1, alpha)))));
        return a << 24 | color & 0xFFFFFF;
    }
    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int g = Math.round(((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
    private static void vertex(float x, float y, int color) {
        GL11.glColor4f(((color >>> 16) & 255) / 255F, ((color >>> 8) & 255) / 255F,
                (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
        GL11.glVertex2f(x, y);
    }
    private static void horizontal(float x, float y, float x2, float y2, int left, int right) {
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x2, y, right); vertex(x, y, left); vertex(x, y2, left); vertex(x2, y2, right);
        GL11.glEnd();
    }
    private static void vertical(float x, float y, float x2, float y2, int top, int bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x2, y, top); vertex(x, y, top); vertex(x, y2, bottom); vertex(x2, y2, bottom);
        GL11.glEnd();
    }
    private static void solid(float x, float y, float width, float height, int color) {
        horizontal(x, y, x + width, y + height, color, color);
    }
    private static void rounded(float x, float y, float width, float height, float radius, int color) {
        float r = Math.min(radius, Math.min(width, height) / 2);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        vertex(x + width / 2, y + height / 2, color);
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner < 2 ? x + width - r : x + r;
            float cy = corner == 0 || corner == 3 ? y + r : y + height - r;
            for (int step = 0; step <= 5; step++) {
                double angle = (-90 + corner * 90 + step * 18) * Math.PI / 180;
                vertex(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r, color);
            }
        }
        vertex(x + width - r, y, color);
        GL11.glEnd();
    }
    /** One rounded primitive with a left-to-right alpha fade; no square fade overlay. */
    private static void roundedFade(float x, float y, float width, float height, float radius,
                                    int left, int right) {
        float r = Math.min(radius, Math.min(width, height) / 2);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        fadeVertex(x + width / 2, y + height / 2, x, width, left, right);
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner < 2 ? x + width - r : x + r;
            float cy = corner == 0 || corner == 3 ? y + r : y + height - r;
            for (int step = 0; step <= 5; step++) {
                double angle = (-90 + corner * 90 + step * 18) * Math.PI / 180;
                fadeVertex(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r,
                        x, width, left, right);
            }
        }
        fadeVertex(x + width - r, y, x, width, left, right);
        GL11.glEnd();
    }
    private static void fadeVertex(float x, float y, float origin, float width, int left, int right) {
        float t = Math.max(0, Math.min(1, (x - origin) / Math.max(1, width)));
        float r = (((left >>> 16) & 255) * (1 - t) + ((right >>> 16) & 255) * t) / 255F;
        float g = (((left >>> 8) & 255) * (1 - t) + ((right >>> 8) & 255) * t) / 255F;
        float b = ((left & 255) * (1 - t) + (right & 255) * t) / 255F;
        float a = (((left >>> 24) & 255) * (1 - t) + ((right >>> 24) & 255) * t) / 255F;
        GL11.glColor4f(r, g, b, a);
        GL11.glVertex2f(x, y);
    }
    private static void outline(float x, float y, float width, float height, float radius, int color) {
        float r = Math.min(radius, Math.min(width, height) / 2);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner < 2 ? x + width - r : x + r;
            float cy = corner == 0 || corner == 3 ? y + r : y + height - r;
            for (int step = 0; step <= 5; step++) {
                double angle = (-90 + corner * 90 + step * 18) * Math.PI / 180;
                vertex(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r, color);
            }
        }
        GL11.glEnd();
    }
}
