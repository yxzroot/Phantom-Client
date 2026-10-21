package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.SkyChanger;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides only the striped client-side wall, never border data, collision or packets. */
@Mixin(RenderGlobal.class)
public abstract class MixinSkyChangerBorder {
    @Inject(method="func_180449_a(Lnet/minecraft/entity/Entity;F)V",remap=false,
            at=@At("HEAD"),cancellable=true,require=1)
    private void sky$border(Entity camera,float partial,CallbackInfo ci) {
        // Cancel before vanilla changes any GL state: no render state to restore.
        if(SkyChanger.hidesWorldBorder()) ci.cancel();
    }
}
