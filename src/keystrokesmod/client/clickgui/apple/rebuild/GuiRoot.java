package keystrokesmod.client.clickgui.apple.rebuild;

import java.io.IOException;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Display;

/** Screen state machine, input routing, cached layout, and the one master open/close transition. */
public final class GuiRoot {
    public static final int CLOSED = 0, OPENING = 1, OPEN = 2, CLOSING = 3;
    private static final String[] MAIN = {"Modules", "Config", "Friends", "HUD"};
    private static final long OPEN_NANOS = (long) (MotionTokens.WINDOW_OPEN * 1_000_000_000L), CLOSE_NANOS = (long) (MotionTokens.WINDOW_CLOSE * 1_000_000_000L), CLOSE_TIMEOUT_NANOS = 300_000_000L;
    private final GuiScreen host;
    private final AnimClock clock = new AnimClock();
    private final MainWindow window = new MainWindow();
    private final HeaderBar header = new HeaderBar();
    private final PreferencesMenu preferences=new PreferencesMenu();
    private final TabBar mainTabs = new TabBar(MAIN), categoryTabs = new TabBar(ModuleGrid.CATEGORY_LABELS);
    private final SearchField search = new SearchField();
    private final ModuleGrid grid = new ModuleGrid();
    private final SettingsDrawer settings;
    private final ConfigTab configs = new ConfigTab();
    private final FriendsTab friends = new FriendsTab();
    private final HudTab hud = new HudTab();
    private final NotificationCenter notifications = new NotificationCenter();
    private final Tooltip tooltip = new Tooltip();
    private final Anim contentAlpha = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER), contentYOffset = new Anim(MotionTokens.CONTENT_RISE_PX, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER), gear = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER);

    private int state = CLOSED, mainTab, category;
    private int pendingTab = -1;
    private long tabExitStarted;
    private boolean favorites, gearOpen, pixelDebugOverlay, renderDebugOverlay, gridNeedsLayout = true, closeDispatched;
    private int screenW, screenH;
    private float mouseX, mouseY, lastWindowX = Float.NaN, lastWindowY = Float.NaN, lastUiScale = -1F;
    private float progress, progressFrom, progressTo;
    private long progressStarted, progressDuration, closeStarted;
    private long blurStarted;
    private long profileGridNanos, profileSettingsNanos, profileModuleTabsNanos;
    private String lastWheelReceiver = "none";

    public GuiRoot(GuiScreen host) { this.host = host; settings = new SettingsDrawer(host); }
    public int state() { return state; }
    public boolean glDebugEnabled() { return renderDebugOverlay; }

    public void open() {
        if (state == CLOSING) { finishCloseSoon(); return; }
        if (state == OPEN || state == OPENING) return;
        GuiPreferences.load();
        GuiPreferences.tooltipDelay(); GuiDescriptions.install();
        ModuleCard.resetColorTrace();
        mainTab = valid(GuiPreferences.lastTab(), MAIN.length);
        category = valid(GuiPreferences.lastCategory(), ModuleGrid.CATEGORY_LABELS.length);
        mainTabs.select(mainTab); categoryTabs.select(category); favorites = GuiPreferences.favoritesOnly();
        pendingTab = -1;
        state = OPENING; closeDispatched = false; gridNeedsLayout = true;
        settings.dismiss(); search.clear(); search.cancelFocus(); grid.invalidate(); hud.resetInteraction(); tooltip.dismiss();
        gearOpen = false; gear.snapTo(0F);
        lastWheelReceiver = "none";
        BackdropBlur.shared().invalidate();
        UiDraw.prepareResources();
        GuiProfiler.openSession();
        progress = 0F; startProgress(1F, OPEN_NANOS);
        blurStarted = System.nanoTime(); window.setBlurAlpha(0F);
        contentAlpha.snapTo(0F); contentAlpha.setTarget(1F);
        contentYOffset.snapTo(MotionTokens.CONTENT_RISE_PX); contentYOffset.setTarget(0F);
        mainTabs.resetInteraction(); categoryTabs.resetInteraction(); grid.replayEntrance(System.nanoTime());
        mainTabs.snapSelectionOnNextDraw(); categoryTabs.snapSelectionOnNextDraw();
        if (mainTab == 1) configs.onTabOpened();
        if (mainTab == 2) friends.onTabOpened();
        if (mainTab == 3) hud.refresh();
        clock.reset();
    }

    public void init(int width, int height) { screenW = width; screenH = height; }

    public void tick() {
        if (settings.dragging() && (!Display.isActive() || !Mouse.isButtonDown(0))) settings.release();
        if (configs.dragging() && (!Display.isActive() || !Mouse.isButtonDown(0))) configs.release();
        updateProgress();
        if (state != CLOSING || closeDispatched) return;
        long now = System.nanoTime();
        if (progress <= .001F || now - closeStarted >= CLOSE_TIMEOUT_NANOS) finishCloseFromTick();
    }

    public void draw(int width, int height, float mx, float my) {
        screenW = width; screenH = height; mouseX = mx; mouseY = my;
        tooltip.pointer(mx, my);
        updateProgress();
        float dt = clock.begin();
        if (mainTab == 0) grid.rebuild(favorites ? -1 : category, search.query(), System.nanoTime());
        float contentHeight = mainTab == 0 ? grid.desiredHeight(Math.min(1200F, width * .66F)
                - GuiTokens.WINDOW_PADDING * 2F - settings.attachedWidth()) : 352F;
        window.layout(width, height, contentHeight);
        window.update(dt);
        window.setProgress(progress);
        window.setBlurAlpha(GuiTokens.animationsEnabled() ? MotionTokens.ease(Math.min(1F, (System.nanoTime() - blurStarted) * GuiTokens.animationSpeed() / MotionTokens.BLUR_FADE_NANOS)) : 1F);
        if (settings.dragging() && (!Display.isActive() || !Mouse.isButtonDown(0))) settings.release();
        BackdropBlur.shared().prepare();
        GuiGlDebug.section("blur capture");

        float scale = GuiRenderContext.uiScale();
        if (scale != lastUiScale) {
            lastUiScale = scale;
            mainTabs.invalidateMetrics(); categoryTabs.invalidateMetrics(); grid.invalidateLayout(); gridNeedsLayout = true;
        }
        float wx = window.x(), wy = window.y();
        boolean moved = !Float.isNaN(lastWindowX) && (wx != lastWindowX || wy != lastWindowY);
        if (moved && !gridNeedsLayout) grid.translate(wx - lastWindowX, wy - lastWindowY);
        lastWindowX = wx; lastWindowY = wy;

        header.update(dt); preferences.update(dt); mainTabs.update(dt); categoryTabs.update(dt); search.update(dt); settings.update(dt); configs.update(dt); friends.update(dt);
        hud.update(dt, mouseX, mouseY, settings.module(), tooltip, state == OPEN && !window.dragging());
        notifications.update(dt); contentAlpha.update(dt); contentYOffset.update(dt); gear.update(dt);
        if (pendingTab >= 0 && (!GuiTokens.animationsEnabled() || System.nanoTime() - tabExitStarted >= MotionTokens.CONTENT_EXIT_NANOS / Math.max(.01F, GuiTokens.animationSpeed()))) {
            saveActiveScroll();
            mainTab = pendingTab; pendingTab = -1;
            settings.dismiss(); search.blur(); gridNeedsLayout = true;
            contentAlpha.snapTo(0F); contentAlpha.setTarget(1F);
            contentYOffset.snapTo(MotionTokens.CONTENT_RISE_PX); contentYOffset.setTarget(0F);
            if (mainTab == 1) configs.onTabOpened(); else if (mainTab == 2) friends.onTabOpened(); else if (mainTab == 3) hud.refresh();
        }
        if (state == OPENING && progress >= .999F) state = OPEN;
        if (state == OPEN && pendingTab < 0) saveActiveScroll();

        UiDraw.setMasterAlpha(progress);
        long mark = System.nanoTime();
        UiDraw.rect(0F, 0F, width, height, GuiTokens.BACKDROP_DIM);
        UiDraw.flushShapes(); // backdrop is outside the window transition matrix.
        float centerX = window.x() + window.width() * .5F, centerY = window.y() + window.height() * .5F, windowScale = window.scale();
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0F); GL11.glScalef(windowScale, windowScale, 1F); GL11.glTranslatef(-centerX, -centerY, 0F);
        UiDraw.setRenderTransform(centerX, centerY, windowScale);
        try {
            window.draw();
            GuiGlDebug.section("window");
            long windowNanos = System.nanoTime() - mark;
            mark = System.nanoTime();
            header.draw(window, mx, my, notifications.unread(), state == OPEN && !window.dragging());
            GuiGlDebug.section("header");
            long headerNanos = System.nanoTime() - mark;
            mark = System.nanoTime();
            float navX = window.x() + GuiTokens.WINDOW_PADDING, mainY = window.y() + 88F, categoryY = window.y() + 128F;
            boolean interactive = state == OPEN && !window.dragging();
            mainTabs.draw(navX, mainY, window.contentW() - 84F, mx, my, true, window.dragging(), interactive);
            search.draw(window.x() + window.width() - GuiTokens.WINDOW_PADDING, mainY, mx, my, System.nanoTime());
            GuiGlDebug.section("tabs/search");
            long tabsNanos = System.nanoTime() - mark;
            float exit = pendingTab < 0 ? 1F : 1F - MotionTokens.ease(Math.min(1F, (System.nanoTime() - tabExitStarted) * Math.max(.01F, GuiTokens.animationSpeed()) / MotionTokens.CONTENT_EXIT_NANOS));
            UiDraw.setMasterAlpha(progress * contentAlpha.get() * exit);
            if (mainTab == 0) { drawModules(navX, categoryY, dt, moved); tabsNanos += profileModuleTabsNanos; }
            else { drawTabContent(dt); }
            UiDraw.setMasterAlpha(progress);
            GuiGlDebug.section("content");
            if (gear.get() > .01F) drawGear();
            notifications.draw(window.x() + window.width() - GuiTokens.WINDOW_PADDING, window.y() + 14F);
            GuiGlDebug.section("menus");
            mark = System.nanoTime();
            tooltip.update(dt);
            if (!window.dragging()) tooltip.draw(width, height);
            GuiGlDebug.section("tooltips");
            long tooltipNanos = System.nanoTime() - mark;
            UiDraw.profileSections(windowNanos, headerNanos, tabsNanos, profileGridNanos, profileSettingsNanos, tooltipNanos);
            if (pixelDebugOverlay) drawPixelDebugOverlay();
            if (renderDebugOverlay) drawRenderDebugOverlay();
            GuiGlDebug.section("F9 overlay");
        } finally {
            UiDraw.flushText();
            UiDraw.flushShapes();
            UiDraw.clearScissor();
            UiDraw.clearRenderTransform();
            GL11.glPopMatrix();
        }
    }

    private void drawModules(float navX, float categoryY, float dt, boolean windowMoved) {
        long mark = System.nanoTime();
        categoryTabs.select(category);
        boolean interactive = state == OPEN && !window.dragging();
        settings.layout(window);
        float gridWidth = window.contentW() - settings.attachedWidth();
        categoryTabs.draw(navX, categoryY, gridWidth - 34F, mouseX, mouseY, false, window.dragging(), interactive);
        float right = window.contentX() + gridWidth;
        float starX = right - 11F, starY = categoryY + UiDraw.mediumHeight() * .5F;
        UiDraw.star(starX, starY, 11F, 5F, favorites ? GuiTokens.STAR_ON : GuiTokens.STAR_OFF, favorites);
        profileModuleTabsNanos = System.nanoTime() - mark;
        mark = System.nanoTime();
        grid.rebuild(favorites ? -1 : category, search.query(), System.nanoTime());
        float dim = 0F;
        if (gridNeedsLayout || !windowMoved) grid.layout(window.contentX(), window.contentY() + contentYOffset.get(), gridWidth, window.contentH(), System.nanoTime(), 0F);
        gridNeedsLayout = false;
        grid.update(dt, System.nanoTime(), mouseX, mouseY, settings.module(), tooltip, interactive);
        grid.draw(mouseX, mouseY, dim, window.dragging());
        if (settings.open() && settings.overlay()) UiDraw.rect(window.contentX(), window.contentY(), window.contentX() + gridWidth, window.contentY() + window.contentH(), UiDraw.alpha(0xFF000000, .55F));
        if (settings.open()) tooltip.clear();
        GuiGlDebug.section("grid");
        profileGridNanos = System.nanoTime() - mark;
        mark = System.nanoTime();
        settings.draw(window, mouseX, mouseY, tooltip);
        GuiGlDebug.section("drawer");
        profileSettingsNanos = System.nanoTime() - mark;
    }

    private void drawTabContent(float dt) {
        float x = window.contentX(), y = window.contentY() + contentYOffset.get(), w = window.contentW(), h = window.contentH();
        long sectionStarted = System.nanoTime();
        try {
            if (mainTab == 3) {
                settings.layout(window);
                float gridW = w - settings.attachedWidth();
                hud.draw(x, y, gridW, h, mouseX, mouseY, window.dragging());
                if (settings.open() && settings.overlay()) UiDraw.rect(x, y, x + gridW, y + h, UiDraw.alpha(0xFF000000, .55F));
                profileGridNanos = System.nanoTime() - sectionStarted;
                GuiGlDebug.section("HUD cards");
                if (settings.open()) tooltip.clear();
                sectionStarted = System.nanoTime();
                settings.draw(window, mouseX, mouseY, tooltip);
                GuiGlDebug.section("HUD drawer");
                profileSettingsNanos = System.nanoTime() - sectionStarted;
                return;
            }
            UiDraw.pushScissor(x, y, w, h);
            if (mainTab == 1) configs.draw(x, y, w, h, mouseX, mouseY, tooltip);
            else friends.draw(x, y, w, h, mouseX, mouseY, tooltip);
            UiDraw.popScissor();
            GuiGlDebug.section(mainTab == 1 ? "configs/widgets" : "friends/widgets");
            profileGridNanos = 0L;
            profileSettingsNanos = System.nanoTime() - sectionStarted;
        } finally {
            UiDraw.setMasterAlpha(progress);
        }
    }

    private void drawGear(){preferences.layout(window,screenW,screenH);preferences.draw(mouseX,mouseY,gear.get());}


    /** Native-pixel comparison aid; hidden unless F8 is pressed. */
    private void drawPixelDebugOverlay() {
        float physical = GuiRenderContext.inverseUiScale();
        float x = 12F * physical, y = 12F * physical, square = 100F * physical;
        UiDraw.rounded(x - 6F * physical, y - 6F * physical, x + square + 6F * physical, y + square + 6F * physical, 6F * physical, 0xD912151C);
        UiDraw.outline(x, y, x + square, y + square, 2F * physical, GuiTokens.accent());
        UiDraw.small("Native-pixel check (F8)", x, y + square + 10F * physical, GuiTokens.TEXT_SECONDARY);
        UiDraw.text("Hamburgefonstiv 15px", x, y + square + 28F * physical, GuiTokens.TEXT_PRIMARY);
        UiDraw.debug30("Hamburgefonstiv 30px", x, y + square + 52F * physical, GuiTokens.TEXT_PRIMARY);
    }

    /** F9 profiler: 120-frame time graph, allocation/GC telemetry and render-path state. */
    private void drawRenderDebugOverlay() {
        float w = 500F, h = 496F, x = window.x() + 12F, y = Math.max(window.y() + 12F, window.y() + window.height() - h - 12F);
        UiDraw.glassRounded(x, y, x + w, y + h, 8F, 0xF012151C, 0xF0101319, GuiTokens.WINDOW_BORDER,
                GuiRenderContext.inverseUiScale(), 0x60000000, 10F);
        UiDraw.small("Render profiler (F9) - " + UiDraw.rendererStatus(), x + 9F, y + 7F, GuiTokens.TEXT_SECONDARY);
        UiDraw.text("avg " + tenths(GuiProfiler.averageMillis()) + "ms   worst " + tenths(GuiProfiler.worstMillis()) + "ms / " + GuiProfiler.samples(), x + 9F, y + 21F, GuiTokens.TEXT_PRIMARY);
        drawFrameGraph(x + 9F, y + 39F, 482F, 40F);
        UiDraw.small("GC " + GuiProfiler.gcCountSinceOpen() + " / " + GuiProfiler.gcMillisSinceOpen() + "ms   alloc " + bytes(GuiProfiler.allocationBytes()), x + 9F, y + 84F, GuiTokens.VALUE_TEXT);
        UiDraw.small("draw " + UiDraw.glDrawCalls() + "   text " + UiDraw.textDrawCalls() + "   atlas " + UiDraw.fontRebuilds()
                + (UiDraw.fontAtlasRebuiltThisFrame() ? " (rebuilt)" : ""), x + 9F, y + 99F, GuiTokens.VALUE_TEXT);
        UiDraw.small("sections W " + tenths(GuiProfiler.windowMillis()) + " H " + tenths(GuiProfiler.headerMillis()) + " T " + tenths(GuiProfiler.tabsMillis())
                + " G " + tenths(GuiProfiler.gridMillis()) + " S " + tenths(GuiProfiler.settingsMillis()), x + 9F, y + 114F, GuiTokens.TEXT_SECONDARY);
        UiDraw.small("GL error: " + GuiGlDebug.status(), x + 9F, y + 128F, GuiTokens.TEXT_SECONDARY);
        UiDraw.small("BLUR: " + BackdropBlur.shared().status(), x + 9F, y + 144F, GuiTokens.TEXT_SECONDARY);
        UiDraw.small(settings.scrollDebug(), x + 9F, y + 161F, GuiTokens.TEXT_SECONDARY);
        UiDraw.small("drawer mouse inside " + settings.contains(drawerMouseX(mouseX), drawerMouseY(mouseY)) + "   last wheel " + lastWheelReceiver,
                x + 9F, y + 178F, GuiTokens.TEXT_SECONDARY);
        UiDraw.small("Icon verification strip — 22px", x + 9F, y + 198F, GuiTokens.TEXT_SECONDARY);
        drawIconStrip(x + 22F, y + 222F, 22F, 30F);
        UiDraw.small("Icon verification strip — 44px", x + 9F, y + 315F, GuiTokens.TEXT_SECONDARY);
        drawIconStrip(x + 34F, y + 356F, 44F, 50F);
        UiDraw.small("card debug: " + (ModuleCard.cardDebug() ? "ON" : "OFF") + " (click to toggle)", x + 9F, y + 461F, GuiTokens.TEXT_PRIMARY);
        UiDraw.small("Missing descriptions: " + GuiDescriptions.missing(), x + 260F, y + 461F, GuiTokens.TEXT_SECONDARY);
    }

    /** Every production icon at both target sizes: bell through save, with no font/icon fallback. */
    private static void drawIconStrip(float x, float y, float size, float step) {
        final int count = GuiIcons.SAVE + 1;
        for (int i = 0; i < count; i++) {
            int column = i % 9, row = i / 9;
            GuiIcons.draw(i, x + column * step, y + row * step, size, i == GuiIcons.STAR ? GuiTokens.STAR_ON : GuiTokens.TEXT_PRIMARY, i == GuiIcons.STAR);
        }
        GuiIcons.draw(GuiIcons.STAR, x, y + 2F * step, size, GuiTokens.STAR_OFF, false);
    }

    private static void drawFrameGraph(float x, float y, float width, float height) {
        UiDraw.rounded(x, y, x + width, y + height, 4F, 0x5410151D);
        UiDraw.rect(x, y + height * .5F, x + width, y + height * .5F + GuiRenderContext.inverseUiScale(), 0x24FFFFFF);
        int samples = GuiProfiler.samples();
        float step = width / 120F;
        for (int i = 0; i < samples; i++) {
            float value = Math.min(20F, GuiProfiler.sample(i));
            float bar = Math.max(1F, value / 20F * (height - 3F));
            int color = value > 8F ? 0xFFE47B7B : value > 4F ? 0xFFF5D742 : GuiTokens.accent();
            UiDraw.rect(x + i * step, y + height - bar - 1F, x + (i + 1) * step - .35F, y + height - 1F, color);
        }
    }

    public void mouseClicked(float x, float y, int button) throws IOException {
        if (state != OPEN) return;
        mouseX = x; mouseY = y;
        float debugTop = Math.max(window.y() + 12F, window.y() + window.height() - 508F);
        if (renderDebugOverlay && UiDraw.inside(x, y, window.x() + 21F, debugTop + 453F, window.x() + 250F, debugTop + 480F)) {
            ModuleCard.toggleCardDebug(); return;
        }
        if (gearOpen) { clickGear(x,y,button); return; }
        if (!UiDraw.inside(x, y, window.x(), window.y(), window.x() + window.width(), window.y() + window.height())) { requestClose(); return; }
        if (pendingTab >= 0) { int tab = mainTabs.click(x, y); if (tab >= 0) switchMain(tab); return; }
        if (gear.get() > .01F) return; // Closing popover remains modal until its fade finishes.
        if (notifications.showing() && notifications.contains(x, y, window.x() + window.width() - 12F, window.y() + 7F)) return;
        int action = header.click(x, y);
        if (action != HeaderBar.NONE) { handleHeader(action); return; }
        float navX = window.x() + GuiTokens.WINDOW_PADDING, mainY = window.y() + 88F, categoryY = window.y() + 128F;
        int tab = mainTabs.click(x, y);
        if (tab >= 0) { switchMain(tab); return; }
        if (mainTab == 0) {
            if (settings.contains(x, y)) { settings.click(x, y, button); return; }
            int categoryAt = categoryTabs.click(x, y);
            if (categoryAt >= 0) { grid.saveScroll(); category = categoryAt; favorites = false; GuiPreferences.setFavoritesOnly(false); grid.invalidate(); gridNeedsLayout = true; GuiPreferences.setLastCategory(category); tooltip.dismiss(); return; }
            settings.layout(window);
            float starX = window.contentX() + window.contentW() - settings.attachedWidth() - 11F;
            if (UiDraw.inside(x, y, starX - 12F, categoryY - 5F, starX + 12F, categoryY + UiDraw.mediumHeight() + 7F)) { grid.saveScroll(); favorites = !favorites; GuiPreferences.setFavoritesOnly(favorites); grid.invalidate(); gridNeedsLayout = true; tooltip.dismiss(); return; }
            if (search.click(x, y, window.x() + window.width() - GuiTokens.WINDOW_PADDING, mainY)) { grid.invalidate(); gridNeedsLayout = true; return; }
            ModuleCard card = grid.click(x, y, button);
            if (card != null) { handleCard(card); return; }
            if (settings.open()) settings.close();
        } else if (mainTab == 1) configs.click(x, y, button);
        else if (mainTab == 2) friends.click(x, y, button);
        else {
            if (settings.contains(x, y)) { settings.click(x, y, button); return; }
            ModuleCard card = hud.click(x, y, button);
            if (card != null) handleCard(card);
        }
        if (window.beginDrag(x, y)) return;
    }

    public void mouseReleased(float x, float y, int button) {
        header.resetInteraction();
        if(gearOpen)preferenceAction(preferences.release());
        window.release(); grid.release(); hud.release(); settings.release(); configs.release();
    }

    /** Window drags only move the window. Scrollbars/sliders never run expensive work in parallel. */
    public void mouseDragged(float x, float y, int button) {
        if (state != OPEN) return;
        if(gearOpen){preferences.drag(x,y);return;}
        if (window.dragging()) { window.drag(x, y, screenW, screenH); return; }
        if (mainTab == 1) { configs.drag(y); return; }
        grid.drag(y); settings.drag(x, y);
    }

    public void wheel(float amount, float x, float y) {
        mouseX = x; mouseY = y;
        tooltip.dismiss();
        if (state != OPEN && state != OPENING && state != CLOSING) return;
        window.layout(GuiRenderContext.designWidth(), GuiRenderContext.designHeight());
        if(gearOpen){preferences.wheel(amount);return;}
        settings.layout(window);
        float localX = drawerMouseX(x), localY = drawerMouseY(y);
        if ((mainTab == 0 || mainTab == 3) && settings.contains(localX, localY)) {
            lastWheelReceiver = settings.wheel(amount, localX, localY);
            return;
        }
        if (state != OPEN) { lastWheelReceiver = "none (window animating)"; return; }
        if (mainTab == 0) { grid.wheel(amount); lastWheelReceiver = "module grid"; }
        else if (mainTab == 1) { configs.wheel(amount); lastWheelReceiver = "configs"; }
        else if (mainTab == 2) { friends.wheel(amount); lastWheelReceiver = "friends"; }
        else { hud.wheel(amount); lastWheelReceiver = "HUD grid"; }
    }

    private float drawerMouseX(float x) { float center = window.x() + window.width() * .5F; return center + (x - center) / window.scale(); }
    private float drawerMouseY(float y) { float center = window.y() + window.height() * .5F; return center + (y - center) / window.scale(); }

    public boolean key(char character, int keyCode) {
        if (state == CLOSING) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) { finishCloseSoon(); return true; }
            return true;
        }
        if (state != OPEN) return true;
        if(gearOpen){if(keyCode==Keyboard.KEY_ESCAPE&&preferences.escape()){gearOpen=false;gear.setTarget(0);preferences.reset();GuiPreferences.save();}return true;}
        if (keyCode == Keyboard.KEY_ESCAPE && settings.open()) { settings.key(character, keyCode); return true; }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) { requestClose(); return true; }
        if (keyCode == Keyboard.KEY_F8) { pixelDebugOverlay = !pixelDebugOverlay; return true; }
        if (keyCode == Keyboard.KEY_F9) { renderDebugOverlay = !renderDebugOverlay; if (renderDebugOverlay) GuiDescriptions.dumpMissing(); return true; }
        if (settings.key(character, keyCode)) return true;
        if (mainTab == 0 && grid.key(character, keyCode)) return true;
        if (mainTab == 0 && search.key(character, keyCode)) { grid.invalidate(); gridNeedsLayout = true; return true; }
        if (mainTab == 1 && configs.key(character, keyCode)) return true;
        if (mainTab == 2 && friends.key(character, keyCode)) return true;
        boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (control && keyCode == Keyboard.KEY_F && mainTab == 0) { search.focus(); return true; }
        if (mainTab == 0 && !search.focused()) {
            if (keyCode == Keyboard.KEY_Q || keyCode == Keyboard.KEY_LEFT) { switchMain((mainTab + MAIN.length - 1) % MAIN.length); return true; }
            if (keyCode == Keyboard.KEY_E || keyCode == Keyboard.KEY_RIGHT) { switchMain((mainTab + 1) % MAIN.length); return true; }
            if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_6) { grid.saveScroll(); category = keyCode - Keyboard.KEY_1; favorites = false; GuiPreferences.setLastCategory(category); GuiPreferences.setFavoritesOnly(false); grid.invalidate(); gridNeedsLayout = true; tooltip.dismiss(); return true; }
            if (keyCode == Keyboard.KEY_UP) { grid.moveFocus(-1); return true; }
            if (keyCode == Keyboard.KEY_DOWN) { grid.moveFocus(1); return true; }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) { grid.toggleFocused(); return true; }
        }
        return false;
    }

    public void requestClose() {
        if (state == CLOSED || state == CLOSING) return;
        saveActiveScroll();
        if (pendingTab >= 0) { mainTab = pendingTab; pendingTab = -1; }
        state = CLOSING; closeStarted = System.nanoTime();
        startProgress(0F, CLOSE_NANOS);
        tooltip.dismiss(); notifications.dismiss(); settings.dismiss(); search.clear(); search.cancelFocus(); configs.cancelFocus(); friends.cancelFocus();
        grid.resetInteraction(); hud.resetInteraction(); window.cancelDrag(); header.resetInteraction(); mainTabs.resetInteraction(); categoryTabs.resetInteraction();
        gearOpen = false; gear.snapTo(0F);
        Keyboard.enableRepeatEvents(false);
        try { Minecraft.getMinecraft().setIngameFocus(); } catch (Throwable ignored) { }
    }

    /** The second Escape/open key never reopens or double-closes; it simply completes the queued close. */
    private void finishCloseSoon() { if (state == CLOSING) { progress = 0F; progressFrom = progressTo = 0F; } }

    private void finishCloseFromTick() {
        if (closeDispatched) return;
        closeDispatched = true;
        state = CLOSED;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == host) mc.displayGuiScreen(null);
    }

    public void closed() {
        saveActiveScroll(); GuiPreferences.setLastTab(mainTab); GuiPreferences.setLastCategory(category); GuiPreferences.save();
        Keyboard.enableRepeatEvents(false); UiDraw.clearScissor(); settings.dismiss(); grid.resetInteraction(); hud.resetInteraction();
        tooltip.dismiss(); search.clear(); search.cancelFocus(); configs.cancelFocus(); friends.cancelFocus();
        gearOpen = false; gear.snapTo(0F); state = CLOSED;
    }

    private void switchMain(int value) {
        if (value == pendingTab || value == mainTab && pendingTab < 0) return;
        saveActiveScroll(); tooltip.dismiss();
        if (value == mainTab) {
            float exit = 1F - MotionTokens.ease(Math.min(1F, (System.nanoTime() - tabExitStarted) * Math.max(.01F, GuiTokens.animationSpeed()) / MotionTokens.CONTENT_EXIT_NANOS));
            contentAlpha.snapTo(contentAlpha.get() * exit); contentAlpha.setTarget(1F);
            pendingTab = -1; mainTabs.select(value); GuiPreferences.setLastTab(value); return;
        }
        mainTabs.select(value); GuiPreferences.setLastTab(value);
        if (!GuiTokens.animationsEnabled()) {
            mainTab = value; pendingTab = -1; settings.dismiss(); search.blur(); gridNeedsLayout = true;
            contentAlpha.snapTo(1F); contentYOffset.snapTo(0F);
            if (value == 1) configs.onTabOpened(); else if (value == 2) friends.onTabOpened(); else if (value == 3) hud.refresh();
        } else { if (pendingTab < 0) tabExitStarted = System.nanoTime(); pendingTab = value; }
    }

    private void handleHeader(int action) {
        if (action == HeaderBar.BELL) notifications.toggle();
        else if (action == HeaderBar.REFRESH) { grid.refreshCards(); grid.invalidate(); gridNeedsLayout = true; hud.refresh(); if (Raven.configManager != null) Raven.configManager.discoverConfigs(); notifications.add("Refreshed client data"); }
        else { gearOpen = !gearOpen; preferences.reset(); preferences.layout(window,screenW,screenH); gear.setTarget(gearOpen ? 1F : 0F); }
    }

    private boolean clickGear(float x,float y,int button){preferences.layout(window,screenW,screenH);preferenceAction(preferences.click(x,y,button));return true;}
    private void preferenceAction(int action){
        if((action&1)!=0){preferenceAction(preferences.release());gearOpen=false;gear.setTarget(0);preferences.reset();GuiPreferences.save();}
        if((action&2)!=0){mainTabs.invalidateMetrics();categoryTabs.invalidateMetrics();grid.invalidateLayout();gridNeedsLayout=true;}
        if((action&4)!=0){grid.invalidate();gridNeedsLayout=true;}
    }


    private void handleCard(ModuleCard card) {
        int action = card.lastAction();
        if (action == ModuleCard.SETTINGS) { tooltip.dismiss(); settings.toggle(card.module()); gridNeedsLayout = true; }
        else if (action == ModuleCard.TOGGLED) notifications.add(card.module().getName() + (card.module().isEnabled() ? " enabled" : " disabled"));
        else if (action == ModuleCard.FAVORITE) { grid.invalidate(); gridNeedsLayout = true; notifications.add(card.module().getName() + " favorite updated"); }
        else if (action == ModuleCard.BINDING) notifications.add("Press a key for " + card.module().getName());
    }

    private void startProgress(float target, long duration) {
        progressFrom = progress; progressTo = target; progressStarted = System.nanoTime(); progressDuration = Math.max(1L, duration); window.setProgress(progress);
    }

    private void updateProgress() {
        if (progress == progressTo) { window.setProgress(progress); return; }
        float t = GuiTokens.animationsEnabled() ? Math.max(0F, Math.min(1F, (System.nanoTime() - progressStarted) * GuiTokens.animationSpeed() / (float) progressDuration)) : 1F;
        float eased = MotionTokens.ease(t);
        progress = progressFrom + (progressTo - progressFrom) * eased;
        if (t >= 1F) progress = progressTo;
        window.setProgress(progress);
    }

    private static String tenths(float value) { return String.valueOf(Math.round(value * 10F) / 10F); }
    private static String bytes(long value) {
        if (value < 0L) return "n/a";
        return value >= 1_048_576L ? (value / 1_048_576L) + "MB" : (value / 1024L) + "KB";
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int valid(int value, int count) { return value >= 0 && value < count ? value : 0; }
    private void saveActiveScroll() {
        if (mainTab == 0) grid.saveScroll();
        else if (mainTab == 1) configs.saveScroll();
        else if (mainTab == 2) friends.saveScroll();
        else hud.saveScroll();
    }
}
