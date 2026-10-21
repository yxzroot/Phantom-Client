package keystrokesmod.client.clickgui.apple.rebuild;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;

/** Descriptions for the installed module registry; never inferred from display names at draw time. */
final class GuiDescriptions {
    private static final Map<String, String> MODULES = new HashMap<String, String>();
    private static boolean installed;
    private static int missing;
    private static final ArrayList<String> emptyItems = new ArrayList<String>();
    private static final Map<String, String> SETTINGS = new HashMap<String, String>();
    static {
        put("ChestStealer", "Transfers items from open chests into your inventory automatically.");
        put("AutoArmour", "Equips stronger armor from your inventory when it is available.");
        put("LeftClicker", "Automatically clicks the enabled mouse buttons while you hold them.");
        put("RightClicker", "Automatically right-clicks while the physical mouse button is held.");
        put("AimAssist", "Smoothly helps you aim at nearby players while you fight.");
        put("DelayRemover", "Removes the 1.8 left-click delay so you can hit faster.");
        put("HitBox", "Increases the targetable size of other players' hitboxes.");
        put("Reach", "Extends player targeting distance; attacks remain manual.");
        put("KnockbackDelay", "Briefly delays incoming velocity packets and transactions after a hit.");
        put("Fly", "Changes your movement to let you fly using the selected flight mode.");
        put("KeepSprint", "Keeps sprinting after hitting a player instead of losing momentum.");
        put("Sprint", "Automatically sprints while moving forward.");
        put("Timer", "Changes the speed of the client's game timer.");
        put("AutoPlace", "Places held blocks on the block face you aim at, with an adjustable frame delay.");
        put("BedAura", "Searches for nearby beds and sends digging packets to break them.");
        put("FastPlace", "Reduces the right-click delay between block placements.");
        put("Freecam", "Moves your camera independently of your player's position.");
        put("NoFall", "Attempts to prevent fall damage using the selected mode.");
        put("Clutch", "Automatically places a block beneath you during a dangerous fall.");
        put("SafeWalk", "Prevents you from walking off block edges while bridging.");
        put("AntiBot", "Filters suspicious player entities from targeting.");
        put("Chams", "Renders colored player models through walls.");
        put("ChestESP", "Highlights nearby chests through blocks.");
        put("Nametags", "Draws enhanced nametags above players.");
        put("PlayerESP", "Highlights players using the selected ESP rendering mode.");
        put("Tracers", "Draws lines toward players in the world.");
        put("HUD", "Displays the client's HUD elements and module list on screen.");
        put("NameHider", "Replaces your displayed player name with a chosen name.");
        put("WaterBucket", "Uses a water bucket to attempt to break a fall.");
        put("GuiModule", "Opens and configures the client's graphical interface.");
        put("SelfDestruct", "Disables the client and removes its visible effects.");
        put("BridgeAssist", "Helps line up your view for different bridging modes.");
        put("Fullbright", "Brightens the world so dark places are easier to see.");
        put("AutoTool", "Selects a suitable tool when you begin breaking a block.");
        put("WTap", "Taps sprint after hits to reset sprint for knockback.");
        put("BlockHit", "Combines sword blocking with attacks at a configured timing.");
        put("AutoBlock", "Automatically blocks with a sword while fighting.");
        put("MiddleClick", "Adds or removes a player from friends with a middle click.");
        put("Projectiles", "Shows the predicted flight path of an arrow from your bow.");
        put("Parkour", "Automatically jumps at the edge of a block while moving.");
        put("JumpReset", "Automatically releases and presses jump to reset knockback.");
        put("KillAura", "Automatically selects and attacks nearby targets.");
        put("Targets", "Chooses which types of entities combat modules may target.");
        put("BridgeHelper", "Holds sneak near edges while you place blocks yourself.");
        put("LegitScaffold", "Sneaks at edges, aims visibly and places blocks using normal item use.");
        put("Freelook", "Lets you look around without changing the direction you face.");
        put("AutoClickCps", "Displays your clicks per second in a draggable HUD pill.");
        put("PlayerHitboxESP", "Draws outlines around player hitboxes without changing their size.");
        put("PhantomHUD", "Shows the Classic Phantom module list with live values.");
        put("PhantomPanels", "Shows the Phantom module list as individually styled panels.");
        setting("Tooltip delay", "Seconds to hover a help icon before its tooltip appears.");
        setting("Menu animation", "Animates the ClickGUI when it opens and closes.");
        setting("Animation speed", "Changes the speed of the ClickGUI's visual transitions.");
        setting("Theme", "Selects the ClickGUI's color theme.");
        setting("GUI scale", "Changes the size of the ClickGUI.");
        setting("GUI sounds", "Plays interface sounds when you use ClickGUI controls.");
        setting("Favorites first", "Lists your favorite modules before other modules.");
        setting("Show values", "Displays each module's current setting next to its name.");
        setting("Background opacity (%)", "Controls how opaque the panel row backgrounds are.");
        setting("Corner radius", "Controls how rounded the panel row corners are.");
        setting("Show logo", "Shows the Phantom wordmark above the module rows.");
        setting("Bar", "Shows a thin accent bar on each module row.");
        setting("Bar color", "Chooses the color of the edge bar on each panel.");
        setting("Value color", "Chooses the color used for module value text.");
        setting("Custom value color", "Sets the value text color when Custom is selected.");
        setting("Sort", "Chooses the order in which module rows appear.");
        setting("Position", "Anchors the module list to a corner of the screen.");
        setting("X Offset", "Moves the module list inward from its horizontal edge.");
        setting("Y Offset", "Moves the module list inward from its vertical edge.");
        setting("Scale", "Changes the size of this HUD element.");
        setting("Animations", "Animates module rows when they enter, leave or change values.");
        setting("Gradient Start", "Sets the first color of the Phantom gradient.");
        setting("Gradient End", "Sets the last color of the Phantom gradient.");
        setting("Accent Theme", "Chooses between Phantom colors and the client theme.");
    }
    private static void put(String className, String text) { MODULES.put(className, text); }
    private static void setting(String name, String text) { SETTINGS.put(name, text); }
    static void install() {
        if (installed || Raven.moduleManager == null) return;
        installed = true; missing = 0; emptyItems.clear();
        for (Module module : Raven.moduleManager.getModules()) {
            String description = MODULES.get(module.getClass().getSimpleName());
            if (description != null) module.withDescription(description);
            if (module.getDescription() == null || module.getDescription().isEmpty()
                    || "No description available.".equals(module.getDescription())) {
                missing++;
                emptyItems.add("module: " + module.getName());
            }
            for (Setting setting : module.getSettings()) if (setting != null) {
                String help = SETTINGS.get(setting.getName());
                if (help != null && setting.getDescription().isEmpty()) setting.withDescription(help);
                if (setting.getDescription().isEmpty()) { missing++; emptyItems.add("setting: " + module.getName() + " / " + setting.getName()); }
            }
        }
        if (Boolean.getBoolean("phantom.gui.debug")) dumpMissing();
    }
    static int missing() { return missing; }
    static String settingHelp(String name) { return SETTINGS.get(name); }
    static void dumpMissing() {
        for (String item : emptyItems) System.out.println("Phantom GUI missing description " + item);
        System.out.println("Phantom GUI empty descriptions: " + missing);
    }
    private GuiDescriptions() { }
}
