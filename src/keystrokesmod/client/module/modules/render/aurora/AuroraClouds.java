package keystrokesmod.client.module.modules.render.aurora;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;

/** Reversible provider override: never changes the user's global cloud/video settings. */
public final class AuroraClouds {
    private final IRenderHandler hidden = new IRenderHandler() {
        @Override public void render(float partial, WorldClient world, Minecraft mc) {
            // Deliberately empty: clouds are hidden without touching any GL state.
        }
    };
    private WorldProvider provider;
    private IRenderHandler previous;

    public void update(WorldProvider target, boolean hide) {
        if (provider != target || !hide) restore();
        if (!hide || target == null || provider != null) return;
        provider = target;
        previous = target.getCloudRenderer();
        target.setCloudRenderer(hidden);
    }
    public void restore() {
        if (provider != null && provider.getCloudRenderer() == hidden)
            provider.setCloudRenderer(previous);
        provider = null;
        previous = null;
    }
}
