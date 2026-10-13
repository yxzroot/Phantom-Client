package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.client.GuiModule;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.font.FontUtil;
import keystrokesmod.client.utils.font.PhantomTTF;
import keystrokesmod.client.utils.font.PhantomDiagnostics;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Independent Phantom arraylist; the original Raven HUD remains installed and may be re-enabled. */
public final class PhantomHUD extends Module {
    public enum AccentBar { Off, Left, Right }
    public enum ColorMode { Gradient, StaticAccent, Rainbow, Aurora, Sunset, Ice, Candy }
    public enum SortMode { Length, Alphabetical, Category }
    public enum Position { TopLeft, TopRight, BottomLeft, BottomRight }
    public enum Palette { Phantom, Client }

    private final TickSetting enabledHud = new TickSetting("Enable HUD", true);
    private final TickSetting values = new TickSetting("Show Values", true);
    private final TickSetting header = new TickSetting("Show Header", true);
    private final ComboSetting<ColorMode> colorMode = new ComboSetting<>("Color Mode", ColorMode.Gradient);
    private final SliderSetting gradientSpeed = new SliderSetting("Gradient Speed", 1, .25, 3, .05);
    private final SliderSetting glowIntensity = new SliderSetting("Glow Intensity (%)", 22, 0, 50, 1);
    private final PickerRGBSetting gradientStart = new PickerRGBSetting("Gradient Start", 123, 92, 255, glowIntensity);
    private final PickerRGBSetting gradientEnd = new PickerRGBSetting("Gradient End", 79, 195, 247, glowIntensity);
    private final TickSetting ghostBackgroundFade = new TickSetting("Ghost Background Fade", true);
    private final ComboSetting<AccentBar> accentBar = new ComboSetting<>("Accent Bar", AccentBar.Left);
    private final TickSetting glow = new TickSetting("Ghost Glow", true);
    private final TickSetting particles = new TickSetting("Ghost Particles", false);
    private final TickSetting idleFloat = new TickSetting("Idle Float", true);
    private final ComboSetting<SortMode> sort = new ComboSetting<>("Sort Mode", SortMode.Length);
    private final SliderSetting scale = new SliderSetting("Scale", 1, .5, 2, .05);
    private final ComboSetting<Position> position = new ComboSetting<>("Position", Position.TopLeft);
    private final ComboSetting<Palette> palette = new ComboSetting<>("Accent Theme", Palette.Phantom);
    private final SliderSetting xOffset = new SliderSetting("X Offset", 4, 0, 150, 1);
    private final SliderSetting yOffset = new SliderSetting("Y Offset", 4, 0, 150, 1);

    private static final PhantomTTF TITLE = new PhantomTTF(11, Font.BOLD);
    private static final PhantomTTF BODY = new PhantomTTF(10, Font.PLAIN);
    private static final PhantomTTF VALUE = new PhantomTTF(9, Font.PLAIN);
    private static final PhantomTTF VERSION_FONT = new PhantomTTF(10, Font.BOLD);
    private static final String LABEL = "PHANTOM", VERSION = "V1";
    private static final int GRAY = 0xFF8A8F98, DARK = 0xFF0E1013;
    private static final int[] HEADER_COLORS = new int[LABEL.length()];
    private static float headerWidth, badgeWidth;
    private static boolean fontFailed;
    private static PhantomHUD instance;
    private static boolean layoutKnown, layoutRight;
    private static float layoutX, layoutY, layoutWidth, layoutHeight;
    private final IdentityHashMap<Module, Row> states = new IdentityHashMap<>();
    private final ArrayList<Row> ordered = new ArrayList<>();
    private List<Module> roster;
    private int sequence;
    private long previousFrame;
    private float listHeight;
    private ScaledResolution screen;
    private int screenWidth, screenHeight, screenGuiScale;
    private int lastSkip = -1;
    private boolean hostWasEnabled;

    private static final class Row {
        final Module module;
        final String name;
        String suffix, oldSuffix;
        int[] colors;
        float nameWidth, suffixWidth, width, y, opacity, valueBlend, shimmer;
        int seen;
        Row(Module module) {
            this.module = module; name = module.getName();
            colors = new int[name.length()];
        }
    }

    private final Comparator<Row> length = (a, b) -> Float.compare(b.width, a.width);
    private final Comparator<Row> alphabetical = (a, b) -> a.name.compareToIgnoreCase(b.name);
    private final Comparator<Row> category = (a, b) -> {
        int difference = a.module.moduleCategory().ordinal() - b.module.moduleCategory().ordinal();
        return difference != 0 ? difference : a.name.compareToIgnoreCase(b.name);
    };

    public PhantomHUD() {
        super("Phantom HUD", ModuleCategory.client);
        instance = this;
        PhantomDiagnostics.begin("constructing module; runtime=" + System.getProperty("java.version"));
        withDescription("Phantom ghost arraylist with live module values.");
        registerSettings(enabledHud, values, header, colorMode, gradientSpeed, gradientStart, gradientEnd,
                ghostBackgroundFade, accentBar, glow, glowIntensity, particles, idleFloat,
                sort, scale, position, palette, xOffset, yOffset);
        showInHud = false;
        PhantomDiagnostics.log("CONSTRUCT complete: settings=" + getSettings().size());
    }

    @Override public void onEnable() {
        Module old = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByName("HUD");
        hostWasEnabled = old != null && old.isEnabled();
        if (old != null && !old.isEnabled()) old.enable();
        roster = null; previousFrame = 0;
        PhantomDiagnostics.log("ENABLE: host HUD=" + (old == null ? "missing" : "active")
                + ", wasActive=" + hostWasEnabled);
    }

    @Override public void onDisable() {
        Module old = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByName("HUD");
        if (!hostWasEnabled && old != null && old.isEnabled()) old.disable();
        states.clear(); ordered.clear(); roster = null; previousFrame = 0; listHeight = 0;
        PhantomDiagnostics.log("DISABLE: cleared HUD render state");
    }

    @Subscribe public void onRender2D(Render2DEvent event) {
        // HUD.class hosts Phantom when available. Keep this direct listener as a compatibility
        // fallback for future builds whose Raven HUD cannot be patched.
        Module old = Raven.moduleManager == null ? null : Raven.moduleManager.getModuleByName("HUD");
        if (old != null && old.isEnabled()) return;
        renderGate(event, "direct");
    }

    /** Called from the original HUD's known-good Render2D subscription. True suppresses Raven HUD drawing. */
    public static boolean renderHosted(Render2DEvent event) {
        PhantomHUD hud = instance;
        if (hud == null || !hud.isEnabled() || !hud.enabledHud.isToggled()) return false;
        PhantomDiagnostics.once("host-hook", "HOST hook active: rendering through original HUD overlay event");
        hud.renderGate(event, "host");
        return true;
    }

    private void renderGate(Render2DEvent event, String source) {
        int skip = (!enabledHud.isToggled() ? 1 : 0) | (mc.thePlayer == null ? 2 : 0)
                | (mc.theWorld == null ? 4 : 0) | (mc.currentScreen != null ? 8 : 0)
                | (mc.gameSettings.showDebugInfo ? 16 : 0) | (Raven.moduleManager == null ? 32 : 0);
        if (skip != 0) {
            if (skip != lastSkip) {
                lastSkip = skip;
                PhantomDiagnostics.log("RENDER " + source + " skipped: mask=" + skip + " [disabled=" + enabledHud.isToggled()
                        + ", player=" + (mc.thePlayer != null) + ", world=" + (mc.theWorld != null)
                        + ", screen=" + (mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName())
                        + ", F3=" + mc.gameSettings.showDebugInfo + ", manager=" + (Raven.moduleManager != null) + "]");
            }
            previousFrame = 0;
            return;
        }
        if (lastSkip != 0) {
            lastSkip = 0;
            PhantomDiagnostics.log("RENDER " + source + " entered world draw path");
        }
        try { render(event); }
        catch (Throwable error) {
            PhantomDiagnostics.failure("2D overlay", error);
            try { mc.fontRendererObj.drawStringWithShadow("PHANTOM HUD ERROR", 4, 4, 0xFFFF5555); }
            catch (Throwable ignored) { }
        }
    }

    private void render(Render2DEvent event) {
        long now = System.nanoTime();
        float dt = previousFrame == 0 ? 1F / 60 : Math.min(.05F, (now - previousFrame) / 1_000_000_000F);
        previousFrame = now;
        if (roster == null) {
            roster = Raven.moduleManager.getModules();
            PhantomDiagnostics.log("ROSTER captured: modules=" + roster.size());
        }
        if (headerWidth == 0) {
            // The renderer already advances by the font's fractional metrics. Do not add
            // synthetic letter spacing here, otherwise the version badge drifts too far away.
            headerWidth = width(TITLE, LABEL);
            badgeWidth = width(VERSION_FONT, VERSION);
        }
        sequence++;
        ordered.clear();
        float widest = header.isToggled() ? headerWidth + badgeWidth + 28 : 0;
        for (Module module : roster) {
            if (module == this || !module.isEnabled() || !module.showInHud()) continue;
            Row row = states.get(module);
            if (row == null) {
                row = new Row(module);
                row.nameWidth = width(BODY, row.name);
                row.y = header.isToggled() ? 27 : 5;
                states.put(module, row);
            }
            row.seen = sequence;
            String suffix = values.isToggled() ? module.getHudSuffix() : null;
            if (!java.util.Objects.equals(row.suffix, suffix)) {
                row.oldSuffix = row.suffix;
                row.suffix = suffix;
                row.suffixWidth = suffix == null ? 0 : width(VALUE, suffix);
                row.valueBlend = 0;
            }
            row.width = row.nameWidth + (row.suffixWidth == 0 ? 0 : 5 + row.suffixWidth) + 16;
            ordered.add(row);
            widest = Math.max(widest, row.width);
        }
        for (Iterator<Map.Entry<Module, Row>> iterator = states.entrySet().iterator(); iterator.hasNext();) {
            Row row = iterator.next().getValue();
            if (row.seen == sequence) continue;
            if (row.opacity <= .012F) iterator.remove();
            else ordered.add(row);
        }
        switch (sort.getMode()) {
            case Alphabetical: ordered.sort(alphabetical); break;
            case Category: ordered.sort(category); break;
            default: ordered.sort(length); break;
        }
        int active = 0;
        float baseY = header.isToggled() ? 30 : 8;
        for (Row row : ordered) {
            boolean visible = row.seen == sequence;
            row.opacity = approach(row.opacity, visible ? 1 : 0, dt, visible ? 16 : 12);
            if (visible) row.y = approach(row.y, baseY + 16 * active++, dt, 13);
            else row.y -= dt * 6;
            row.valueBlend = approach(row.valueBlend, 1, dt, 12);
            row.shimmer += dt;
            widest = Math.max(widest, row.width);
        }
        listHeight = listHeight == 0 ? baseY + 16 * active + 4
                : approach(listHeight, baseY + 16 * active + 4, dt, 13);
        if (screen == null || screenWidth != mc.displayWidth || screenHeight != mc.displayHeight
                || screenGuiScale != mc.gameSettings.guiScale) {
            screen = new ScaledResolution(mc);
            screenWidth = mc.displayWidth;
            screenHeight = mc.displayHeight;
            screenGuiScale = mc.gameSettings.guiScale;
        }
        boolean right = position.getMode() == Position.TopRight || position.getMode() == Position.BottomRight;
        boolean bottom = position.getMode() == Position.BottomLeft || position.getMode() == Position.BottomRight;
        float zoom = (float) scale.getInput(), panelWidth = widest + 4;
        float px = right ? screen.getScaledWidth() - (float) xOffset.getInput() - panelWidth * zoom
                : (float) xOffset.getInput();
        float py = bottom ? screen.getScaledHeight() - (float) yOffset.getInput() - listHeight * zoom
                : (float) yOffset.getInput();
        if (idleFloat.isToggled()) py += (float) Math.sin(now / 950_000_000D) * .8F;
        layoutKnown = true;
        layoutRight = right;
        layoutX = px;
        layoutY = py;
        layoutWidth = panelWidth * zoom;
        layoutHeight = listHeight * zoom;
        int start = palette.getMode() == Palette.Client ? GuiModule.getEnabledTopRGB()
                : rgb(gradientStart);
        int end = palette.getMode() == Palette.Client ? GuiModule.getEnabledBottomRGB()
                : rgb(gradientEnd);
        start = 0xFF000000 | start & 0xFFFFFF;
        end = 0xFF000000 | end & 0xFFFFFF;
        float phase = (float) (((now / 4_000_000_000D * gradientSpeed.getInput()) % 1D)
                * (2 * Math.PI));
        for (int i = 0; i < LABEL.length(); i++)
            HEADER_COLORS[i] = mix(color(start, end, phase, i, -1), 0xFFFFFFFF, .13F);
        int index = 0;
        for (Row row : ordered) {
            for (int i = 0; i < row.colors.length; i++)
                row.colors[i] = color(start, end, phase, i, index);
            index++;
        }
        float pulse = .75F + .25F * (float) Math.sin(now * (2 * Math.PI / 3_000_000_000D));
        PhantomDiagnostics.once("layout", "LAYOUT ready: activeRows=" + active + ", states=" + states.size()
                + ", panel=" + panelWidth + "x" + listHeight + ", pos=" + position.getMode()
                + ", scale=" + zoom + ", header=" + header.isToggled() + ", values=" + values.isToggled());
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(px, py, 0);
            GlStateManager.scale(zoom, zoom, 1);
            drawShapes(panelWidth, right, start, end, pulse, now);
            if (header.isToggled()) {
                float headX = right ? panelWidth - headerWidth - badgeWidth - 16 : 9;
                // The gradient is one color per character; the font atlas, glyphs and colors are cached.
                drawText(TITLE, LABEL, headX, 7, 0xFFFFFFFF, HEADER_COLORS);
                drawText(VERSION_FONT, VERSION, headX + headerWidth + 3, 7, mix(start, end, .45F), null);
            }
            for (Row row : ordered) {
                if (row.opacity <= .01F) continue;
                float rowX = right ? panelWidth - row.width - 8 : 8;
                rowX += (right ? 1 : -1) * (1 - row.opacity) * 12;
                float textX = rowX + 4;
                if (right) textX += row.width - 12 - row.nameWidth
                        - (row.suffixWidth == 0 ? 0 : 5 + row.suffixWidth);
                drawText(BODY, row.name, textX, row.y + 2, rgba(0xFFFFFFFF, row.opacity), row.colors);
                float valueX = textX + row.nameWidth + 5;
                if (row.oldSuffix != null && row.valueBlend < .99F)
                    drawText(VALUE, row.oldSuffix, valueX, row.y + 3,
                            rgba(GRAY, row.opacity * (1 - row.valueBlend)), null);
                if (row.suffix != null)
                    drawText(VALUE, row.suffix, valueX, row.y + 3,
                            rgba(GRAY, row.opacity * row.valueBlend), null);
            }
        } finally { GlStateManager.popMatrix(); }
        PhantomDiagnostics.once("render-complete", "RENDER complete: shapes and text submitted successfully");
    }

    private void drawShapes(float panelWidth, boolean right, int start, int end, float pulse, long now) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int src = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        // The GUI projection flips Y; never inherit the world's face-cull state for HUD quads.
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        try {
            float a = glow.isToggled() ? (float) glowIntensity.getInput() / 100 * pulse : 0;
            if (header.isToggled()) {
                float headX = right ? panelWidth - headerWidth - badgeWidth - 16 : 9;
                if (a > 0) for (int layer = 3; layer >= 1; layer--)
                    rounded(headX - layer, 6 - layer, headerWidth + 8 + 2 * layer,
                            15 + 2 * layer, 4 + layer, rgba(start, a * .40F / layer));
                horizontal(9, 24, panelWidth - 9, 25, rgba(start, .6F), rgba(end, 0), right);
            }
            for (Row row : ordered) {
                if (row.opacity <= .01F) continue;
                float rowX = right ? panelWidth - row.width - 8 : 8;
                rowX += (right ? 1 : -1) * (1 - row.opacity) * 12;
                float y = row.y;
                if (ghostBackgroundFade.isToggled())
                    horizontal(rowX - 3, y, rowX + row.width + 3, y + 15,
                            rgba(DARK, row.opacity * .70F), rgba(DARK, 0), right);
                else rounded(rowX - 3, y, row.width + 6, 15, 3, rgba(DARK, row.opacity * .70F));
                if (a > 0) for (int layer = 3; layer >= 1; layer--)
                    rounded(rowX + 1 - layer, y + 2 - layer, row.nameWidth + 10 + 2 * layer,
                            11 + 2 * layer, 3 + layer, rgba(row.colors[0], a * row.opacity * .27F / layer));
                if (accentBar.getMode() != AccentBar.Off) {
                    boolean barRight = (accentBar.getMode() == AccentBar.Right) != right;
                    float barX = barRight ? rowX + row.width + 1 : rowX - 3;
                    vertical(barX, y + 1, barX + 2, y + 14,
                            rgba(row.colors[0], row.opacity * .83F), rgba(row.colors[0], 0));
                }
                if (row.shimmer < .25F && row.seen == sequence) {
                    float hit = rowX + 4 + row.shimmer * row.nameWidth / .25F;
                    horizontal(hit - 1, y + 3, hit, y + 12,
                            rgba(0xFFFFFFFF, .27F * row.opacity), rgba(start, 0), false);
                }
            }
            if (particles.isToggled()) for (int p = 0; p < 8; p++) {
                float progress = (float) ((now / 1_000_000_000D * (.14 + p % 3 * .06) + p * .137) % 1);
                float edge = right ? panelWidth - 5 : 5;
                solid(edge + (float) Math.sin(now / 1_200_000_000D + p * 2) * 3,
                        7 + (listHeight - 15) * (1 - progress), 1, 1,
                        rgba(p % 2 == 0 ? start : end, .11F * (1 - progress)));
            }
        } finally {
            GL11.glShadeModel(shade);
            GlStateManager.tryBlendFuncSeparate(src, dst, srcA, dstA);
            GlStateManager.depthMask(depthMask);
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (!blend) GlStateManager.disableBlend();
            if (texture) GlStateManager.enableTexture2D();
            GlStateManager.resetColor();
        }
    }

    /** Default CPS anchor: eight pixels below the arraylist and aligned to its active edge. */
    public static int defaultCpsX(int screenWidth, int cardWidth) {
        if (!layoutKnown) return 4;
        int value = Math.round(layoutRight ? layoutX + layoutWidth - cardWidth : layoutX);
        return Math.max(0, Math.min(screenWidth - cardWidth, value));
    }

    public static int defaultCpsY(int screenHeight, int cardHeight) {
        if (!layoutKnown) return 4;
        int value = Math.round(layoutY + layoutHeight + 8);
        return Math.max(0, Math.min(screenHeight - cardHeight, value));
    }

    private static void drawText(PhantomTTF font, String text, float x, float y, int color, int[] colors) {
        if (!fontFailed) try { font.draw(text, x, y, color, colors); return; }
        catch (RuntimeException | LinkageError ex) {
            fontFailed = true;
            System.err.println("[Phantom HUD] TTF renderer failed; using the bundled Poppins fallback");
            ex.printStackTrace();
        }
        if (FontUtil.poppinsMedium != null) {
            if (colors == null) FontUtil.poppinsMedium.drawStringWithShadow(text, x, y, color);
            else {
                float advance = x;
                for (int i = 0; i < text.length(); i++) {
                    String letter = text.substring(i, i + 1);
                    FontUtil.poppinsMedium.drawString(letter, advance, y, rgba(colors[i],
                            ((color >>> 24) & 255) / 255F));
                    advance += FontUtil.poppinsMedium.getStringWidth(letter);
                }
            }
        }
    }

    private static float width(PhantomTTF font, String text) {
        if (!fontFailed) try { return font.width(text); }
        catch (RuntimeException | LinkageError ex) {
            fontFailed = true;
            System.err.println("[Phantom HUD] TTF atlas failed; falling back to bundled Poppins");
            ex.printStackTrace();
        }
        return FontUtil.poppinsMedium == null ? text.length() * 6 : (float) FontUtil.poppinsMedium.getStringWidth(text);
    }

    private int color(int start, int end, float phase, int character, int row) {
        ColorMode mode = colorMode.getMode();
        if (mode == ColorMode.StaticAccent) return start;
        float wave = phase - character * .30F - row * .48F;
        if (mode == ColorMode.Rainbow)
            return 0xFF000000 | java.awt.Color.HSBtoRGB((wave / (2 * (float) Math.PI)) % 1, .65F, 1F) & 0xFFFFFF;
        if (mode == ColorMode.Aurora) return wave(0xFF00E7A0, 0xFF7B5CFF, wave);
        if (mode == ColorMode.Sunset) return wave(0xFFFF5F8F, 0xFFFFC857, wave);
        if (mode == ColorMode.Ice) return wave(0xFF4FC3F7, 0xFFD8F8FF, wave);
        if (mode == ColorMode.Candy) return wave(0xFFFF6EDC, 0xFF8B7CFF, wave);
        return wave(start, end, wave);
    }

    private static int wave(int start, int end, float phase) {
        return mix(start, end, .5F + .5F * (float) Math.sin(phase));
    }

    private static int rgb(PickerRGBSetting c) {
        return 0xFF000000 | c.getRed() << 16 | c.getGreen() << 8 | c.getBlue();
    }
    private static int rgba(int color, float alpha) {
        return (Math.max(0, Math.min(255, Math.round((color >>> 24) * Math.max(0, Math.min(1, alpha))))) << 24)
                | color & 0xFFFFFF;
    }
    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int g = Math.round(((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
    private static float approach(float current, float target, float dt, float speed) {
        return current + (target - current) * (1 - (float) Math.exp(-speed * dt));
    }
    private static void vertex(float x, float y, int color) {
        GL11.glColor4f(((color >>> 16) & 255) / 255F, ((color >>> 8) & 255) / 255F,
                (color & 255) / 255F, ((color >>> 24) & 255) / 255F);
        GL11.glVertex2f(x, y);
    }
    private static void horizontal(float x, float y, float x2, float y2, int left, int right, boolean reverse) {
        if (reverse) { int swap = left; left = right; right = swap; }
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x, y, left); vertex(x2, y, right);
        vertex(x2, y2, right); vertex(x, y2, left);
        GL11.glEnd();
    }
    private static void vertical(float x, float y, float x2, float y2, int top, int bottom) {
        GL11.glBegin(GL11.GL_QUADS);
        vertex(x, y, top); vertex(x2, y, top);
        vertex(x2, y2, bottom); vertex(x, y2, bottom);
        GL11.glEnd();
    }
    private static void solid(float x, float y, float w, float h, int color) {
        horizontal(x, y, x + w, y + h, color, color, false);
    }
    private static void rounded(float x, float y, float w, float h, float radius, int color) {
        if ((color >>> 24) == 0 || w <= 0 || h <= 0) return;
        float r = Math.min(radius, Math.min(w, h) / 2);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        vertex(x + w / 2, y + h / 2, color);
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner < 2 ? x + w - r : x + r;
            float cy = corner == 0 || corner == 3 ? y + r : y + h - r;
            for (int step = 0; step <= 5; step++) {
                double angle = (-90 + corner * 90 + step * 18) * Math.PI / 180;
                vertex(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r, color);
            }
        }
        vertex(x + w - r, y, color);
        GL11.glEnd();
    }
}
