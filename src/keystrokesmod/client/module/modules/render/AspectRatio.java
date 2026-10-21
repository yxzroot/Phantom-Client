package keystrokesmod.client.module.modules.render;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.render.aspect.*;

/** Retargets the completed world projection; independent of Lunar's perspective helper. */
public final class AspectRatio extends Module {
    public enum Mode {
        Custom("Custom",0),FourThree("4:3",4F/3),FiveFour("5:4",1.25F),SixteenTen("16:10",1.6F),SixteenNine("16:9",16F/9),TwentyOneNine("21:9",21F/9),Square("1:1",1);
        public final float ratio;private final String label;Mode(String text,float value){label=text;ratio=value;}public String toString(){return label;}
    }
    private final ComboSetting<Mode> mode=new ComboSetting<>("Mode",Mode.FourThree);
    private final SliderSetting custom=new SliderSetting("Custom ratio",1.333,.5,3,.01);
    private final TickSetting smooth=new TickSetting("Smooth transition",true);
    private final TickSetting vertical=new TickSetting("Keep vertical FOV",true);
    private final TickSetting hand=new TickSetting("Stretch hand",false);
    private final AspectAnimator animator=new AspectAnimator();
    private final AspectProjection projection=new AspectProjection();
    private static AspectRatio instance;
    private long previous;
    private int width=-1,height=-1;
    private Object world;
    private float current=16F/9,nativeRatio=16F/9;
    private boolean failed,keepVertical=true,stretchHand;
    private float lastCustom=-1;private String customLabel="Custom";
    public AspectRatio(){super("AspectRatio",ModuleCategory.render);withDescription("Stretch the 3D world without stretching the HUD or hand.");registerSettings(mode,custom,smooth,vertical,hand);instance=this;}
    @Override public void onEnable(){failed=false;}
    /** Moving the custom slider should not silently leave a fixed preset selected. */
    @Override public void guiUpdate(){float value=(float)custom.getInput();if(lastCustom>=0&&value!=lastCustom){mode.setMode(Mode.Custom);lastCustom=value;customLabel=Float.toString(value);}}
    @Override public String getHudSuffix(){return mode.getMode()==Mode.Custom?customLabel:mode.getMode().toString();}
    private void fail(Throwable error){if(!failed){failed=true;java.util.logging.Logger.getLogger("Phantom/AspectRatio").log(java.util.logging.Level.WARNING,"AspectRatio disabled after projection failure",error);}if(isEnabled())disable();}
    public static void frame(){
        AspectRatio module=instance;if(module==null)return;
        try{
            long now=System.nanoTime();double dt=module.previous==0?0:Math.max(0,(now-module.previous)/1e9);module.previous=now;
            float nativeRatio=AspectAnimator.safe(Math.max(1,mc.displayWidth)/(float)Math.max(1,mc.displayHeight));module.nativeRatio=nativeRatio;
            float custom=Float.isFinite((float)module.custom.getInput())?Math.max(.5F,Math.min(3,(float)module.custom.getInput())):4F/3;
            if(custom!=module.lastCustom){module.lastCustom=custom;module.customLabel=Float.toString(custom);}
            float target=module.isEnabled()&&!module.failed?(module.mode.getMode()==Mode.Custom?custom:module.mode.getMode().ratio):nativeRatio;
            boolean resize=module.width!=mc.displayWidth||module.height!=mc.displayHeight;
            if(module.world!=mc.theWorld||resize||mc.theWorld==null){module.animator.reset(mc.theWorld==null?nativeRatio:target);module.world=mc.theWorld;module.width=mc.displayWidth;module.height=mc.displayHeight;}
            module.current=module.animator.update(mc.theWorld==null?nativeRatio:target,dt,module.smooth.isToggled());
            module.keepVertical=module.vertical.isToggled();module.stretchHand=module.hand.isToggled();
        }catch(RuntimeException|LinkageError error){module.fail(error);}
    }
    private static boolean active(boolean hand){AspectRatio m=instance;return m!=null&&!m.failed&&mc!=null&&mc.theWorld!=null&&(!hand||m.stretchHand)&&m.current!=m.nativeRatio;}
    public static float ratio(float vanilla,boolean hand){return active(hand)?instance.current:vanilla;}
    public static float fov(float vanilla,boolean hand){return active(hand)?AspectAnimator.fov(vanilla,instance.nativeRatio,instance.current,instance.keepVertical):vanilla;}
    public static void applyWorld(){AspectRatio m=instance;if(!active(false))return;try{m.projection.retarget(m.current,m.keepVertical);}catch(RuntimeException|LinkageError error){m.fail(error);}}
    public static void prepareHand(){AspectRatio m=instance;if(!active(false)||m.stretchHand)return;try{m.projection.retarget(m.nativeRatio,m.keepVertical);}catch(RuntimeException|LinkageError error){m.fail(error);}}
    public static void applyHand(){AspectRatio m=instance;if(!active(true))return;try{m.projection.retarget(m.current,m.keepVertical);}catch(RuntimeException|LinkageError error){m.fail(error);}}
    public static void begin(){AspectRatio m=instance;if(m==null||!active(false))return;try{m.projection.begin();}catch(RuntimeException|LinkageError error){m.fail(error);}}
    public static void end(){AspectRatio m=instance;if(m==null)return;try{m.projection.restore();}catch(RuntimeException|LinkageError error){m.fail(error);}}
}
