import keystrokesmod.client.module.modules.movement.Airstrafe;
import keystrokesmod.client.module.modules.movement.airstrafe.*;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.event.impl.MoveInputEvent;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.util.MovementInput;
import java.lang.reflect.Field;
import java.util.Random;
import org.lwjgl.opengl.*;

/** Production movement listener, deterministic physics, config and offscreen GL checks. */
public final class AirstrafeTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void near(double a,double b,double e,String why){check(Math.abs(a-b)<=e,why+": "+a+" vs "+b);}
    static final sun.misc.Unsafe U;
    static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
    static <T>T blank(Class<T> c)throws Exception{return c.cast(U.allocateInstance(c));}
    static void set(Object owner,Class<?> c,String name,Object value)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    public static class QuietAir extends Airstrafe {
        int failures;
        @Override public void setToggled(boolean value){enabled=value;}
        @Override public void disable(){failures++;enabled=false;controller.reset();}
    }
    public static class Local extends EntityPlayerSP {
        boolean alive,water,lava,ladder,spectator,sprint;
        public Local(){super(null,null,null,null);}
        @Override public boolean isEntityAlive(){return alive;}
        @Override public boolean isInWater(){return water;}
        @Override public boolean isInLava(){return lava;}
        @Override public boolean isOnLadder(){return ladder;}
        @Override public boolean isSpectator(){return spectator;}
        @Override public boolean isSprinting(){return sprint;}
        void web(boolean value){isInWeb=value;}
    }
    static Minecraft mc;static Local p;static QuietAir m;static EventBus bus;
    static void setup()throws Exception{
        mc=blank(Minecraft.class);set(null,Minecraft.class,"theMinecraft",mc);
        mc.gameSettings=blank(net.minecraft.client.settings.GameSettings.class);
        mc.theWorld=blank(net.minecraft.client.multiplayer.WorldClient.class);mc.inGameHasFocus=true;
        p=blank(Local.class);p.alive=true;p.capabilities=new PlayerCapabilities();p.movementInput=new MovementInput();p.height=1.8F;p.width=.6F;p.motionY=.314159;
        mc.thePlayer=p;m=new QuietAir();m.setToggled(true);bus=new EventBus();bus.register(m);
    }
    static MoveInputEvent post(float s,float f){p.ticksExisted++;MoveInputEvent e=new MoveInputEvent(s,f,.02F,0);double y=p.motionY;bus.post(e);near(p.motionY,y,0,"Vertical velocity changed");return e;}
    static void blocked(String why){double x=p.motionX,z=p.motionZ;MoveInputEvent e=post(1,0);near(e.getFriction(),.02F,0,why+" changed vanilla friction");near(p.motionX,x,0,why+" changed X");near(p.motionZ,z,0,why+" changed Z");near(m.controller.steering.strength(),0,0,why+" retains ramp");}
    static void toggle(String name,boolean v){((TickSetting)m.getSettingByName(name)).setEnabled(v);}
    static void slider(String name,double v){((SliderSetting)m.getSettingByName(name)).setValue(v);}
    static void kernel(){
        AirOptions o=new AirOptions();o.smoothing=0;AirSteering a=new AirSteering();
        a.step(0,.3,.02,0,1,0,o);near(a.fade(),.5,1e-12,"100ms fade first tick");near(a.x,.044,1e-12,"Gentle first tick");
        a.step(0,.3,.02,0,1,0,o);near(a.fade(),1,0,"Fade not complete in 100ms");near(a.x,.068,1e-12,"Full sideways strength acceleration");
        a.step(0,.5,.02,0,1,0,o);check(a.x>0,"Cannot steer at cap");near(Math.hypot(a.x,a.z),.5,1e-12,"Cap overshoot");check(a.z>.49,"Momentum snaps");
        a.step(0,.9,.02,0,1,0,o);near(Math.hypot(a.x,a.z),.9,1e-12,"Knockback deleted or increased");
        Random r=new Random(48);double x=0,z=0;
        for(int i=0;i<20000;i++){double angle=r.nextDouble()*Math.PI*2,wx=Math.sin(angle),wz=Math.cos(angle);a.step(x,z,.02*wx,.02*wz,wx,wz,o);check(Math.hypot(a.x,a.z)<=o.cap+1e-12,"Speed cap violation");check(Math.hypot(a.x-x,a.z-z)<=.09200001,"Abrupt velocity snap");x=a.x;z=a.z;}
        a.reset();near(a.fade(),0,0,"Reset retains fade");near(a.strength(),0,0,"Reset retains strength");
        AirSteering smooth=new AirSteering();o.smoothing=1;smooth.step(0,0,0,0,1,0,o);check(smooth.x<.012,"Smoothing ignored");
        System.out.println("PASS: 20,000 steering/cap/momentum cases, 100ms fade, smoothing, no snap, external knockback preservation");
    }
    static void quickTurns()throws Exception{
        AirOptions o=new AirOptions();AirSteering a=new AirSteering();
        // Fully established steering, then reverse keys at sprint-jump speed.
        for(int i=0;i<30;i++)a.step(.5,0,.02,0,1,0,o);
        double vx=.5;int ticks=0;while(vx>0&&ticks<30){a.step(vx,0,-.02,0,-1,0,o);check(Math.abs(a.x-vx)<=.09200001,"Reversal snaps velocity");vx=a.x;ticks++;}
        int oldTicks=(int)Math.ceil(.5/(.02+.06*o.strength));check(ticks<=oldTicks*.7,"Reversal still feels delayed: "+ticks+" vs "+oldTicks);
        a.reset();for(int i=0;i<30;i++)a.step(0,.5,0,.02,0,1,o);
        double x=0,z=.5,oldX=0,oldZ=.5;
        for(int i=0;i<3;i++){
            a.step(x,z,.02,0,1,0,o);x=a.x;z=a.z;
            oldX+=.02+.06*o.strength;double length=Math.hypot(oldX,oldZ);if(length>.5){oldX*=.5/length;oldZ*=.5/length;}
        }
        check(Math.atan2(x,z)>Math.atan2(oldX,oldZ)*1.3,"Right-angle turn not more responsive");
        setup();for(int i=0;i<30;i++){p.motionX=.5;p.motionZ=0;post(1,0);}
        p.motionX=.5;post(-1,0);check(p.motionX<.43,"Actual listener delays opposite key");near(p.motionY,.314159,0,"Turn changes vertical motion");
        o.strength=0;a.step(.3,0,-.02,0,-1,0,o); // Blend decays only in kernel; controller explicitly bypasses at strength 0.
        System.out.println("PASS: opposite input reverses in "+ticks+" ticks vs old "+oldTicks+"; faster 90-degree response and actual listener reacts on first input tick");
    }
    static void controller()throws Exception{
        setup();p.motionZ=.3;MoveInputEvent e=post(1,0);near(e.getFriction(),0,0,"Real event listener never consumes acceleration");check(p.motionX>.02&&p.motionZ==.3,"No extra airborne steering");
        double x=p.motionX;bus.post(new MoveInputEvent(1,0,.02F,0));near(p.motionX,x,0,"Double-applied same tick");
        p.onGround=true;blocked("Ground");p.onGround=false;
        p.water=true;blocked("Water");p.water=false;p.lava=true;blocked("Lava");p.lava=false;p.ladder=true;blocked("Ladder / vine");p.ladder=false;p.web(true);blocked("Web");p.web(false);
        p.capabilities.isFlying=true;blocked("Flight");p.capabilities.isFlying=false;p.spectator=true;blocked("Spectator");p.spectator=false;p.noClip=true;blocked("Noclip");p.noClip=false;
        p.ridingEntity=p;blocked("Riding");p.ridingEntity=null;p.alive=false;blocked("Dead");p.alive=true;
        mc.inGameHasFocus=false;blocked("Unfocused");mc.inGameHasFocus=true;mc.currentScreen=blank(net.minecraft.client.gui.GuiMainMenu.class);blocked("Menu");mc.currentScreen=null;
        set(mc,Minecraft.class,"isGamePaused",true);blocked("Paused");set(mc,Minecraft.class,"isGamePaused",false);
        toggle("Only while sprinting",true);blocked("Sprint required");p.sprint=true;check(post(1,0).getFriction()==0,"Sprint never admitted");toggle("Only while sprinting",false);
        toggle("Only while holding jump",true);blocked("Jump required");p.movementInput.jump=true;check(post(1,0).getFriction()==0,"Held jump never admitted");toggle("Only while holding jump",false);
        toggle("Disable in water/lava/webs/ladders",false);p.water=true;check(post(1,0).getFriction()==0,"Explicit special-medium opt-in ignored");p.water=false;toggle("Disable in water/lava/webs/ladders",true);
        slider("Air control strength (%)",0);blocked("Zero strength");slider("Air control strength (%)",40);
        double beforeX=p.motionX,beforeZ=p.motionZ;e=post(0,0);near(e.getFriction(),.02F,0,"Idle input consumed vanilla");near(p.motionX,beforeX,0,"Idle adds acceleration");near(p.motionZ,beforeZ,0,"Idle changes momentum");
        m.setToggled(false);blocked("Module OFF");
        System.out.println("PASS: actual Guava movement listener, once/tick, all environmental/input gates, zero/OFF inert and vertical motion unchanged");
    }
    static void resets()throws Exception{
        setup();for(int i=0;i<6;i++)post(1,0);check(m.controller.steering.fade()==1,"Setup ramp missing");
        p.onGround=true;m.controller.afterTick(mc);near(m.controller.steering.fade(),0,0,"Landing retains boost");p.onGround=false;post(1,0);near(m.controller.steering.fade(),.5,0,"Landing did not reset fade");
        p.posX=100;blocked("Teleport");post(1,0);near(m.controller.steering.fade(),.5,0,"Teleport retains boost");
        mc.theWorld=blank(net.minecraft.client.multiplayer.WorldClient.class);post(1,0);near(m.controller.steering.fade(),.5,0,"World change retains boost");
        p.dimension++;post(1,0);near(m.controller.steering.fade(),.5,0,"Dimension change retains boost");
        p.ticksExisted=-1;post(1,0);near(m.controller.steering.fade(),.5,0,"Respawn tick rewind retains boost");
        Local fresh=blank(Local.class);fresh.alive=true;fresh.capabilities=new PlayerCapabilities();fresh.movementInput=new MovementInput();fresh.motionY=.314159;mc.thePlayer=p=fresh;post(1,0);near(m.controller.steering.fade(),.5,0,"Respawn player retains boost");
        m.controller.unload(mc.theWorld);near(m.controller.steering.fade(),0,0,"World unload retains state");mc.theWorld=null;m.controller.afterTick(mc);near(m.controller.speed,0,0,"No-world retains speed");
        System.out.println("PASS: landing / teleport / world / dimension / respawn / unload reset");
    }
    static double[] simulate(int fps)throws Exception{
        setup();p.motionZ=.28;for(int tick=0;tick<100;tick++){
            for(int frame=0;frame<fps/20;frame++)m.hud(null); // No render-frame physics.
            MoveInputEvent e=post(1,1);check(e.getFriction()==0,"Air hook inactive");p.motionX*=.91;p.motionZ*=.91;p.motionY=(p.motionY-.08)*.98;m.controller.afterTick(mc);
        }return new double[]{p.motionX,p.motionZ,p.motionY};
    }
    static void configs()throws Exception{
        setup();check(m.getKeycode()==0&&!((TickSetting)m.getSettingByName("Debug")).isToggled(),"Unsafe defaults");
        slider("Air control strength (%)",73);slider("Speed cap (blocks/tick)",.43);slider("Smoothing (%)",91);toggle("Only while holding jump",true);
        JsonObject json=m.getConfigAsJson();json.addProperty("keycode",37);json.addProperty("showInHud",false);QuietAir loaded=new QuietAir();loaded.applyConfigFromJson(json);
        near(loaded.options.strength,.73,1e-12,"Strength config");near(loaded.options.cap,.43,1e-12,"Cap config");near(loaded.options.smoothing,.91,1e-12,"Smoothing config");check(loaded.options.jumpOnly&&loaded.getKeycode()==37&&!loaded.showInHud(),"Toggle / binding / HUD config lost");
        SliderSetting s=(SliderSetting)m.getSettingByName("Air control strength (%)");s.setValue(Double.NaN);near(s.getInput(),0,0,"NaN slider");s.setValue(1000);near(s.getInput(),100,0,"Slider range");s.resetToDefaults();near(s.getInput(),40,0,"Cached reset");
        double[] a=simulate(60),b=simulate(240);for(int i=0;i<3;i++)near(a[i],b[i],0,"60/240 FPS mismatch / vertical gravity changed");
        setup();p.capabilities=null;post(1,0);check(!m.isEnabled()&&m.failures==1,"Exception did not disable safely");post(1,0);check(m.failures==1,"Exception repeatedly logged / disabled");
        System.out.println("PASS: settings/config/keybind/HUD round-trip, cached sliders, identical 60/240 FPS & vanilla gravity, safe once-only failure disable; real configs untouched");
    }
    public static class TestFont extends net.minecraft.client.gui.FontRenderer {
        public TestFont(){super(null,null,null,false);}
        @Override public int drawString(String text,int x,int y,int color){return text.length()*6;}
    }
    static void gl()throws Exception{
        Pbuffer context=new Pbuffer(128,128,new PixelFormat().withDepthBits(24),null);context.makeCurrent();try{
            setup();mc.fontRendererObj=blank(TestFont.class);net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;
            GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_FOG);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            int texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glColor4f(1,1,1,1);AirDebug debug=new AirDebug();
            for(int i=0;i<200;i++)debug.draw(mc,m.controller);
            check(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)&&GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_FOG)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST),"Debug leaks GL enables");
            check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==texture,"Debug leaks depth mask / binding");check(GL11.glGetInteger(GL11.GL_MATRIX_MODE)==GL11.GL_MODELVIEW,"Debug leaks matrix mode");
            java.nio.FloatBuffer color=org.lwjgl.BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,color);for(int i=0;i<4;i++)near(color.get(i),1,.00001,"Debug tint leak");
            mc.fontRendererObj=null;try{debug.draw(mc,m.controller);throw new AssertionError("Missing synthetic failure");}catch(NullPointerException expected){}
            check(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==texture&&GL11.glGetError()==0,"Exception leaks GL state");
            System.out.println("PASS: 200 offscreen debug renders + forced exception restore GL state");
        }finally{context.destroy();}
    }
    public static void main(String[] args)throws Exception{if(args.length>0){gl();return;}kernel();quickTurns();controller();resets();configs();}
}
