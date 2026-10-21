package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.modules.client.GuiModule;

/** Central visual constants; keep visual values out of widgets. */
public final class GuiTokens {
    public static final float WINDOW_RADIUS = 16F, CARD_RADIUS = 12F, ROW_RADIUS = 5F;
    public static final float GRID_GAP = 12F, CARD_HEIGHT = 128F, CARD_MIN_WIDTH = 220F, CARD_TARGET_WIDTH = 273F;
    public static final float HEADER_HEIGHT = 76F, NAV_ROW_HEIGHT = 30F, WINDOW_PADDING = 32F;
    public static final float ROW_HEIGHT = 28F, ROW_GAP = 6F, ROW_PADDING_X = 14F, ROW_BAR_WIDTH = 2F;
    public static final float SCROLLBAR_WIDTH = 3F;
    public static final int WINDOW_BG = 0xE00D1016, WINDOW_TOP = 0xE012151D, WINDOW_BORDER = 0x14FFFFFF;
    public static final int BACKDROP_DIM = 0x59000000;
    public static final int CARD_BG = 0xFF171B23, CARD_BG_HOVER = 0xFF1E232D;
    public static final int CARD_BORDER = 0xFF252A35, CARD_BORDER_HOVER = 0xFF343A48;
    public static final int ICON_HOVER_BG = 0xFF242832, FIELD_BG = 0xFF191D25, FIELD_BORDER = 0xFF272C36;
    public static final int BUTTON_BG = 0xFF20242D, BUTTON_HOVER_BG = 0xFF292E39;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF, TEXT_SECONDARY = 0xFFA8AFC0;
    public static final int TEXT_DISABLED = 0xC7FFFFFF, VALUE_TEXT = 0xFFA8AFC0;
    public static final int ICON_REST = 0xB3FFFFFF;
    public static final int STAR_ON = 0xFFF5D742, STAR_OFF = ICON_REST;
    public static final int ROW_BG = 0xBF12151C, ROW_BAR = 0xD9FFFFFF;
    private static boolean cachedAccent;
    private static int frameAccent = 0xFF6C6FF0, frameAccentHover = 0xFF8184F5;

    private GuiTokens() { }

    static void beginFrame() {
        boolean fallback = GuiPreferences.usesDefaultTheme();
        frameAccent = fallback ? 0xFF6C6FF0 : themeAccent();
        frameAccentHover = fallback ? 0xFF8184F5 : UiDraw.mix(frameAccent, 0xFFFFFFFF, .16F);
        cachedAccent = true;
    }
    static void endFrame() { cachedAccent = false; }

    public static int accent() {
        return cachedAccent ? frameAccent : GuiPreferences.usesDefaultTheme() ? 0xFF6C6FF0 : themeAccent();
    }

    public static int accentHover() { return cachedAccent ? frameAccentHover : GuiPreferences.usesDefaultTheme() ? 0xFF8184F5 : UiDraw.mix(accent(), 0xFFFFFFFF, .16F); }
    private static int themeAccent() { try { return 0xFF000000 | (GuiModule.getEnabledTopRGB() & 0xFFFFFF); } catch (Throwable ignored) { return 0xFF6C6FF0; } }
    public static boolean animationsEnabled() { return GuiPreferences.menuAnimationEnabled(); }
    public static float animationSpeed() { return GuiPreferences.animationSpeed(); }
}
