package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.AspectRatio;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Final draw boundaries: Lunar/OptiFine can rebuild projection AFTER camera setup. */
@Mixin(RenderGlobal.class)
public abstract class MixinAspectRatioWorld {
    @Inject(method="func_174970_a(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$terrain(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_174977_a(Lnet/minecraft/util/EnumWorldBlockLayer;DILnet/minecraft/entity/Entity;)I",remap=false,at=@At("HEAD"),require=1)
    private void aspect$blocks(CallbackInfoReturnable<Integer> ci){AspectRatio.applyWorld();}
    @Inject(method="func_180446_a(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$entities(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_174976_a(FI)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$sky(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_72731_b(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IF)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$outline(CallbackInfo ci){AspectRatio.applyWorld();}
}
