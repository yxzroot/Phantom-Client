package keystrokesmod.client.clickgui.apple.rebuild;

/** One place to tune ClickGUI duration, travel and primary timing curve. Durations are seconds. */
public final class MotionTokens {
    public static final float CARD_HOVER = .14F, CARD_PRESS = .09F, CARD_ENABLE = .18F;
    public static final float ICON_HOVER = .12F, FAVORITE_COLOR = .14F, FAVORITE_BUMP = .16F;
    public static final float TOGGLE_KNOB = .20F, TOGGLE_TRACK = .16F;
    public static final float TAB_TEXT = .12F, TAB_UNDERLINE = .22F;
    public static final float WINDOW_OPEN = .24F, WINDOW_CLOSE = .16F, BLUR_FADE = .22F;
    public static final long BLUR_FADE_NANOS = 220_000_000L;
    public static final float DRAWER_OPEN = .24F, DRAWER_CLOSE = .16F;
    public static final float GRID_REFLOW = .24F, ROW_ENTER = .16F, VALUE_CHANGE = .12F;
    public static final float INLINE_EXPAND = .18F, SLIDER_PRESS = .10F;
    public static final float TOOLTIP_ENTER = .12F, TOOLTIP_EXIT = .06F, SEARCH_EXPAND = .18F;
    public static final long TOOLTIP_DELAY_NANOS = 250_000_000L;
    public static final float SCROLL_RETURN = .22F;
    public static final float CARD_LIFT_PX = 2F, CARD_PRESS_SCALE = .985F;
    public static final float WINDOW_START_SCALE = .97F, FAVORITE_SCALE = 1.12F;
    public static final float DRAWER_SLIDE_PX = 24F, SLIDER_PRESS_GROW_PX = 3F;
    public static final float CARD_ENTER_PX = 6F, CONTENT_RISE_PX = 4F, ROW_ENTER_PX = 4F;
    public static final float VALUE_SLIDE_PX = 3F, TOOLTIP_RISE_PX = 3F;
    public static final int CARD_STAGGER_MS = 6, CARD_STAGGER_CAP_MS = 120;
    public static final int ROW_STAGGER_MS = 4, ROW_STAGGER_CAP_MS = 100;
    public static final int CONTENT_STAGGER_MS = 5, CONTENT_STAGGER_CAP_MS = 100;
    public static final long CONTENT_EXIT_NANOS = 90_000_000L;
    public static final float SCROLL_OVERSCROLL_PX = 16F;
    public static final float SCROLL_WHEEL_IMPULSE = 42F, SCROLL_VELOCITY_DECAY = 10F;
    private MotionTokens() { }

    public static long staggerNanos(int index, int stepMs, int capMs) {
        return GuiTokens.animationsEnabled() ? (long) (Math.min(capMs, index * stepMs) * 1_000_000L / Math.max(.01F, GuiTokens.animationSpeed())) : 0L;
    }

    /** CSS cubic-bezier(0.22, 1, 0.36, 1), Newton inversion of the x coordinate. */
    public static float ease(float progress) {
        float x = Math.max(0F, Math.min(1F, progress)), t = x;
        for (int i = 0; i < 4; i++) {
            float inverse = 1F - t;
            float estimate = 3F * inverse * inverse * t * .22F + 3F * inverse * t * t * .36F + t * t * t;
            float derivative = 3F * inverse * inverse * .22F + 6F * inverse * t * (.36F - .22F) + 3F * t * t * (1F - .36F);
            t = Math.max(0F, Math.min(1F, t - (estimate - x) / Math.max(.0001F, derivative)));
        }
        float inverse = 1F - t;
        return 3F * inverse * inverse * t + 3F * inverse * t * t + t * t * t;
    }
}
