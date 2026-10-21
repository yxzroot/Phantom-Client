package keystrokesmod.client.mixin.mixins;

import keystrokesmod.client.module.modules.render.AspectRatio;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinAspectRatio {
    @Inject(method="func_181560_a(FJ)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$frame(float partial,long deadline,CallbackInfo ci){AspectRatio.end();AspectRatio.frame();}
    @Inject(method="func_175068_a(IFJ)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$begin(int pass,float partial,long deadline,CallbackInfo ci){AspectRatio.begin();}
    @Inject(method="func_175068_a(IFJ)V",remap=false,at=@At("RETURN"),require=1)
    private void aspect$end(CallbackInfo ci){AspectRatio.end();}
    @Inject(method="func_181560_a(FJ)V",remap=false,at=@At("RETURN"),require=1)
    private void aspect$frameEnd(CallbackInfo ci){AspectRatio.end();}
    @Inject(method="func_78478_c()V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$overlay(CallbackInfo ci){AspectRatio.end();}
    // Lunar replaces the hand's Project invocation. The core hook must NOT depend on it.
    @Inject(method="func_78479_a(FI)V",remap=false,at=@At("RETURN"),require=1)
    private void aspect$cameraReady(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_78476_b(FI)V",remap=false,at=@At("HEAD"),require=1)
    private void aspect$handStart(CallbackInfo ci){AspectRatio.prepareHand();}
    @Inject(method="func_175068_a(IFJ)V",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",shift=At.Shift.AFTER,remap=false),require=0)
    private void aspect$skyReady(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_180437_a(Lnet/minecraft/client/renderer/RenderGlobal;FI)V",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",shift=At.Shift.AFTER,remap=false),require=0)
    private void aspect$cloudReady(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_78476_b(FI)V",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",shift=At.Shift.AFTER,remap=false),require=0)
    private void aspect$handReady(CallbackInfo ci){AspectRatio.applyHand();}
    @Inject(method="func_175068_a(IFJ)V",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/util/glu/GLU;gluPerspective(FFFF)V",shift=At.Shift.AFTER,remap=false),require=0)
    private void aspect$skyGLU(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_180437_a(Lnet/minecraft/client/renderer/RenderGlobal;FI)V",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/util/glu/GLU;gluPerspective(FFFF)V",shift=At.Shift.AFTER,remap=false),require=0)
    private void aspect$cloudGLU(CallbackInfo ci){AspectRatio.applyWorld();}
    @Inject(method="func_78476_b(FI)V",remap=false,at=@At(value="INVOKE",target="Lorg/lwjgl/util/glu/GLU;gluPerspective(FFFF)V",shift=At.Shift.AFTER,remap=false),require=0)
    private void aspect$handGLU(CallbackInfo ci){AspectRatio.applyHand();}
    // Also covers Lunar's private perspective helper: normalize/retarget immediately
    // before the native first-person item pass, regardless of who built its matrix.
    @Inject(method="func_78476_b(FI)V",remap=false,at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/ItemRenderer;func_78440_a(F)V",remap=false),require=0)
    private void aspect$beforeItem(CallbackInfo ci){AspectRatio.prepareHand();AspectRatio.applyHand();}
}
