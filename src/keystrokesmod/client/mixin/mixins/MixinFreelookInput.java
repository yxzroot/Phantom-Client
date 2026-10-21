package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.Freelook;
import keystrokesmod.client.module.modules.movement.LegitScaffold;
import keystrokesmod.client.module.modules.combat.AimAssist;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Let Lunar keep its renderer mouse redirect; consume only the local player's actual turn. */
@Mixin(Entity.class)
public abstract class MixinFreelookInput {
    @Inject(method = "func_70082_c(FF)V", remap = false, cancellable = true, at = @At("HEAD"))
    private void freelook$turn(float yaw, float pitch, CallbackInfo ci) {
        LegitScaffold.observeMouseTurn((Entity) (Object) this, yaw, pitch);
        AimAssist.observeMouseTurn((Entity) (Object) this, yaw, pitch);
        if (Freelook.consumeMouse((Entity) (Object) this, yaw, pitch)) ci.cancel();
    }
}
