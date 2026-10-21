package keystrokesmod.client.clickgui.apple.rebuild;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;

public final class TextRow extends SettingRow {
    private final DescriptionSetting description;
    private final String fixed;
    private String clippedSource = "", clippedNote = "";
    private float clippedWidth = -1F, clippedScale = -1F;
    public TextRow(Module module, Setting setting) { super(module, setting, ""); description = setting instanceof DescriptionSetting ? (DescriptionSetting) setting : null; fixed = null; }
    public TextRow(Module module, String label, String value) { super(module, null, label); description = null; fixed = value; }
    @Override protected boolean onClick(float mouseX, float mouseY, int button) { return true; }
    @Override protected String readValue() { return description == null ? fixed : description.getDesc(); }
    @Override protected float controlWidth() { return 0F; }
    @Override protected float baseHeight() { return 56F; }
    @Override protected void drawValue(float right, float textY, float alpha) {
        GuiIcons.draw(GuiIcons.INFO, rowX + 24F, rowY + 27F, 17F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha));
        String note = value.length() == 0 ? label : value;
        float limit = Math.max(30F, rowW - 60F);
        float scale = GuiRenderContext.uiScale();
        if (clippedSource != note || clippedWidth != limit || clippedScale != scale) {
            clippedSource = note; clippedWidth = limit; clippedScale = scale; clippedNote = note;
            if (UiDraw.smallWidth(note) > limit) { int end = note.length(); while (end > 0 && UiDraw.smallWidth(note.substring(0, end) + "...") > limit) --end; clippedNote = end == 0 ? "..." : note.substring(0, end) + "..."; }
        }
        UiDraw.small(clippedNote, rowX + 42F, rowY + (baseHeight() - UiDraw.smallHeight()) * .5F, UiDraw.alpha(GuiTokens.TEXT_SECONDARY, alpha));
    }
}
