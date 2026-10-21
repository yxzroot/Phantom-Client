package keystrokesmod.client.clickgui.apple.rebuild;

import java.lang.reflect.Field;
import keystrokesmod.client.clickgui.raven.components.SettingComponent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.modules.render.PhantomHUD;
import keystrokesmod.client.module.modules.render.PhantomPanels;

/** Full-width drawer setting row. The prior right-aligned loose-text renderer is removed. */
public abstract class SettingRow {
    private static final Field COMPONENT_FIELD = findComponentField();
    protected final Module module;
    protected final Setting setting;
    protected final String label;
    protected final float labelWidth;
    protected final Anim hover = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_HOVER);
    protected final Anim press = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.CARD_PRESS);
    protected final Anim enter = new Anim(MotionTokens.ROW_ENTER_PX, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER);
    protected final Anim valueSlide = new Anim(0F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.VALUE_CHANGE);
    private final Anim visibility = new Anim(1F, Anim.Mode.EASE_OUT_CUBIC, MotionTokens.ROW_ENTER);
    protected String value = "", oldValue = "";
    protected float valueWidth, rowX, rowY, rowW;
    private String clippedLabel;
    private float clippedLabelWidth = -1F, clippedLabelScale = -1F;
    protected int index;
    private float helpX, helpY, helpProgress;
    private final GlassRow glass = new GlassRow();
    private long revealAt;

    protected SettingRow(Module module, Setting setting, String fallback) {
        this.module = module; this.setting = setting; label = setting == null ? fallback : setting.getName(); labelWidth = UiDraw.textWidth(label);
        if (module instanceof PhantomHUD || module instanceof PhantomPanels) visibility.snapTo(visibleBySetting() ? 1F : 0F);
    }
    public final void setIndex(int value) { index = value; enter.snapTo(MotionTokens.ROW_ENTER_PX); revealAt = System.nanoTime() + MotionTokens.staggerNanos(value, MotionTokens.ROW_STAGGER_MS, MotionTokens.ROW_STAGGER_CAP_MS); }
    public void update(float dt) {
        if (revealAt != 0L && System.nanoTime() >= revealAt) { revealAt = 0L; enter.setTarget(0F); }
        visibility.setTarget(visibleBySetting() ? 1F : 0F);
        refreshValue(); hover.update(dt); press.update(dt); enter.update(dt); valueSlide.update(dt); visibility.update(dt); glass.update(dt);
        if (valueSlide.target() <= .001F && valueSlide.isDone()) oldValue = "";
    }
    public float height() { return (baseHeight() + expandedHeight()) * visibility.get(); }
    public boolean visible() { return visibility.get() > .01F; }
    public final void draw(float left, float right, float y, float mouseX, float mouseY, float alpha) {
        if (!visible()) return;
        alpha *= visibility.get();
        rowX = left; rowW = Math.max(1F, right - left); rowY = y + enter.get();
        boolean over = UiDraw.inside(mouseX, mouseY, rowX, rowY, right, rowY + baseHeight()); hover.setTarget(over ? 1F : 0F);
        glass.draw(rowX, rowY, rowW, baseHeight(), mouseX, mouseY, false, alpha);
        if (hover.get() > .01F) UiDraw.rounded(rowX, rowY + 8F, rowX + 2F, rowY + baseHeight() - 8F, 1F, UiDraw.alpha(GuiTokens.accent(), alpha * hover.get()));
        float labelY = rowY + (baseHeight() - UiDraw.mediumHeight()) * .5F;
        float controlWidth = controlWidth();
        boolean hasHelp = setting != null && !setting.getDescription().isEmpty();
        float maxWidth = Math.max(32F, rowW - controlWidth - (hasHelp ? 66F : 40F));
        if (!(this instanceof TextRow)) UiDraw.medium(ellipsizedLabel(maxWidth), rowX + 14F, labelY, UiDraw.alpha(GuiTokens.TEXT_PRIMARY, alpha));
        if (hasHelp && !(this instanceof TextRow)) {
            helpX = rowX + 14F + Math.min(labelWidth, maxWidth) + 16F; helpY = rowY + baseHeight() * .5F;
            UiDraw.outline(helpX - 8F, helpY - 8F, helpX + 8F, helpY + 8F, 8F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha));
            UiDraw.small("?", helpX - 4F, helpY - UiDraw.smallHeight() * .5F - 1F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha));
        }
        drawValue(right, labelY, alpha);
        drawExpanded(right, rowY + baseHeight(), alpha);
    }
    protected float baseHeight() { return 52F; }
    protected float controlWidth() { return valueWidth + 14F; }
    private String ellipsizedLabel(float available) {
        float scale = GuiRenderContext.uiScale();
        if (clippedLabelWidth == available && clippedLabelScale == scale) return clippedLabel;
        clippedLabelWidth = available; clippedLabelScale = scale;
        if (UiDraw.mediumWidth(label) <= available) return clippedLabel = label;
        int end = label.length();
        while (end > 0 && UiDraw.mediumWidth(label.substring(0, end) + "...") > available) --end;
        return clippedLabel = end == 0 ? "..." : label.substring(0, end) + "...";
    }
    protected void drawValue(float right, float textY, float alpha) {
        float vx = right - 14F - valueWidth;
        if (valueSlide.get() > .01F && oldValue.length() > 0) UiDraw.animatedText(oldValue, vx, textY - (1F - valueSlide.get()) * MotionTokens.VALUE_SLIDE_PX, UiDraw.alpha(valueColor(), alpha * valueSlide.get()));
        UiDraw.animatedText(value, vx, textY + valueSlide.get() * MotionTokens.VALUE_SLIDE_PX, UiDraw.alpha(valueColor(), alpha * (1F - valueSlide.get())));
    }
    protected int valueColor() { return GuiTokens.VALUE_TEXT; }
    public boolean click(float mouseX, float mouseY, int button) {
        if (visibility.target() < .5F || !UiDraw.inside(mouseX, mouseY, rowX, rowY, rowX + rowW, rowY + height())) return false;
        press.setTarget(1F); glass.press(); return onClick(mouseX, mouseY, button);
    }
    public void release() { press.setTarget(0F); glass.release(); }
    public void drag(float mouseX, boolean slow) { }
    public boolean dragging() { return false; }
    public void wheel(float amount) { }
    public boolean key(char character, int keyCode) { return false; }
    public boolean contains(float mouseX, float mouseY) { return UiDraw.inside(mouseX, mouseY, rowX, rowY, rowX + rowW, rowY + height()); }
    public boolean helpHit(float x, float y) {
        if (setting == null || setting.getDescription().isEmpty() || this instanceof TextRow) return false;
        float dx = x - helpX, dy = y - helpY;
        return dx * dx + dy * dy <= 12F * 12F;
    }
    public void help(Tooltip tooltip, float mouseX, float mouseY) {
        helpProgress = helpHit(mouseX, mouseY) ? tooltip.hover(setting, label, setting.getDescription(), "", helpX, helpY, mouseX, mouseY) : 0F;
        if (helpProgress > 0F) {
            UiDraw.small("?", helpX - 4F, helpY - UiDraw.smallHeight() * .5F - 1F, 0xFFFFFFFF);
            Tooltip.ring(helpX, helpY, 10F, helpProgress, 1F);
        }
    }
    public boolean matches(String query) {
        if (query == null || query.length() == 0) return true;
        int limit = label.length() - query.length();
        for (int i = 0; i <= limit; i++) { int j = 0; while (j < query.length() && Character.toLowerCase(label.charAt(i + j)) == Character.toLowerCase(query.charAt(j))) ++j; if (j == query.length()) return true; }
        return false;
    }
    protected float expandedHeight() { return 0F; }
    public boolean expanding() { return false; }
    protected void drawExpanded(float right, float y, float alpha) { }
    protected abstract boolean onClick(float mouseX, float mouseY, int button);
    protected abstract String readValue();
    protected void refreshValue() {
        String next = readValue(); if (next == null) next = "";
        if (!next.equals(value)) { oldValue = valueSlide.get() > .01F ? "" : value; value = next; valueWidth = UiDraw.textWidth(value); valueSlide.snapTo(1F); valueSlide.setTarget(0F); }
    }
    private boolean visibleBySetting() {
        if (module instanceof PhantomHUD && !((PhantomHUD) module).classicSettingVisible(setting)) return false;
        if (module instanceof PhantomPanels && !((PhantomPanels) module).settingVisible(setting)) return false;
        if (setting == null || COMPONENT_FIELD == null) return true;
        try { SettingComponent component = (SettingComponent) COMPONENT_FIELD.get(setting); return component == null || component.visable; }
        catch (IllegalAccessException ignored) { return true; }
    }
    private static Field findComponentField() {
        try { Field field = Setting.class.getDeclaredField("component"); field.setAccessible(true); return field; }
        catch (Throwable ignored) { return null; }
    }
}
