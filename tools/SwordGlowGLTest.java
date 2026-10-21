import keystrokesmod.client.module.modules.render.swordglow.AnimationStyle;
import keystrokesmod.client.module.modules.render.swordglow.GlintPalette;
import keystrokesmod.client.module.modules.render.swordglow.SwordAnimator;

/**
 * Regression for the adapter that feeds delta-time animation values into
 * Minecraft 1.8.9's RenderItem.renderEffect texture-matrix arithmetic.
 * Rendering itself is intentionally vanilla-owned; it is no longer duplicated here.
 */
public final class SwordGlowGLTest {
    private static final float EPSILON = 1F / (3000F * 8F) + .000001F;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static float vanillaOffset(long clock, int pass) {
        if (pass == 0) return (clock % 3000L) / 3000F / 8F;
        return -(clock % 4873L) / 4873F / 8F;
    }
    private static long clock(SwordAnimator.Frame frame, int pass) {
        return pass == 0 ? Math.round(frame.scroll[0] * 3000F * 8F)
                : Math.round(-frame.scroll[1] * 4873F * 8F);
    }

    public static void main(String[] args) {
        int frames = 0;
        for (GlintPalette.ColorMode color : GlintPalette.ColorMode.values()) {
            for (AnimationStyle style : AnimationStyle.values()) {
                SwordAnimator animator = new SwordAnimator();
                animator.update(0, 0, true, style, .8F, 8, -50);
                float previous = Float.NaN;
                for (int i = 1; i <= 120; i++) {
                    SwordAnimator.Frame frame = animator.update(i / 240D, 1F / 240F,
                            true, style, .8F, 8, -50);
                    check(frame.visibility > 0 && frame.brightness > 0, "No visible native glint energy");
                    check(Math.abs(vanillaOffset(clock(frame, 0), 0) - frame.scroll[0]) <= EPSILON,
                            "First vanilla texture pass drifted");
                    check(Math.abs(vanillaOffset(clock(frame, 1), 1) - frame.scroll[1]) <= EPSILON,
                            "Second vanilla texture pass drifted");
                    check(frame.textureScale >= 1 && frame.textureScale <= 32 * style.scale,
                            "Invalid texture scale");
                    check(frame.angle[0] == -50 && frame.angle[1] == 10,
                            "Vanilla diagonal angles were not preserved");
                    if (i > 60 && style != AnimationStyle.Slow) {
                        if (!Float.isNaN(previous)) check(frame.scroll[0] != previous, "Glint stopped scrolling");
                        previous = frame.scroll[0];
                    }
                    frames++;
                }
            }
        }
        System.out.println("PASS: " + frames + " delta-time native-glint plans at 240Hz; both vanilla texture passes preserve scale, diagonal angle and animated scroll");
    }
}
