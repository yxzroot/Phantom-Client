import keystrokesmod.client.module.modules.render.swordglow.*;

public final class SwordAnimatorTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void close(float a, float b, String message) { check(Math.abs(a - b) < .0002F, message + ": " + a + "/" + b); }
    private static SwordAnimator.Frame run(int fps, AnimationStyle style) {
        SwordAnimator animator = new SwordAnimator();
        animator.update(0, 0, true, style, .8F, 8, -50);
        for (int i = 1; i <= fps * 2; i++) animator.update(i / (double) fps, 1F / fps, true, style, .8F, 8, -50);
        return animator.frame();
    }
    public static void main(String[] args) {
        for (AnimationStyle style : AnimationStyle.values()) {
            SwordAnimator.Frame a = run(60, style), b = run(240, style);
            close(a.scroll[0], b.scroll[0], "First-pass FPS independence");
            close(a.scroll[1], b.scroll[1], "Second-pass FPS independence");
            close(a.brightness, b.brightness, "Intensity FPS independence");
        }
        SwordAnimator animator = new SwordAnimator();
        animator.update(0, 0, true, AnimationStyle.Classic, 1, 8, -50);
        close(animator.update(.1, .1F, true, AnimationStyle.Classic, 1, 8, -50).visibility, .5F, "Equip fade midpoint");
        close(animator.update(.2, .1F, true, AnimationStyle.Classic, 1, 8, -50).visibility, 1, "Equip fade duration");
        close(animator.update(.3, .1F, false, AnimationStyle.Classic, 1, 8, -50).visibility, .5F, "Disable fade midpoint");
        close(animator.update(.4, .1F, false, AnimationStyle.Classic, 1, 8, -50).visibility, 0, "Disable/unequip fade duration");
        SwordAnimator.Frame vanilla = run(60, AnimationStyle.Classic);
        close(vanilla.scroll[0], (2F / 3) / 8, "Vanilla 3000 ms period");
        close(vanilla.scroll[1], -(2F / 4.873F) / 8, "Vanilla 4873 ms period");
        close(vanilla.angle[0], -50, "Vanilla first angle"); close(vanilla.angle[1], 10, "Vanilla second angle");
        close(vanilla.textureScale, 8, "Vanilla texture scale");
        SwordAnimator.Frame zero = animator.update(.5, .1F, true, AnimationStyle.Classic, 0, 8, -50);
        check(zero.brightness == 0, "Zero intensity must draw nothing");
        SwordAnimator.Frame invalid = animator.update(Double.NaN, Float.NaN, true, AnimationStyle.Classic,
                Float.NaN, Float.NaN, Float.NaN);
        check(Float.isFinite(invalid.brightness) && Float.isFinite(invalid.scroll[0]), "NaN settings leaked into GL");
        System.out.println("PASS: all animation IDs at 60/240 FPS, legacy texture transforms, 200ms compatibility fades, zero intensity and NaN safety");
    }
}
