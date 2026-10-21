package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/** Backing implementation for Module.getHudSuffix(), injected into the existing base class. */
public final class HudSuffixes {
    private static final Map<Module, State> CACHE = new IdentityHashMap<>();
    private HudSuffixes() { }

    private static final class State {
        Setting setting, extra;
        double min = Double.NaN, max = Double.NaN;
        boolean flag;
        String suffix;
        long nextScan;
    }

    public static String resolve(Module module) {
        State state = CACHE.get(module);
        if (state == null) {
            state = new State();
            CACHE.put(module, state);
            String name = module.getName();
            if (name.equals("Left Clicker")) state.setting = module.getSettingByName("Left CPS");
            else if (name.equals("Right Clicker")) state.setting = module.getSettingByName("RightCPS");
            else if (name.equals("Legit Scaffold")) {
                state.setting = module.getSettingByName("Placement Delay (ms)");
                state.extra = module.getSettingByName("Auto Aim");
            } else if (name.equals("KeepSprint")) state.setting = module.getSettingByName("Slow %");
            else if (name.equals("Reach")) state.setting = module.getSettingByName("Reach (Blocks)");
            if (state.setting == null) {
                for (Setting setting : module.getSettings()) {
                    String key = setting.getName().toLowerCase(Locale.ROOT);
                    if (setting instanceof DoubleSliderSetting && (key.contains("cps") || key.contains("delay")
                            || key.contains("reach") || key.contains("speed"))) { state.setting = setting; break; }
                    if (setting instanceof SliderSetting && (key.contains("delay") || key.contains("speed")
                            || key.contains("chance") || key.contains("distance"))) { state.setting = setting; break; }
                }
            }
        }
        long now = System.nanoTime();
        if (now < state.nextScan) return state.suffix;
        state.nextScan = now + 50_000_000L; // Live within 50ms, without allocating on each render frame.
        Setting setting = state.setting;
        boolean flag = state.extra instanceof TickSetting && ((TickSetting) state.extra).isToggled();
        double min = setting instanceof DoubleSliderSetting ? ((DoubleSliderSetting) setting).getInputMin()
                : setting instanceof SliderSetting ? ((SliderSetting) setting).getInput() : Double.NaN;
        double max = setting instanceof DoubleSliderSetting ? ((DoubleSliderSetting) setting).getInputMax() : Double.NaN;
        if (Double.compare(min, state.min) == 0 && Double.compare(max, state.max) == 0
                && flag == state.flag) return state.suffix;
        state.min = min; state.max = max; state.flag = flag;
        if (module.getName().equals("Legit Scaffold")) {
            state.suffix = flag ? "Legit Aim" : "Legit";
        } else if (module.getName().equals("KeepSprint") && !Double.isNaN(min)) {
            state.suffix = format((100 - min) / 100);
        } else if (setting instanceof DoubleSliderSetting) {
            state.suffix = format(min) + "-" + format(max)
                    + (setting.getName().toLowerCase(Locale.ROOT).contains("cps") ? "cps" : "");
        } else if (setting instanceof SliderSetting) {
            String key = setting.getName().toLowerCase(Locale.ROOT);
            state.suffix = format(min) + (key.contains("delay") || key.contains("time") ? "ms"
                    : key.contains("chance") ? "%" : "");
        } else state.suffix = null;
        return state.suffix;
    }

    private static String format(double number) {
        return number == Math.rint(number) ? Long.toString(Math.round(number))
                : String.format(Locale.ROOT, "%.2f", number).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
