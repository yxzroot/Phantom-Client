import keystrokesmod.client.module.modules.combat.AimAssist;
import keystrokesmod.client.module.modules.combat.aim.*;
import keystrokesmod.client.module.setting.impl.*;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.opengl.*;

/** Deterministic engine, real controller with isolated entities, legacy config, and GL isolation tests. */
public final class AimAssistTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void near(double a,double b,double epsilon,String why){check(Math.abs(a-b)<=epsilon,why+": "+a+" vs "+b);}
    static final sun.misc.Unsafe U;
    static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
    static <T>T blank(Class<T> c)throws Exception{return c.cast(U.allocateInstance(c));}
    static void set(Object owner,Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    static void slider(AimAssist m,String key,double value){((SliderSetting)m.getSettingByName(key)).setValue(value);}
    static void tick(AimAssist m,String key,boolean value){((TickSetting)m.getSettingByName(key)).setEnabled(value);}
    static void history(){
        PositionHistory h=new PositionHistory();for(int i=0;i<=240;i++)h.add(i/240D,i/240D,2,3);
        check(h.size()<=132,"Unbounded history");h.sample(.88);near(h.x,.88,.00001,"History interpolation / 120 ms delay");h.sample(-100);check(h.x>=.49,"History retains more than 500 ms");
        h.add(1.01,20,2,3);check(h.size()==1,"Teleport does not clear target history");h.sample(0);near(h.x,20,0,"Teleport samples stale path");
        h.clear();h.sample(1);near(h.x,0,0,"Clear retains sample");
        for(int i=0;i<2000;i++)h.add(i/1000D,i/1000D,0,0);check(h.size()<=132,"1000 FPS overflows history");h.sample(1.879);near(h.x,1.879,.006,"High-FPS history loses delayed positions");
    }
    static double simulate(int fps,boolean human){
        TrailingPull p=new TrailingPull();AssistOptions o=new AssistOptions();o.humanization=human?.2:0;double yaw=0;
        for(int i=0;i<fps*4;i++){double error=12-yaw;p.step(error,0,error,0,0,0,1D/fps,1+i/(double)fps,true,TrailingPull.quantum(.5F),o);yaw+=p.yawStep;check(yaw<11.31,"Assist centers/snaps at "+fps+" FPS");check(p.yawStep<.2,"Large single-frame turn");}
        return yaw;
    }
    static void pull(){
        double a=simulate(60,false),b=simulate(240,false);near(a,b,.151,"60/240 FPS mismatch");near(simulate(60,true),simulate(240,true),.151,"Humanization FPS mismatch");
        TrailingPull p=new TrailingPull();AssistOptions o=new AssistOptions();o.humanization=0;double q=TrailingPull.quantum(.5F);
        for(int i=0;i<200;i++)p.step(6,4,6,4,0,0,.016,1+i*.016,true,q,o);
        p.step(6,4,6,4,-.15,-.15,.016,5,true,q,o);check(p.yawStep==0&&p.pitchStep==0,"Mouse opposition fights accumulated velocity");
        for(int i=0;i<50;i++){p.step(6,4,6,4,0,0,.016,5.016+i*.016,false,q,o);check(p.yawStep==0&&p.pitchStep==0,"Invalid gate / target still moves camera");}near(p.fade,0,.00001,"Lost target does not fade out");
        p.reset();for(int i=0;i<100;i++){p.step(.69,0,.69,0,0,0,.016,i*.016,true,q,o);check(p.yawStep==0,"Dead zone aims itself onto target");}
        p.reset();o.pitchStrength=0;for(int i=0;i<120;i++){p.step(8,8,8,8,0,0,1D/240,i/240D,true,q,o);near(p.pitchStep,0,0,"Zero pitch strength ignored");near(p.yawStep/q,Math.rint(p.yawStep/q),.000001,"Not a sensitivity step");}
        p.reset();p.step(Double.NaN,0,0,0,0,0,.016,1,true,q,o);check(p.yawStep==0,"NaN propagates into rotation");
        System.out.println("PASS: history bounds/interpolation/teleports; 60/240 FPS pull ("+a+" / "+b+" deg), noise, fades, dead zone, immediate mouse override and sensitivity steps");
    }
    public static class QuietAim extends AimAssist {double testTime;@Override protected double clock(){return testTime==0?super.clock():testTime;}@Override public void setToggled(boolean value){enabled=value;}@Override public void disable(){enabled=false;controller.reset();}public void enabled(boolean value){enabled=value;}}
    public static class Local extends EntityPlayerSP {
        ItemStack held;boolean alive=true,visible=true;Entity teammate;
        public Local(){super(null,null,null,null);}
        @Override public boolean isEntityAlive(){return alive;}
        @Override public float getEyeHeight(){return 1.62F;}
        @Override public ItemStack getHeldItem(){return held;}
        @Override public boolean canEntityBeSeen(Entity e){return visible;}
        @Override public boolean isOnSameTeam(EntityLivingBase e){return e==teammate;}
    }
    public static class Enemy extends EntityPlayer {
        boolean alive=true,invisible;
        public Enemy(){super(null,new GameProfile(UUID.randomUUID(),"unused"));}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isEntityAlive(){return alive;}
        @Override public boolean isInvisible(){return invisible;}
        @Override public float getEyeHeight(){return 1.62F;}
    }
    public static class Controller extends net.minecraft.client.multiplayer.PlayerControllerMP {
        boolean mining;
        public Controller(){super(null,null);}
        @Override public boolean func_181040_m(){return mining;}
    }
    static Minecraft mc;static Local local;static Enemy enemy;static QuietAim module;static List<Entity> entities;
    static Enemy enemy(String name,double x,double z)throws Exception{
        Enemy e=blank(Enemy.class);e.alive=true;e.capabilities=new PlayerCapabilities();e.height=1.8F;e.width=.6F;
        set(e,EntityPlayer.class,"gameProfile",new GameProfile(UUID.randomUUID(),name));set(e,Entity.class,"entityUniqueID",UUID.randomUUID());position(e,x,0,z);return e;
    }
    static void position(Entity e,double x,double y,double z){e.posX=e.prevPosX=x;e.posY=e.prevPosY=y;e.posZ=e.prevPosZ=z;}
    static void setup()throws Exception{
        mc=blank(Minecraft.class);set(null,Minecraft.class,"theMinecraft",mc);
        mc.gameSettings=blank(net.minecraft.client.settings.GameSettings.class);mc.gameSettings.mouseSensitivity=.5F;
        local=blank(Local.class);local.alive=local.visible=true;local.capabilities=new PlayerCapabilities();local.height=1.8F;
        mc.thePlayer=local;mc.theWorld=blank(net.minecraft.client.multiplayer.WorldClient.class);entities=new ArrayList<>();set(mc.theWorld,net.minecraft.world.World.class,"loadedEntityList",entities);
        mc.inGameHasFocus=true;mc.playerController=blank(Controller.class);set(mc,Minecraft.class,"renderViewEntity",local);
        module=new QuietAim();module.enabled(true);enemy=enemy("Target",.6,3);entities.add(enemy);
    }
    static void frame(double time){module.controller.frame(mc,1,time);}
    static void controller()throws Exception{
        setup();for(int i=0;i<120;i++)frame(1+i/240D);check(module.controller.getTarget()==enemy,"No valid nearby player acquired");
        check(local.rotationYaw<0&&local.rotationYaw> -3,"Pull snaps or wrong yaw sign");
        for(int i=0;i<100;i++){double x=.6+i*.004;position(enemy,x,0,3);frame(2+i/240D);}
        check(module.controller.aimX<enemy.posX-.02,"Delayed point does not trail strafe");
        slider(module,"Trail strength (%)",0);frame(2.5);near(module.controller.aimX,enemy.posX,.00001,"Zero trail is not the real position");slider(module,"Trail strength (%)",100);
        for(int i=0;i<130;i++)frame(3+i/240D);near(module.controller.aimX,enemy.posX,.00001,"Stopped target retains artificial offset");
        module.controller.mouse(.3,0);float before=local.rotationYaw;frame(3.55);near(local.rotationYaw,before,0,"Full controller fights opposing mouse movement");
        enemy.invisible=true;frame(4);check(!module.controller.hasTarget,"Invisible target not filtered");enemy.invisible=false;
        enemy.capabilities.isCreativeMode=true;frame(4.01);check(!module.controller.hasTarget,"Creative player not filtered");enemy.capabilities.isCreativeMode=false;
        local.teammate=enemy;frame(4.02);check(!module.controller.hasTarget,"Teammate not filtered");local.teammate=null;
        AimAssist.addFriend(enemy);frame(4.03);check(!module.controller.hasTarget,"Friend not filtered");check(AimAssist.removeFriend("Target"),"Legacy friend removal broken");
        position(enemy,20,0,3);frame(4.04);check(!module.controller.hasTarget,"Range ignored");position(enemy,3,0,0);frame(4.05);check(!module.controller.hasTarget,"FOV ignored");position(enemy,.6,0,3);
        tick(module,"Weapon only",true);frame(4.06);check(!module.controller.hasTarget,"Weapon gate ignored");tick(module,"Weapon only",false);
        tick(module,"Only while moving mouse",true);frame(5);check(!module.controller.hasTarget,"Mouse movement gate ignored");module.controller.mouse(-.3,0);frame(5.01);check(module.controller.hasTarget,"Real mouse movement not recognized");tick(module,"Only while moving mouse",false);
        ((Controller)mc.playerController).mining=true;frame(5.02);check(!module.controller.hasTarget,"Mining gate ignored");((Controller)mc.playerController).mining=false;
        mc.currentScreen=blank(net.minecraft.client.gui.GuiMainMenu.class);frame(5.03);check(!module.controller.hasTarget,"GUI gate ignored");mc.currentScreen=null;
        module.controller.reset();local.rotationYaw=local.rotationPitch=0;frame(6);check(module.controller.getTarget()==enemy,"Reacquisition failed");
        Enemy better=enemy("Better",.05,3);entities.add(better);frame(6.01);check(module.controller.getTarget()==enemy,"Target changed instantly");frame(6.1);check(module.controller.getTarget()==enemy,"Switch delay ignored");frame(6.2);check(module.controller.getTarget()==better,"Better target never selected");
        local.alive=false;frame(7);check(module.controller.getTarget()==null&&!module.controller.hasTarget,"Death does not clear history");local.alive=true;frame(7.01);
        mc.theWorld=blank(net.minecraft.client.multiplayer.WorldClient.class);set(mc.theWorld,net.minecraft.world.World.class,"loadedEntityList",new ArrayList<Entity>());frame(7.02);check(!module.controller.hasTarget,"World change retains target");
        module.enabled(false);before=local.rotationYaw;AimAssist.renderFrame(1);near(local.rotationYaw,before,0,"Disabled module moves camera");
        System.out.println("PASS: production controller trailing/stopping/zero trail, mouse override, invisible/creative/team/friend/FOV/range/weapon/mouse/mining/GUI gates, sticky delayed switch, death/world reset and OFF inert");
    }
    static void extra()throws Exception{
        setup();tick(module,"Only while clicking",true);frame(1);check(!module.controller.hasTarget,"Click gate ignored with no mouse device/button");tick(module,"Only while clicking",false);
        // Hold a real sword and make sure the weapon gate admits it.
        local.held=new ItemStack(new ItemSword(Item.ToolMaterial.IRON));tick(module,"Weapon only",true);frame(1.01);check(module.controller.hasTarget,"Sword weapon gate rejects swords");
        tick(module,"Only while moving mouse",false);tick(module,"Weapon only",false);
        slider(module,"Max offset (degrees)",1);for(int i=0;i<100;i++){position(enemy,.6+i*.01,0,3);frame(2+i/240D);}
        double realYaw=TrailingPull.wrap(Math.toDegrees(Math.atan2(enemy.posZ,enemy.posX))-90-local.rotationYaw);
        double realPitch=-Math.toDegrees(Math.atan2(enemy.height*.72-local.getEyeHeight(),Math.hypot(enemy.posX,enemy.posZ)))-local.rotationPitch;
        check(Math.hypot(TrailingPull.wrap(module.controller.yawError-realYaw),module.controller.pitchError-realPitch)<=1.00001,"Angular trail cap ignored");
        position(local,20,0,20);frame(2.5);check(!module.controller.hasTarget,"Local teleport retains old target");position(local,0,0,0);module.controller.reset();local.visible=false;frame(3);check(!module.controller.hasTarget,"Assist sees through walls");local.visible=true;
        TrailingPull p=new TrailingPull();AssistOptions o=new AssistOptions();double angle=0;for(int i=0;i<240*30;i++){p.step(4-angle,0,4-angle,0,0,0,1D/240,1+i/240D,true,TrailingPull.quantum(.5F),o);angle+=p.yawStep;}
        check(4-angle>=TrailingPull.DEAD_ZONE&&4-angle<1.1,"Long idle pull centers or stops too far away");
        setup();int frames=20000;long begin=System.nanoTime();for(int i=0;i<frames;i++)frame(1+i/240D);double micros=(System.nanoTime()-begin)/1000D/frames;
        check(micros<1000,"Controller simulation unexpectedly slow: "+micros+" us/frame");
        System.out.println("PASS: click/real-sword/visibility/teleport gates, angular offset cap, 30-second no-centering test; isolated controller "+micros+" us/frame (not a full-game FPS benchmark)");
    }
    static void clickerAndDriver()throws Exception{
        AttackGate gate=new AttackGate();check(!gate.active(false,false,true,1),"Idle enabled clicker activates assist");
        gate.press(1);check(gate.active(false,false,true,1.1),"Synthetic UP half-cycle closes gate");check(!gate.active(false,false,true,1.181),"Released input latches indefinitely");check(!gate.active(false,false,false,1.05),"Ordinary release incorrectly gets clicker grace");gate.reset();check(!gate.active(false,false,true,1.1),"Reset retains clicks");
        setup();frame(1);tick(module,"Only while clicking",true);mc.gameSettings.keyBindAttack=new net.minecraft.client.settings.KeyBinding("test attack",-100,"test");
        keystrokesmod.client.module.modules.combat.LeftClicker.autoClickerEnabled=true;
        try{
            for(int i=0;i<240;i++){
                double now=1.01+i/240D;module.testTime=now;boolean down=i%16<3;
                net.minecraft.client.settings.KeyBinding.setKeyBindState(-100,down);
                if(i%16==0){net.minecraftforge.client.event.MouseEvent event=new net.minecraftforge.client.event.MouseEvent();set(event,event.getClass(),"button",0);set(event,event.getClass(),"buttonstate",true);module.mouse(event);}
                frame(now);check(module.controller.hasTarget,"Autoclicker release repeatedly discards target at frame "+i);
            }
            check(local.rotationYaw< -.5,"Clicker half-cycles starve camera pull/fade");
            module.testTime=2.3;frame(2.3);check(!module.controller.hasTarget,"Idle clicker continues aiming after release");
        }finally{keystrokesmod.client.module.modules.combat.LeftClicker.autoClickerEnabled=false;net.minecraft.client.settings.KeyBinding.setKeyBindState(-100,false);}
        setup();
        // Deliberately never invoke either native renderer hook: exercise actual Forge
        // listener entry points, not the direct controller calls used by older tests.
        for(int i=0;i<240;i++){
            module.testTime=1+i/240D;
            net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent start=new net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent(net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START,1);
            module.renderTick(start);net.minecraftforge.client.event.RenderWorldLastEvent world=new net.minecraftforge.client.event.RenderWorldLastEvent(null,1);module.worldLast(world);
            float yaw=local.rotationYaw;AimAssist.renderFrame(1);AimAssist.cameraFrame(1);module.onRender(new keystrokesmod.client.event.impl.ForgeEvent(start));module.onRender(new keystrokesmod.client.event.impl.ForgeEvent(world));
            near(local.rotationYaw,yaw,0,"Fallback/primary/multipass/forwarded events double-apply rotation");
        }
        check(local.rotationYaw< -.5,"Enabled module never updates without native hook");
        module.testTime=2.01;net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent next=new net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent(net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START,1);module.renderTick(next);AimAssist.cameraFrame(1);float yaw=local.rotationYaw;module.worldLast(new net.minecraftforge.client.event.RenderWorldLastEvent(null,1));near(local.rotationYaw,yaw,0,"Camera fallback and Forge both apply");
        System.out.println("PASS: active autoclicker synthetic press/release gaps keep target/fade, idle/released/disabled clicker stops; real Forge event driver works with no native hooks and never double-applies");
    }
    static JsonObject setting(String type,Object value){JsonObject o=new JsonObject();o.addProperty("type",type);if(value instanceof Boolean)o.addProperty("value",(Boolean)value);else o.addProperty("value",(Number)value);return o;}
    static void configs()throws Exception{
        setup();JsonObject old=new JsonObject(),s=new JsonObject();old.addProperty("enabled",false);old.addProperty("keycode",37);old.addProperty("showInHud",false);old.add("settings",s);
        s.add("Speed 1 (yaw)",setting("slider",45));s.add("Speed 2 (yaw)",setting("slider",15));s.add("Distance",setting("slider",4.3));s.add("Click aim",setting("tick",true));s.add("Aim pitch",setting("tick",false));s.add("FOV",setting("slider",80));
        String original=old.toString();module.applyConfigFromJson(old);near(AimAssist.speedYaw.getInput(),4.5,0,"Legacy speed migration");near(AimAssist.distance.getInput(),4.3,0,"Legacy range migration");
        check(module.getKeycode()==37&&!module.showInHud()&&!module.isEnabled()&&AimAssist.clickAim.isToggled(),"Legacy bind/toggle/HUD/click compatibility");check(old.toString().equals(original),"Migration mutates original config data");
        slider(module,"Pitch strength (%)",17);slider(module,"Trail delay (ms)",175);JsonObject saved=module.getConfigAsJson();QuietAim other=new QuietAim();other.applyConfigFromJson(saved);near(other.options.delay,.175,.000001,"New settings round trip");near(other.options.pitchStrength,.17,.000001,"New pitch round trip");check(other.getName().equals("AimAssist")&&other.getKeycode()==37,"Module/config identity changed");
        AssistSlider slider=new AssistSlider("Test",1,0,3,.1);slider.setValue(Double.NaN);near(slider.getInput(),0,0,"NaN setting accepted");slider.setValue(8);near(slider.getInput(),3,0,"Setting max ignored");slider.resetToDefaults();near(slider.getInput(),1,0,"Cached slider reset broken");
        System.out.println("PASS: old controls migrate without modifying JSON, existing name/keybind/toggle/HUD flags, new settings round trip, cached slider sanitation/reset; no real config files touched");
    }
    static double fastPull(int fps,double speed,boolean movingTarget){
        TrailingPull p=new TrailingPull();AssistOptions o=new AssistOptions();o.speed=speed;o.humanization=0;
        p.fade=1; // Compare sustained top-end speed, independently of fade-in frame boundaries.
        double total=0;for(int i=0;i<fps*2;i++){
            double error=movingTarget?10:20-total;
            p.step(error,0,error,0,0,0,1D/fps,1+i/(double)fps,true,TrailingPull.quantum(.5F),o);
            check(p.yawStep>=0&&p.yawStep<=2.01,"High speed snaps / reverses direction");total+=p.yawStep;
            if(!movingTarget)check(total<=20-TrailingPull.DEAD_ZONE+.00001,"High speed crosses no-centering zone");
        }
        p.step(10,0,10,0,-.15,0,1D/fps,4,true,TrailingPull.quantum(.5F),o);near(p.yawStep,0,0,"High speed fights opposing mouse");return total;
    }
    static void higherSpeed()throws Exception{
        setup();SliderSetting speed=(SliderSetting)module.getSettingByName("Speed");near(speed.getInput(),4,0,"Gentle default changed");near(speed.getMax(),60,0,"Higher slider maximum missing");
        speed.setValue(20);JsonObject old=module.getConfigAsJson();QuietAim same=new QuietAim();same.applyConfigFromJson(old);near(same.options.speed,20,0,"Existing speed remapped");
        AimAssist.speedYaw.setValue(60);JsonObject saved=same.getConfigAsJson();QuietAim loaded=new QuietAim();loaded.applyConfigFromJson(saved);near(loaded.options.speed,60,0,"New max speed does not save/load");AimAssist.speedYaw.setValue(100);near(AimAssist.speedYaw.getInput(),60,0,"New max is not bounded");
        double oldMax=fastPull(240,20,true),newMax=fastPull(240,60,true);near(newMax/oldMax,3,.03,"Maximum pull not three times faster");
        near(fastPull(60,60,true),newMax,.151,"High-speed 60/240 FPS mismatch");near(fastPull(60,60,false),fastPull(240,60,false),.151,"High-speed closed-loop FPS mismatch");
        System.out.println("PASS: Speed 20 -> 60 (3x top-end), default/existing values unchanged, new max config round-trip, 60/240 FPS, no-centering and immediate override preserved");
    }
    static void audit(String folder)throws Exception{
        setup();int count=0;for(java.io.File file:new java.io.File(folder).listFiles())if(file.getName().endsWith(".bplus")){
            byte[] before=java.nio.file.Files.readAllBytes(file.toPath());JsonObject profile=new keystrokesmod.client.config.Config(file).getData();
            if(profile==null||!profile.has("modules")||!profile.getAsJsonObject("modules").has("AimAssist"))continue;
            JsonObject saved=profile.getAsJsonObject("modules").getAsJsonObject("AimAssist");QuietAim aim=new QuietAim();aim.applyConfigFromJson(saved);
            check(aim.getKeycode()==saved.get("keycode").getAsInt()&&aim.isEnabled()==saved.get("enabled").getAsBoolean(),"Real config identity/toggle migration failed");
            check(Arrays.equals(before,java.nio.file.Files.readAllBytes(file.toPath())),"Read-only migration touched real file");count++;
        }check(count>=3,"Expected real current/preserved/test configs");System.out.println("PASS: read-only AimAssist migration of "+count+" real profiles; saved keybinds/toggles preserved");
    }
    public static class TestFont extends net.minecraft.client.gui.FontRenderer {
        public TestFont(){super(null,null,null,false);}
        @Override public int getStringWidth(String text){return text.length()*6;}
        @Override public int drawString(String text,int x,int y,int color){return text.length()*6;}
    }
    static void gl()throws Exception{
        Pbuffer context=new Pbuffer(128,128,new PixelFormat().withDepthBits(24),null);context.makeCurrent();try{
            setup();net.minecraft.client.renderer.entity.RenderManager manager=blank(net.minecraft.client.renderer.entity.RenderManager.class);set(mc,Minecraft.class,"renderManager",manager);mc.fontRendererObj=blank(TestFont.class);
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            AssistDebug debug=new AssistDebug();AssistController c=module.controller;c.aimX=.1;c.aimY=.2;c.aimZ=.3;c.yawError=3;
            GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            int tex=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex);GL11.glColor4f(1,1,1,1);
            for(int i=0;i<200;i++){debug.draw(mc,c);debug.hud(mc,c,"Forge fallback");}
            check(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)&&GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST),"Debug leaks enabled states");
            check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==tex,"Debug leaks depth mask / binding");check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"Debug leaks matrix mode");
            java.nio.FloatBuffer color=org.lwjgl.BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);for(int i=0;i<4;i++)near(color.get(i),1,.00001,"Debug tints other renders");
            mc.fontRendererObj=null;try{debug.draw(mc,c);throw new AssertionError("Synthetic debug failure missing");}catch(NullPointerException expected){}
            check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==tex,"Exception skips GL restoration");check(GL11.glGetError()==0,"Debug GL error");
            System.out.println("PASS: real offscreen debug rendering, 200 frames and synthetic exception restore GL depth/texture/lighting/fog/color/matrices");
        }finally{context.destroy();}
    }
    public static void main(String[] args)throws Exception{if(args.length>0){if(args[0].equals("audit"))audit(args[1]);else gl();return;}history();pull();controller();extra();clickerAndDriver();configs();higherSpeed();}
}
