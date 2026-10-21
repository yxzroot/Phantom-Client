package keystrokesmod.client.clickgui.apple.rebuild;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import keystrokesmod.client.config.Config;
import keystrokesmod.client.main.Raven;
import org.lwjgl.input.Keyboard;

/** Config manager presentation backed directly by the existing ConfigManager. */
public final class ConfigTab {
    private static final class Visual {
        final GlassRow row = new GlassRow();
        final GuiButton load = new GuiButton(GuiButton.SECONDARY, "Load", GuiIcons.DOWNLOAD);
        final GuiButton save = new GuiButton(GuiButton.SECONDARY, "Save", GuiIcons.SAVE);
        final GuiButton delete = new GuiButton(GuiButton.DANGER, "Delete", GuiIcons.TRASH);
        final GuiButton cancel = new GuiButton(GuiButton.SECONDARY, "Cancel", -1);
        final GuiButton confirm = new GuiButton(GuiButton.DANGER, "Delete", GuiIcons.TRASH);
        final long modified;
        final String modifiedLabel;
        final String name;
        final Anim reveal = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER);
        long showAt;
        boolean removing;
        String clippedName;
        float clippedWidth = -1F, clippedScale = -1F, nameWidth;
        float helpX, helpY;
        Visual(Config config) {
            name = config.getName(); clippedName = name;
            modified = config.file.lastModified();
            long minutes = Math.max(0L, (System.currentTimeMillis() - modified) / 60_000L);
            modifiedLabel = modified <= 0L ? "Saved config" : minutes < 1L ? "Last modified just now"
                    : minutes < 60L ? "Last modified " + minutes + "m ago"
                    : minutes < 1440L ? "Last modified " + minutes / 60L + "h ago"
                    : "Last modified " + minutes / 1440L + "d ago";
        }
        void show(int index) { if (showAt == 0L && reveal.target() == 0F && !removing) showAt = System.nanoTime() + MotionTokens.staggerNanos(index, MotionTokens.CONTENT_STAGGER_MS, MotionTokens.CONTENT_STAGGER_CAP_MS); }
        void update(float dt) { if (showAt != 0L && System.nanoTime() >= showAt) { showAt = 0L; reveal.setTarget(1F); } reveal.update(dt); row.update(dt); load.update(dt); save.update(dt); delete.update(dt); cancel.update(dt); confirm.update(dt); }
        float progress() { return Math.max(0F, Math.min(1F, reveal.get())); }
        String displayName(float limit) {
            float scale = GuiRenderContext.uiScale();
            if (clippedWidth != limit || clippedScale != scale) {
                clippedWidth = limit; clippedScale = scale; clippedName = name;
                if (UiDraw.boldWidth(name) > limit) { int end = name.length(); while (end > 0 && UiDraw.boldWidth(name.substring(0, end) + "...") > limit) --end; clippedName = end == 0 ? "..." : name.substring(0, end) + "..."; }
                nameWidth = UiDraw.boldWidth(clippedName);
            }
            return clippedName;
        }
    }
    private final GuiTextField field = new GuiTextField("Config name...", GuiIcons.FILE, 30);
    private final GuiButton create = new GuiButton(GuiButton.PRIMARY, "Create", GuiIcons.PLUS);
    private final GlassRow createRow = new GlassRow();
    private final ScrollView scroll = new ScrollView();
    private final IdentityHashMap<Config, Visual> visuals = new IdentityHashMap<Config, Visual>();
    private Config confirmDelete;
    private float x, y, width, height, listTop, rowsTop, viewport;
    private String validation = "";
    private int cachedCount = -1;
    private String countLabel = "0";
    private boolean restoreScroll = true;
    private ArrayList<Config> configs = new ArrayList<Config>();
    private boolean success;

    private void refresh() {
        if (Raven.configManager == null) return;
        Raven.configManager.discoverConfigs();
        configs = Raven.configManager.getConfigs();
        visuals.keySet().retainAll(configs);
    }
    private void error(RuntimeException failure) { validation = failure.getMessage() == null ? "Config action failed; your files were kept." : failure.getMessage(); success = false; }

    public void update(float dt) {
        field.update(dt); create.update(dt); createRow.update(dt); scroll.update(dt);
        for (Iterator<Map.Entry<Config, Visual>> it = visuals.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Config, Visual> item = it.next(); Visual visual = item.getValue(); visual.update(dt);
            if (visual.removing && visual.reveal.isDone() && visual.progress() <= .01F && Raven.configManager != null) {
                try { Raven.configManager.deleteConfig(item.getKey()); validation = "Config removed (recoverable backup kept)."; success = true; it.remove(); }
                catch (RuntimeException failure) { visual.removing = false; visual.reveal.setTarget(1F); error(failure); }
            }
        }
    }
    public void onTabOpened() { scroll.release(); try { refresh(); } catch (RuntimeException failure) { error(failure); } for (Visual visual : visuals.values()) if (!visual.removing) { visual.reveal.snapTo(0F); visual.showAt = 0L; } restoreScroll = true; }
    public void draw(float left, float top, float w, float h, float mouseX, float mouseY, Tooltip tooltip) {
        x = left + 32F; y = top + 8F; width = Math.max(1F, w - 64F); height = h - 8F;
        GuiWidgets.section("CREATE NEW CONFIG", x, y, width);
        float createY = y + 25F;
        createRow.draw(x, createY, width, 64F, mouseX, mouseY, false, 1F);
        field.draw(x + 14F, createY + 12F, width - 14F - 112F, mouseX, mouseY);
        create.draw(x + width - 88F, createY + 15F, 74F, mouseX, mouseY, true);
        if (validation.length() > 0) UiDraw.small(validation, x + 14F, createY + 66F, success ? 0xFF64D99B : 0xFFE5484D);
        listTop = createY + 94F;
        int count = configs == null ? 0 : configs.size();
        if (cachedCount != count) { cachedCount = count; countLabel = String.valueOf(count); }
        GuiWidgets.section("YOUR CONFIGS", x, listTop, width - 64F);
        GuiWidgets.badge(countLabel, x + width - 54F, listTop - 4F, false);
        rowsTop = listTop + 25F; viewport = Math.max(1F, top + h - rowsTop);
        if (configs == null || configs.isEmpty()) { scroll.setMetrics(0F, viewport); GuiWidgets.empty(GuiIcons.FILE, "No configs yet", "Create one to save your current client setup.", x, rowsTop, width, viewport, 1F); return; }
        // Reserve full rows during entrance: growing content used to eat early wheel input
        // and the delayed saved-position restore could then jump straight back to the top.
        float content = 4F;
        for (int i = 0; i < configs.size(); i++) { Visual visual = visual(configs.get(i)); visual.show(i); content += rowSpace(visual); }
        scroll.setMetrics(content, viewport);
        if (restoreScroll) { scroll.restore(GuiPreferences.scroll(7)); restoreScroll = false; }
        UiDraw.pushScissor(x - 8F, rowsTop, width + 8F, viewport);
        float cursor = rowsTop + 4F - scroll.visualPosition(); Config active = Raven.configManager.getConfig();
        for (int i = 0; i < configs.size(); i++) {
            Config config = configs.get(i); Visual visual = visual(config); boolean selected = active != null && config.getName().equals(active.getName());
            float reveal = visual.progress(), space = rowSpace(visual);
            if (reveal <= .01F) { cursor += space; continue; }
            float previousAlpha = UiDraw.masterAlpha(); UiDraw.setMasterAlpha(previousAlpha * reveal);
            float visualY = cursor + (1F - reveal) * MotionTokens.CONTENT_RISE_PX;
            visual.row.draw(x, visualY, width, 72F, mouseX, mouseY, selected, 1F);
            UiDraw.glassRounded(x + 14F, visualY + 16F, x + 54F, visualY + 56F, 9F, UiDraw.alpha(GuiTokens.accent(), .24F), UiDraw.alpha(0xFF161B27, .95F), UiDraw.alpha(GuiTokens.accent(), .35F), GuiRenderContext.inverseUiScale(), 0, 0F);
            GuiIcons.draw(GuiIcons.FILE, x + 34F, visualY + 36F, 20F, GuiTokens.accent());
            float limit = Math.max(52F, width - 68F - (selected ? 278F : 234F));
            String name = visual.displayName(limit);
            UiDraw.bold(name, x + 68F, visualY + 16F, GuiTokens.TEXT_PRIMARY);
            UiDraw.small(visual.modifiedLabel, x + 68F, visualY + 39F, GuiTokens.TEXT_SECONDARY);
            visual.helpX = x + width - 224F; visual.helpY = visualY + 36F;
            tooltip.helpIcon(visual, config.getName(), "A saved client setup. Load it, update it with Save, or remove it with Delete.",
                    visual.helpX, visual.helpY, mouseX, mouseY >= rowsTop && mouseY <= top + h ? mouseY : -10000F, reveal);
            if (selected) GuiWidgets.badge("Active", x + 68F + visual.nameWidth + 10F, visualY + 14F, true);
            if (confirmDelete == config) {
                UiDraw.text("Delete this config?", x + width - 245F, visualY + 27F, 0xFFE5484D);
                visual.cancel.draw(x + width - 122F, visualY + 19F, 56F, mouseX, mouseY, true); visual.confirm.draw(x + width - 60F, visualY + 19F, 60F, mouseX, mouseY, true);
            } else {
                visual.load.draw(x + width - 206F, visualY + 19F, 62F, mouseX, mouseY, !visual.removing); visual.save.draw(x + width - 136F, visualY + 19F, 62F, mouseX, mouseY, !visual.removing); visual.delete.draw(x + width - 66F, visualY + 19F, 66F, mouseX, mouseY, !visual.removing);
            }
            UiDraw.setMasterAlpha(previousAlpha);
            cursor += space;
        }
        UiDraw.popScissor();
        if (scroll.canScroll()) {
            // Persistent, discoverable thumb in its own gutter, clear of row action buttons.
            float thumb = Math.min(viewport, Math.max(20F, viewport * viewport / scroll.content()));
            float at = Math.max(0F, Math.min(1F, scroll.position() / scroll.max()));
            float barY = rowsTop + (viewport - thumb) * at;
            boolean hover = UiDraw.inside(mouseX, mouseY, x + width + 3F, rowsTop, x + width + 19F, rowsTop + viewport);
            UiDraw.rounded(x + width + 9F, rowsTop, x + width + 13F, rowsTop + viewport, 2F, 0x18FFFFFF);
            UiDraw.rounded(x + width + 9F, barY, x + width + 13F, barY + thumb, 2F,
                    UiDraw.alpha(GuiTokens.TEXT_SECONDARY, hover || scroll.dragging() ? .95F : .5F));
        }
    }
    public boolean click(float mouseX, float mouseY, int button) {
        if (button != 0 || Raven.configManager == null) return false;
        field.blurIfOutside(mouseX, mouseY);
        if (scroll.beginBar(mouseX, mouseY, x + width + 8F, rowsTop, 6F, viewport)) { restoreScroll = false; return true; }
        if (field.click(mouseX, mouseY, button)) return true;
        if (create.click(mouseX, mouseY, button)) { createConfig(); return true; }
        float rowsTop = listTop + 25F;
        if (!UiDraw.inside(mouseX, mouseY, x - 8F, rowsTop, x + width + 8F, y + height)) return false;
        for (int i = 0; i < configs.size(); i++) {
            Config config = configs.get(i); Visual visual = visual(config);
            if (visual.removing || visual.progress() < .5F) continue;
            if (Tooltip.hit(visual.helpX, visual.helpY, mouseX, mouseY)) return true;
            if (confirmDelete == config) {
                if (visual.cancel.click(mouseX, mouseY, button)) { confirmDelete = null; return true; }
                if (visual.confirm.click(mouseX, mouseY, button)) { visual.removing = true; visual.reveal.setTarget(0F); confirmDelete = null; return true; }
            } else {
                if (visual.load.click(mouseX, mouseY, button)) {
                    try { Raven.configManager.setConfig(config); validation = "Loaded " + config.getName() + "."; success = true; }
                    catch (RuntimeException failure) { error(failure); }
                    return true;
                }
                if (visual.save.click(mouseX, mouseY, button)) {
                    try { Raven.configManager.save(config); validation = "Saved current settings to " + config.getName() + "."; success = true; }
                    catch (RuntimeException failure) { error(failure); }
                    return true;
                }
                if (visual.delete.click(mouseX, mouseY, button)) { confirmDelete = config; return true; }
            }
        }
        return false;
    }
    public void wheel(float amount) {
        if (amount == 0F || scroll.dragging()) return;
        restoreScroll = false;
        // One notch moves most of a row instead of the previous ~9 logical pixels.
        scroll.page(-amount * 64F);
    }
    public void drag(float mouseY) { scroll.dragBar(mouseY, rowsTop, viewport); }
    public void release() { scroll.release(); }
    public boolean dragging() { return scroll.dragging(); }
    public void saveScroll() { if (!restoreScroll) GuiPreferences.setScroll(7, scroll.target()); }
    private static float rowSpace(Visual visual) { return 86F * (visual.removing ? visual.progress() : 1F); }
    public void cancelFocus() {
        field.cancel(); confirmDelete = null; scroll.release();
        if (Raven.configManager != null) for (Iterator<Map.Entry<Config, Visual>> it = visuals.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Config, Visual> item = it.next();
            if (item.getValue().removing) { try { Raven.configManager.deleteConfig(item.getKey()); it.remove(); } catch (RuntimeException failure) { item.getValue().removing = false; item.getValue().reveal.setTarget(1F); error(failure); } }
        }
    }
    public boolean key(char character, int keyCode) {
        if (field.key(character, keyCode)) { if (field.consumeSubmit()) createConfig(); return true; }
        switch (keyCode) {
            case Keyboard.KEY_HOME: scroll.home(); break;
            case Keyboard.KEY_END: scroll.end(); break;
            case Keyboard.KEY_PRIOR: scroll.page(-Math.max(64F, viewport - 24F)); break;
            case Keyboard.KEY_NEXT: scroll.page(Math.max(64F, viewport - 24F)); break;
            case Keyboard.KEY_UP: scroll.page(-64F); break;
            case Keyboard.KEY_DOWN: scroll.page(64F); break;
            default: return false;
        }
        restoreScroll = false; return true;
    }
    private Visual visual(Config config) { Visual visual = visuals.get(config); if (visual == null || visual.modified != config.file.lastModified()) { visual = new Visual(config); visuals.put(config, visual); } return visual; }
    private void createConfig() {
        validation = ""; success = false;
        if (Raven.configManager == null || field.empty()) { validation = "Enter a config name."; field.invalid(); return; }
        try {
            Config created = Raven.configManager.createConfig(field.value()); refresh(); field.clear();
            validation = "Created " + created.getName() + " with your current settings."; success = true;
        } catch (RuntimeException failure) { error(failure); field.invalid(); }
    }
}
