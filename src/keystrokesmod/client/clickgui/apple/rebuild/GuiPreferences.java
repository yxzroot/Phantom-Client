package keystrokesmod.client.clickgui.apple.rebuild;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Set;
import keystrokesmod.client.config.Config;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;

/** Persists only the approved gui.* additions in the active existing config. */
public final class GuiPreferences {
    private static final String FAVORITES = "gui.favorites", WINDOW_X = "gui.window.x", WINDOW_Y = "gui.window.y";
    private static final String TAB = "gui.lastTab", CATEGORY = "gui.lastCategory", SOUNDS = "gui.sounds", FAVORITES_FIRST = "gui.favoritesFirst", SCALE = "gui.scale";
    private static final String THEME_EXPLICIT = "gui.themeExplicit";
    private static final String FAVORITES_ONLY = "gui.favoritesOnly", SCROLL_PREFIX = "gui.scroll.";
    private static final String TOOLTIP_DELAY = "gui.tooltipDelay";
    private static final String[] SCROLL_KEYS = {"modules.combat", "modules.movement", "modules.players", "modules.render",
            "modules.exploits", "modules.client", "modules.favorites", "config", "friends", "hud"};
    private static Set<String> favorites;
    private static boolean loaded, sounds = true, favoritesFirst = true, inspected, themeExplicit, favoritesOnly;
    private static Config activeConfig;
    private static final float[] scrolls = new float[SCROLL_KEYS.length];
    private static float windowX = Float.NaN, windowY = Float.NaN, guiScale = 1F;
    private static float tooltipDelay = 3F;
    private static SliderSetting tooltipSlider;
    private static int lastTab, lastCategory;
    private static boolean fallbackAnimation=true;
    private static float fallbackSpeed=1F;
    private static final Enum<?>[] NO_THEMES=new Enum<?>[0];private static Enum<?>[] themeOptions=NO_THEMES;
    private static TickSetting menuAnimation;
    private static SliderSetting animationSpeed;
    private static ComboSetting<?> theme;
    private static long speedBits = Long.MIN_VALUE;
    private static String speedDisplay = "1.0x";
    private static int scaleBits = Integer.MIN_VALUE;
    private static String scaleDisplay = "1.0x";

    private GuiPreferences() { }

    public static void attach(Set<String> set) { favorites = set; load(); }

    public static void load() {
        Config config = Raven.configManager == null ? null : Raven.configManager.getConfig();
        if (loaded && config == activeConfig) return;
        activeConfig = config; loaded = true;
        themeExplicit = false;
        if (favorites == null) return;
        JsonObject root = config == null ? null : config.getData();
        if (root == null) { loaded = false; return; }
        favorites.clear();
        if (root.has(FAVORITES) && root.get(FAVORITES).isJsonArray()) {
            for (JsonElement item : root.getAsJsonArray(FAVORITES)) if (item.isJsonPrimitive()) favorites.add(item.getAsString());
        }
        windowX = number(root, WINDOW_X, Float.NaN); windowY = number(root, WINDOW_Y, Float.NaN);
        lastTab = integer(root, TAB, 0); lastCategory = integer(root, CATEGORY, 0);
        favoritesOnly = bool(root, FAVORITES_ONLY, false);
        for (int i = 0; i < scrolls.length; i++) scrolls[i] = Math.max(0F, number(root, SCROLL_PREFIX + SCROLL_KEYS[i], 0F));
        sounds = bool(root, SOUNDS, true); favoritesFirst = bool(root, FAVORITES_FIRST, true);
        themeExplicit = bool(root, THEME_EXPLICIT, false);
        guiScale = clamp(number(root, SCALE, 1F), .75F, 1.5F);
        tooltipDelay = clamp(number(root, TOOLTIP_DELAY, 3F), .2F, 5F);
        fallbackAnimation=bool(root,"gui.menuAnimation",true);fallbackSpeed=clamp(number(root,"gui.animationSpeed",1F),.3F,3F);
        if (tooltipSlider != null) tooltipSlider.setValue(tooltipDelay);
    }

    public static void save() {
        if (!loaded || favorites == null) return;
        Config config = Raven.configManager == null ? null : Raven.configManager.getConfig();
        JsonObject root = config == null ? null : config.getData();
        if (root == null) return;
        JsonArray names = new JsonArray();
        for (String name : favorites) names.add(new JsonPrimitive(name));
        root.add(FAVORITES, names);
        root.addProperty(WINDOW_X, windowX); root.addProperty(WINDOW_Y, windowY);
        root.addProperty(TAB, lastTab); root.addProperty(CATEGORY, lastCategory);
        root.addProperty(FAVORITES_ONLY, favoritesOnly);
        for (int i = 0; i < scrolls.length; i++) root.addProperty(SCROLL_PREFIX + SCROLL_KEYS[i], scrolls[i]);
        root.addProperty(SOUNDS, sounds); root.addProperty(FAVORITES_FIRST, favoritesFirst); root.addProperty(SCALE, guiScale);
        root.addProperty(THEME_EXPLICIT, themeExplicit);
        root.addProperty(TOOLTIP_DELAY, tooltipDelay);
        root.addProperty("gui.menuAnimation",menuAnimationEnabled());root.addProperty("gui.animationSpeed",animationSpeed());
        config.save(root);
    }

    public static boolean menuAnimationEnabled() { inspect(); return menuAnimation == null ? fallbackAnimation : menuAnimation.isToggled(); }
    public static float animationSpeed() { inspect(); return animationSpeed == null ? fallbackSpeed : (float) animationSpeed.getInput(); }
    public static String animationSpeedDisplay() {
        inspect();
        double speed = animationSpeed(); long bits = Double.doubleToLongBits(speed);
        if (bits != speedBits) { speedBits = bits; speedDisplay = Double.toString(speed) + "x"; }
        return speedDisplay;
    }
    public static void toggleMenuAnimation() { inspect(); if (menuAnimation != null) menuAnimation.toggle();else fallbackAnimation=!fallbackAnimation; }
    public static void setAnimationSpeed(float speed){inspect();speed=clamp(speed,.3F,3F);if(animationSpeed!=null)animationSpeed.setValue(speed);else fallbackSpeed=speed;}
    public static void setGuiScale(float scale){guiScale=clamp(Math.round(scale*20F)/20F,.75F,1.5F);}
    public static Enum<?>[] themeOptions(){inspect();return themeOptions;}
    public static String themeName(){inspect();return theme==null?"Phantom":theme.getMode().toString();}
    @SuppressWarnings({"rawtypes","unchecked"}) public static void selectTheme(int index){inspect();if(theme!=null&&index>=0&&index<themeOptions.length){themeExplicit=true;((ComboSetting)theme).setMode(themeOptions[index]);}}
    public static boolean hasMenuAnimationSetting() { inspect(); return menuAnimation != null; }
    public static boolean sounds() { return sounds; }
    public static void toggleSounds() { sounds = !sounds; }
    public static boolean favoritesFirst() { return favoritesFirst; }
    public static void toggleFavoritesFirst() { favoritesFirst = !favoritesFirst; }
    public static float guiScale() { return guiScale; }
    public static void nextGuiScale() { guiScale = guiScale >= 1.5F ? .75F : Math.round((guiScale + .1F) * 10F) / 10F; }
    public static String guiScaleDisplay() {
        int bits = Float.floatToIntBits(guiScale);
        if (bits != scaleBits) { scaleBits = bits; scaleDisplay = Float.toString(guiScale) + "x"; }
        return scaleDisplay;
    }
    public static float windowX() { return windowX; }
    public static float windowY() { return windowY; }
    public static void setWindow(float x, float y) { windowX = x; windowY = y; save(); }
    public static int lastTab() { return lastTab; }
    public static float tooltipDelay() {
        inspect();
        if (tooltipSlider != null) {
            float updated = (float) tooltipSlider.getInput();
            if (updated != tooltipDelay) { tooltipDelay = updated; save(); }
        }
        return tooltipDelay;
    }
    public static void adjustTooltipDelay(float seconds) { inspect(); if (tooltipSlider != null) tooltipSlider.setValue(seconds); else tooltipDelay = clamp(seconds, .2F, 5F); save(); }
    public static void previewTooltipDelay(float seconds){inspect();if(tooltipSlider!=null){tooltipSlider.setValue(seconds);tooltipDelay=(float)tooltipSlider.getInput();}else tooltipDelay=clamp(seconds,.2F,5F);}
    public static int lastCategory() { return lastCategory; }
    public static void setLastTab(int value) { if (lastTab != value) { lastTab = value; save(); } }
    public static void setLastCategory(int value) { if (lastCategory != value) { lastCategory = value; save(); } }
    public static boolean favoritesOnly() { return favoritesOnly; }
    public static void setFavoritesOnly(boolean value) { if (favoritesOnly != value) { favoritesOnly = value; save(); } }
    public static float scroll(int index) { return index >= 0 && index < scrolls.length ? scrolls[index] : 0F; }
    public static void setScroll(int index, float value) {
        if (index < 0 || index >= scrolls.length || !Float.isFinite(value)) return;
        value = Math.max(0F, value);
        if (Math.abs(scrolls[index] - value) > .01F) { scrolls[index] = value; save(); }
    }

    public static void nextTheme() {
        inspect();
        if (theme != null) { themeExplicit = true; theme.nextMode(); }
    }
    /** Legacy presets can be loaded by old configs without a ClickGUI theme choice. */
    public static boolean usesDefaultTheme() { return !themeExplicit; }

    private static void inspect() {
        if (inspected || Raven.moduleManager == null) return;
        inspected = true;
        java.util.List<Module> modules = Raven.moduleManager.getModules();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            if (!"Gui".equalsIgnoreCase(module.getName())) continue;
            if (module.getSettingByName("Tooltip delay") == null) {
                tooltipSlider = new SliderSetting("Tooltip delay", 3D, .2D, 5D, .1D);
                tooltipSlider.setValue(tooltipDelay);
                tooltipSlider.withDescription("How long to hover a help icon before its explanation appears.");
                module.registerSetting(tooltipSlider);
            } else tooltipSlider = (SliderSetting) module.getSettingByName("Tooltip delay");
            java.util.ArrayList<Setting> settings = module.getSettings();
            for (int j = 0; j < settings.size(); j++) {
                Setting setting = settings.get(j);
                if ("Menu animation".equalsIgnoreCase(setting.getName()) && setting instanceof TickSetting) menuAnimation = (TickSetting) setting;
                else if ("Animation speed".equalsIgnoreCase(setting.getName()) && setting instanceof SliderSetting) animationSpeed = (SliderSetting) setting;
                else if (("Theme".equalsIgnoreCase(setting.getName()) || "Preset".equalsIgnoreCase(setting.getName())) && setting instanceof ComboSetting)
                    {theme = (ComboSetting<?>) setting;themeOptions=theme.getMode().getDeclaringClass().getEnumConstants();}
            }
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        try { return root.has(key) ? root.get(key).getAsBoolean() : fallback; } catch (Exception ignored) { return fallback; }
    }
    private static int integer(JsonObject root, String key, int fallback) {
        try { return root.has(key) ? root.get(key).getAsInt() : fallback; } catch (Exception ignored) { return fallback; }
    }
    private static float number(JsonObject root, String key, float fallback) {
        try { return root.has(key) ? root.get(key).getAsFloat() : fallback; } catch (Exception ignored) { return fallback; }
    }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
