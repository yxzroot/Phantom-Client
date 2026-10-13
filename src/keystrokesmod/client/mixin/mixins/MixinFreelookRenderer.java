package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Change only reads inside vanilla's camera renderer, including its collision rays. */
@Mixin(EntityRenderer.class)
public abstract class MixinFreelookRenderer {
    @Shadow(remap = false) private float thirdPersonDistance;
    @Shadow(remap = false) private float thirdPersonDistanceTemp;
    @Inject(method = "func_78467_g(F)V", remap = false, at = @At("HEAD"))
    private void freelook$frame(float partialTicks, CallbackInfo ci) {
        Freelook.frame(Minecraft.getMinecraft().getRenderViewEntity());
    }

    @Redirect(method = "func_78467_g(F)V", remap = false,
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;field_70177_z:F", remap = false))
    private float freelook$yaw(Entity entity) {
        return Freelook.angle(entity, entity.rotationYaw, 0);
    }

    @Redirect(method = "func_78467_g(F)V", remap = false,
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;field_70126_B:F", remap = false))
    private float freelook$previousYaw(Entity entity) {
        return Freelook.angle(entity, entity.prevRotationYaw, 0);
    }

    @Redirect(method = "func_78467_g(F)V", remap = false,
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;field_70125_A:F", remap = false))
    private float freelook$pitch(Entity entity) {
        return Freelook.angle(entity, entity.rotationPitch, 1);
    }

    @Redirect(method = "func_78467_g(F)V", remap = false,
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;field_70127_C:F", remap = false))
    private float freelook$previousPitch(Entity entity) {
        return Freelook.angle(entity, entity.prevRotationPitch, 1);
    }

    @Redirect(method = "func_78467_g(F)V", remap = false,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;field_78490_B:F", remap = false))
    private float freelook$distance(EntityRenderer renderer) {
        return Freelook.cameraDistance(thirdPersonDistance);
    }

    @Redirect(method = "func_78467_g(F)V", remap = false,
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/EntityRenderer;field_78491_C:F", remap = false))
    private float freelook$previousDistance(EntityRenderer renderer) {
        return Freelook.cameraDistance(thirdPersonDistanceTemp);
    }
}
