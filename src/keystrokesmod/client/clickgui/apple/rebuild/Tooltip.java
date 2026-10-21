package keystrokesmod.client.clickgui.apple.rebuild;

import java.awt.Font;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import org.lwjgl.opengl.GL11;

/** One hover timer and one un-clipped tooltip for every help circle. */
public final class Tooltip {
    private static final GuiFont TITLE = new GuiFont(15F, Font.BOLD);
    private static final GuiFont DESCRIPTION = new GuiFont(14F, Font.PLAIN);
    private static final GuiFont KEY = new GuiFont(12F, Font.PLAIN);
    private static final float MAX_TEXT = 280F;
    private final Anim alpha = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, .12F);
    private final Anim lift = new Anim(3F, Anim.Mode.EASE_OUT_CUBIC, .12F);
    private final IdentityHashMap<Object, Lines> cache = new IdentityHashMap<Object, Lines>();
    private Object owner;
    private Lines active;
    private float circleX, circleY, boxX, boxY, boxW, boxH, mouseX, mouseY;
    private long started, lastSeen;
    private boolean seen;

    private static final class Lines {
        final String title, description, key;
        final ArrayList<String> wrapped = new ArrayList<String>();
        float width, scale = -1F;
        Lines(String title, String description, String key) {
            this.title = title; this.description = description; this.key = key;
        }
        void measure() {
            float current = GuiRenderContext.uiScale();
            if (scale == current) return;
            scale = current; width = Math.max(TITLE.width(title), key.length() == 0 ? 0F : KEY.width(key));
            wrapped.clear();
            for (String paragraph : description.split("\\n", -1)) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split(" +")) {
                    if (line.length() > 0 && DESCRIPTION.width(line.toString() + " " + word) > MAX_TEXT) {
                        String text = line.toString(); wrapped.add(text); width = Math.max(width, DESCRIPTION.width(text)); line.setLength(0);
                    }
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
                String text = line.toString(); wrapped.add(text); width = Math.max(width, DESCRIPTION.width(text));
            }
            width = Math.min(MAX_TEXT, width);
        }
    }

    /** Returns completion of the delay for the caller's accent ring. */
    public float hover(Object item, String title, String description, String key, float cx, float cy, float mx, float my) {
        if (description == null || description.isEmpty()) return 0F;
        long now = System.nanoTime();
        if (owner != item || now - lastSeen > 100_000_000L) {
            owner = item; started = now;
            alpha.snapTo(0F); lift.snapTo(3F);
        }
        Lines lines = cache.get(item);
        if (lines == null || !lines.title.equals(title) || !lines.description.equals(description) || !lines.key.equals(key)) {
            lines = new Lines(title, description, key); cache.put(item, lines);
        }
        active = lines; active.measure();
        circleX = cx; circleY = cy; mouseX = mx; mouseY = my; lastSeen = now; seen = true;
        float progress = Math.min(1F, (now - started) / (GuiPreferences.tooltipDelay() * 1_000_000_000F));
        if (progress >= 1F) { alpha.setDuration(.12F); lift.setDuration(.12F); alpha.setTarget(1F); lift.setTarget(0F); }
        return progress;
    }

    /** Legacy text without a help circle is intentionally not a hover target. */
    public void hover(String value, float mx, float my) { }
    public void clear() { seen = false; }
    public void pointer(float x, float y) { mouseX = x; mouseY = y; }
    public static boolean hit(float cx, float cy, float x, float y) {
        float dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= 12F * 12F;
    }
    public void helpIcon(Object item, String title, String description, float cx, float cy, float mx, float my, float opacity) {
        UiDraw.outline(cx - 8F, cy - 8F, cx + 8F, cy + 8F, 8F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, opacity));
        boolean over = opacity > .01F && hit(cx, cy, mx, my);
        UiDraw.small("?", cx - 4F, cy - UiDraw.smallHeight() * .5F - 1F,
                UiDraw.alpha(over ? 0xFFFFFFFF : GuiTokens.TEXT_SECONDARY, opacity));
        if (over) ring(cx, cy, 10F, hover(item, title, description, "", cx, cy, mx, my), opacity);
    }
    public void dismiss() { owner = active = null; seen = false; started = lastSeen = 0L; alpha.snapTo(0F); lift.snapTo(3F); }
    public void update(float dt) {
        boolean overBox = alpha.get() > .01F && UiDraw.inside(mouseX, mouseY, boxX, boxY, boxX + boxW, boxY + boxH);
        if (!seen && !overBox) {
            owner = null; started = 0L;
            alpha.setDuration(.06F); lift.setDuration(.06F); alpha.setTarget(0F); lift.setTarget(3F);
        }
        seen = false; alpha.update(dt); lift.update(dt);
    }
    public void draw(float screenW, float screenH) {
        if (active == null) return;
        if (alpha.get() > .01F && UiDraw.inside(mouseX, mouseY, boxX, boxY, boxX + boxW, boxY + boxH)) {
            seen = true; lastSeen = System.nanoTime(); alpha.setTarget(1F);
        }
        if (alpha.get() <= .01F) return;
        active.measure();
        float width = Math.min(screenW - 16F, active.width + 28F);
        float height = 26F + active.wrapped.size() * 19F + (active.key.length() == 0 ? 0F : 21F) + 13F;
        float x = circleX + 19F, y = circleY + 19F;
        if (x + width > screenW - 8F) x = circleX - width - 19F;
        if (y + height > screenH - 8F) y = circleY - height - 19F;
        boxX = x = Math.max(8F, x); boxY = y = Math.max(8F, y); boxW = width; boxH = height;
        float a = alpha.get(), drawnY = y - lift.get();
        UiDraw.shadow(x, drawnY, x + width, drawnY + height, 10F, UiDraw.alpha(0xFF000000, a * .55F), 10);
        UiDraw.rounded(x, drawnY, x + width, drawnY + height, 10F, UiDraw.alpha(0xF50A0D12, a));
        UiDraw.outline(x, drawnY, x + width, drawnY + height, 10F, UiDraw.alpha(0xFF2A2F3B, a), GuiRenderContext.inverseUiScale());
        boolean flippedLeft = x + width < circleX;
        float caretX = Math.max(x + 10F, Math.min(x + width - 10F, circleX));
        boolean above = y < circleY;
        float pointY = above ? drawnY + height + 8F : drawnY - 8F;
        float baseY = above ? drawnY + height : drawnY;
        UiDraw.flushShapes();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(10F / 255F, 13F / 255F, 18F / 255F, a * 245F / 255F);
        GL11.glBegin(GL11.GL_TRIANGLES);
        if (flippedLeft) {
            float caretY = Math.max(drawnY + 12F, Math.min(drawnY + height - 12F, circleY));
            GL11.glVertex2f(x + width, caretY - 8F); GL11.glVertex2f(x + width, caretY + 8F);
            GL11.glVertex2f(x + width + 8F, caretY);
        } else {
            GL11.glVertex2f(caretX - 8F, baseY); GL11.glVertex2f(caretX + 8F, baseY); GL11.glVertex2f(caretX, pointY);
        }
        GL11.glEnd(); GL11.glPopAttrib();
        TITLE.draw(active.title, x + 14F, drawnY + 12F, UiDraw.alpha(0xFFFFFFFF, a));
        float lineY = drawnY + 35F;
        for (int i = 0; i < active.wrapped.size(); i++) { DESCRIPTION.draw(active.wrapped.get(i), x + 14F, lineY, UiDraw.alpha(0xFFA8AFC0, a)); lineY += 19F; }
        if (active.key.length() > 0) KEY.draw(active.key, x + 14F, lineY + 3F, UiDraw.alpha(GuiTokens.accent(), a));
    }

    public static void ring(float x, float y, float radius, float progress, float alpha) {
        if (progress <= 0F || alpha <= 0F) return;
        UiDraw.flushShapes(); UiDraw.flushText();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(1.5F * GuiRenderContext.uiScale());
        int c = GuiTokens.accent(); GL11.glColor4f((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, alpha);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        int count = Math.max(1, (int) (48F * progress));
        for (int i = 0; i <= count; i++) {
            double angle = (-.5D + i / 48D * 2D) * Math.PI;
            GL11.glVertex2d(x + radius * Math.cos(angle), y + radius * Math.sin(angle));
        }
        GL11.glEnd(); GL11.glPopAttrib();
    }
}
