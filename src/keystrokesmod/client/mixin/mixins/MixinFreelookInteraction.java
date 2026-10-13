package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.Freelook;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Do not target blocks/entities with the character's hidden facing direction. */
@Mixin(Minecraft.class)
public abstract class MixinFreelookInteraction {
    @Inject(method = "func_147116_af()V", remap = false, cancellable = true, at = @At("HEAD"))
    private void freelook$attack(CallbackInfo ci) {
        if (Freelook.blockInteraction()) ci.cancel();
    }

    @Inject(method = "func_147121_ag()V", remap = false, cancellable = true, at = @At("HEAD"))
    private void freelook$use(CallbackInfo ci) {
        if (Freelook.blockInteraction()) ci.cancel();
    }

    @Inject(method = "func_147115_a(Z)V", remap = false, cancellable = true, at = @At("HEAD"))
    private void freelook$mine(boolean held, CallbackInfo ci) {
        if (Freelook.blockInteraction()) ci.cancel();
    }
}
