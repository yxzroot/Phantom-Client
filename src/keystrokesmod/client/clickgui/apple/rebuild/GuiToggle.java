package keystrokesmod.client.clickgui.apple.rebuild;

/** Flat 44x24 toggle shared by cards and setting rows. Animation state belongs to the owner. */
public final class GuiToggle {
    private GuiToggle() { }
    public static void draw(float x, float y, float knobProgress, float trackProgress, float hover, float alpha, boolean disabled) {
        float p = Math.max(0F, Math.min(1F, knobProgress));
        float track = Math.max(0F, Math.min(1F, trackProgress));
        float a = alpha * (disabled ? .40F : 1F);
        int off = UiDraw.alpha(0xFFFFFFFF, .16F + .06F * hover);
        int on = UiDraw.mix(GuiTokens.accent(), GuiTokens.accentHover(), hover);
        UiDraw.rounded(x, y, x + 44F, y + 24F, 12F, UiDraw.alpha(UiDraw.mix(off, on, track), a));
        float knob = x + 12F + p * 20F;
        UiDraw.glassRounded(knob - 9F, y + 4F, knob + 9F, y + 22F, 9F,
                0, 0, 0, 0F, UiDraw.alpha(0xFF000000, a * .25F), 4F);
        UiDraw.rounded(knob - 9F, y + 3F, knob + 9F, y + 21F, 9F,
                UiDraw.alpha(0xFFFFFFFF, a * (.90F + .10F * track)));
    }
}
