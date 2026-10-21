package keystrokesmod.client.module.modules.render;

import java.awt.Font;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import keystrokesmod.client.clickgui.apple.rebuild.MotionTokens;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.utils.font.PhantomTTF;
import keystrokesmod.client.utils.font.PhantomWordmark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/** HUD-safe immediate AA and cached Poppins atlas; never enters the ClickGUI frame session. */
final class PanelsHud {
    private static final PhantomTTF FONT = new PhantomTTF(11, Font.PLAIN);
    private static final float HEIGHT = 24F, GAP = 3F, PADDING = 10F;
    private final IdentityHashMap<Module, Entry> cache = new IdentityHashMap<>();
    private final ArrayList<Entry> rows = new ArrayList<>();
    private final ArrayList<Entry> sorted = new ArrayList<>();
    private long previous;
    private ScaledResolution screen;
    private int displayWidth, displayHeight, guiScale;
    private PhantomHUD.SortMode lastSort;
    private boolean lastValues, dirty = true;

    private static final class Motion {
        float value, from, target, elapsed, duration, velocity;
        void set(float next, float seconds, boolean animate) {
            if (!animate) { value = from = target = next; elapsed = duration = velocity = 0; return; }
            if (target == next) return;
            from = value; target = next; duration = seconds; elapsed = 0F;
        }
        void update(float dt) {
            if (value == target) return;
            float before = value;
            elapsed = Math.min(duration, elapsed + dt);
            value = from + (target - from) * MotionTokens.ease(elapsed / duration);
            velocity = (value - before) / Math.max(dt, .001F);
            if (elapsed == duration) value = target;
        }
    }

    private static final class Entry {
        final Module module;
        final String name;
        String suffix, previousSuffix;
        float nameWidth, suffixWidth;
        final Motion width = new Motion(), opacity = new Motion(), slot = new Motion(), value = new Motion();
        boolean visible;
        Entry(Module module) { this.module = module; name = module.getName(); }
    }

    void reset() { cache.clear(); rows.clear(); sorted.clear(); dirty = true; previous = 0; screen = null; }

    void render(PhantomPanels hud) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();
        float dt = previous == 0 ? .016F : Math.min(.05F, (now - previous) / 1_000_000_000F);
        previous = now;
        boolean animate = hud.panelAnimations();
        boolean values = hud.panelShowValues();
        if (lastSort != hud.panelSort() || lastValues != values) { lastSort = hud.panelSort(); lastValues = values; dirty = true; }
        List<Module> roster = hud.panelRoster();
        for (int i = 0; i < roster.size(); i++) {
            Module module = roster.get(i);
            if (module == hud) continue;
            boolean enabled = module.isEnabled() && module.showInHud();
            Entry row = cache.get(module);
            if (row == null && !enabled) continue;
            if (row == null) {
                row = new Entry(module); row.nameWidth = FONT.width(row.name);
                cache.put(module, row); rows.add(row); dirty = true;
            }
            if (row.visible != enabled) {
                row.visible = enabled; dirty = true;
                row.opacity.set(enabled ? 1F : 0F, enabled ? .20F : .16F, animate);
                // Disable fades/slides first; its slot collapses when the exit has completed.
                if (enabled) row.slot.set(1F, .20F, animate);
            }
            if (!enabled) continue;
            String suffix = values ? module.getHudSuffix() : null;
            if (suffix != null && suffix.isEmpty()) suffix = null;
            if (suffix != row.suffix && (suffix == null || !suffix.equals(row.suffix))) {
                row.previousSuffix = row.suffix;
                row.suffix = suffix; row.suffixWidth = suffix == null ? 0F : FONT.width(suffix);
                row.value.set(0F, .12F, false); row.value.set(1F, .12F, animate);
                dirty = true;
            }
            row.width.set(row.nameWidth + (row.suffix == null ? 0F : 6F + row.suffixWidth) + 2F * PADDING,
                    .20F, animate);
            if (row.width.value == 0F) row.width.set(row.width.target, .20F, false);
        }
        for (int i = rows.size() - 1; i >= 0; i--) {
            Entry row = rows.get(i);
            if (!row.visible && row.opacity.value <= .001F) row.slot.set(0F, .16F, animate);
            row.opacity.update(dt); row.slot.update(dt); row.width.update(dt); row.value.update(dt);
            if (!row.visible && row.slot.value <= .001F) { cache.remove(row.module); rows.remove(i); dirty = true; }
        }
        if (dirty) {
            sorted.clear(); sorted.addAll(rows);
            if (hud.panelSort() == PhantomHUD.SortMode.Alphabetical) sorted.sort((a,b) -> a.name.compareToIgnoreCase(b.name));
            else if (hud.panelSort() == PhantomHUD.SortMode.Category) sorted.sort((a,b) -> {
                int n = a.module.moduleCategory().ordinal() - b.module.moduleCategory().ordinal();
                return n != 0 ? n : a.name.compareToIgnoreCase(b.name);
            });
            else sorted.sort((a,b) -> Float.compare(b.width.target, a.width.target));
            dirty = false;
        }
        if (screen == null || displayWidth != mc.displayWidth || displayHeight != mc.displayHeight || guiScale != mc.gameSettings.guiScale) {
            screen = new ScaledResolution(mc);
            displayWidth = mc.displayWidth; displayHeight = mc.displayHeight; guiScale = mc.gameSettings.guiScale;
        }
        PhantomHUD.Position corner = hud.panelPosition();
        boolean right = corner == PhantomHUD.Position.TopRight || corner == PhantomHUD.Position.BottomRight;
        boolean bottom = corner == PhantomHUD.Position.BottomRight || corner == PhantomHUD.Position.BottomLeft;
        float zoom = hud.panelScale();
        float total = hud.panelLogo() ? HEIGHT + 6F : 0F;
        for (int i = 0; i < sorted.size(); i++) total += (HEIGHT + GAP) * sorted.get(i).slot.value;
        float edgeX = right ? screen.getScaledWidth() - Math.max(6F, hud.panelXOffset()) : Math.max(6F, hud.panelXOffset());
        float top = bottom ? screen.getScaledHeight() - Math.max(6F, hud.panelYOffset()) - total * zoom : Math.max(6F, hud.panelYOffset());
        float widest = 0F;
        for (int i = 0; i < sorted.size(); i++) widest = Math.max(widest, sorted.get(i).width.value);
        if (hud.panelLogo()) widest = Math.max(widest, PhantomWordmark.available() ? PhantomWordmark.width(16F) : FONT.width("PHANTOM"));
        hud.panelBounds(right ? edgeX - widest * zoom : edgeX, top, widest * zoom, total * zoom, right);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(edgeX, top, 0F); GL11.glScalef(zoom, zoom, 1F);
            GL11.glEnable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL13.GL_MULTISAMPLE);
            float cursor = 0F;
            if (hud.panelLogo()) {
                float logoWidth = PhantomWordmark.available() ? PhantomWordmark.width(16F) : FONT.width("PHANTOM");
                if (!PhantomWordmark.draw(right ? -logoWidth : 0F, 4F, 16F, 1F))
                    FONT.drawHud("PHANTOM", right ? -logoWidth : 0F, 3F, 0xFFFFFFFF);
                cursor = HEIGHT + 6F;
            }
            int count = sorted.size();
            for (int i = 0; i < count; i++) {
                Entry row = sorted.get(i);
                float alpha = row.opacity.value;
                if (alpha > .001F) {
                    float w = row.width.value, xx = (right ? -w : 0F) + w * (1F - alpha);
                    float yy = cursor;
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    rounded(xx, yy, w, HEIGHT, hud.panelRadius(), rgba(0xFF0D1016, alpha * hud.panelOpacity()));
                    if (hud.panelBar()) {
                        int tint = barColor(hud, now, i, Math.max(1,count));
                        edgeBar(xx, yy, w, HEIGHT, hud.panelRadius(), right,
                                rgba(tint, alpha * (hud.panelBarColor() == PhantomHUD.BarColor.White ? .85F : 1F)));
                    }
                    FONT.drawHud(row.name, xx + PADDING, yy + 5F, rgba(0xFFFFFFFF, alpha));
                    float valueX = xx + PADDING + row.nameWidth + 6F;
                    int valueTint = hud.panelValueColor() == PhantomHUD.ValueColor.Custom ? hud.panelCustomColor()
                            : hud.panelValueColor() == PhantomHUD.ValueColor.Accent ? mix(hud.panelAccent(), 0xFFFFFFFF, .30F) : 0xFF8FA3C7;
                    if (row.previousSuffix != null && row.value.value < .999F)
                        FONT.drawHud(row.previousSuffix, valueX, yy + 5F, rgba(valueTint, alpha * (1F - row.value.value)));
                    if (row.suffix != null) FONT.drawHud(row.suffix, valueX, yy + 5F, rgba(valueTint, alpha * row.value.value));
                }
                cursor += (HEIGHT + GAP) * row.slot.value;
            }
        } finally {
            GL11.glPopMatrix(); GL11.glPopClientAttrib(); GL11.glPopAttrib();
        }
    }

    private static int barColor(PhantomPanels hud, long now, int i, int count) {
        switch (hud.panelBarColor()) {
            case Accent: return hud.panelAccent();
            case Theme: return keystrokesmod.client.clickgui.apple.rebuild.GuiTokens.accent();
            case Gradient: return mix(hud.panelAccent(), hud.panelGradientEnd(), count == 1 ? 0F : i / (float) (count - 1));
            case Rainbow: return 0xFF000000 | java.awt.Color.HSBtoRGB((float) ((now / 8_000_000_000D + i * .06D) % 1D), .55F, 1F) & 0xFFFFFF;
            default: return 0xFFFFFFFF;
        }
    }

    private static void rounded(float x, float y, float w, float h, float radius, int color) {
        if ((color >>> 24) == 0) return;
        float r = Math.max(0F, Math.min(radius, Math.min(w,h) * .5F));
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        vertex(x+w*.5F,y+h*.5F,color);
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner < 2 ? x+w-r : x+r;
            float cy = corner == 0 || corner == 3 ? y+r : y+h-r;
            for (int step = 0; step <= 8; step++) {
                double angle = (-90 + corner*90 + step*11.25) * Math.PI / 180D;
                vertex(cx+(float)Math.cos(angle)*r,cy+(float)Math.sin(angle)*r,color);
            }
        }
        // GL_TRIANGLE_FAN does not close itself: without this top edge the
        // center-to-top-left diagonal cuts a wedge out of every row.
        vertex(x+w-r,y,color);
        GL11.glEnd();
    }

    /** Two-pixel edge strip clipped to the same rounded outline as the row. */
    private static void edgeBar(float x, float y, float w, float h, float radius, boolean right, int color) {
        if ((color >>> 24) == 0) return;
        float r = Math.max(0F, Math.min(radius, Math.min(w, h) * .5F));
        float outer = right ? x+w : x, inner = right ? x+w-2F : x+2F;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        if (r == 0F) {
            barPair(inner, outer, y, right, color);
            barPair(inner, outer, y+h, right, color);
        } else {
            for (int step = 0; step <= 8; step++) {
                float dy = r * step / 8F;
                float inset = r - (float) Math.sqrt(Math.max(0F, r*r - (r-dy)*(r-dy)));
                float edge = outer + (right ? -inset : inset);
                barPair(right ? Math.min(inner, edge) : Math.max(inner, edge), edge, y+dy, right, color);
            }
            barPair(inner, outer, y+h-r, right, color);
            for (int step = 1; step <= 8; step++) {
                float dy = r * step / 8F;
                float inset = r - (float) Math.sqrt(Math.max(0F, r*r - dy*dy));
                float edge = outer + (right ? -inset : inset);
                barPair(right ? Math.min(inner, edge) : Math.max(inner, edge), edge, y+h-r+dy, right, color);
            }
        }
        GL11.glEnd();
    }
    private static void barPair(float inner, float outer, float y, boolean right, int color) {
        vertex(right ? inner : outer, y, color);
        vertex(right ? outer : inner, y, color);
    }
    private static void vertex(float x, float y, int color) {
        GL11.glColor4f((color >>> 16 & 255)/255F,(color >>> 8 & 255)/255F,(color & 255)/255F,(color >>> 24)/255F);
        GL11.glVertex2f(x,y);
    }
    private static int rgba(int color, float alpha) {
        return (Math.max(0,Math.min(255,Math.round((color >>> 24)*Math.max(0F,Math.min(1F,alpha))))) << 24) | color & 0xFFFFFF;
    }
    private static int mix(int a,int b,float p) {
        p=Math.max(0F,Math.min(1F,p));
        return 0xFF000000 | Math.round((a>>>16&255)*(1F-p)+(b>>>16&255)*p)<<16
                | Math.round((a>>>8&255)*(1F-p)+(b>>>8&255)*p)<<8
                | Math.round((a&255)*(1F-p)+(b&255)*p);
    }
}
