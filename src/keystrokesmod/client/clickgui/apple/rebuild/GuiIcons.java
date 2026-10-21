package keystrokesmod.client.clickgui.apple.rebuild;

/** Shared SDF icon vocabulary. Every icon is composed from the same round-cap primitives. */
public final class GuiIcons {
    public static final int BELL = 0, REFRESH = 1, GEAR = 2, SEARCH = 3, GRID = 4, STAR = 5, DOTS = 6, QUESTION = 7;
    public static final int CHECK = 8, CHEVRON = 9, PLUS = 10, TRASH = 11, CLOSE = 12, FILE = 13, INFO = 14, MOVE = 15, DOWNLOAD = 16, SAVE = 17;
    private GuiIcons() { }

    public static void draw(int icon, float x, float y, float size, int color) { draw(icon, x, y, size, color, false); }
    public static void draw(int icon, float x, float y, float size, int color, boolean filled) {
        float h = size * .5F, stroke = Math.max(1.35F * GuiRenderContext.inverseUiScale(), size * .082F);
        if (icon == BELL) {
            UiDraw.outline(x - h * .63F, y - h * .70F, x + h * .63F, y + h * .35F, h * .63F, color, stroke);
            UiDraw.line(x - h * .77F, y + h * .34F, x + h * .77F, y + h * .34F, stroke, color); UiDraw.circle(x, y + h * .63F, stroke * .62F, color);
        } else if (icon == REFRESH) {
            UiDraw.line(x - h * .77F, y - h * .06F, x - h * .38F, y - h * .65F, stroke, color);
            UiDraw.line(x - h * .38F, y - h * .65F, x + h * .48F, y - h * .55F, stroke, color);
            UiDraw.line(x + h * .73F, y - h * .10F, x + h * .51F, y + h * .55F, stroke, color);
            UiDraw.line(x + h * .51F, y + h * .55F, x - h * .44F, y + h * .63F, stroke, color);
            UiDraw.line(x + h * .36F, y - h * .82F, x + h * .77F, y - h * .54F, stroke, color);
            UiDraw.line(x + h * .77F, y - h * .54F, x + h * .36F, y - h * .31F, stroke, color);
        } else if (icon == GEAR) {
            UiDraw.outline(x - h * .40F, y - h * .40F, x + h * .40F, y + h * .40F, h * .40F, color, stroke);
            for (int i = 0; i < 8; i++) {
                float angle = i * .785398163F, cx = (float) Math.cos(angle), cy = (float) Math.sin(angle);
                UiDraw.line(x + cx * h * .59F, y + cy * h * .59F, x + cx * h * .84F, y + cy * h * .84F, stroke, color);
            }
        } else if (icon == SEARCH) {
            UiDraw.outline(x - h * .67F, y - h * .67F, x + h * .33F, y + h * .33F, h * .5F, color, stroke);
            UiDraw.line(x + h * .26F, y + h * .26F, x + h * .82F, y + h * .82F, stroke, color);
        } else if (icon == GRID) {
            float q = size * .27F, gap = size * .09F;
            for (int ix = 0; ix < 2; ix++) for (int iy = 0; iy < 2; iy++) UiDraw.rounded(x - q - gap * .5F + ix * (q + gap), y - q - gap * .5F + iy * (q + gap), x - gap * .5F + ix * (q + gap), y - gap * .5F + iy * (q + gap), q * .22F, color);
        } else if (icon == STAR) {
            UiDraw.star(x, y, h * .92F, h * .386F, color, filled);
        } else if (icon == DOTS) {
            UiDraw.circle(x, y - h * .38F, stroke * .72F, color); UiDraw.circle(x, y, stroke * .72F, color); UiDraw.circle(x, y + h * .38F, stroke * .72F, color);
        } else if (icon == QUESTION) {
            UiDraw.outline(x - h * .78F, y - h * .78F, x + h * .78F, y + h * .78F, h * .78F, color, stroke);
            UiDraw.smallBold("?", x - 4F, y - UiDraw.smallBoldHeight() * .5F - 1F, color);
        } else if (icon == CHECK) UiDraw.check(x, y, size * .86F, stroke, color);
        else if (icon == CHEVRON) UiDraw.chevron(x, y, size * .82F, true, stroke, color);
        else if (icon == PLUS) { UiDraw.line(x - h * .45F, y, x + h * .45F, y, stroke, color); UiDraw.line(x, y - h * .45F, x, y + h * .45F, stroke, color); }
        else if (icon == TRASH) { UiDraw.rounded(x - h * .32F, y - h * .18F, x + h * .32F, y + h * .45F, h * .08F, color); UiDraw.line(x - h * .45F, y - h * .30F, x + h * .45F, y - h * .30F, stroke, color); UiDraw.line(x - h * .13F, y - h * .43F, x + h * .13F, y - h * .43F, stroke, color); }
        else if (icon == CLOSE) { UiDraw.line(x - h * .38F, y - h * .38F, x + h * .38F, y + h * .38F, stroke, color); UiDraw.line(x + h * .38F, y - h * .38F, x - h * .38F, y + h * .38F, stroke, color); }
        else if (icon == FILE) { UiDraw.outline(x - h * .34F, y - h * .46F, x + h * .34F, y + h * .46F, h * .08F, color, stroke); UiDraw.line(x - h * .16F, y - h * .08F, x + h * .18F, y - h * .08F, stroke, color); UiDraw.line(x - h * .16F, y + h * .18F, x + h * .18F, y + h * .18F, stroke, color); }
        else if (icon == INFO) { UiDraw.outline(x - h * .76F, y - h * .76F, x + h * .76F, y + h * .76F, h * .76F, color, stroke); UiDraw.smallBold("i", x - 2F, y - UiDraw.smallBoldHeight() * .5F - 1F, color); }
        else if (icon == MOVE) { UiDraw.line(x - h * .45F, y, x + h * .45F, y, stroke, color); UiDraw.line(x, y - h * .45F, x, y + h * .45F, stroke, color); UiDraw.chevron(x - h * .40F, y, size * .25F, false, stroke, color); UiDraw.chevron(x + h * .40F, y, size * .25F, true, stroke, color); }
        else if (icon == DOWNLOAD) { UiDraw.line(x, y - h * .44F, x, y + h * .22F, stroke, color); UiDraw.chevron(x, y + h * .15F, size * .46F, true, stroke, color); UiDraw.line(x - h * .45F, y + h * .45F, x + h * .45F, y + h * .45F, stroke, color); }
        else if (icon == SAVE) { UiDraw.outline(x - h * .43F, y - h * .43F, x + h * .43F, y + h * .43F, h * .08F, color, stroke); UiDraw.rect(x - h * .20F, y - h * .40F, x + h * .20F, y - h * .05F, color); UiDraw.check(x, y + h * .17F, size * .50F, stroke, color); }
    }
}
