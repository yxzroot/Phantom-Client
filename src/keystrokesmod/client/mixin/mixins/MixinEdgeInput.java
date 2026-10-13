package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.movement.BridgeHelper;
import keystrokesmod.client.module.modules.movement.LegitScaffold;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Protect the same movement update that observes a released physical Shift key. */
@Mixin(MovementInputFromOptions.class)
public abstract class MixinEdgeInput {
    @Inject(method = "func_78898_a()V", remap = false, at = @At("TAIL"), require = 1)
    private void bridge$protectShift(CallbackInfo ci) {
        BridgeHelper.protectInput((MovementInputFromOptions) (Object) this);
        LegitScaffold.protectInput((MovementInputFromOptions) (Object) this);
    }
}
