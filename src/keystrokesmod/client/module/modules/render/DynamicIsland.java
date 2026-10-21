package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.config.ConfigManager;import keystrokesmod.client.main.ClientConfig;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.module.Module;import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.module.modules.render.island.*;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.ScaledResolution;import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.lang.reflect.Field;import java.text.SimpleDateFormat;import java.util.Date;

/** Native Phantom HUD module. Service failures never disable ordinary island events. */
public final class DynamicIsland extends Module {
    private final ComboSetting<IslandOptions.Idle> idle=new ComboSetting<>("Idle Content",IslandOptions.Idle.Name);
    private final SliderSetting scale=new SliderSetting("Scale",1,.6,2,.05),y=new SliderSetting("Y Offset",8,0,200,1),maxWidth=new SliderSetting("Max Pill Width",360,150,500,5),textSize=new SliderSetting("Text Size",1,.75,1.4,.05);
    private final TickSetting menus=new TickSetting("Show In Menus",false),theme=new TickSetting("Theme Accent",true),shadow=new TickSetting("Shadow",true),glow=new TickSetting("Glow",false),matchArt=new TickSetting("Match Album Art Color",true),albumArt=new TickSetting("Show Album Art",true);
    private final PickerRGBSetting color=new PickerRGBSetting("Accent Color",155,140,255,null);
    private final SliderSetting eventDuration=new SliderSetting("Event Duration (s)",2.5,.5,8,.1),speed=new SliderSetting("Animation Speed",1,.3,3,.05),overshoot=new SliderSetting("Overshoot Strength",.3,0,1,.05),scroll=new SliderSetting("Lyrics Scroll Duration (s)",.6,.2,1,.05),marquee=new SliderSetting("Marquee Speed",20,5,60,1);
    private final TickSetting toggles=new TickSetting("Module Toggle Events",true),notifications=new TickSetting("Notification Events",true),combat=new TickSetting("Combat Events",false),timers=new TickSetting("Timer Events",true),lowHealth=new TickSetting("Low Health Warnings",true),lowDurability=new TickSetting("Low Durability Warnings",true),replace=new TickSetting("Replace Existing Notifications",false);
    private final TickSetting music=new TickSetting("Now Playing",true),showLyrics=new TickSetting("Show Lyrics",true),karaoke=new TickSetting("Karaoke Word Fill",true),online=new TickSetting("Online Lyrics",false),dots=new TickSetting("Instrumental Dots",true),progress=new TickSetting("Music Progress Bar",true);
    private final ComboSetting<IslandOptions.MusicMode> musicMode=new ComboSetting<>("Music Display Mode",IslandOptions.MusicMode.Always);
    private final TickSetting lyricsOnly=new TickSetting("Lyrics only",false);
    public enum Lines { One,Three }
    private final ComboSetting<Lines> lines=new ComboSetting<>("Lyrics Lines",Lines.Three);
    private final SliderSetting offset=new SliderSetting("Lyrics Offset (s)",0,-30,30,.1);
    private static DynamicIsland instance;
    private final IslandQueue queue=new IslandQueue();private final IslandRenderer renderer=new IslandRenderer();private final IslandOptions options=new IslandOptions();
    private MediaSessionManager media;private LyricsManager lyrics;
    private final KeyBinding lyricsKey=new KeyBinding("Phantom: Toggle Lyrics View",0,"Phantom DynamicIsland"),musicKey=new KeyBinding("Phantom: Show Now Playing",0,"Phantom DynamicIsland"),plusKey=new KeyBinding("Phantom: Lyrics Offset +0.1",0,"Phantom DynamicIsland"),minusKey=new KeyBinding("Phantom: Lyrics Offset -0.1",0,"Phantom DynamicIsland");
    private String track="",idleText="Phantom",timerName="";private boolean onlineBefore,registered,failed,lyricsExpanded=true,healthWarned,durabilityWarned,offsetLoaded;
    private long songChanged,showUntil,lastSecond=-1,timerStart,timerEnd,lastCombat;private Object lastWorld;private EntityLivingBase target;
    private double lastOffset;private Field notificationTitle,notificationMessage;private final SimpleDateFormat clock=new SimpleDateFormat("HH:mm");
    private ScaledResolution screen;private int screenWidth,screenHeight,screenScale;
    public DynamicIsland(){
        super("DynamicIsland",ModuleCategory.render);instance=this;withDescription("Spring-morphing notifications, OS Now Playing and synchronized lyrics.");
        registerSettings(idle,scale,y,maxWidth,menus,theme,color,shadow,glow,textSize,matchArt,albumArt,eventDuration,speed,overshoot,scroll,marquee,toggles,notifications,combat,timers,lowHealth,lowDurability,replace,music,musicMode,showLyrics,lyricsOnly,lines,karaoke,offset,online,dots,progress);
        for(KeyBinding binding:new KeyBinding[]{lyricsKey,musicKey,plusKey,minusKey})ClientRegistry.registerKeyBinding(binding);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{if(media!=null)media.close();if(lyrics!=null)lyrics.close();},"Phantom-island-shutdown"));
    }
    @Override public void onEnable(){
        failed=false;queue.clear();track="";lastSecond=-1;lyricsExpanded=true;media=new MediaSessionManager(mc.mcDataDir.toPath().resolve("phantom"));lyrics=new LyricsManager(mc.mcDataDir.toPath().resolve("phantom"));lastOffset=offset.getInput();
        if(music.isToggled())media.start();if(!registered){registered=true;MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);}
    }
    @Override public void onDisable(){
        if(media!=null)media.close();if(lyrics!=null)lyrics.close();queue.clear();target=null;
        if(registered){MinecraftForge.EVENT_BUS.unregister(this);FMLCommonHandler.instance().bus().unregister(this);registered=false;}
        mc.addScheduledTask(()->renderer.dispose());
    }
    public static boolean moduleChanged(Module module){
        DynamicIsland island=instance;if(island==null||!island.isEnabled()||!island.toggles.isToggled()||module==island||ConfigManager.applyingConfig||ClientConfig.applyingConfig)return false;
        boolean enabled=module.isEnabled();island.emit(new IslandEvent(IslandEvent.Kind.Toggle,"module:"+module.getName(),module.getName(),enabled?"Enabled":"Disabled","*",enabled?0xFF57D68D:0xFFED687B,1,island.duration(),enabled));return island.replace.isToggled();
    }
    /** Public custom notification API; icon is a short font glyph/text, never arbitrary GL work. */
    public static void post(String title,String message,String icon,long millis){DynamicIsland island=instance;if(island!=null&&island.isEnabled()&&island.notifications.isToggled())island.emit(new IslandEvent(IslandEvent.Kind.Notification,"notice:"+title,title,message,icon,0xFF9B8CFF,2,millis,true));}
    public static boolean notification(Object notification){
        DynamicIsland island=instance;if(island==null||!island.isEnabled()||!island.notifications.isToggled())return false;
        try{if(island.notificationTitle==null){island.notificationTitle=notification.getClass().getDeclaredField("title");island.notificationMessage=notification.getClass().getDeclaredField("messsage");island.notificationTitle.setAccessible(true);island.notificationMessage.setAccessible(true);}String title=(String)island.notificationTitle.get(notification);if(title==null||!title.startsWith("Module "))post(title,(String)island.notificationMessage.get(notification),"i",island.duration());}
        catch(ReflectiveOperationException|RuntimeException failure){IslandLog.once("Legacy notification bridge unavailable",failure);return false;}return island.replace.isToggled();
    }
    public static void startTimer(String name,double seconds){DynamicIsland island=instance;if(island==null||!island.isEnabled())return;island.timerName=name==null?"Timer":name;island.timerStart=System.nanoTime();island.timerEnd=seconds>0?island.timerStart+(long)(Math.min(86400,seconds)*1e9):0;}
    public static void stopTimer(){DynamicIsland island=instance;if(island!=null)island.timerStart=0;}
    private long duration(){return(long)(eventDuration.getInput()*1000);}
    private void emit(IslandEvent event){queue.post(event,System.nanoTime());}
    @Subscribe public void overlay(Render2DEvent event){if(mc.currentScreen==null)draw();}
    @SubscribeEvent public void menu(GuiScreenEvent.DrawScreenEvent.Post event){if(menus.isToggled())draw();}
    @SubscribeEvent public void attack(AttackEntityEvent event){if(event.entityPlayer==mc.thePlayer&&event.target instanceof EntityLivingBase){target=(EntityLivingBase)event.target;lastCombat=System.nanoTime();}}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!isEnabled())return;
        if(lyricsKey.isPressed())lyricsExpanded=!lyricsExpanded;
        if(musicKey.isPressed())showUntil=System.nanoTime()+(long)(eventDuration.getInput()*1e9);
        if(plusKey.isPressed())offset.setValue(Math.min(30,offset.getInput()+.1));if(minusKey.isPressed())offset.setValue(Math.max(-30,offset.getInput()-.1));
        if(lastWorld!=mc.theWorld){lastWorld=mc.theWorld;target=null;timerStart=0;healthWarned=durabilityWarned=false;queue.clear();}
        if(mc.thePlayer==null||mc.theWorld==null)return;
        boolean health=mc.thePlayer.getHealth()>0&&mc.thePlayer.getHealth()<=6;
        if(health&&!healthWarned&&lowHealth.isToggled())emit(new IslandEvent(IslandEvent.Kind.Warning,"health","Low health","Find somewhere safe","!",0xFFED687B,5,4500,true));healthWarned=health;
        ItemStack stack=mc.thePlayer.getHeldItem();boolean durability=stack!=null&&stack.isItemStackDamageable()&&stack.getMaxDamage()>0&&(stack.getMaxDamage()-stack.getItemDamage())/(double)stack.getMaxDamage()<=.1;
        if(durability&&!durabilityWarned&&lowDurability.isToggled())emit(new IslandEvent(IslandEvent.Kind.Warning,"durability","Low durability","Your held item is nearly broken","!",0xFFFFBE68,4,4000,true));durabilityWarned=durability;
    }
    private void draw(){
        if(!isEnabled()||failed||mc.gameSettings.hideGUI||mc.gameSettings.showDebugInfo)return;
        try{
            long now=System.nanoTime();snapshot();
            if(music.isToggled()){if(media==null)media=new MediaSessionManager(mc.mcDataDir.toPath().resolve("phantom"));media.start();}else if(media!=null){media.close();media=null;}
            MediaSnapshot m=media!=null?media.snapshot():MediaSnapshot.EMPTY;
            if(!track.equals(m.song())||onlineBefore!=online.isToggled()){
                track=m.song();onlineBefore=online.isToggled();songChanged=now;offsetLoaded=false;lyrics.request(m,onlineBefore);
            }
            LyricsManager.Result result=lyrics.result();
            if(offset.getInput()!=lastOffset){lastOffset=offset.getInput();offsetLoaded=true;lyrics.offset(track,lastOffset);}
            if(result.ready&&result.song.equals(track)&&!offsetLoaded){offset.setValue(result.offset);lastOffset=result.offset;offsetLoaded=true;}
            options.lyrics=options.lyricsOnly||options.lyrics&&lyricsExpanded;
            long second=now/1000000000L;
            if(second!=lastSecond){lastSecond=second;switch(options.idle){case FPS:idleText=Minecraft.getDebugFPS()+" FPS";break;case Clock:idleText=clock.format(new Date());break;case Nothing:idleText="";break;default:idleText="Phantom";}
                lyrics.request(m,onlineBefore); // Deduplicated; retries transient failures off-thread after backoff.
                if(combat.isToggled()&&target!=null&&now-lastCombat<3000000000L)emit(new IslandEvent(IslandEvent.Kind.Combat,"combat",target.getName(),Math.max(0,Math.round(target.getHealth()*10)/10F)+" HP","+",target.hurtTime>0?0xFFED687B:0xFF9B8CFF,2,1000,true));
                if(timers.isToggled()&&timerStart!=0){boolean finished=timerEnd>0&&now>=timerEnd;double seconds=timerEnd>0?Math.max(0,(timerEnd-now)/1e9):(now-timerStart)/1e9;emit(new IslandEvent(finished?IslandEvent.Kind.Warning:IslandEvent.Kind.Timer,"timer",timerName,finished?"Time's up":time(seconds),"o",0xFF9B8CFF,finished?4:1,finished?4000:1100,true));if(finished)timerStart=0;}
            }
            boolean visible=m.present()&&(options.musicMode==IslandOptions.MusicMode.Always||options.musicMode==IslandOptions.MusicMode.SongChange&&now-songChanged<(long)(eventDuration.getInput()*1e9)||options.musicMode==IslandOptions.MusicMode.KeyPress&&now<showUntil);
            double hp=target==null?1:Math.max(0,Math.min(1,target.getHealth()/Math.max(1,target.getMaxHealth())));
            if(screen==null||screenWidth!=mc.displayWidth||screenHeight!=mc.displayHeight||screenScale!=mc.gameSettings.guiScale){screenWidth=mc.displayWidth;screenHeight=mc.displayHeight;screenScale=mc.gameSettings.guiScale;screen=new ScaledResolution(mc);}
            renderer.draw(mc,screen,options,options.lyricsOnly?null:queue.current(now),m,result,idleText,hp,visible||options.lyricsOnly&&m.present());
        }catch(RuntimeException|LinkageError failure){failed=true;IslandLog.once("Island renderer disabled safely",failure);disable();}
    }
    private static String time(double value){int total=(int)value;return total/60+":"+(total%60<10?"0":"")+total%60;}
    private void snapshot(){
        options.lyricsOnly=lyricsOnly.isToggled();
        options.idle=idle.getMode();options.scale=(float)scale.getInput();options.y=(float)y.getInput();options.maxWidth=(float)maxWidth.getInput();options.textSize=(float)textSize.getInput();options.speed=(float)speed.getInput();options.overshoot=(float)overshoot.getInput();options.eventSeconds=(float)eventDuration.getInput();options.lyricsDuration=(float)scroll.getInput();options.marquee=(float)marquee.getInput();
        options.accent=theme.isToggled()?PhantomHUD.visualAccent(false):color.getRGB();options.shadow=shadow.isToggled();options.glow=glow.isToggled();options.matchArt=matchArt.isToggled();options.art=albumArt.isToggled();options.lyrics=showLyrics.isToggled();options.karaoke=karaoke.isToggled();options.online=online.isToggled();options.dots=dots.isToggled();options.progress=progress.isToggled();options.lines=lines.getMode()==Lines.One?1:3;options.musicMode=musicMode.getMode();
    }
}
