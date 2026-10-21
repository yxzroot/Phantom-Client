package keystrokesmod.client.clickgui.apple.rebuild;

import java.util.ArrayList;
import keystrokesmod.client.clickgui.color.ColorPickerPopup;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.ButtonSetting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/** In-window settings drawer. It replaces the overlapping floating inspector entirely. */
public final class SettingsDrawer {
    private final GuiScreen host;
    private final ArrayList<SettingRow> rows = new ArrayList<SettingRow>();
    private final ScrollView scroll = new ScrollView();
    private final Anim open = new Anim(0F, Anim.Mode.SPRING, MotionTokens.DRAWER_OPEN);
    private final Anim togglePosition = new Anim(0F, Anim.Mode.SPRING, MotionTokens.TOGGLE_KNOB);
    private final Anim toggleTrack = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.TOGGLE_TRACK);
    private final Anim swap = new Anim(1F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.VALUE_CHANGE);
    private final GuiButton closeButton = new GuiButton(GuiButton.GHOST_ICON, "", GuiIcons.CLOSE);
    private final GuiButton resetButton = new GuiButton(GuiButton.SECONDARY, "Reset to defaults", GuiIcons.REFRESH);
    private final GuiTextField filter = new GuiTextField("Search settings...", GuiIcons.SEARCH, 42);
    private Module module;
    private KeybindRow keybind;
    private SettingRow dragging;
    private SettingRow revealRow;
    private float left, right, top, bottom, listTop, footerTop, drawerWidth, total;
    private long closingStarted;
    private boolean overlay;
    private String measuredName = "", rawName = "", measuredDescription = "", rawDescription = "";
    private String priorName = "", priorDescription = "";
    private float nameWidth, descriptionWidth, metricScale = -1F;

    public SettingsDrawer(GuiScreen host) { this.host = host; }
    public Module module() { return module; }
    private float progress() { return Math.max(0F, Math.min(1F, open.get())); }
    public boolean open() { return module != null && (open.get() > .01F || open.target() > .01F); }
    public boolean opening() { return module != null; }
    public boolean overlay() { return overlay; }
    public float attachedWidth() { return !overlay && module != null ? (drawerWidth + 14F) * progress() : 0F; }

    /** Same source card toggles the drawer; another source card swaps content without closing. */
    public void toggle(Module next) {
        if (next == null) { close(); return; }
        if (module == next && open.target() > .5F) { close(); return; }
        show(next);
    }
    public void show(Module next) {
        if (next == null) { close(); return; }
        if (module == next && open.target() > .5F) { open.setTarget(1F); return; }
        release();
        if (keybind != null) keybind.cancel();
        if (module != null && module != next && open.target() > .5F) {
            priorName = measuredName; priorDescription = measuredDescription;
            swap.snapTo(0F); swap.setTarget(1F);
        } else { priorName = priorDescription = ""; swap.snapTo(1F); }
        module = next; rows.clear(); revealRow = null; filter.clear(); Keyboard.enableRepeatEvents(false);
        measuredName = ""; rawName = ""; measuredDescription = ""; rawDescription = ""; scroll.reset();
        ArrayList<Setting> settings = next.getSettings();
        for (int i = 0; i < settings.size(); i++) { SettingRow row = create(next, settings.get(i)); if (row != null) rows.add(row); }
        keybind = next.isBindable() ? new KeybindRow(next) : null;
        for (int i = 0; i < rows.size(); i++) rows.get(i).setIndex(i);
        open.setDuration(MotionTokens.DRAWER_OPEN); open.setTarget(1F);
    }
    public void close() { release(); revealRow = null; scroll.reset(); open.setDuration(MotionTokens.DRAWER_CLOSE); open.setTarget(0F); closingStarted = System.nanoTime(); filter.cancel(); if (keybind != null) keybind.cancel(); Keyboard.enableRepeatEvents(false); }
    public void dismiss() { release(); revealRow = null; open.snapTo(0F); module = null; keybind = null; rows.clear(); filter.clear(); scroll.reset(); Keyboard.enableRepeatEvents(false); }
    public void update(float dt) {
        togglePosition.setTarget(module != null && module.isEnabled() ? 1F : 0F);
        togglePosition.update(dt);
        toggleTrack.setTarget(module != null && module.isEnabled() ? 1F : 0F); toggleTrack.update(dt);
        open.update(dt); swap.update(dt); closeButton.update(dt); resetButton.update(dt); filter.update(dt);
        if (swap.isDone()) priorName = priorDescription = "";
        for (int i = 0; i < rows.size(); i++) rows.get(i).update(dt);
        if (keybind != null) keybind.update(dt);
        refreshMetrics();
        if (revealRow != null) {
            if (!revealRow.expanding() || !revealRow.visible() || !revealRow.matches(filter.value())) revealRow = null;
            else {
                float end = 4F;
                for (int i = 0; i < rows.size(); i++) {
                    SettingRow row = rows.get(i);
                    if (!row.visible() || !row.matches(filter.value())) continue;
                    end += row.height();
                    if (row == revealRow) { scroll.ensureVisible(end); break; }
                    end += 8F;
                }
            }
        }
        scroll.update(dt);
        if (open.target() <= .01F && (open.get() <= .01F || closingStarted != 0L && System.nanoTime() - closingStarted >= 300_000_000L)) { open.snapTo(0F); module = null; keybind = null; rows.clear(); scroll.reset(); }
    }

    /** Resolves geometry before ModuleGrid lays itself out; no grid card can render under an attached drawer. */
    public void layout(MainWindow window) {
        overlay = window.width() < 900F;
        drawerWidth = Math.min(window.contentW(), Math.max(320F, Math.min(380F, window.width() * .36F)));
        top = window.y() + 128F; bottom = window.contentY() + window.contentH();
        right = window.contentX() + window.contentW();
        left = right - drawerWidth + (1F - progress()) * MotionTokens.DRAWER_SLIDE_PX;
        listTop = top + 72F + (rows.size() > 8 ? 48F : 0F) + 8F;
        footerTop = bottom - 56F;
        refreshMetrics();
    }

    private void refreshMetrics() {
        if (module == null) { total = 0F; scroll.setMetrics(0F, 0F); return; }
        total = 4F;
        String query = filter.value();
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).matches(query) && rows.get(i).visible()) total += rows.get(i).height() + 8F;
        scroll.setMetrics(total, Math.max(0F, footerTop - listTop - 8F));
    }

    public void draw(MainWindow window, float mouseX, float mouseY, Tooltip tooltip) {
        if (module == null || open.get() <= .01F) return;
        layout(window); ensureMetrics();
        float a = progress();
        BackdropBlur.shared().draw(left, top, right - left, bottom - top, 14F, a);
        UiDraw.glassRounded(left, top, right, bottom, 14F, UiDraw.alpha(0xF00A0D12, a), UiDraw.alpha(0xF00A0D12, a), UiDraw.alpha(0xFFFFFFFF, a * .07F), GuiRenderContext.inverseUiScale(), 0, 0F);
        // Header: no duplicate brand; module identity, control and close are vertically centered.
        if (priorName.length() > 0) {
            UiDraw.bold(priorName, left + 18F, top + 15F - swap.get() * 6F, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, a * (1F - swap.get())));
            UiDraw.small(priorDescription, left + 18F, top + 41F - swap.get() * 6F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, a * (1F - swap.get())));
        }
        UiDraw.bold(measuredName, left + 18F, top + 15F + (1F - swap.get()) * 6F, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, a * swap.get()));
        UiDraw.small(measuredDescription, left + 18F, top + 41F + (1F - swap.get()) * 6F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, a * swap.get()));
        float toggleX = right - 34F - 14F - 44F;
        GuiToggle.draw(toggleX, top + 24F, togglePosition.get(), toggleTrack.get(), 0F, a, false);
        closeButton.draw(right - 34F - 8F, top + 19F, 34F, mouseX, mouseY, true);
        if (rows.size() > 8) filter.draw(left + 14F, top + 80F, drawerWidth - 28F, mouseX, mouseY);
        String query = filter.value();
        float viewport = scroll.viewport();
        if (rows.isEmpty()) {
            GuiWidgets.empty(GuiIcons.INFO, "No settings for this module", "This module only exposes its keybind.", left, listTop, drawerWidth, viewport, a);
        } else {
            UiDraw.pushScissor(left + 8F, listTop, drawerWidth - 16F, viewport);
            float cursor = listTop + 4F - scroll.visualPosition();
            for (int i = 0; i < rows.size(); i++) {
                SettingRow row = rows.get(i); if (!row.matches(query) || !row.visible()) continue;
                if (cursor + row.height() >= listTop - 18F && cursor <= footerTop + 18F) {
                    row.draw(left + 14F, right - 14F, cursor, mouseX, mouseY, a * swap.get());
                    if (mouseY >= listTop && mouseY < footerTop) row.help(tooltip, mouseX, mouseY);
                }
                cursor += row.height() + 8F;
            }
            UiDraw.popScissor();
            UiDraw.verticalGradient(left + 8F, listTop, right - 8F, listTop + 20F, UiDraw.alpha(0xFF10141B, .72F), 0);
            UiDraw.verticalGradient(left + 8F, footerTop - 20F, right - 8F, footerTop, 0, UiDraw.alpha(0xFF10141B, .72F));
            if (viewport > 8F) scroll.renderBar(right - 8F, listTop + 4F, viewport - 8F, UiDraw.inside(mouseX, mouseY, right - 16F, listTop, right, footerTop));
        }
        UiDraw.rect(left + 14F, footerTop - GuiRenderContext.inverseUiScale(), right - 14F, footerTop, UiDraw.alpha(0xFFFFFFFF, .08F * a));
        if (keybind != null) keybind.draw(left + 14F, right - 190F, footerTop + 2F, mouseX, mouseY, a);
        resetButton.draw(right - 174F, footerTop + 11F, 160F, mouseX, mouseY, true);
        if (open.get() >= .995F && UiDraw.inside(mouseX, mouseY, left + 18F, top + 35F, right - 110F, top + 56F) && measuredDescription.length() < rawDescription.length()) tooltip.hover(rawDescription, mouseX, mouseY);
    }

    public boolean click(float mouseX, float mouseY, int button) {
        if (module == null || open.get() <= .01F) return false;
        if (!contains(mouseX, mouseY)) return false;
        filter.blurIfOutside(mouseX, mouseY);
        if (closeButton.click(mouseX, mouseY, button)) { close(); return true; }
        if (resetButton.click(mouseX, mouseY, button)) { module.resetToDefaults(); refreshMetrics(); return true; }
        float toggleX = right - 34F - 14F - 44F;
        if (button == 0 && UiDraw.inside(mouseX, mouseY, toggleX, top + 20F, toggleX + 44F, top + 52F)) { module.toggle(); return true; }
        if (rows.size() > 8 && filter.click(mouseX, mouseY, button)) return true;
        if (keybind != null && keybind.contains(mouseX, mouseY)) { keybind.click(mouseX, mouseY, button); return true; }
        if (button == 0 && scroll.viewport() > 8F && scroll.beginBar(mouseX, mouseY, right - 8F, listTop + 4F, 4F, scroll.viewport() - 8F)) return true;
        if (mouseY < listTop || mouseY >= footerTop) return true;
        for (int i = rows.size() - 1; i >= 0; i--) {
            SettingRow row = rows.get(i); if (!row.matches(filter.value()) || !row.visible() || !row.contains(mouseX, mouseY)) continue;
            if (row.helpHit(mouseX, mouseY)) return true;
            if (row instanceof ColorRow && button == 0 && row.setting instanceof RGBSetting) { float factor=GuiRenderContext.uiScale()/GuiRenderContext.minecraftScale(); ColorPickerPopup.open(host,(RGBSetting)row.setting,mouseX*factor,mouseY*factor); return true; }
            if (row.click(mouseX, mouseY, button)) { dragging = row.dragging() ? row : null; revealRow = row.expanding() ? row : null; return true; }
        }
        return true;
    }
    public void drag(float mouseX, float mouseY) { if (scroll.viewport() > 8F) scroll.dragBar(mouseY, listTop + 4F, scroll.viewport() - 8F); if (dragging != null) dragging.drag(mouseX, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)); }
    public void release() { closeButton.release(); resetButton.release(); if (dragging != null) dragging.release(); dragging = null; scroll.release(); for (int i = 0; i < rows.size(); i++) rows.get(i).release(); if (keybind != null) keybind.release(); }
    public boolean dragging() { return dragging != null || scroll.dragging(); }
    /** Always consumes a wheel inside the whole drawer, including fixed chrome and empty lists. */
    public String wheel(float amount, float mouseX, float mouseY) {
        if (dragging()) return "drawer: ignored (drag)";
        refreshMetrics();
        boolean fine = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        if (fine && mouseY >= listTop && mouseY < footerTop && mouseX >= left + 14F && mouseX < right - 14F) {
            float cursor = listTop + 4F - scroll.visualPosition();
            for (int i = 0; i < rows.size(); i++) {
                SettingRow row = rows.get(i);
                if (!row.visible() || !row.matches(filter.value())) continue;
                if (row instanceof SliderRow && mouseY >= cursor && mouseY < cursor + row.baseHeight()) { row.wheel(amount); return "drawer: slider (Ctrl/Alt)"; }
                cursor += row.height() + 8F;
            }
        }
        revealRow = null;
        scroll.wheel(amount);
        return "drawer: list";
    }
    public boolean key(char character, int keyCode) {
        if (module == null) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) { release(); revealRow = null; scroll.reset(); }
        if (keybind != null && keybind.key(character, keyCode)) return true;
        if (keyCode == Keyboard.KEY_ESCAPE) { close(); return true; }
        if (filter.key(character, keyCode)) { revealRow = null; refreshMetrics(); return true; }
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).key(character, keyCode)) return true;
        if (keybind != null && keybind.capturing()) return false;
        refreshMetrics();
        if (keyCode == Keyboard.KEY_PRIOR) scroll.page(-scroll.viewport());
        else if (keyCode == Keyboard.KEY_NEXT) scroll.page(scroll.viewport());
        else if (keyCode == Keyboard.KEY_HOME) scroll.home();
        else if (keyCode == Keyboard.KEY_END) scroll.end();
        else if (keyCode == Keyboard.KEY_UP) scroll.page(-40F);
        else if (keyCode == Keyboard.KEY_DOWN) scroll.page(40F);
        else return false;
        revealRow = null;
        return true;
    }
    public boolean contains(float mouseX, float mouseY) { return module != null && UiDraw.inside(mouseX, mouseY, left, top, right, bottom); }
    public String scrollDebug() { return "drawer contentH " + (int) total + " viewH " + (int) scroll.viewport() + " max " + (int) scroll.max()
            + "  current " + scroll.position() + " target " + scroll.target(); }

    private void ensureMetrics() {
        float scale = GuiRenderContext.uiScale(); String name = module.getName(), description = module.getDescription() == null ? "" : module.getDescription();
        if (scale == metricScale && name.equals(rawName) && description.equals(rawDescription)) return;
        metricScale = scale; rawName = name; measuredName = name; nameWidth = UiDraw.boldWidth(name);
        float nameLimit = drawerWidth - 158F;
        if (nameWidth > nameLimit) {
            int end = name.length();
            while (end > 0 && UiDraw.boldWidth(name.substring(0, end) + "...") > nameLimit) --end;
            measuredName = end == 0 ? "..." : name.substring(0, end) + "...";
            nameWidth = UiDraw.boldWidth(measuredName);
        }
        float max = drawerWidth - 142F; int end = description.length();
        while (end > 0 && UiDraw.smallWidth(description.substring(0, end)) > max) --end;
        rawDescription = description;
        measuredDescription = end < description.length() ? description.substring(0, Math.max(0, end)) + "..." : description;
        descriptionWidth = UiDraw.smallWidth(measuredDescription);
    }
    private static SettingRow create(Module module, Setting setting) {
        if (setting instanceof TickSetting) return new BoolRow(module, (TickSetting) setting);
        if (setting instanceof SliderSetting) return new SliderRow(module, (SliderSetting) setting);
        if (setting instanceof DoubleSliderSetting) return new SliderRow(module, (DoubleSliderSetting) setting);
        if (setting instanceof ComboSetting) return new ModeRow(module, (ComboSetting<?>) setting);
        if (setting instanceof RGBSetting) return new ColorRow(module, (RGBSetting) setting);
        if (setting instanceof ButtonSetting) return new ActionRow(module, (ButtonSetting) setting);
        if (setting instanceof DescriptionSetting) return new TextRow(module, setting);
        return new TextRow(module, setting);
    }
}
