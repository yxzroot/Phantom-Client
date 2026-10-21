package keystrokesmod.client.main;

/** Test-only bootstrap fixture. Never packaged; avoids launching the whole client/network updater. */
public final class Raven {
    public static keystrokesmod.client.config.ConfigManager configManager;
    public static keystrokesmod.client.module.ModuleManager moduleManager;
    public static keystrokesmod.client.main.ClientConfig clientConfig;
    public static keystrokesmod.client.clickgui.raven.ClickGui clickGui;
    public static keystrokesmod.client.utils.version.VersionManager versionManager;
}
