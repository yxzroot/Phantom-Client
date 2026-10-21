/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * skid FDP Client
 * https://github.com/SkidderMC/FDPClient
 * Adapted from RoundedUtil.kt / rounded gradient presentation, lmx0721/AirClient.
 * GPL-3.0-or-later; WITHOUT ANY WARRANTY. See LICENSE and THIRD_PARTY_NOTICES.
 * Modified for Phantom on 2026-10-19: precomputed arcs, bounded shadows, exact scoped GL restore.
 */
package keystrokesmod.client.module.modules.render.airvisuals;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

/** In-game HUD geometry only. All GL mutations are raw and are undone before returning. */
public final class AirDraw {
    private static final float[] COS = new float[37], SIN = new float[37];
    static {
        for (int i = 0; i <= 36; i++) {
            double angle = Math.toRadians(-90 + i * 10);
            COS[i] = (float) Math.cos(angle); SIN[i] = (float) Math.sin(angle);
        }
    }
    private AirDraw() { }

    public static void card(float x, float y, float w, float h, float radius, int color, float shadow) {
        if (!valid(x, y, w, h, radius)) return;
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        begin();
        try {
            // Three bounded rings instead of AirClient's framebuffer shadow pipeline.
            if (shadow > 0) for (int ring = 3; ring > 0; ring--)
                shape(x - ring, y - ring + 1, w + ring * 2, h + ring * 2,
                        radius + ring, AirTheme.alpha(0xFF000000, shadow * .08F), AirTheme.alpha(0xFF000000, shadow * .08F), false);
            shape(x, y, w, h, radius, color, color, false);
        } finally { end(unit); }
    }

    public static void gradient(float x, float y, float w, float h, float radius, int start, int finish, boolean vertical) {
        if (!valid(x, y, w, h, radius)) return;
        int unit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        begin();
        try { shape(x, y, w, h, radius, start, finish, vertical); }
        finally { end(unit); }
    }

    private static boolean valid(float x, float y, float w, float h, float radius) {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(w) && Float.isFinite(h)
                && Float.isFinite(radius) && w > 0 && h > 0 && w <= 16384 && h <= 16384;
    }

    private static void begin() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING); GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false); GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }
    private static void end(int unit) {
        GL11.glPopAttrib(); GL13.glActiveTexture(unit);
    }
    private static void shape(float x, float y, float w, float h, float radius, int first, int last, boolean vertical) {
        float r = Math.max(0, Math.min(radius, Math.min(w, h) * .5F));
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        try {
            vertex(x + w * .5F, y + h * .5F, blend(first, last, .5F));
            for (int corner = 0; corner < 4; corner++) {
                float cx = corner < 2 ? x + w - r : x + r;
                float cy = corner == 0 || corner == 3 ? y + r : y + h - r;
                for (int step = 0; step <= 9; step++) {
                    int i = corner * 9 + step;
                    float vx = cx + COS[i] * r, vy = cy + SIN[i] * r;
                    vertex(vx, vy, blend(first, last, vertical ? (vy - y) / h : (vx - x) / w));
                }
            }
            vertex(x + w - r, y, blend(first, last, vertical ? 0 : (w - r) / w));
        } finally { GL11.glEnd(); }
    }
    private static int blend(int first, int last, float t) {
        int rgb = AirTheme.mix(first, last, t);
        int alpha = Math.round((first >>> 24) + ((last >>> 24) - (first >>> 24)) * t);
        return alpha << 24 | rgb & 0xFFFFFF;
    }
    private static void vertex(float x, float y, int color) {
        GL11.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                (color & 255) / 255F, (color >>> 24) / 255F);
        GL11.glVertex2f(x, y);
    }
}
