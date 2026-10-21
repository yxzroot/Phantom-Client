/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * skid FDP Client
 * https://github.com/SkidderMC/FDPClient
 * Visual adaptation of NewArrayList.kt and Watermark.kt from lmx0721/AirClient.
 * LiquidBounce / CCBlueX, AirClient and FDPClient contributors; GPL-3.0-or-later.
 * Modified for Phantom on 2026-10-19: Phantom modules/settings/fonts/config, cached rows,
 * delta-time motion, null-safe game info, isolated GL and per-element failure handling.
 * WITHOUT ANY WARRANTY. See LICENSE and THIRD_PARTY_NOTICES.
 */
package keystrokesmod.client.module.modules.render;

import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.render.airvisuals.AirDraw;
import keystrokesmod.client.module.modules.render.airvisuals.AirTheme;
import keystrokesmod.client.utils.font.PhantomTTF;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/** Presentation only: retains Phantom's module identity, HUD host, setting values and config. */
final class AirHudRenderer {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Logger LOG = Logger.getLogger("Phantom/AirVisuals");
    private static final PhantomTTF BODY = new PhantomTTF(10, Font.PLAIN);
    private static final PhantomTTF TITLE = new PhantomTTF(11, Font.BOLD);
    private static final String BRAND = "PHANTOM";
    private static final int LIST = 1, WATERMARK = 2, INFO = 4;
    private static final float GAP = 17, HEIGHT = 16;
    private final IdentityHashMap<Module, Row> cache = new IdentityHashMap<>();
    private final ArrayList<Row> rows = new ArrayList<>();
    private final StringBuilder text = new StringBuilder(160);
    private final Date date = new Date();
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.ROOT);
    private long previousFrame, nextRefresh, nextInfo;
    private double seconds;
    private int disabledElements;
    private int displayWidth, displayHeight, guiScale;
    private ScaledResolution resolution;
    private String watermark = "", info = "";
    private float watermarkWidth, infoWidth, brandWidth, panelWidth, panelHeight;
    private int generation;

    private static final class Row {
        final Module module;
        final String name;
        String suffix;
        float nameWidth, suffixWidth, width, alpha, y;
        int seen;
        boolean active, broken;
        Row(Module module) { this.module = module; name = module.getName(); }
    }
    private static final Comparator<Row> LENGTH = (a, b) -> Float.compare(b.width, a.width);
    private static final Comparator<Row> NAME = (a, b) -> a.name.compareToIgnoreCase(b.name);
    private static final Comparator<Row> CATEGORY = (a, b) -> {
        int category = a.module.moduleCategory().ordinal() - b.module.moduleCategory().ordinal();
        return category == 0 ? NAME.compare(a, b) : category;
    };

    void reset() {
        cache.clear(); rows.clear(); previousFrame = nextRefresh = nextInfo = 0;
        disabledElements = 0; panelWidth = panelHeight = 0;
    }

    void render(PhantomHUD owner) {
        if ((disabledElements & (LIST | WATERMARK | INFO)) == (LIST | WATERMARK | INFO)) return;
        if (MC.thePlayer == null || MC.theWorld == null || MC.gameSettings.hideGUI) {
            previousFrame = 0;
            return;
        }
        long now = System.nanoTime();
        float dt = previousFrame == 0 ? 0 : Math.min(.1F, Math.max(0, (now - previousFrame) / 1_000_000_000F));
        previousFrame = now; seconds += dt;
        boolean resized = resolution == null || displayWidth != MC.displayWidth || displayHeight != MC.displayHeight
                || guiScale != MC.gameSettings.guiScale;
        if (resized) {
            resolution = new ScaledResolution(MC); displayWidth = MC.displayWidth;
            displayHeight = MC.displayHeight; guiScale = MC.gameSettings.guiScale;
            nextRefresh = nextInfo = 0;
        }
        // Font uploads use Minecraft's cache bookkeeping outside the raw-GL drawing scope.
        try { BODY.width(""); TITLE.width(""); brandWidth = TITLE.width(BRAND); }
        catch (RuntimeException | LinkageError error) { fail(LIST | WATERMARK | INFO, "HUD font", error); return; }
        if (now >= nextRefresh && (disabledElements & LIST) == 0) {
            try { refresh(owner); }
            catch (RuntimeException | LinkageError error) { fail(LIST, "module list", error); }
            nextRefresh = now + 100_000_000L;
        }
        if (now >= nextInfo) {
            if ((disabledElements & WATERMARK) == 0) try { refreshWatermark(owner); }
            catch (RuntimeException | LinkageError error) { fail(WATERMARK, "watermark", error); }
            if ((disabledElements & INFO) == 0) try { refreshInfo(); }
            catch (RuntimeException | LinkageError error) { fail(INFO, "game info", error); }
            nextInfo = now + 250_000_000L;
        }
        float targetY = owner.airHeader() && (disabledElements & WATERMARK) == 0 ? 29 : 0;
        float widest = 30;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            row.alpha = AirTheme.approach(row.alpha, row.active && !row.broken ? 1 : 0, dt, row.active ? 16 : 12);
            row.y = AirTheme.approach(row.y, targetY, dt, 15);
            targetY += GAP * row.alpha;
            if (row.alpha > .01F) widest = Math.max(widest, row.width);
        }
        float headerWidth = owner.airHeader() && (disabledElements & WATERMARK) == 0
                ? brandWidth + (owner.airWatermarkInfo() ? watermarkWidth + 16 : 0) + 18 : 0;
        widest = Math.max(widest, headerWidth);
        if (owner.airGameInfo() && (disabledElements & INFO) == 0) widest = Math.max(widest, infoWidth + 16);
        panelWidth = panelWidth == 0 ? widest : AirTheme.approach(panelWidth, widest, dt, 15);
        float targetH = targetY + (owner.airGameInfo() && (disabledElements & INFO) == 0 ? 23 : 0);
        panelHeight = panelHeight == 0 ? targetH : AirTheme.approach(panelHeight, targetH, dt, 15);
        float zoom = Math.max(.5F, Math.min(2F, owner.airScale()));
        zoom = Math.min(zoom, Math.max(.25F, (resolution.getScaledWidth() - 8F) / Math.max(1, panelWidth)));
        boolean right = owner.airPosition() == PhantomHUD.Position.TopRight || owner.airPosition() == PhantomHUD.Position.BottomRight;
        boolean bottom = owner.airPosition() == PhantomHUD.Position.BottomRight || owner.airPosition() == PhantomHUD.Position.BottomLeft;
        float x = right ? resolution.getScaledWidth() - owner.airX() - panelWidth * zoom : owner.airX();
        float y = bottom ? resolution.getScaledHeight() - owner.airY() - panelHeight * zoom : owner.airY();
        x = Math.max(2, x); y = Math.max(2, y);
        PhantomHUD.panelBounds(x, y, panelWidth * zoom, panelHeight * zoom, right);
        int start = owner.airTop(), end = owner.airBottom();
        if (owner.airColorMode() == PhantomHUD.ColorMode.Aurora) { start = 0xFF00E7A0; end = 0xFF7B5CFF; }
        else if (owner.airColorMode() == PhantomHUD.ColorMode.Sunset) { start = 0xFFFF5F8F; end = 0xFFFFC857; }
        else if (owner.airColorMode() == PhantomHUD.ColorMode.Ice) { start = 0xFF4FC3F7; end = 0xFFD8F8FF; }
        else if (owner.airColorMode() == PhantomHUD.ColorMode.Candy) { start = 0xFFFF6EDC; end = 0xFF8B7CFF; }

        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int program = GLContext.getCapabilities().OpenGL20 ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
        try {
            if (GLContext.getCapabilities().OpenGL20) GL20.glUseProgram(0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glTranslatef(x, y, 0); GL11.glScalef(zoom, zoom, 1);
            if (owner.airHeader() && (disabledElements & WATERMARK) == 0) try {
                float hx = right ? panelWidth - headerWidth : 0;
                AirDraw.card(hx, 0, headerWidth, 23, 5.4F, 0xB510131B, .55F);
                AirDraw.gradient(hx + 5, 0, headerWidth - 10, 1.5F, .75F, start, end, false);
                text(TITLE, BRAND, hx + 8, 6, color(owner, start, end, 0, 1));
                if (owner.airWatermarkInfo()) text(BODY, watermark, hx + brandWidth + 16, 7, 0xFFE3E6ED);
            } catch (RuntimeException | LinkageError error) { fail(WATERMARK, "watermark draw", error); }
            if ((disabledElements & LIST) == 0) try {
                for (int i = 0; i < rows.size(); i++) {
                    Row row = rows.get(i);
                    if (row.alpha <= .01F || row.broken) continue;
                    float rx = (right ? panelWidth - row.width : 0) + (right ? 1 : -1) * (1 - row.alpha) * 12;
                    int accent = color(owner, start, end, i, row.alpha);
                    AirDraw.card(rx, row.y, row.width, HEIGHT, 3.5F, AirTheme.alpha(0xA810131B, row.alpha), row.alpha * .4F);
                    AirDraw.gradient(right ? rx + row.width - 2 : rx, row.y + 2, 2, HEIGHT - 4, 1,
                            accent, AirTheme.alpha(accent, .4F), true);
                    text(BODY, row.name, rx + 7, row.y + 3, accent);
                    if (row.suffix != null) text(BODY, row.suffix, rx + 11 + row.nameWidth, row.y + 3,
                            AirTheme.alpha(0xFFB5BBC8, row.alpha));
                }
            } catch (RuntimeException | LinkageError error) { fail(LIST, "arraylist draw", error); }
            if (owner.airGameInfo() && (disabledElements & INFO) == 0) try {
                float ix = right ? panelWidth - infoWidth - 16 : 0;
                AirDraw.card(ix, targetY + 3, infoWidth + 16, 18, 4, 0xA810131B, .4F);
                text(BODY, info, ix + 8, targetY + 7, 0xFFD8DEEA);
            } catch (RuntimeException | LinkageError error) { fail(INFO, "game info draw", error); }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
            GL11.glPopAttrib();
            if (GLContext.getCapabilities().OpenGL20) GL20.glUseProgram(program);
            GL13.glActiveTexture(unit); GL11.glMatrixMode(mode);
        }
    }

    private void refresh(PhantomHUD owner) {
        generation++;
        List<Module> roster = owner.airRoster();
        for (int i = 0; i < roster.size(); i++) {
            Module module = roster.get(i);
            if (module == owner || !module.showInHud()) continue;
            Row row = cache.get(module);
            if (row == null) { row = new Row(module); cache.put(module, row); }
            row.seen = generation;
            row.active = module.isEnabled();
            if (row.broken) continue;
            try {
                String suffix = row.active && owner.airValues() ? module.getHudSuffix() : row.active ? null : row.suffix;
                if (suffix != null && suffix.isEmpty()) suffix = null;
                row.suffix = suffix;
                row.nameWidth = BODY.width(row.name);
                row.suffixWidth = suffix == null ? 0 : BODY.width(suffix);
                row.width = row.nameWidth + (suffix == null ? 0 : row.suffixWidth + 4) + 16;
            } catch (RuntimeException | LinkageError error) {
                row.broken = true; row.active = false;
                LOG.log(Level.WARNING, "Disabled only the HUD row for " + row.name, error);
            }
        }
        rows.clear();
        for (Row row : cache.values()) {
            if (row.seen != generation) row.active = false;
            if (!row.broken && (row.active || row.alpha > .005F)) rows.add(row);
        }
        Comparator<Row> order = owner.airSort() == PhantomHUD.SortMode.Alphabetical ? NAME
                : owner.airSort() == PhantomHUD.SortMode.Category ? CATEGORY : LENGTH;
        rows.sort(order);
    }

    private void refreshWatermark(PhantomHUD owner) {
        int ping = 0;
        if (MC.getNetHandler() != null) {
            NetworkPlayerInfo playerInfo = MC.getNetHandler().getPlayerInfo(MC.thePlayer.getUniqueID());
            if (playerInfo != null) ping = Math.max(0, playerInfo.getResponseTime());
        }
        date.setTime(System.currentTimeMillis());
        text.setLength(0);
        text.append(MC.thePlayer.getName()).append("  |  ").append(Minecraft.getDebugFPS()).append(" FPS  |  ")
                .append(ping).append(" ms  |  ").append(clock.format(date));
        String updated = text.toString();
        if (!updated.equals(watermark)) { watermark = updated; watermarkWidth = BODY.width(watermark); }
    }

    private void refreshInfo() {
        double dx = MC.thePlayer.posX - MC.thePlayer.prevPosX, dz = MC.thePlayer.posZ - MC.thePlayer.prevPosZ;
        if (!Double.isFinite(dx) || !Double.isFinite(dz)) return;
        long speed = Math.round(Math.sqrt(dx * dx + dz * dz) * 200);
        text.setLength(0);
        text.append("XYZ ").append((int) Math.floor(MC.thePlayer.posX)).append(' ')
                .append((int) Math.floor(MC.thePlayer.posY)).append(' ').append((int) Math.floor(MC.thePlayer.posZ))
                .append("  |  ").append(speed / 10).append('.').append(speed % 10).append(" BPS");
        String updated = text.toString();
        if (!updated.equals(info)) { info = updated; infoWidth = BODY.width(info); }
    }

    private int color(PhantomHUD owner, int start, int finish, int index, float alpha) {
        int color;
        double phase = seconds * owner.airSpeed();
        if (owner.airColorMode() == PhantomHUD.ColorMode.StaticAccent) color = start;
        else if (owner.airColorMode() == PhantomHUD.ColorMode.Rainbow)
            color = java.awt.Color.HSBtoRGB((float) ((phase * .12 + index * .035) % 1), .65F, 1);
        else color = AirTheme.mix(start, finish, .5F + .5F * (float) Math.sin(phase * 1.6 - index * .35));
        return AirTheme.alpha(color, alpha);
    }
    private static void text(PhantomTTF font, String value, float x, float y, int color) {
        font.drawHud(value, x + .5F, y + .5F, AirTheme.alpha(0xFF000000, (color >>> 24) / 255F * .4F));
        font.drawHud(value, x, y, color);
    }
    private void fail(int element, String name, Throwable error) {
        if ((disabledElements & element) == element) return;
        disabledElements |= element;
        LOG.log(Level.WARNING, "Disabled " + name + " until Phantom HUD is toggled; game rendering continues", error);
    }
}
