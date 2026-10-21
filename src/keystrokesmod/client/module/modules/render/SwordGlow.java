package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import java.util.IdentityHashMap;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.render.swordglow.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/** Cosmetic held-sword energy. Keeps Lunar's proven explicit held/GUI ownership hooks. */
public final class SwordGlow extends Module {
    public enum Activation {
        Always,OnSwingOnly,OnIdleOnly;
        public String toString(){return this==Always?"Always":this==OnSwingOnly?"On-swing-only":"On-idle-only";}
    }
    private static final SwordRenderContext CONTEXT=new SwordRenderContext();
    private static SwordGlow instance;
    private final ComboSetting<AnimationStyle> animation=new ComboSetting<>("Animation",AnimationStyle.Theme);
    private final ComboSetting<SwordOptions.Secondary> secondary=new ComboSetting<>("Secondary animation",SwordOptions.Secondary.None);
    private final ComboSetting<SwordOptions.Preset> preset=new ComboSetting<>("Preset",SwordOptions.Preset.Custom);
    private final ComboSetting<GlintPalette.ColorMode> colorMode=new ComboSetting<>("Color mode",GlintPalette.ColorMode.Theme);
    private final ComboSetting<SwordOptions.Preset> palettePreset=new ComboSetting<>("Palette preset",SwordOptions.Preset.Plasma);
    private final ComboSetting<SwordOptions.Signature> signature=new ComboSetting<>("Theme signature",SwordOptions.Signature.Auto);
    private final PickerRGBSetting staticColor=new PickerRGBSetting("Static color",160,100,255,null);
    private final PickerRGBSetting gradientStart=new PickerRGBSetting("Gradient start",140,80,255,null);
    private final PickerRGBSetting gradientEnd=new PickerRGBSetting("Gradient end",80,210,255,null);
    private final SliderSetting speed=new SliderSetting("Speed",1,.1,3,.1);
    private final SliderSetting intensity=new SliderSetting("Intensity (%)",75,0,100,1);
    private final SliderSetting bandWidth=new SliderSetting("Band width",.3,.08,.7,.01);
    private final SliderSetting brightness=new SliderSetting("Brightness",1.2,.25,3,.05);
    private final SliderSetting bloom=new SliderSetting("Bloom strength (%)",45,0,100,1);
    private final SliderSetting scale=new SliderSetting("Scale",8,1,32,.5);
    private final SliderSetting angle=new SliderSetting("Angle",-50,-180,180,1);
    private final ComboSetting<Activation> activation=new ComboSetting<>("Activation",Activation.Always);
    private final TickSetting trail=new TickSetting("Swing trail",true),hit=new TickSetting("Hit flash",true),block=new TickSetting("Block glow",true);
    private final SliderSetting swingEnergy=new SliderSetting("Swing energy (%)",65,0,100,1);
    private final SliderSetting trailStrength=new SliderSetting("Trail strength (%)",75,0,100,1);
    private final SliderSetting trailLife=new SliderSetting("Trail lifetime (ms)",200,80,220,10);
    private final TickSetting equip=new TickSetting("Equip animation",true),particles=new TickSetting("Particles",true),lowQuality=new TickSetting("Low quality",false);
    private final TickSetting thirdPerson=new TickSetting("Third person",true),others=new TickSetting("Animations on other players",false);
    private final SliderSetting fade=new SliderSetting("Fade time (ms)",250,200,300,10);
    private final EntityPlayer[] players=new EntityPlayer[32];
    private final ItemStack[] equipped=new ItemStack[32];
    private final int[] equippedSlot=new int[32];
    private final SwordAnimator[] states=new SwordAnimator[32];
    private final SwordMotion[] motions=new SwordMotion[32];
    private final boolean[] seen=new boolean[32],pendingHit=new boolean[32];
    private final SwordOptions options=new SwordOptions();
    private final SwordParticles sparks=new SwordParticles();
    private final GlintPalette.Palette palette=new GlintPalette.Palette();
    private final SwordEnergyTexture energy=new SwordEnergyTexture();
    private final SwordEnergyRenderer renderer=new SwordEnergyRenderer();
    private final SwordGLState stencilState=new SwordGLState();
    private final NativeSwordGlint fallback=new NativeSwordGlint();
    private final SwordGlintTexture fallbackTexture=new SwordGlintTexture();
    private final IdentityHashMap<IBakedModel,SwordModelBounds> models=new IdentityHashMap<>();
    private final float[] colorChannels=new float[6];
    private SwordOptions.Preset lastPreset=SwordOptions.Preset.Custom;
    private Setting themeSetting;
    private Object lastTheme;
    private AnimationStyle themeAnimation=AnimationStyle.Pulse;
    private boolean themePetals,themeWhite,colorsKnown,shaderFailed,renderFailed,stencilAttempted,stencilFailed;
    private int stencilBit=-1;
    private World world;
    private long previousNanos,frameId;
    private double time,realTime;
    public SwordGlow(){
        super("SwordGlow",ModuleCategory.render);
        withDescription("Layered on-blade energy with stencil-clipped bloom and cosmetic swing reactions.");
        registerSettings(animation,secondary,preset,colorMode,palettePreset,signature,staticColor,gradientStart,gradientEnd,
                speed,intensity,bandWidth,brightness,bloom,scale,angle,activation,trail,swingEnergy,trailStrength,trailLife,hit,block,equip,particles,thirdPerson,others,fade,lowQuality);
        for(int i=0;i<32;i++){states[i]=new SwordAnimator();motions[i]=new SwordMotion();}
        instance=this; // No Raven/event bus/world/GL calls during registry construction.
    }
    @Override public String getHudSuffix(){return animation.getMode()+" "+Math.round(intensity.getInput())+"%";}
    @Override public void onEnable(){shaderFailed=renderFailed=stencilAttempted=stencilFailed=false;renderer.retryParts();}
    @Override public void postApplyConfig(){lastPreset=preset.getMode();}
    @Override public void guiUpdate(){if(preset.getMode()!=lastPreset)applyPreset();}
    @Override public void onGuiClose(){if(preset.getMode()!=lastPreset)applyPreset();}
    @Override public void guiButtonToggled(Setting setting){if(setting==preset)applyPreset();}
    private void applyPreset(){
        lastPreset=preset.getMode();if(lastPreset==SwordOptions.Preset.Custom)return;
        animation.setMode(lastPreset.style);colorMode.setMode(GlintPalette.ColorMode.Palette);palettePreset.setMode(lastPreset);
        intensity.setValue(85);speed.setValue(lastPreset==SwordOptions.Preset.Galaxy?.6:lastPreset==SwordOptions.Preset.Void||lastPreset==SwordOptions.Preset.Aurora?.7:lastPreset==SwordOptions.Preset.Inferno?1.2:lastPreset==SwordOptions.Preset.Neon?1.4:1);
        secondary.setMode(SwordOptions.Secondary.None);bandWidth.setValue(.3);brightness.setValue(1.2);bloom.setValue(45);scale.setValue(8);angle.setValue(-40);
    }
    public static void beginFrame(float partialTicks){if(instance!=null)instance.update(partialTicks);}
    private void clear(){for(int i=0;i<32;i++){players[i]=null;equipped[i]=null;equippedSlot[i]=-1;states[i].reset();motions[i].reset();pendingHit[i]=false;}sparks.clear();CONTEXT.clear();}
    private void update(float partial){
        long now=System.nanoTime();double elapsed=previousNanos==0?0:Math.max(0,(now-previousNanos)/1e9);previousNanos=now;
        CONTEXT.clear();frameId++;realTime+=elapsed;
        if(preset.getMode()!=lastPreset)applyPreset();
        if(world!=mc.theWorld||mc.thePlayer==null){clear();world=mc.theWorld;}
        if(world==null||mc.thePlayer==null){releaseStencil();return;}
        boolean fading=false;for(int i=0;i<32;i++)if(players[i]!=null){fading=true;break;}
        if(!isEnabled()&&!fading){releaseStencil();return;}
        float dt=(float)Math.min(.25,elapsed);time+=elapsed*SwordAnimator.finiteClamp((float)speed.getInput(),.1F,3,1);
        snapshot(dt);sparks.update(dt);
        for(int i=0;i<32;i++)seen[i]=false;
        for(int index=0;index<world.playerEntities.size();index++){
            EntityPlayer player=world.playerEntities.get(index);if(!eligible(player))continue;
            int slot=find(player);boolean sword=sword(player.getHeldItem());
            if(slot<0&&isEnabled()&&sword){slot=player==mc.thePlayer?0:empty();if(slot>=0){players[slot]=player;equipped[slot]=null;states[slot].reset();motions[slot].reset();sparks.clearOwner(slot);}}
            if(slot<0)continue;seen[slot]=true;
            if(sword&&(equipped[slot]==null||equipped[slot].getItem()!=player.getHeldItem().getItem()||player==mc.thePlayer&&equippedSlot[slot]!=player.inventory.currentItem)){
                states[slot].equipped();motions[slot].reset();equipped[slot]=player.getHeldItem();equippedSlot[slot]=player.inventory.currentItem;}
            if(!sword)equipped[slot]=null;
            boolean swinging=player.isSwingInProgress||player.getSwingProgress(partial)>.001;
            boolean mode=activation.getMode()==Activation.Always||(activation.getMode()==Activation.OnSwingOnly?swinging:!swinging);
            SwordAnimator.Frame frame=states[slot].advance(time,dt,isEnabled()&&sword&&mode,options,player.getSwingProgress(partial),swinging,pendingHit[slot],player.isBlocking());pendingHit[slot]=false;
            if((!isEnabled()||!sword)&&frame.visibility<=.0001)players[slot]=null;
        }
        boolean active=false;for(int i=0;i<32;i++){if(!seen[i])players[i]=null;if(players[i]!=null)active=true;}
        if(active&&isEnabled())prepareStencil();
        if(!active&&!isEnabled())releaseStencil();
    }
    private int find(EntityPlayer player){for(int i=0;i<32;i++)if(players[i]==player)return i;return -1;}
    private int empty(){for(int i=1;i<32;i++)if(players[i]==null)return i;return -1;}
    private boolean eligible(EntityPlayer player){return !player.isDead&&(player==mc.thePlayer||others.isToggled()&&!player.isInvisible()&&mc.thePlayer.getDistanceSqToEntity(player)<=256);}
    @Subscribe public void attack(ForgeEvent wrapper){
        if(!(wrapper.getEvent() instanceof AttackEntityEvent))return;
        AttackEntityEvent event=(AttackEntityEvent)wrapper.getEvent();if(event.isCanceled()||event.entityPlayer!=mc.thePlayer||event.target==null)return;
        int slot=find(event.entityPlayer);if(slot>=0)pendingHit[slot]=true;
    }
    private void theme(){
        if(themeSetting==null&&Raven.moduleManager!=null){Module hud=Raven.moduleManager.getModuleByClazz(PhantomHUD.class);if(hud!=null)themeSetting=hud.getSettingByName("Visual theme");}
        Object selected=signature.getMode()!=SwordOptions.Signature.Auto?signature.getMode():themeSetting instanceof ComboSetting?((ComboSetting<?>)themeSetting).getMode():SwordOptions.Signature.Midnight;
        if(selected!=lastTheme){lastTheme=selected;String name=selected.toString();themeAnimation=SwordOptions.signature(name);String lower=name.toLowerCase(java.util.Locale.ROOT);themePetals=lower.contains("sakura");themeWhite=lower.contains("amoled");}
    }
    private void snapshot(float dt){
        theme();options.style=animation.getMode()==AnimationStyle.Theme?themeAnimation:animation.getMode();options.secondary=secondary.getMode();
        options.intensity=SwordAnimator.finiteClamp((float)intensity.getInput()/100,0,1,.75F);options.bandWidth=SwordAnimator.finiteClamp((float)bandWidth.getInput(),.08F,.7F,.3F);
        options.brightness=SwordAnimator.finiteClamp((float)brightness.getInput(),.25F,3,1.2F);options.bloom=SwordAnimator.finiteClamp((float)bloom.getInput()/100,0,1,.45F);
        options.scale=SwordAnimator.finiteClamp((float)scale.getInput(),1,32,8);options.angle=SwordAnimator.finiteClamp((float)angle.getInput(),-180,180,-50);options.fade=SwordAnimator.finiteClamp((float)fade.getInput()/1000,.2F,.3F,.25F);
        options.trail=trail.isToggled();options.hit=hit.isToggled();options.block=block.isToggled();options.equip=equip.isToggled();options.particles=particles.isToggled();options.lowQuality=lowQuality.isToggled();
        options.swingEnergy=SwordAnimator.finiteClamp((float)swingEnergy.getInput()/100,0,1,.65F);
        options.trailStrength=SwordAnimator.finiteClamp((float)trailStrength.getInput()/100,0,1,.75F);
        options.trailLife=SwordAnimator.finiteClamp((float)trailLife.getInput()/1000,.08F,.22F,.20F);
        palette.mode=colorMode.getMode();int start,end;
        if(palette.mode==GlintPalette.ColorMode.Theme){start=themeWhite?0xFFFFFF:PhantomHUD.visualAccent(false);end=themeWhite?0xDDEEFF:PhantomHUD.visualAccent(true);}
        else if(palette.mode==GlintPalette.ColorMode.Palette){start=palettePreset.getMode().start;end=palettePreset.getMode().end;}
        else{start=rgb(palette.mode==GlintPalette.ColorMode.Static?staticColor:gradientStart);end=palette.mode==GlintPalette.ColorMode.Static?start:rgb(gradientEnd);}
        float mix=colorsKnown?1-(float)Math.exp(-dt/.09):1;colorsKnown=true;
        for(int i=0;i<6;i++){int target=i<3?start:end;int shift=(2-i%3)*8;colorChannels[i]+=((target>>shift&255)-colorChannels[i])*mix;}
        palette.start=Math.round(colorChannels[0])<<16|Math.round(colorChannels[1])<<8|Math.round(colorChannels[2]);
        palette.end=Math.round(colorChannels[3])<<16|Math.round(colorChannels[4])<<8|Math.round(colorChannels[5]);
        options.petals=themePetals||palette.mode==GlintPalette.ColorMode.Palette&&palettePreset.getMode()==SwordOptions.Preset.Sakura;
    }
    private void prepareStencil(){
        if(options.lowQuality||options.bloom<=0){releaseStencil();return;}
        if(stencilFailed)return;
        if(stencilBit>=0&&mc.getFramebuffer()!=null&&mc.getFramebuffer().isStencilEnabled()){options.stencilMask=1<<stencilBit;return;}
        stencilState.begin();
        try{
            net.minecraft.client.shader.Framebuffer fb=mc.getFramebuffer();if(fb==null)return;
            if(!fb.isStencilEnabled()&&!stencilAttempted){stencilAttempted=true;fb.enableStencil();}
            if(!fb.isStencilEnabled()){stencilFailed=true;options.stencilMask=0;return;}
            if(fb.isStencilEnabled()&&stencilBit<0)stencilBit=MinecraftForgeClient.reserveStencilBit();
            if(stencilBit<0){stencilFailed=true;options.stencilMask=0;return;}
            options.stencilMask=stencilBit<0?0:1<<stencilBit;
        }catch(RuntimeException|LinkageError e){stencilAttempted=stencilFailed=true;options.stencilMask=0;java.util.logging.Logger.getLogger("Phantom/SwordGlow").warning("Bloom unavailable; on-blade energy remains active");}
        finally{stencilState.end();}
    }
    private void releaseStencil(){if(stencilBit>=0){MinecraftForgeClient.releaseStencilBit(stencilBit);stencilBit=-1;}options.stencilMask=0;}
    public static void beginItem(EntityLivingBase owner,TransformType transform){CONTEXT.model(owner,view(transform));}
    public static void beginHeldItem(EntityLivingBase owner,TransformType transform){CONTEXT.held(owner,view(transform),mc!=null&&owner==mc.thePlayer);}
    public static void beginGuiItem(){CONTEXT.gui();}public static void endItem(){CONTEXT.end();}
    private static int view(TransformType transform){return transform==TransformType.FIRST_PERSON?SwordRenderContext.FIRST_PERSON:transform==TransformType.THIRD_PERSON?SwordRenderContext.THIRD_PERSON:transform==TransformType.GUI?SwordRenderContext.GUI:SwordRenderContext.NONE;}
    private static EntityPlayer renderOwner(ItemStack item){
        SwordGlow glow=instance;if(glow==null||!sword(item)||!(CONTEXT.owner() instanceof EntityPlayer))return null;
        EntityPlayer player=(EntityPlayer)CONTEXT.owner();if(!glow.eligible(player))return null;
        if(CONTEXT.view()==SwordRenderContext.FIRST_PERSON)return player==mc.thePlayer?player:null;
        return CONTEXT.view()==SwordRenderContext.THIRD_PERSON&&glow.thirdPerson.isToggled()?player:null;
    }
    public static boolean controlsHeldGlint(ItemStack item){EntityPlayer player=renderOwner(item);if(player==null||instance.renderFailed)return false;int slot=instance.find(player);
        return slot>=0&&(instance.isEnabled()||instance.states[slot].frame().visibility>.0001);}
    public static void drawHeldGlint(ItemStack item,IBakedModel model,NativeSwordGlint.ModelDraw nativeModel){
        if(!controlsHeldGlint(item)||model==null||model.isBuiltInRenderer())return;SwordGlow glow=instance;int slot=glow.find((EntityPlayer)CONTEXT.owner());SwordAnimator.Frame frame=glow.states[slot].frame();if(frame.brightness<=.0001)return;
        int atlas=glow.options.atlas;
        try{
            net.minecraft.client.renderer.texture.ITextureObject atlasTexture=mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture);if(atlasTexture==null)return;
            atlas=atlasTexture.getGlTextureId();glow.options.atlas=atlas;
            if(!glow.shaderFailed){try{
                SwordModelBounds bounds=glow.models.get(model);if(bounds==null){if(glow.models.size()>=32)glow.models.clear();bounds=new SwordModelBounds(model);glow.models.put(model,bounds);}
                glow.renderer.draw(frame,glow.palette,glow.options,model,bounds,glow.energy.id(),nativeModel,glow.motions[slot],glow.sparks,slot,glow.frameId,glow.realTime);return;
            }catch(RuntimeException|LinkageError error){glow.shaderFailed=true;java.util.logging.Logger.getLogger("Phantom/SwordGlow").log(java.util.logging.Level.WARNING,"Energy shader disabled; falling back to safe on-sword glint",error);}}
            glow.fallback.draw(frame,glow.palette,model,glow.fallbackTexture.id(),nativeModel);
        }catch(RuntimeException|LinkageError error){glow.renderFailed=true;java.util.logging.Logger.getLogger("Phantom/SwordGlow").log(java.util.logging.Level.WARNING,"Sword effect disabled after failure",error);}
        finally{SwordGLState.restoreDefaultState(atlas);}
    }
    private static boolean sword(ItemStack item){return item!=null&&item.getItem() instanceof ItemSword;}
    private static int rgb(PickerRGBSetting c){return c.getRed()<<16|c.getGreen()<<8|c.getBlue();}
}
