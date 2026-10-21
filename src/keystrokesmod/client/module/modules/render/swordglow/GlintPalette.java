package keystrokesmod.client.module.modules.render.swordglow;

/** Settings-owned colour data for the native Minecraft glint renderer. */
public final class GlintPalette {
    private GlintPalette() {}

    public enum ColorMode { Theme, Static, Rainbow, Gradient, Palette }

    public static final class Palette {
        public ColorMode mode = ColorMode.Theme;
        public int start;
        public int end;
    }

    /** Tip-trail/spark tint follows the same moving rainbow as the on-blade shader. */
    public static int motionTint(SwordAnimator.Frame frame,Palette palette){
        if(palette.mode==ColorMode.Rainbow||frame.style.effect()==7){
            float hue=.7F-frame.phase*.16F;hue-=(float)Math.floor(hue);
            return java.awt.Color.HSBtoRGB(hue,1,1)&0xFFFFFF;
        }
        return palette.mode==ColorMode.Static?palette.start:palette.end;
    }

    /** Native RenderItem receives a separate selected tint for each moving band. */
    public static int tint(SwordAnimator.Frame frame, Palette palette, int pass) {
        int color;
        if (palette.mode == ColorMode.Rainbow || frame.style == AnimationStyle.Rainbow || frame.style == AnimationStyle.RainbowWave)
            color = java.awt.Color.HSBtoRGB((frame.hue + pass * .12F) % 1F, .78F, 1F);
        else if (palette.mode == ColorMode.Static) color = palette.start;
        else {
            float amount = .5F + .5F * (float) Math.sin(frame.hue * Math.PI * 2 + pass * Math.PI);
            int r = Math.round((palette.start >> 16 & 255) * (1 - amount) + (palette.end >> 16 & 255) * amount);
            int g = Math.round((palette.start >> 8 & 255) * (1 - amount) + (palette.end >> 8 & 255) * amount);
            int b = Math.round((palette.start & 255) * (1 - amount) + (palette.end & 255) * amount);
            color = r << 16 | g << 8 | b;
        }
        float brightness = frame.brightness;
        return 0xFF000000 | (Math.min(255, Math.round((color >> 16 & 255) * brightness)) << 16)
                | (Math.min(255, Math.round((color >> 8 & 255) * brightness)) << 8)
                | Math.min(255, Math.round((color & 255) * brightness));
    }
}
