package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.SwordGlow;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinSwordGlowHand {
    @Inject(method = "func_178099_a(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
            remap = false, at = @At("HEAD"), require = 1)
    private void swordglow$held(EntityLivingBase owner, ItemStack item, TransformType type, CallbackInfo ci) {
        SwordGlow.beginHeldItem(owner, type);
    }

    @Inject(method = "func_178099_a(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
            remap = false, at = @At("RETURN"), require = 1)
    private void swordglow$heldEnd(EntityLivingBase owner, ItemStack item, TransformType type, CallbackInfo ci) {
        SwordGlow.endItem();
    }
}
