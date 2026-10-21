package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.SkyChanger;
import keystrokesmod.client.module.modules.render.aurora.AuroraLightmap;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinSkyChangerLightmap {
    @Shadow(remap=false) @Final private int[] field_78504_Q;
    @Shadow(remap=false) @Final private DynamicTexture field_78513_d;
    @Unique private final AuroraLightmap sky$lightmap=new AuroraLightmap();
    @Unique private boolean sky$worldPass,sky$failed;
    @Unique private void sky$restore(boolean upload) {
        if(sky$lightmap.restore(field_78504_Q) && upload) sky$lightmap.upload(field_78513_d);
    }
    @Unique private void sky$apply() {
        SkyChanger module=SkyChanger.current();
        if(module==null || sky$failed) return;
        try {
            if(sky$lightmap.apply(field_78504_Q,module.aurora.cold(),module.aurora.reflection(),module.aurora.reflectionColor(),module.aurora.dusk(),module.options.duskColor,module.options.duskDarkness,module.aurora.artTint(),module.aurora.artColor()))
                sky$lightmap.upload(field_78513_d);
        } catch(RuntimeException|LinkageError failure) {
            sky$failed=true;
            sky$restore(true);
            java.util.logging.Logger.getLogger("Phantom/SkyChanger").log(java.util.logging.Level.WARNING,"Cold lightmap disabled after failure",failure);
        }
    }
    @Inject(method="func_175068_a(IFJ)V",remap=false,at=@At("HEAD"),require=1)
    private void sky$world(int pass,float partial,long finish,CallbackInfo ci) { sky$worldPass=true;sky$apply(); }
    @Inject(method="func_175068_a(IFJ)V",remap=false,at=@At("RETURN"),require=1)
    private void sky$worldEnd(int pass,float partial,long finish,CallbackInfo ci) { sky$worldPass=false;sky$restore(true); }
    @Inject(method="func_78472_g(F)V",remap=false,at=@At("HEAD"),require=1)
    private void sky$beforeLightmap(float partial,CallbackInfo ci) { sky$restore(false); }
    @Inject(method="func_78472_g(F)V",remap=false,at=@At("RETURN"),require=1)
    private void sky$afterLightmap(float partial,CallbackInfo ci) { if(sky$worldPass) sky$apply(); }
    @Inject(method="func_78476_b(FI)V",remap=false,at=@At("HEAD"),require=1)
    private void sky$hand(float partial,int pass,CallbackInfo ci) { sky$worldPass=false;sky$restore(true); }
}
