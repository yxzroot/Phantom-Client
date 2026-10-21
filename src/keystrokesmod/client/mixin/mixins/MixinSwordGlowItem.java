package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.SwordGlow;
import keystrokesmod.client.module.modules.render.swordglow.NativeSwordGlint;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinSwordGlowItem implements NativeSwordGlint.ModelDraw {
    @Shadow(remap = false)
    private void func_175035_a(IBakedModel model, int tint) { throw new AssertionError("Mixin shadow"); }

    @Override public void swordglow$drawNativeModel(IBakedModel model, int tint) {
        func_175035_a(model, tint);
    }

    @Inject(method = "func_175042_a(Lnet/minecraft/item/ItemStack;II)V", remap = false, at = @At("HEAD"), require = 1)
    private void swordglow$inventory(ItemStack stack, int x, int y, CallbackInfo ci) { SwordGlow.beginGuiItem(); }

    @Inject(method = "func_175042_a(Lnet/minecraft/item/ItemStack;II)V", remap = false, at = @At("RETURN"), require = 1)
    private void swordglow$inventoryEnd(ItemStack stack, int x, int y, CallbackInfo ci) { SwordGlow.endItem(); }

    @Inject(method = "func_175049_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
            remap = false, at = @At("HEAD"), require = 1)
    private void swordglow$owner(ItemStack stack, EntityLivingBase owner, TransformType transform, CallbackInfo ci) {
        SwordGlow.beginItem(owner, transform);
    }

    @Inject(method = "func_175049_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
            remap = false, at = @At("RETURN"), require = 1)
    private void swordglow$end(ItemStack stack, EntityLivingBase owner, TransformType transform, CallbackInfo ci) { SwordGlow.endItem(); }

    @Inject(method = "func_180454_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;func_175036_a(Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/item/ItemStack;)V", shift = At.Shift.AFTER, remap = false), require = 1)
    private void swordglow$controlledShimmer(ItemStack stack, IBakedModel model, CallbackInfo ci) {
        SwordGlow.drawHeldGlint(stack, model, this);
    }

    @Redirect(method = "func_180454_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;func_77962_s()Z", remap = false), require = 1)
    private boolean swordglow$replacePurple(ItemStack item) {
        return !SwordGlow.controlsHeldGlint(item) && item.hasEffect();
    }
}
