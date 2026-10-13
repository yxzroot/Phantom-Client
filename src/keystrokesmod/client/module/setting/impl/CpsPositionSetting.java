package keystrokesmod.client.module.setting.impl;

import com.google.gson.JsonObject;

/** Screen-relative position persisted with the CPS module settings. */
public final class CpsPositionSetting extends DescriptionSetting {
    private double x = 0.96, y = 0.89;
    private boolean placed;

    public CpsPositionSetting() {
        super("Drag CPS Counter to position");
    }

    public int x(int screenWidth, int cardWidth) {
        return (int) Math.round(Math.max(0, screenWidth - cardWidth) * x);
    }

    public int y(int screenHeight, int cardHeight) {
        return (int) Math.round(Math.max(0, screenHeight - cardHeight) * y);
    }

    public boolean hasPlaced() { return placed; }

    public void move(int left, int top, int screenWidth, int screenHeight, int cardWidth, int cardHeight) {
        x = Math.max(0, Math.min(1, left / (double) Math.max(1, screenWidth - cardWidth)));
        y = Math.max(0, Math.min(1, top / (double) Math.max(1, screenHeight - cardHeight)));
        placed = true;
    }

    @Override public void resetToDefaults() {
        x = 0.96;
        y = 0.89;
        placed = false;
    }

    @Override public JsonObject getConfigAsJson() {
        JsonObject data = super.getConfigAsJson();
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("placed", placed);
        return data;
    }

    @Override public void applyConfigFromJson(JsonObject data) {
        if (!data.has("x") || !data.has("y")) return;
        try {
            double nextX = data.get("x").getAsDouble();
            double nextY = data.get("y").getAsDouble();
            if (Double.isFinite(nextX) && Double.isFinite(nextY)) {
                x = Math.max(0, Math.min(1, nextX));
                y = Math.max(0, Math.min(1, nextY));
                placed = data.has("placed") ? data.get("placed").getAsBoolean()
                        : Math.abs(x - 0.96) > 0.001 || Math.abs(y - 0.89) > 0.001;
            }
        } catch (RuntimeException ignored) {
            // Keep the last valid position when a profile contains corrupt coordinates.
        }
    }
}
