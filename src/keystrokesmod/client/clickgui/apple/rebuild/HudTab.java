package keystrokesmod.client.clickgui.apple.rebuild;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.module.modules.render.PhantomPanels;

/** HUD elements reuse the module-card surface, interaction model and settings drawer trigger. */
public final class HudTab {
    private final ArrayList<Module> entries = new ArrayList<Module>();
    private final IdentityHashMap<Module, ModuleCard> cards = new IdentityHashMap<Module, ModuleCard>();
    private final ScrollView scroll = new ScrollView();
    private final GuiButton edit = new GuiButton(GuiButton.PRIMARY, "Edit layout", GuiIcons.MOVE);
    private float x, y, width, height, cardWidth, viewY, viewH;
    private int columns;
    private boolean restoreScroll = true;
    public void refresh() {
        entries.clear(); List<Module> modules = Raven.moduleManager.getModules();
        for (int i = 0; i < modules.size(); i++) { Module module = modules.get(i); String name = module.getName(); if (module instanceof PhantomPanels || contains(name, "hud") || contains(name, "watermark") || contains(name, "cps") || contains(name, "array")) entries.add(module); }
    }
    public void update(float dt, float mouseX, float mouseY, Module selected, Tooltip tooltip, boolean interactive) {
        scroll.update(dt); long now = System.nanoTime();
        for (int i = 0; i < entries.size(); i++) card(entries.get(i)).update(dt, now, mouseX, mouseY, entries.get(i) == selected, tooltip, interactive);
        edit.update(dt);
    }
    public void draw(float left, float top, float w, float h, float mouseX, float mouseY, boolean draggingWindow) {
        x = left; y = top; width = w; height = h;
        GuiWidgets.section("HUD ELEMENTS", x, y + 4F, width - 120F); edit.draw(x + width - 104F, y, 104F, mouseX, mouseY, true);
        viewY = y + 36F; viewH = Math.max(1F, h - 36F);
        columns = Math.max(1, (int) Math.floor((width + GuiTokens.GRID_GAP) / ((width < 850F ? GuiTokens.CARD_MIN_WIDTH : GuiTokens.CARD_TARGET_WIDTH) + GuiTokens.GRID_GAP)));
        while (columns > 2 && (width - (columns - 1) * GuiTokens.GRID_GAP) / columns < GuiTokens.CARD_MIN_WIDTH) --columns;
        cardWidth = (width - (columns - 1) * GuiTokens.GRID_GAP) / columns;
        int rows = (entries.size() + columns - 1) / columns; float content = rows == 0 ? 0F : rows * GuiTokens.CARD_HEIGHT + Math.max(0, rows - 1) * GuiTokens.GRID_GAP + 16F;
        scroll.setMetrics(content, viewH);
        if (restoreScroll) { scroll.restore(GuiPreferences.scroll(9)); restoreScroll = false; }
        float sy = viewY + 8F - scroll.visualPosition(); long now = System.nanoTime();
        for (int i = 0; i < entries.size(); i++) { int col = i % columns, row = i / columns; card(entries.get(i)).place(x + col * (cardWidth + GuiTokens.GRID_GAP), sy + row * (GuiTokens.CARD_HEIGHT + GuiTokens.GRID_GAP), cardWidth, i, true, now, draggingWindow); }
        if (entries.isEmpty()) { GuiWidgets.empty(GuiIcons.INFO, "No HUD elements", "Enable a HUD module to customize it here.", x, viewY, width, viewH, 1F); return; }
        UiDraw.pushScissor(x, viewY, width, viewH);
        for (int i = 0; i < entries.size(); i++) { ModuleCard card = card(entries.get(i)); if (card.y() + card.height() >= viewY && card.y() <= viewY + viewH) card.drawSurface(0F, !draggingWindow); }
        for (int i = 0; i < entries.size(); i++) { ModuleCard card = card(entries.get(i)); if (card.y() + card.height() >= viewY && card.y() <= viewY + viewH) card.drawQuestionText(0F); }
        for (int i = 0; i < entries.size(); i++) { ModuleCard card = card(entries.get(i)); if (card.y() + card.height() >= viewY && card.y() <= viewY + viewH) card.drawNameText(0F); }
        UiDraw.popScissor(); scroll.renderBar(x + width - 1F, viewY, viewH, UiDraw.inside(mouseX, mouseY, x + width - 8F, viewY, x + width, viewY + viewH));
    }
    public ModuleCard click(float mouseX, float mouseY, int button) {
        if (edit.click(mouseX, mouseY, button)) { editLayout(); return null; }
        if (!UiDraw.inside(mouseX, mouseY, x, viewY, x + width, viewY + viewH)) return null;
        for (int i = entries.size() - 1; i >= 0; i--) { ModuleCard card = card(entries.get(i)); if (card.click(mouseX, mouseY, button) != ModuleCard.NONE) return card; }
        return null;
    }
    public void wheel(float amount) { scroll.wheel(amount); }
    public void saveScroll() { if (!restoreScroll) GuiPreferences.setScroll(9, scroll.target()); }
    public void release() { edit.release(); for (int i = 0; i < entries.size(); i++) card(entries.get(i)).release(); }
    public void resetInteraction() { edit.release(); scroll.release(); for (ModuleCard card : cards.values()) card.resetInteraction(); }
    private ModuleCard card(Module module) { ModuleCard card = cards.get(module); if (card == null) { card = new ModuleCard(module, true); cards.put(module, card); } return card; }
    private static void editLayout() { Module hud = Raven.moduleManager.getModuleByName("HUD"); if (hud == null) return; Setting setting = hud.getSettingByName("Edit position"); if (setting instanceof TickSetting) { ((TickSetting) setting).toggle(); hud.guiButtonToggled(setting); } }
    private static boolean contains(String name, String fragment) { if (name == null) return false; for (int i = 0; i <= name.length() - fragment.length(); i++) { int j = 0; while (j < fragment.length() && Character.toLowerCase(name.charAt(i + j)) == fragment.charAt(j)) ++j; if (j == fragment.length()) return true; } return false; }
}
