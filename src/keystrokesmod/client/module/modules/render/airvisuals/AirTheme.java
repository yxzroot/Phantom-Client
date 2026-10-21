/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * Adapted from ClientThemesUtils.kt and ThemeManager.kt, lmx0721/AirClient.
 * GPL-3.0-or-later; WITHOUT ANY WARRANTY. See LICENSE and THIRD_PARTY_NOTICES.
 * Modified for Phantom on 2026-10-19: packed colors, cached state and time-based cross-fades.
 */
package keystrokesmod.client.module.modules.render.airvisuals;

/** Render-thread color state. No Minecraft, event bus, settings, Color objects or per-frame maps. */
public final class AirTheme {
    private AirPalette selected;
    private int fromTop, fromBottom, top, bottom, targetTop, targetBottom;
    private long started;
    private float duration;

    public void update(long now, AirPalette preset, int clientTop, int clientBottom, float fadeSeconds) {
        if (preset == null) preset = AirPalette.Client;
        int nextTop = preset == AirPalette.Client ? clientTop | 0xFF000000 : preset.start;
        int nextBottom = preset == AirPalette.Client ? clientBottom | 0xFF000000 : preset.end;
        sample(now);
        if (selected == null) {
            top = fromTop = targetTop = clientTop | 0xFF000000;
            bottom = fromBottom = targetBottom = clientBottom | 0xFF000000;
        }
        if (selected != preset) {
            fromTop = top; fromBottom = bottom; started = now;
            duration = Float.isFinite(fadeSeconds) ? Math.max(.05F, Math.min(2F, fadeSeconds)) : .2F;
            targetTop = nextTop; targetBottom = nextBottom; selected = preset;
        } else if (preset == AirPalette.Client) {
            targetTop = nextTop; targetBottom = nextBottom;
        }
        sample(now);
    }

    private void sample(long now) {
        if (selected == null) return;
        float t = duration <= 0 ? 1 : (float) Math.max(0, Math.min(1, (now - started) / (duration * 1_000_000_000D)));
        t = t * t * (3 - 2 * t);
        top = mix(fromTop, targetTop, t); bottom = mix(fromBottom, targetBottom, t);
    }

    public int top() { return top; }
    public int bottom() { return bottom; }
    public int color(double seconds, int index, boolean rainbow) {
        if (rainbow) return java.awt.Color.HSBtoRGB((float) ((seconds * .12 + index * .035) % 1), .65F, 1);
        return mix(top, bottom, .5F + .5F * (float) Math.sin(seconds * 1.6 - index * .35));
    }
    public static int mix(int a, int b, float t) {
        t = Float.isFinite(t) ? Math.max(0, Math.min(1, t)) : 0;
        int r = Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int blue = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | blue;
    }
    public static int alpha(int color, float opacity) {
        float safe = Float.isFinite(opacity) ? Math.max(0, Math.min(1, opacity)) : 0;
        return Math.round((color >>> 24) * safe) << 24 | color & 0xFFFFFF;
    }
    public static float approach(float current, float target, float seconds, float rate) {
        if (!Float.isFinite(current)) current = target;
        if (!Float.isFinite(seconds) || seconds <= 0) return current;
        return current + (target - current) * (1 - (float) Math.exp(-Math.min(.25F, seconds) * rate));
    }
}
