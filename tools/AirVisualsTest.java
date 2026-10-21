import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;
import keystrokesmod.client.module.modules.render.airvisuals.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

public final class AirVisualsTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Set<String> names = new HashSet<>();
        for (AirPalette p : AirPalette.values()) check(names.add(p.label), "Duplicate theme " + p.label);
        check(names.size() == 285, "Expected Client plus 284 upstream themes");
        for (int fps : new int[]{60, 240}) {
            AirTheme theme = new AirTheme();
            theme.update(0, AirPalette.Client, 0xFF112233, 0xFF556677, .2F);
            theme.update(1_000_000_000L, AirPalette.MOONPURPLE, 0xFF112233, 0xFF556677, .2F);
            for (int i = 1; i <= fps / 10; i++) theme.update(1_000_000_000L + Math.round(i * 1_000_000_000D / fps), AirPalette.MOONPURPLE, 0, 0, .2F);
            int middle = AirTheme.mix(0xFF112233, AirPalette.MOONPURPLE.start, .5F);
            check(theme.top() == middle, "Cross-fade midpoint differs at " + fps + " FPS");
            int before = theme.top();
            theme.update(1_100_000_000L, AirPalette.WATER, 0, 0, .2F);
            check(theme.top() == before, "Switching themes popped mid-transition");
            theme.update(1_301_000_000L, AirPalette.WATER, 0, 0, .2F);
            check(theme.top() == AirPalette.WATER.start, "Theme did not settle");
            float value = 0;
            for (int i = 0; i < fps; i++) value = AirTheme.approach(value, 1, 1F / fps, 8);
            check(Math.abs(value - (1 - Math.exp(-8))) < .00001, "Row animation depends on frame rate");
        }
        System.out.println("PASS: 284 upstream palettes, 60/240Hz transitions, interrupted cross-fade continuity and delta-time row motion");
        if (args.length == 0) return;
        Pbuffer context = new Pbuffer(96, 96, new PixelFormat().withDepthBits(24), null);
        try {
            context.makeCurrent();
            GL11.glViewport(0, 0, 96, 96);
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glLoadIdentity(); GL11.glOrtho(0,96,96,0,-1,1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glLoadIdentity();
            GL11.glEnable(GL11.GL_LIGHTING); GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_ALPHA_TEST); GL11.glEnable(GL11.GL_CULL_FACE); GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(.2F,.3F,.4F,.5F); GL11.glBlendFunc(GL11.GL_ONE,GL11.GL_ZERO);
            GL13.glActiveTexture(GL13.GL_TEXTURE0); int atlas = GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas);
            GL13.glActiveTexture(GL13.GL_TEXTURE1); int lightmap = GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightmap);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            FloatBuffer color = BufferUtils.createFloatBuffer(16);
            for (int i = 0; i < 240; i++) {
                AirDraw.card(8, 8, 75, 28, 5, 0xA0101520, .6F);
                AirDraw.gradient(8, 8, 75, 2, 1, 0xFF4E54C8, 0xFF8F94FB, false);
                AirDraw.card(Float.NaN, 0, 10, 10, 3, -1, 1);
                check(GL11.glGetError() == 0, "GL error");
                check(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) == GL13.GL_TEXTURE1, "Active unit leaked");
                check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == lightmap, "Lightmap binding leaked");
                check(GL11.glIsEnabled(GL11.GL_LIGHTING) && GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                        && GL11.glIsEnabled(GL11.GL_ALPHA_TEST) && GL11.glIsEnabled(GL11.GL_CULL_FACE), "Enable flags leaked");
                check(!GL11.glIsEnabled(GL11.GL_BLEND) && GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), "Blend/depth mask leaked");
                check(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) == GL11.GL_ONE && GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) == GL11.GL_ZERO, "Blend function leaked");
                color.clear(); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
                check(color.get(0) == .2F && color.get(3) == .5F, "Tint leaked");
                GL13.glActiveTexture(GL13.GL_TEXTURE0); check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == atlas, "Atlas leaked");
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
            }
            System.out.println("PASS: 240 GL frames; shapes/gradients/shadows restore texture units/bindings, lighting, depth, alpha, blend and color");
        } finally { context.destroy(); }
    }
}
