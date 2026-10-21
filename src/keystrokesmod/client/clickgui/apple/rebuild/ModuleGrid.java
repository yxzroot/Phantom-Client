package keystrokesmod.client.clickgui.apple.rebuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;

/** Cached responsive grid; cards stay alive and glide between slots. */
public final class ModuleGrid {
    private static final float VIEW_PADDING = 8F, EDGE_FADE = 20F;
    public static final ModuleCategory[] CATEGORIES = {ModuleCategory.combat, ModuleCategory.movement, ModuleCategory.player,
            ModuleCategory.render, ModuleCategory.other, ModuleCategory.client};
    public static final String[] CATEGORY_LABELS = {"Combat", "Movement", "Players", "Render", "Exploits", "Client"};
    private static final Comparator<ModuleCard> SORT = new Comparator<ModuleCard>() {
        @Override public int compare(ModuleCard a, ModuleCard b) {
            boolean af = ModuleCard.GuiPreferencesFavorite.isFavorite(a.module().getName()), bf = ModuleCard.GuiPreferencesFavorite.isFavorite(b.module().getName());
            if (GuiPreferences.favoritesFirst() && af != bf) return af ? -1 : 1;
            return a.module().getName().compareToIgnoreCase(b.module().getName());
        }
    };
    private final IdentityHashMap<Module, ModuleCard> map = new IdentityHashMap<Module, ModuleCard>();
    private final ArrayList<ModuleCard> all = new ArrayList<ModuleCard>();
    private final ArrayList<ModuleCard> visible = new ArrayList<ModuleCard>();
    private final ArrayList<ModuleCard> previous = new ArrayList<ModuleCard>();
    private final ArrayList<ModuleCard> leaving = new ArrayList<ModuleCard>();
    private final ScrollView scroll = new ScrollView();
    private int category = -1, columns = 2;
    private String query = "\u0000";
    private float viewX, viewY, viewW, viewH, cardW, contentH;
    private float layoutX, layoutY, layoutW, layoutH, layoutShift, layoutScroll = Float.NaN;
    private boolean layoutReady, layoutDirty = true;
    private boolean categoryEntry;
    private boolean restoreScroll;
    private ModuleCard focused;

    public ModuleGrid() { refreshCards(); }
    public void invalidate() { category = Integer.MIN_VALUE; layoutDirty = true; }
    public void invalidateLayout() { layoutDirty = true; }
    public void refreshCards() {
        List<Module> modules = Raven.moduleManager.getModules();
        for (int i = 0; i < modules.size(); i++) if (!map.containsKey(modules.get(i))) { ModuleCard card = new ModuleCard(modules.get(i)); map.put(modules.get(i), card); all.add(card); }
    }
    public void rebuild(int selectedCategory, String search, long now) {
        if (category == selectedCategory && query.equals(search)) return;
        categoryEntry = category != selectedCategory && category >= 0;
        boolean searching = search.length() > 0;
        category = selectedCategory; query = search; layoutDirty = true;
        previous.clear(); previous.addAll(visible);
        visible.clear();
        for (int i = 0; i < all.size(); i++) {
            ModuleCard card = all.get(i); Module module = card.module();
            boolean match = searching ? match(search, module) : category == -1
                    ? ModuleCard.GuiPreferencesFavorite.isFavorite(module.getName())
                    : category >= 0 && category < CATEGORIES.length && module.moduleCategory() == CATEGORIES[category];
            if (match) { visible.add(card); leaving.remove(card); }
            else card.place(card.x(), card.y(), card.width(), 0, false, now);
        }
        for (int i = 0; i < previous.size(); i++) {
            ModuleCard old = previous.get(i);
            if (visible.contains(old)) continue;
            old.place(old.x(), old.y(), old.width(), 0, false, now);
            if (!leaving.contains(old)) leaving.add(old);
        }
        previous.clear();
        Collections.sort(visible, SORT);
        restoreScroll = !searching;
        if (searching) scroll.home();
    }
    /** Recomputes geometry only after a real layout change. Scroll updates only reposition existing slots. */
    public void layout(float x, float y, float width, float height, long now, float shift) {
        boolean geometryChanged = layoutDirty || !layoutReady || width != layoutW || height != layoutH || shift != layoutShift;
        if (geometryChanged) {
            layoutW = width; layoutH = height; layoutShift = shift;
            columns = Math.max(1, (int) Math.floor((width + GuiTokens.GRID_GAP) / ((width < 850F ? GuiTokens.CARD_MIN_WIDTH : GuiTokens.CARD_TARGET_WIDTH) + GuiTokens.GRID_GAP)));
            while (columns > 2 && (width - (columns - 1) * GuiTokens.GRID_GAP) / columns < GuiTokens.CARD_MIN_WIDTH) --columns;
            cardW = (width - (columns - 1) * GuiTokens.GRID_GAP) / columns;
            int rows = (visible.size() + columns - 1) / columns;
            contentH = rows == 0 ? VIEW_PADDING * 2F : VIEW_PADDING * 2F + rows * GuiTokens.CARD_HEIGHT + Math.max(0, rows - 1) * GuiTokens.GRID_GAP;
            scroll.setMetrics(contentH, height);
            if (restoreScroll) { scroll.restore(GuiPreferences.scroll(category == -1 ? 6 : category)); restoreScroll = false; }
            layoutReady = true; layoutDirty = false;
        }
        viewX = x - shift; viewY = y; viewW = width; viewH = height;
        float scrollAt = scroll.visualPosition();
        boolean positionsChanged = geometryChanged || x != layoutX || y != layoutY || scrollAt != layoutScroll;
        if (!positionsChanged) return;
        float sy = y + VIEW_PADDING - scrollAt;
        for (int i = 0; i < visible.size(); i++) {
            int col = i % columns, row = i / columns;
            visible.get(i).place(viewX + col * (cardW + GuiTokens.GRID_GAP), sy + row * (GuiTokens.CARD_HEIGHT + GuiTokens.GRID_GAP), cardW, i, true, now, false, categoryEntry);
        }
        categoryEntry = false;
        layoutX = x; layoutY = y; layoutScroll = scrollAt;
    }
    /** Rigid window movement: translated cards do not animate, invalidate, or restart their stagger. */
    public void translate(float dx, float dy) {
        if (dx == 0F && dy == 0F) return;
        viewX += dx; viewY += dy; layoutX += dx; layoutY += dy;
        for (int i = 0; i < visible.size(); i++) visible.get(i).translate(dx, dy);
        for (int i = 0; i < leaving.size(); i++) leaving.get(i).translate(dx, dy);
    }
    public void update(float dt, long now, float mouseX, float mouseY, Module selected, Tooltip tooltip, boolean allowHover) {
        if (allowHover) scroll.update(dt);
        for (int i = 0; i < visible.size(); i++) visible.get(i).update(dt, now, mouseX, mouseY, visible.get(i).module() == selected, tooltip, allowHover);
        boolean tooltipHovered = false;
        for (int i = 0; i < visible.size(); i++) {
            ModuleCard card = visible.get(i);
            if (card.tooltipHovered()) tooltipHovered = true;
        }
        if (!tooltipHovered) tooltip.clear();
        for (int i = leaving.size() - 1; i >= 0; i--) {
            ModuleCard card = leaving.get(i); card.update(dt, now, -10000F, -10000F, false, tooltip, false);
            if (card.alpha() <= .01F && !card.listed()) leaving.remove(i);
        }
    }
    public void draw(float mouseX, float mouseY, float dim, boolean draggingWindow) {
        UiDraw.pushScissor(viewX, viewY, viewW, viewH);
        drawSurfaces(leaving, dim, !draggingWindow);
        drawSurfaces(visible, dim, !draggingWindow);
        // The three passes deliberately keep identical Poppins atlases contiguous for batching.
        drawQuestions(leaving, dim); drawQuestions(visible, dim);
        drawNames(leaving, dim); drawNames(visible, dim);
        drawBindings(leaving, dim); drawBindings(visible, dim);
        UiDraw.popScissor();
        UiDraw.verticalGradient(viewX, viewY, viewX + viewW, viewY + EDGE_FADE, UiDraw.alpha(GuiTokens.WINDOW_BG, .92F), UiDraw.alpha(GuiTokens.WINDOW_BG, 0F));
        UiDraw.verticalGradient(viewX, viewY + viewH - EDGE_FADE, viewX + viewW, viewY + viewH, UiDraw.alpha(GuiTokens.WINDOW_BG, 0F), UiDraw.alpha(GuiTokens.WINDOW_BG, .92F));
        scroll.renderBar(viewX + viewW - 1F, viewY, viewH, UiDraw.inside(mouseX, mouseY, viewX + viewW - 8F, viewY, viewX + viewW, viewY + viewH));
        if (visible.isEmpty()) UiDraw.text("No modules found", viewX + (viewW - UiDraw.textWidth("No modules found")) * .5F, viewY + viewH * .5F - 3F, GuiTokens.TEXT_SECONDARY);
    }
    public ModuleCard click(float mouseX, float mouseY, int button) {
        if (!UiDraw.inside(mouseX, mouseY, viewX, viewY, viewX + viewW, viewY + viewH)) return null;
        if (button == 0 && scroll.beginBar(mouseX, mouseY, viewX + viewW - 2F, viewY, 4F, viewH)) return null;
        for (int i = visible.size() - 1; i >= 0; --i) {
            ModuleCard card = visible.get(i); int action = card.click(mouseX, mouseY, button);
            if (action != ModuleCard.NONE) { focused = card; return card; }
        }
        return null;
    }
    public void drag(float mouseY) { scroll.dragBar(mouseY, viewY, viewH); }
    public void release() { scroll.release(); for (int i = 0; i < visible.size(); i++) visible.get(i).release(); }
    public void resetInteraction() {
        scroll.release();
        for (int i = 0; i < visible.size(); i++) visible.get(i).resetInteraction();
        for (int i = 0; i < leaving.size(); i++) leaving.get(i).resetInteraction();
    }
    public void replayEntrance(long now) {
        for (int i = 0; i < visible.size(); i++) visible.get(i).replayEntrance(i, now);
    }
    public void wheel(float amount) { scroll.wheel(amount); }
    public float scrollTarget() { return scroll.target(); }
    public void saveScroll() { if (!restoreScroll && query.isEmpty() && category >= -1 && category < CATEGORIES.length) GuiPreferences.setScroll(category == -1 ? 6 : category, scroll.target()); }
    /** Desired unscrolled grid height at the current drawer width, before viewport clipping. */
    public float desiredHeight(float width) {
        int count = Math.max(1, (int) Math.floor((width + GuiTokens.GRID_GAP)
                / ((width < 850F ? GuiTokens.CARD_MIN_WIDTH : GuiTokens.CARD_TARGET_WIDTH) + GuiTokens.GRID_GAP)));
        while (count > 2 && (width - (count - 1) * GuiTokens.GRID_GAP) / count < GuiTokens.CARD_MIN_WIDTH) --count;
        int rows = (visible.size() + count - 1) / count;
        return rows == 0 ? 16F : 16F + rows * GuiTokens.CARD_HEIGHT + (rows - 1) * GuiTokens.GRID_GAP;
    }
    public boolean key(char character, int key) {
        for (int i = 0; i < visible.size(); i++) if (visible.get(i).key(character, key)) return true;
        return false;
    }
    public ModuleCard focused() { return focused; }
    public void moveFocus(int direction) { if (visible.isEmpty()) return; int at = focused == null ? 0 : visible.indexOf(focused); focused = visible.get(Math.max(0, Math.min(visible.size() - 1, at + direction))); }
    public void toggleFocused() { if (focused != null) focused.module().toggle(); }
    private void drawSurfaces(ArrayList<ModuleCard> cards, float dim, boolean shadows) {
        for (int i = 0; i < cards.size(); i++) if (visible(cards.get(i))) cards.get(i).drawSurface(dim, shadows);
    }
    private void drawQuestions(ArrayList<ModuleCard> cards, float dim) {
        for (int i = 0; i < cards.size(); i++) if (visible(cards.get(i))) cards.get(i).drawQuestionText(dim);
    }
    private void drawNames(ArrayList<ModuleCard> cards, float dim) {
        for (int i = 0; i < cards.size(); i++) if (visible(cards.get(i))) cards.get(i).drawNameText(dim);
    }
    private void drawBindings(ArrayList<ModuleCard> cards, float dim) {
        for (int i = 0; i < cards.size(); i++) if (visible(cards.get(i))) cards.get(i).drawBindingText(dim);
    }
    private boolean visible(ModuleCard card) {
        return card.y() + card.height() >= viewY && card.y() <= viewY + viewH && card.x() + card.width() >= viewX && card.x() <= viewX + viewW;
    }
    private static boolean match(String query, Module module) {
        return containsIgnoreCase(module.getName(), query) || containsIgnoreCase(module.getDescription(), query)
                || containsIgnoreCase(module.moduleCategory().getName(), query);
    }
    private static boolean containsIgnoreCase(String source, String find) {
        if (source == null) return false; int end = source.length() - find.length();
        for (int i = 0; i <= end; i++) { int j = 0; while (j < find.length() && Character.toLowerCase(source.charAt(i + j)) == Character.toLowerCase(find.charAt(j))) ++j; if (j == find.length()) return true; }
        return find.length() == 0;
    }
}
