package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.SwordGlow;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinSwordGlowFrame {
    @Inject(method = "func_181560_a(FJ)V", remap = false, at = @At("HEAD"), require = 1)
    private void swordglow$clock(float partial, long deadline, CallbackInfo ci) { SwordGlow.beginFrame(partial); }

}
