package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import org.lwjgl.input.Keyboard;

/** Persistent card state: restrained, allocation-free hover and state transitions. */
public final class ModuleCard {
    public static final int NONE = 0, TOGGLED = 1, SETTINGS = 2, FAVORITE = 3, BINDING = 4, HELP = 5;
    private final Module module;
    private final boolean hudCard;
    private final String name, description;
    private String clippedName;
    private float clippedFor = -1F, nameWidth;
    private final Anim x = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.GRID_REFLOW), y = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.GRID_REFLOW);
    private final Anim alpha = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER), hover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_HOVER);
    private final Anim press = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_PRESS), enabled = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_ENABLE);
    private final Anim knob = new Anim(0F, Anim.Mode.SPRING, MotionTokens.TOGGLE_KNOB);
    private final Anim track = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.TOGGLE_TRACK);
    private final Anim star = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.FAVORITE_COLOR);
    private final Anim enterY = new Anim(MotionTokens.CARD_ENTER_PX, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER);
    private final Anim questionHover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER), dotsHover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER), starHover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ICON_HOVER);
    private final ColorAnim starColor = new ColorAnim(GuiTokens.STAR_OFF, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.FAVORITE_COLOR);
    private float width, height = GuiTokens.CARD_HEIGHT;
    private boolean listed, shownBefore, favoriteKnown, binding, hovered, enabledKnown, lastEnabled, selectedCard;
    private long showAt, favoriteAt;
    private int lastAction;
    private float helpProgress;
    private int cachedKeycode = Integer.MIN_VALUE;
    private String cachedKey = "";
    private static boolean cardDebug, loggedIdle, loggedEnabled;

    public static void toggleCardDebug() { cardDebug = !cardDebug; }
    public static boolean cardDebug() { return cardDebug; }
    public static void resetColorTrace() { loggedIdle = loggedEnabled = false; }

    public ModuleCard(Module module) { this(module, false); }
    public ModuleCard(Module module, boolean hudCard) {
        this.module = module; this.hudCard = hudCard; name = module.getName(); description = module.getDescription() == null ? "" : module.getDescription();
        clippedName = name; nameWidth = UiDraw.mediumWidth(name);
    }
    public Module module() { return module; }
    public boolean listed() { return listed; }
    /** Logical slot top. Visual hover lift/scale belongs only to rendering so layout never moves. */
    public float y() { return y.get() + enterY.get(); }
    public float x() { return x.get(); }
    public float width() { return width; }
    public float height() { return height; }
    public boolean binding() { return binding; }
    public int lastAction() { return lastAction; }
    public float alpha() { return alpha.get(); }
    public boolean hovered() { return hovered; }
    public boolean tooltipHovered() { return questionHover.target() > .5F; }

    public void place(float targetX, float targetY, float cardWidth, int index, boolean visible, long now) { place(targetX, targetY, cardWidth, index, visible, now, false); }
    public void place(float targetX, float targetY, float cardWidth, int index, boolean visible, long now, boolean snapPosition) {
        place(targetX, targetY, cardWidth, index, visible, now, snapPosition, false);
    }
    public void place(float targetX, float targetY, float cardWidth, int index, boolean visible, long now, boolean snapPosition, boolean categorySwitch) {
        width = cardWidth;
        if (snapPosition || visible && !shownBefore) { x.snapTo(targetX); y.snapTo(targetY); }
        else { x.setTarget(targetX); y.setTarget(targetY); }
        if (visible && !listed) {
            listed = true; alpha.setDuration(MotionTokens.ROW_ENTER);
            if (!shownBefore) { alpha.snapTo(0F); enterY.snapTo(categorySwitch ? MotionTokens.CONTENT_RISE_PX : MotionTokens.CARD_ENTER_PX); }
            shownBefore = true;
            showAt = now + (categorySwitch ? MotionTokens.staggerNanos(index, MotionTokens.CONTENT_STAGGER_MS, MotionTokens.CONTENT_STAGGER_CAP_MS)
                    : MotionTokens.staggerNanos(index, MotionTokens.CARD_STAGGER_MS, MotionTokens.CARD_STAGGER_CAP_MS));
        } else if (!visible && listed) { listed = false; alpha.setDuration(MotionTokens.CONTENT_EXIT_NANOS / 1_000_000_000F); alpha.setTarget(0F); enterY.setTarget(0F); }
        if (clippedFor != cardWidth) clip(cardWidth);
    }
    public void translate(float dx, float dy) { if (dx != 0F || dy != 0F) { x.snapTo(x.get() + dx); y.snapTo(y.get() + dy); } }
    public void replayEntrance(int index, long now) { if (listed) { alpha.snapTo(0F); enterY.snapTo(MotionTokens.CARD_ENTER_PX); showAt = now + MotionTokens.staggerNanos(index, MotionTokens.CARD_STAGGER_MS, MotionTokens.CARD_STAGGER_CAP_MS); resetInteraction(); } }
    public void resetInteraction() {
        hover.snapTo(0F); press.snapTo(0F); questionHover.snapTo(0F); dotsHover.snapTo(0F); starHover.snapTo(0F);
        binding = false; hovered = false; lastAction = NONE;
    }

    public void update(float dt, long now, float mouseX, float mouseY, boolean selected, Tooltip tooltip, boolean allowHover) {
        selectedCard = selected;
        if (listed && now >= showAt) { alpha.setTarget(1F); enterY.setTarget(0F); }
        float left = x.get(), top = y();
        hovered = allowHover && alpha.get() > .01F && UiDraw.inside(mouseX, mouseY, left, top, left + width, top + height);
        hover.setTarget(hovered ? 1F : 0F);
        boolean help = hovered && helpHit(mouseX, mouseY, left, top);
        questionHover.setTarget(help ? 1F : 0F);
        dotsHover.setTarget(hovered && UiDraw.inside(mouseX, mouseY, left + width - 35F, top + 16F, left + width - 7F, top + 44F) ? 1F : 0F);
        starHover.setTarget(hovered && UiDraw.inside(mouseX, mouseY, left + width - 52F, top + height - 52F, left + width - 6F, top + height - 6F) ? 1F : 0F);
        boolean on = module.isEnabled();
        if (!enabledKnown) { enabled.snapTo(on ? 1F : 0F); knob.snapTo(on ? 1F : 0F); track.snapTo(on ? 1F : 0F); lastEnabled = on; enabledKnown = true; }
        if (on != lastEnabled) lastEnabled = on;
        enabled.setTarget(on ? 1F : 0F);
        knob.setTarget(on ? 1F : 0F);
        track.setTarget(on ? 1F : 0F);
        boolean favorite = GuiPreferencesFavorite.isFavorite(name);
        if (!favoriteKnown) { star.snapTo(favorite ? 1F : 0F); favoriteKnown = true; }
        if (favorite != (star.target() > .5F)) { star.setTarget(favorite ? 1F : 0F); favoriteAt = favorite ? now : 0L; }
        starColor.setTarget(favorite ? GuiTokens.STAR_ON : GuiTokens.STAR_OFF);
        if (help && cachedKeycode != module.getKeycode()) {
            cachedKeycode = module.getKeycode();
            cachedKey = cachedKeycode == 0 ? "" : "Keybind: " + (cachedKeycode < 0 ? "Mouse " + (cachedKeycode + 100) : module.getBindAsString());
        }
        helpProgress = help ? tooltip.hover(module, name, module.getDescription(), cachedKey,
                frameLeft() + 31F, frameTop() + 30F, mouseX, mouseY) : 0F;
        x.update(dt); y.update(dt); alpha.update(dt); hover.update(dt); press.update(dt); enabled.update(dt); knob.update(dt); track.update(dt); star.update(dt); enterY.update(dt);
        questionHover.update(dt); dotsHover.update(dt); starHover.update(dt); starColor.update(dt);
    }

    /** Surface pass; SDF batches each card into a few quads rather than dozens of immediate-mode rings. */
    public void drawSurface(float dim, boolean drawShadow) {
        float a = drawableAlpha(dim); if (a <= .01F) return;
        float left = frameLeft(), top = frameTop(), right = frameRight(), bottom = frameBottom();
        int fillColor = UiDraw.mix(GuiTokens.CARD_BG, GuiTokens.CARD_BG_HOVER, hover.get());
        fillColor = UiDraw.mix(fillColor, UiDraw.mix(GuiTokens.CARD_BG, GuiTokens.accent(), .14F), enabled.get());
        int edge = UiDraw.mix(UiDraw.mix(GuiTokens.CARD_BORDER, GuiTokens.CARD_BORDER_HOVER, hover.get()),
                UiDraw.alpha(GuiTokens.accent(), .60F), Math.max(selectedCard ? 1F : 0F, enabled.get()));
        if (SdfRenderer.active() && a > .99F && UiDraw.masterAlpha() > .99F && (!module.isEnabled() && !loggedIdle || module.isEnabled() && !loggedEnabled)) {
            int shaderFill = UiDraw.alpha(fillColor, a * UiDraw.masterAlpha());
            int shaderBorder = UiDraw.alpha(edge, a * UiDraw.masterAlpha());
            System.out.println("Phantom card RGBA " + name + " enabled=" + module.isEnabled()
                    + " shaderFill=#" + Integer.toHexString(shaderFill) + " shaderBorder=#" + Integer.toHexString(shaderBorder)
                    + " enter=" + alpha.get() + " dim=" + dim + " hover=" + hover.get());
            if (module.isEnabled()) loggedEnabled = true; else loggedIdle = true;
        }
        if (cardDebug && !module.isEnabled()) fillColor = 0xFFFF00FF;
        UiDraw.glassRounded(left, top, right, bottom, GuiTokens.CARD_RADIUS, UiDraw.alpha(fillColor, a), UiDraw.alpha(fillColor, a), UiDraw.alpha(edge, a),
                GuiRenderContext.inverseUiScale(), 0, 0F);
        drawQuestionFrame(left + 31F, top + 30F, UiDraw.alpha(UiDraw.mix(GuiTokens.ICON_REST, GuiTokens.TEXT_PRIMARY, questionHover.get()), a), questionHover.get());
        Tooltip.ring(left + 31F, top + 30F, 11F, helpProgress, a);
        GuiToggle.draw(right - 81F, top + 18F, knob.get(), track.get(), hover.get(), a, hudCard && !module.canBeEnabled());
        drawDots(right - 21F, top + 30F, UiDraw.alpha(UiDraw.mix(GuiTokens.ICON_REST, GuiTokens.TEXT_PRIMARY, dotsHover.get()), a), dotsHover.get());
        float starX = right - 29F, starY = bottom - 29F;
        if (starHover.get() > .01F) UiDraw.circle(starX, starY, 14F, UiDraw.alpha(GuiTokens.ICON_HOVER_BG, a * starHover.get()));
        float bump = favoriteAt == 0L || !GuiTokens.animationsEnabled() ? 1F : 1F + (MotionTokens.FAVORITE_SCALE - 1F) * (float) Math.sin(Math.PI * Math.max(0F, Math.min(1F, (System.nanoTime() - favoriteAt) * GuiTokens.animationSpeed() / (MotionTokens.FAVORITE_BUMP * 1_000_000_000F))));
        float favored = Math.max(0F, Math.min(1F, star.get()));
        UiDraw.star(starX, starY, 9F * bump, 3.78F * bump, UiDraw.alpha(UiDraw.mix(GuiTokens.ICON_REST, GuiTokens.TEXT_PRIMARY, starHover.get()), a * (1F - favored)), false);
        UiDraw.star(starX, starY, 9F * bump, 3.78F * bump, UiDraw.alpha(starColor.get(), a * favored), true);
        if (binding) {
            float chipW = 94F, chipX = left + (right - left - chipW) * .5F;
            UiDraw.glassRounded(chipX, top + 51F, chipX + chipW, top + 75F, 6F, UiDraw.alpha(0xE612151C, a), UiDraw.alpha(0xEE0E1118, a), UiDraw.alpha(GuiTokens.CARD_BORDER, a), GuiRenderContext.inverseUiScale(), 0, 0F);
        }
    }

    public void drawQuestionText(float dim) {
        float a = drawableAlpha(dim); if (a <= .01F) return;
        UiDraw.small("?", frameLeft() + 27F, frameTop() + 30F - UiDraw.smallHeight() * .5F - 1F,
                UiDraw.alpha(helpProgress > 0F ? 0xFFFFFFFF : UiDraw.mix(GuiTokens.ICON_REST, GuiTokens.TEXT_PRIMARY, questionHover.get()), a));
    }
    public void drawNameText(float dim) {
        float a = drawableAlpha(dim); if (a <= .01F) return;
        int nameColor = UiDraw.mix(module.isEnabled() ? GuiTokens.TEXT_PRIMARY : GuiTokens.TEXT_DISABLED, GuiTokens.TEXT_PRIMARY, hover.get());
        UiDraw.medium(clippedName, frameLeft() + 18F, frameBottom() - 18F - UiDraw.mediumHeight(), UiDraw.alpha(nameColor, a));
    }
    public void drawBindingText(float dim) {
        if (!binding) return;
        float a = drawableAlpha(dim); if (a <= .01F) return;
        float chipX = frameLeft() + (frameRight() - frameLeft() - 94F) * .5F;
        UiDraw.text("Press a key...", chipX + 9F, frameTop() + 58F, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, a));
    }

    public int click(float mouseX, float mouseY, int button) {
        lastAction = NONE;
        float left = x.get(), top = y();
        if (!UiDraw.inside(mouseX, mouseY, left, top, left + width, top + height)) return NONE;
        if (helpHit(mouseX, mouseY, left, top)) return lastAction = HELP;
        if (button == 2) { binding = true; return lastAction = BINDING; }
        if (button == 1) return lastAction = SETTINGS;
        if (button != 0) return NONE;
        press.setTarget(1F);
        if (UiDraw.inside(mouseX, mouseY, left + width - 52F, top + height - 52F, left + width - 6F, top + height - 6F)) { GuiPreferencesFavorite.toggle(name); return lastAction = FAVORITE; }
        if (UiDraw.inside(mouseX, mouseY, left + width - 35F, top + 16F, left + width - 7F, top + 44F)) return lastAction = SETTINGS;
        if (hudCard && !module.canBeEnabled()) return NONE;
        if (UiDraw.inside(mouseX, mouseY, left + width - 91F, top + 14F, left + width - 31F, top + 48F)) { triggerToggle(); return lastAction = TOGGLED; }
        triggerToggle(); return lastAction = TOGGLED;
    }
    public void release() { press.setTarget(0F); }
    public boolean key(char character, int keyCode) {
        if (!binding) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) binding = false;
        else if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) { module.setbind(0); binding = false; }
        else { module.setbind(keyCode); binding = false; }
        return true;
    }

    private void triggerToggle() { module.toggle(); }
    private float drawableAlpha(float dim) {
        float a = alpha.get() * (1F - dim);
        return a <= .01F || UiDraw.outside(frameLeft(), frameTop(), frameRight(), frameBottom()) ? 0F : a;
    }
    private float renderScale() { return 1F - press.get() * (1F - MotionTokens.CARD_PRESS_SCALE); }
    private float frameLeft() { float scale = renderScale(); return x.get() + (width - width * scale) * .5F; }
    private float frameRight() { return frameLeft() + width * renderScale(); }
    private float frameTop() { float scale = renderScale(); return y() + (height - height * scale) * .5F - hover.get() * MotionTokens.CARD_LIFT_PX; }
    private float frameBottom() { return frameTop() + height * renderScale(); }
    private void clip(float cardWidth) {
        clippedFor = cardWidth; float limit = cardWidth - 68F;
        if (nameWidth <= limit) { clippedName = name; return; }
        int end = name.length();
        while (end > 0 && UiDraw.mediumWidth(name.substring(0, end) + "...") > limit) --end;
        clippedName = end == 0 ? "..." : name.substring(0, end) + "...";
    }
    private static void drawQuestionFrame(float x, float y, int color, float hover) {
        if (hover > .01F) UiDraw.circle(x, y, 14F, UiDraw.alpha(GuiTokens.ICON_HOVER_BG, hover));
        UiDraw.outline(x - 9F, y - 9F, x + 9F, y + 9F, 9F, color, 1.5F * GuiRenderContext.inverseUiScale());
    }
    private static void drawDots(float x, float y, int color, float hover) {
        if (hover > .01F) UiDraw.circle(x, y, 14F, UiDraw.alpha(GuiTokens.ICON_HOVER_BG, hover));
        UiDraw.circle(x, y - 5F, 1.5F, color); UiDraw.circle(x, y, 1.5F, color); UiDraw.circle(x, y + 5F, 1.5F, color);
    }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static boolean helpHit(float mouseX, float mouseY, float left, float top) {
        float dx = mouseX - left - 31F, dy = mouseY - top - 30F;
        return dx * dx + dy * dy <= 13F * 13F;
    }

    /** Keeps the legacy Apple public favorite set behind a no-allocation facade. */
    public static final class GuiPreferencesFavorite {
        private GuiPreferencesFavorite() { }
        public static boolean isFavorite(String name) { return keystrokesmod.client.clickgui.apple.AppleClickGui.isFavorite(name); }
        public static void toggle(String name) { keystrokesmod.client.clickgui.apple.AppleClickGui.toggleFavorite(name); }
    }
}
