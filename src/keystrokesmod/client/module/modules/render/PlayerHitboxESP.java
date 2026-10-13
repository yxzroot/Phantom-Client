package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.PickerRGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

/** Player-only visual outlines; never changes actual entity hitboxes or packets. */
public final class PlayerHitboxESP extends Module {
    private final SliderSetting opacity = new SliderSetting("Opacity (%)", 85, 0, 100, 5);
    private final PickerRGBSetting color = new PickerRGBSetting("Outline Color", 170, 105, 255, opacity);
    private final SliderSetting thickness = new SliderSetting("Line Thickness", 2.0, 1.0, 6.0, 0.5);
    private final SliderSetting range = new SliderSetting("Distance", 64, 8, 128, 4);
    private final TickSetting throughWalls = new TickSetting("Through Walls", true);
    private final TickSetting showInvisible = new TickSetting("Show Invisible", false);

    public PlayerHitboxESP() {
        super("Player Hitbox ESP", ModuleCategory.render);
        withDescription("Draws player-only hitbox outlines; does not change reach or collision.");
        registerSettings(new DescriptionSetting(getDescription()), color, thickness, opacity,
                range, throughWalls, showInvisible);
    }

    @Subscribe
    public void onForgeEvent(ForgeEvent wrapper) {
        if (!(wrapper.getEvent() instanceof RenderWorldLastEvent)
                || mc.theWorld == null || mc.thePlayer == null) return;
        float partial = ((RenderWorldLastEvent) wrapper.getEvent()).partialTicks;
        double maxDistanceSq = range.getInput() * range.getInput();
        RenderManager camera = mc.getRenderManager();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            if (throughWalls.isToggled()) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
            }
            GL11.glLineWidth((float) thickness.getInput());
            GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F,
                    color.getBlue() / 255.0F, (float) (opacity.getInput() / 100.0));

            for (EntityPlayer other : mc.theWorld.playerEntities) {
                if (other == mc.thePlayer || other.isDead || other.deathTime > 0
                        || (!showInvisible.isToggled() && other.isInvisible())
                        || mc.thePlayer.getDistanceSqToEntity(other) > maxDistanceSq) continue;
                double x = other.lastTickPosX + (other.posX - other.lastTickPosX) * partial - camera.viewerPosX;
                double y = other.lastTickPosY + (other.posY - other.lastTickPosY) * partial - camera.viewerPosY;
                double z = other.lastTickPosZ + (other.posZ - other.lastTickPosZ) * partial - camera.viewerPosZ;
                AxisAlignedBB hitbox = other.getEntityBoundingBox();
                RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(
                        hitbox.minX - other.posX + x, hitbox.minY - other.posY + y,
                        hitbox.minZ - other.posZ + z, hitbox.maxX - other.posX + x,
                        hitbox.maxY - other.posY + y, hitbox.maxZ - other.posZ + z));
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
