package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.SkyChanger;
import keystrokesmod.client.module.modules.render.aurora.AuroraSimulation;
import net.minecraft.world.World;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinSkyChangerWorld {
    @Inject(method="func_72826_c(F)F",remap=false,at=@At("HEAD"),cancellable=true,require=1)
    private void sky$time(float partial,CallbackInfoReturnable<Float> ci) {
        SkyChanger sky=SkyChanger.current();
        if(sky!=null && sky.aurora.visualTime((World)(Object)this))
            ci.setReturnValue(AuroraSimulation.lockedAngle(sky.options.time,partial));
    }
    @Inject(method="func_72824_f(F)Lnet/minecraft/util/Vec3;",remap=false,at=@At("RETURN"),cancellable=true,require=1)
    private void sky$cloud(float partial,CallbackInfoReturnable<Vec3> ci) {
        SkyChanger sky=SkyChanger.current();
        if(sky!=null) ci.setReturnValue(sky.aurora.clouds((World)(Object)this,ci.getReturnValue()));
    }
}
