package keystrokesmod.client.clickgui.apple.rebuild;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import keystrokesmod.client.module.modules.combat.AimAssist;
import net.minecraft.entity.Entity;

/** Friend presentation over the existing AimAssist friend list; no parallel list is stored. */
public final class FriendsTab {
    private static final class Visual {
        final GlassRow row = new GlassRow(); final GuiButton remove = new GuiButton(GuiButton.GHOST_ICON, "", GuiIcons.CLOSE);
        final String letter;
        final String name;
        final Anim reveal = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER);
        long showAt;
        boolean removing;
        String clippedName;
        float clippedWidth = -1F, clippedScale = -1F, letterWidth;
        float helpX, helpY;
        Visual(Entity entity) { name = entity.getName(); clippedName = name; letter = name.length() == 0 ? "?" : name.substring(0, 1).toUpperCase(); }
        void show(int index) { if (showAt == 0L && reveal.target() == 0F && !removing) showAt = System.nanoTime() + MotionTokens.staggerNanos(index, MotionTokens.CONTENT_STAGGER_MS, MotionTokens.CONTENT_STAGGER_CAP_MS); }
        void update(float dt) { if (showAt != 0L && System.nanoTime() >= showAt) { showAt = 0L; reveal.setTarget(1F); } reveal.update(dt); row.update(dt); remove.update(dt); }
        float progress() { return Math.max(0F, Math.min(1F, reveal.get())); }
        String displayName(float limit) {
            float scale = GuiRenderContext.uiScale();
            if (clippedWidth != limit || clippedScale != scale) {
                clippedWidth = limit; clippedScale = scale; clippedName = name; letterWidth = UiDraw.boldWidth(letter);
                if (UiDraw.boldWidth(name) > limit) { int end = name.length(); while (end > 0 && UiDraw.boldWidth(name.substring(0, end) + "...") > limit) --end; clippedName = end == 0 ? "..." : name.substring(0, end) + "..."; }
            }
            return clippedName;
        }
    }
    private final GuiTextField field = new GuiTextField("Player name...", GuiIcons.PLUS, 24);
    private final GuiTextField filter = new GuiTextField("Filter friends...", GuiIcons.SEARCH, 24);
    private final GuiButton add = new GuiButton(GuiButton.PRIMARY, "Add", GuiIcons.PLUS);
    private final GlassRow addRow = new GlassRow();
    private final ScrollView scroll = new ScrollView();
    private final IdentityHashMap<Entity, Visual> visuals = new IdentityHashMap<Entity, Visual>();
    private float x, y, width, height, listTop;
    private String validation = "";
    private int cachedCount = -1;
    private String countLabel = "0";
    private boolean restoreScroll = true;

    public void update(float dt) {
        field.update(dt); filter.update(dt); add.update(dt); addRow.update(dt); scroll.update(dt);
        for (Iterator<Map.Entry<Entity, Visual>> it = visuals.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Entity, Visual> item = it.next(); Visual visual = item.getValue(); visual.update(dt);
            if (visual.removing && visual.reveal.isDone() && visual.progress() <= .01F) { AimAssist.removeFriend(item.getKey()); it.remove(); }
        }
    }
    public void onTabOpened() { for (Visual visual : visuals.values()) if (!visual.removing) { visual.reveal.snapTo(0F); visual.showAt = 0L; } restoreScroll = true; }
    public void draw(float left, float top, float w, float h, float mouseX, float mouseY, Tooltip tooltip) {
        x = left + 32F; y = top + 8F; width = Math.max(1F, w - 64F); height = h - 8F;
        GuiWidgets.section("ADD FRIEND", x, y, width);
        float addY = y + 25F; addRow.draw(x, addY, width, 64F, mouseX, mouseY, false, 1F);
        field.draw(x + 14F, addY + 12F, width - 14F - 94F, mouseX, mouseY); add.draw(x + width - 80F, addY + 15F, 66F, mouseX, mouseY, true);
        if (validation.length() > 0) UiDraw.small(validation, x + 14F, addY + 66F, 0xFFE5484D);
        ArrayList<Entity> friends = AimAssist.getFriends();
        if (cachedCount != friends.size()) { cachedCount = friends.size(); countLabel = String.valueOf(cachedCount); }
        listTop = addY + 94F; GuiWidgets.section("FRIENDS", x, listTop, width - 64F); GuiWidgets.badge(countLabel, x + width - 54F, listTop - 4F, false);
        float rowsTop = listTop + 25F;
        if (friends.size() > 6) { filter.draw(x, rowsTop, width, mouseX, mouseY); rowsTop += 50F; }
        float viewport = Math.max(1F, top + h - rowsTop);
        if (friends.isEmpty()) { GuiWidgets.empty(GuiIcons.INFO, "No friends added", "Friends are excluded from Aim Assist targeting.", x, rowsTop, width, viewport, 1F); return; }
        String query = filter.value();
        float content = 0F; for (int i = 0; i < friends.size(); i++) if (matches(friends.get(i).getName(), query)) { Visual visual = visual(friends.get(i)); visual.show(i); content += 72F * visual.progress(); }
        scroll.setMetrics(content, viewport);
        if (restoreScroll && allRevealed(friends, query)) { scroll.restore(GuiPreferences.scroll(8)); restoreScroll = false; }
        UiDraw.pushScissor(x - 8F, rowsTop, width + 8F, viewport);
        float cursor = rowsTop + 4F - scroll.visualPosition();
        for (int i = 0; i < friends.size(); i++) {
            Entity friend = friends.get(i); if (!matches(friend.getName(), query)) continue; Visual visual = visual(friend);
            float reveal = visual.progress(), space = 72F * reveal;
            if (reveal <= .01F) continue;
            float previousAlpha = UiDraw.masterAlpha(); UiDraw.setMasterAlpha(previousAlpha * reveal);
            float visualY = cursor + (1F - reveal) * MotionTokens.CONTENT_RISE_PX;
            visual.row.draw(x, visualY, width, 64F, mouseX, mouseY, false, 1F);
            UiDraw.circleGradient(x + 34F, visualY + 32F, 20F, UiDraw.alpha(GuiTokens.accent(), .42F), UiDraw.alpha(0xFF20263B, .94F), UiDraw.alpha(GuiTokens.accent(), .42F), GuiRenderContext.inverseUiScale(), 0, 0F);
            float limit = Math.max(30F, width - 156F);
            String name = visual.displayName(limit);
            UiDraw.bold(visual.letter, x + 34F - visual.letterWidth * .5F, visualY + 32F - UiDraw.boldHeight() * .5F, GuiTokens.TEXT_PRIMARY);
            UiDraw.bold(name, x + 68F, visualY + 20F, GuiTokens.TEXT_PRIMARY);
            UiDraw.small("Excluded from Aim Assist", x + 68F, visualY + 42F, GuiTokens.TEXT_SECONDARY);
            visual.helpX = x + width - 82F; visual.helpY = visualY + 32F;
            tooltip.helpIcon(visual, friend.getName(), "This player is excluded from Aim Assist targeting. Use the X to remove them.",
                    visual.helpX, visual.helpY, mouseX, mouseY >= rowsTop && mouseY <= top + h ? mouseY : -10000F, reveal);
            visual.remove.draw(x + width - 48F, visualY + 15F, 34F, mouseX, mouseY, !visual.removing);
            UiDraw.setMasterAlpha(previousAlpha);
            cursor += space;
        }
        UiDraw.popScissor(); scroll.renderBar(x + width, rowsTop, viewport, UiDraw.inside(mouseX, mouseY, x + width - 8F, rowsTop, x + width + 2F, rowsTop + viewport));
    }
    public boolean click(float mouseX, float mouseY, int button) {
        if (button != 0) return false;
        field.blurIfOutside(mouseX, mouseY); filter.blurIfOutside(mouseX, mouseY);
        if (field.click(mouseX, mouseY, button) || filter.click(mouseX, mouseY, button)) return true;
        if (add.click(mouseX, mouseY, button)) { addFriend(); return true; }
        ArrayList<Entity> friends = AimAssist.getFriends(); float rowsTop = listTop + 25F + (friends.size() > 6 ? 50F : 0F); String query = filter.value();
        if (!UiDraw.inside(mouseX, mouseY, x - 8F, rowsTop, x + width + 8F, y + height)) return false;
        for (int i = 0; i < friends.size(); i++) { Entity friend = friends.get(i); if (!matches(friend.getName(), query)) continue; Visual visual = visual(friend); if (visual.progress() > .5F && Tooltip.hit(visual.helpX, visual.helpY, mouseX, mouseY)) return true; if (!visual.removing && visual.progress() > .5F && visual.remove.click(mouseX, mouseY, button)) { visual.removing = true; visual.reveal.setTarget(0F); return true; } }
        return false;
    }
    public void wheel(float amount) { scroll.wheel(amount); }
    public void saveScroll() { if (!restoreScroll) GuiPreferences.setScroll(8, scroll.target()); }
    private boolean allRevealed(ArrayList<Entity> friends, String query) {
        for (int i = 0; i < friends.size(); i++) if (matches(friends.get(i).getName(), query) && visual(friends.get(i)).progress() < .999F) return false;
        return true;
    }
    public void cancelFocus() {
        field.cancel(); filter.clear(); filter.cancel();
        for (Iterator<Map.Entry<Entity, Visual>> it = visuals.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Entity, Visual> item = it.next();
            if (item.getValue().removing) { AimAssist.removeFriend(item.getKey()); it.remove(); }
        }
    }
    public boolean key(char character, int keyCode) { if (field.key(character, keyCode)) { if (field.consumeSubmit()) addFriend(); return true; } return filter.key(character, keyCode); }
    private void addFriend() {
        validation = "";
        if (field.empty()) { validation = "Enter a player name."; field.invalid(); return; }
        String name = field.value(); ArrayList<Entity> friends = AimAssist.getFriends();
        for (int i = 0; i < friends.size(); i++) if (friends.get(i).getName().equalsIgnoreCase(name)) { validation = "That player is already a friend."; field.invalid(); return; }
        AimAssist.addFriend(name); field.clear();
    }
    private Visual visual(Entity friend) { Visual visual = visuals.get(friend); if (visual == null) { visual = new Visual(friend); visuals.put(friend, visual); } return visual; }
    private static boolean matches(String name, String query) { if (query == null || query.length() == 0) return true; int max = name.length() - query.length(); for (int i = 0; i <= max; i++) { int j = 0; while (j < query.length() && Character.toLowerCase(name.charAt(i + j)) == Character.toLowerCase(query.charAt(j))) ++j; if (j == query.length()) return true; } return false; }
}
