import com.google.gson.*;
import keystrokesmod.client.config.*;
import keystrokesmod.client.clickgui.apple.rebuild.*;
import java.nio.file.*;
import java.nio.*;
import java.lang.reflect.*;
import java.util.*;
import org.lwjgl.*;
import org.lwjgl.opengl.*;

/** Tests real profile persistence in a fresh temp folder and real Config-tab buttons in GL. */
public final class ConfigProfilesTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static final class State implements ConfigManager.State {
        int value=11;
        public JsonObject snapshot(){JsonObject data=new JsonObject();data.addProperty("value",value);return data;}
        public void apply(JsonObject data){if(data.has("reject")){value=-1;throw new IllegalArgumentException("Synthetic bad module");}value=data.get("value").getAsInt();}
    }
    static void cpu()throws Exception{
        Path folder=Files.createTempDirectory("phantom-profiles-test-");State state=new State();ConfigManager manager=new ConfigManager(folder.toFile(),state,false);
        check(manager.getConfig().file.length()>0&&manager.getConfigs().size()==1,"Fresh default file empty/invisible");
        Config first=manager.getConfigs().get(0);for(int i=0;i<500;i++)check(manager.getConfigs().get(0)==first,"Frame getter replaces profile identities");
        state.value=42;Config fresh=manager.createConfig("  My setup.BPLUS  ");check(fresh.getName().equals("My setup")&&manager.getConfigs().size()==2,"Create doesn't add named row");
        check(fresh.getData().getAsJsonObject("modules").get("value").getAsInt()==42,"Create copies stale file instead of live state");
        check(first.getData().getAsJsonObject("modules").get("value").getAsInt()==11,"Create overwrites original profile");
        state.value=99;manager.save(first);check(manager.getConfig()==fresh,"Saving another row changes active profile");
        check(first.getData().getAsJsonObject("modules").get("value").getAsInt()==99&&fresh.getData().getAsJsonObject("modules").get("value").getAsInt()==42,"Save writes wrong row");
        check(Files.exists(folder.resolve("default.bplus.previous")),"Overwrite lacks recoverable previous version");
        manager.setConfig(fresh);check(state.value==42,"Load doesn't apply stored values");
        ConfigManager restarted=new ConfigManager(folder.toFile(),state,false);state.value=0;restarted.loadConfigByName("MY SETUP.bplus");check(state.value==42,"Restart/case/extension load failed");
        for(String bad:new String[]{""," ","..","../escape","a/b","a\\b","x:y","CON","nul.txt","bad.","My setup"}){
            int count=manager.getConfigs().size();try{manager.createConfig(bad);throw new AssertionError("Invalid/duplicate name accepted: "+bad);}catch(IllegalArgumentException expected){}
            check(manager.getConfigs().size()==count,"Rejected name changes profile list");
        }
        Config unicode=manager.createConfig("Зима 星空");check(unicode.getData()!=null,"Unicode profile name/UTF-8 save failed");
        JsonObject legacyData=new JsonObject(),legacyModules=state.snapshot();legacyModules.addProperty("label","\u00A7bFrost \u2014 setup");legacyData.add("modules",legacyModules);
        Path ansi=folder.resolve("old-windows.bplus");Files.write(ansi,legacyData.toString().getBytes(java.nio.charset.Charset.forName("windows-1252")));
        byte[] ansiBefore=Files.readAllBytes(ansi);Config legacy=new Config(ansi.toFile());
        check(legacy.getData()!=null&&legacy.getData().getAsJsonObject("modules").get("label").getAsString().equals("\u00A7bFrost \u2014 setup"),"Old Windows-1252 color codes were rejected/corrupted");
        manager.discoverConfigs();check(manager.getConfigs().stream().anyMatch(c->c.getName().equals("old-windows")),"Legacy encoding hidden from config list");
        check(Arrays.equals(ansiBefore,Files.readAllBytes(ansi)),"Reading legacy encoding modifies its original bytes");
        Path utf16=folder.resolve("utf16.bplus");Files.write(utf16,("\uFEFF"+legacyData.toString()).getBytes(java.nio.charset.StandardCharsets.UTF_16LE));check(new Config(utf16.toFile()).getData()!=null,"UTF-16 BOM config rejected");
        legacy.save(legacy.getData());check(Arrays.equals(ansiBefore,Files.readAllBytes(folder.resolve("old-windows.bplus.previous"))),"Encoding conversion doesn't preserve original backup");
        java.nio.charset.StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(ansi)));
        Path broken=folder.resolve("broken.bplus");Files.write(broken,"{not json".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Config selected=manager.getConfig();int previous=state.value;
        try{manager.setConfig(new Config(broken.toFile()));throw new AssertionError("Broken file loaded");}catch(IllegalArgumentException expected){}
        check(manager.getConfig()==selected&&state.value==previous&&!ConfigManager.applyingConfig,"Bad load modifies active settings or leaves applying flag stuck");
        Config reject=new Config(folder.resolve("reject.bplus").toFile());JsonObject data=new JsonObject(),modules=new JsonObject();modules.addProperty("reject",true);data.add("modules",modules);reject.save(data);
        try{manager.setConfig(reject);throw new AssertionError("Failed apply not reported");}catch(IllegalStateException expected){}
        check(state.value==previous&&manager.getConfig()==selected&&!ConfigManager.applyingConfig,"Failed apply does not roll back");
        byte[] kept=Files.readAllBytes(unicode.file.toPath());manager.deleteConfig(unicode);
        check(state.value==previous&&manager.getConfig()!=unicode,"Deleting active profile resets live settings");
        boolean recovered=false;try(java.nio.file.DirectoryStream<Path> entries=Files.newDirectoryStream(folder,"*.deleted-*")){for(Path path:entries)if(Arrays.equals(Files.readAllBytes(path),kept))recovered=true;}
        check(recovered,"Deleted profile cannot be recovered");
        manager.discoverConfigs();for(Config item:manager.getConfigs())check(!item.getName().equals("broken"),"Malformed file included as loadable");
        Config nonexistent=new Config(folder.resolve("untouched.bplus").toFile());check(!nonexistent.file.exists(),"Reading config creates empty file");
        System.out.println("PASS: fresh default, Create/live values, stable rows, targeted Save/Load, restart, unsafe names, Windows-1252 section/color codes, UTF-8/UTF-16, read-only legacy discovery, original-byte backup on conversion, rollback and recoverable deletion; isolated folder "+folder);
    }
    static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    static float coordinate(Object owner,String name)throws Exception{return((Number)field(owner,name)).floatValue();}
    static void name(ConfigTab tab,String value)throws Exception{Object input=field(tab,"field");Field f=input.getClass().getDeclaredField("value");f.setAccessible(true);f.set(input,value);StringBuilder b=(StringBuilder)field(input,"input");b.setLength(0);b.append(value);}
    static final keystrokesmod.client.module.modules.render.aurora.AuroraGLState DRAW_STATE=new keystrokesmod.client.module.modules.render.aurora.AuroraGLState();
    static float drawHeight=550;
    static void draw(ConfigTab tab,Tooltip tip){
        GL11.glClearColor(.025F,.03F,.045F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);DRAW_STATE.begin();
        try{GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glScalef(GuiRenderContext.uiScale(),GuiRenderContext.uiScale(),1);UiDraw.beginFrame();try{tab.draw(0,0,1000,drawHeight,-100,-100,tip);}finally{UiDraw.endFrame();}}
        finally{DRAW_STATE.end();}int error=GL11.glGetError();check(error==0,"Config frame GL error: "+error);
    }
    static void settled(ConfigTab tab,Tooltip tip)throws Exception{
        draw(tab,tip);Map<?,?> visuals=(Map<?,?>)field(tab,"visuals");
        for(Object item:visuals.values()){((Anim)field(item,"reveal")).snapTo(1);Field f=item.getClass().getDeclaredField("showAt");f.setAccessible(true);f.setLong(item,0);}
        tab.update(.016F);draw(tab,tip);
    }
    static void click(ConfigTab tab,Object button){check(tab.click(coordinateUnchecked(button,"x")+8,coordinateUnchecked(button,"y")+8,0),"Button click wasn't routed");}
    static float coordinateUnchecked(Object owner,String name){try{return coordinate(owner,name);}catch(Exception e){throw new RuntimeException(e);}}
    static void gl(String preview)throws Exception{
        Pbuffer context=new Pbuffer(1000,550,new PixelFormat().withDepthBits(24),null);context.makeCurrent();
        try{
            Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)unsafeField.get(null);
            net.minecraft.client.Minecraft mc=(net.minecraft.client.Minecraft)unsafe.allocateInstance(net.minecraft.client.Minecraft.class);
            mc.displayWidth=1000;mc.displayHeight=550;Field directory=net.minecraft.client.Minecraft.class.getDeclaredField("mcDataDir");directory.setAccessible(true);directory.set(mc,Files.createTempDirectory("phantom-config-ui-").toFile());mc.gameSettings=(net.minecraft.client.settings.GameSettings)unsafe.allocateInstance(net.minecraft.client.settings.GameSettings.class);mc.gameSettings.guiScale=1;
            Field singleton=net.minecraft.client.Minecraft.class.getDeclaredField("theMinecraft");singleton.setAccessible(true);singleton.set(null,mc);
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;GuiRenderContext.update(mc);
            Field inspected=GuiPreferences.class.getDeclaredField("inspected");inspected.setAccessible(true);inspected.setBoolean(null,true);
            State state=new State();ConfigManager manager=new ConfigManager(new java.io.File(mc.mcDataDir,"profiles"),state,false);keystrokesmod.client.main.Raven.configManager=manager;
            GL11.glViewport(0,0,1000,550);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,1000,550,0,-100,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glDisable(GL11.GL_DEPTH_TEST);
            UiDraw.prepareResources();ConfigTab tab=new ConfigTab();Tooltip tip=new Tooltip();tab.onTabOpened();settled(tab,tip);
            name(tab,"Fresh");state.value=24;click(tab,field(tab,"create"));check(manager.getConfigs().size()==2&&manager.getConfig().getName().equals("Fresh"),"Actual Create button failed");
            Config fresh=manager.getConfig(),original=manager.getConfigs().get(0);settled(tab,tip);
            Map<?,?> visuals=(Map<?,?>)field(tab,"visuals");Object visual=visuals.get(original);
            for(int i=0;i<100;i++){tab.update(.016F);draw(tab,tip);}check(((Map<?,?>)field(tab,"visuals")).size()==2&&visuals.get(original)==visual,"Rows recreated every draw / never become clickable");
            state.value=78;click(tab,field(visual,"save"));check(original.getData().getAsJsonObject("modules").get("value").getAsInt()==78,"Actual row Save failed");
            check(fresh.getData().getAsJsonObject("modules").get("value").getAsInt()==24,"Save overwrote active instead of clicked row");
            settled(tab,tip);visual=((Map<?,?>)field(tab,"visuals")).get(original);state.value=0;click(tab,field(visual,"load"));check(state.value==78,"Actual Load button failed");
            settled(tab,tip);visual=((Map<?,?>)field(tab,"visuals")).get(fresh);click(tab,field(visual,"delete"));settled(tab,tip);click(tab,field(visual,"cancel"));check(fresh.file.exists(),"Cancel deletes profile");
            settled(tab,tip);click(tab,field(visual,"delete"));settled(tab,tip);click(tab,field(visual,"confirm"));for(int i=0;i<30;i++)tab.update(.1F);check(!fresh.file.exists()&&manager.getConfigs().size()==1,"Confirmed Delete does not finish");
            settled(tab,tip);name(tab,"../bad");click(tab,field(tab,"create"));check(!((String)field(tab,"validation")).isEmpty()&&manager.getConfigs().size()==1,"Create error not shown inline");
            name(tab,"Fresh");click(tab,field(tab,"create"));settled(tab,tip);check(manager.getConfigs().size()==2,"Create unusable after error/delete");
            scrollRegression(manager,tab,tip);
            IntBuffer pixels=BufferUtils.createIntBuffer(1000*550);GL11.glReadPixels(0,0,1000,550,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,pixels);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(1000,550,java.awt.image.BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<550;y++)for(int x=0;x<1000;x++)image.setRGB(x,549-y,pixels.get(y*1000+x));javax.imageio.ImageIO.write(image,"png",new java.io.File(preview));
            int error=GL11.glGetError();check(error==0,"Config tab readback GL error: "+error);System.out.println("PASS: actual Config-tab Create / row Save / Load / Delete / Cancel / error recovery buttons, stable revealed rows over 100 GL frames; "+preview);
        }finally{context.destroy();}
    }
    static void scrollRegression(ConfigManager manager,ConfigTab tab,Tooltip tip)throws Exception{
        for(int i=0;i<22;i++)manager.createConfig(String.format("Scroll %02d",i));
        drawHeight=352;tab.onTabOpened();settled(tab,tip);ScrollView scroll=(ScrollView)field(tab,"scroll");
        check(scroll.max()>1000,"Overflow configs did not create scroll range");scroll.restore(0);
        tab.wheel(-1);for(int i=0;i<60;i++){tab.update(1F/60);draw(tab,tip);}
        check(scroll.position()>=40,"One wheel notch barely moves the config list: "+scroll.position());
        tab.wheel(-100);for(int i=0;i<60;i++){tab.update(1F/60);draw(tab,tip);}
        check(Math.abs(scroll.position()-scroll.max())<1,"Wheel cannot reach last config");
        Config last=manager.getConfigs().get(manager.getConfigs().size()-1);Object lastVisual=((Map<?,?>)field(tab,"visuals")).get(last);
        Object load=field(lastVisual,"load");float bottom=coordinate(load,"y")+34F;
        check(bottom<drawHeight,"Last row action remains clipped at bottom: "+bottom);click(tab,load);
        check(manager.getConfig()==last,"Last row cannot be loaded after scrolling");
        check(tab.key('\0',org.lwjgl.input.Keyboard.KEY_HOME),"Home not handled");for(int i=0;i<60;i++)tab.update(.016F);draw(tab,tip);check(scroll.position()<1,"Home cannot reach first config");
        check(tab.key('\0',org.lwjgl.input.Keyboard.KEY_NEXT),"Page Down not handled");check(scroll.target()>100,"Page Down does not move configs");
        check(tab.key('\0',org.lwjgl.input.Keyboard.KEY_END),"End not handled");for(int i=0;i<60;i++)tab.update(.016F);draw(tab,tip);
        check(Math.abs(scroll.position()-scroll.max())<1,"End cannot reach last config");
        // Reopening/restoring must not overwrite wheel input during the entrance animation.
        GuiPreferences.setScroll(7,0);tab.onTabOpened();draw(tab,tip);tab.wheel(-3);settled(tab,tip);
        for(int i=0;i<60;i++){tab.update(.016F);draw(tab,tip);}check(scroll.position()>=100,"Entrance restore erased wheel input");
        float barX=coordinate(tab,"x")+coordinate(tab,"width")+11,barY=coordinate(tab,"listTop")+25;
        scroll.restore(0);draw(tab,tip);check(tab.click(barX,barY+5,0),"Config scrollbar click not routed");
        check(scroll.dragging(),"Config scrollbar cannot start dragging");
        tab.getClass().getMethod("drag",float.class).invoke(tab,drawHeight-2);draw(tab,tip);
        check(Math.abs(scroll.position()-scroll.max())<1,"Scrollbar drag cannot reach bottom");
        tab.getClass().getMethod("release").invoke(tab);check(!scroll.dragging(),"Scrollbar stays grabbed on release");
        drawHeight=220;draw(tab,tip);tab.key('\0',org.lwjgl.input.Keyboard.KEY_END);for(int i=0;i<60;i++)tab.update(.016F);draw(tab,tip);
        check(Math.abs(scroll.position()-scroll.max())<1,"Small viewport cannot reach bottom");
        drawHeight=352;draw(tab,tip);check(scroll.target()<=scroll.max(),"Resize leaves scroll out of bounds");
        tab.saveScroll();float saved=scroll.target();tab.onTabOpened();settled(tab,tip);check(Math.abs(scroll.target()-saved)<1,"Config scroll position not restored");
        rootRouting(tab,scroll,barX,barY);
        check(!ConfigManager.applyingConfig,"Scrolling changed config apply state");
        System.out.println("PASS: 24 overflowing configs, useful wheel distance, first/last row access, keyboard navigation, entrance input, scrollbar drag/release, resize and saved scroll position");
    }
    static void set(Object owner,String name,Object value)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    static void rootRouting(ConfigTab tab,ScrollView scroll,float barX,float barY)throws Exception{
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        keystrokesmod.client.module.ModuleManager modules=(keystrokesmod.client.module.ModuleManager)unsafe.allocateInstance(keystrokesmod.client.module.ModuleManager.class);
        set(modules,"modules",new ArrayList<keystrokesmod.client.module.Module>());keystrokesmod.client.main.Raven.moduleManager=modules;
        GuiRoot root=new GuiRoot(null);set(root,"configs",tab);set(root,"state",GuiRoot.OPEN);set(root,"mainTab",1);
        MainWindow window=(MainWindow)field(root,"window");window.layout(1600,1000,352);window.setProgress(1);root.init(1600,1000);
        // Put the already-rendered tab's scrollbar inside the root window. Its draw
        // remains isolated above; these calls exercise the actual screen input routing.
        set(window,"width",1100F);set(window,"height",600F);((Anim)field(window,"x")).snapTo(0);((Anim)field(window,"y")).snapTo(0);
        scroll.restore(0);root.wheel(-1,barX-100,barY+30);check(scroll.target()>=40,"Root swallows Config wheel input");
        set(window,"width",1100F);set(window,"height",600F);
        scroll.restore(0);root.mouseClicked(barX,barY+5,0);check(scroll.dragging(),"Root does not route Config scrollbar click");
        root.mouseDragged(barX,drawHeight-2,0);check(Math.abs(scroll.position()-scroll.max())<1,"Root does not route Config scrollbar drag");
        root.mouseReleased(barX,drawHeight-2,0);check(!scroll.dragging(),"Root does not release Config scrollbar");
        root.key('\0',org.lwjgl.input.Keyboard.KEY_HOME);check(scroll.target()==0,"Root does not route Config keyboard scroll");
        System.out.println("PASS: actual GuiRoot wheel/click/drag/release/keyboard routing to Config tab");
    }
    static void audit(String directory){
        java.io.File[] files=new java.io.File(directory).listFiles();check(files!=null,"Audit folder missing");int count=0;
        for(java.io.File file:files)if(file.getName().endsWith(".bplus")){JsonObject data=new Config(file).getData();check(data!=null&&data.has("modules")&&data.get("modules").isJsonObject(),"Real profile unreadable: "+file.getName());System.out.println("PASS read-only real profile: "+file.getName()+", "+data.getAsJsonObject("modules").entrySet().size()+" modules");count++;}
        check(count>=3,"Expected original, test and preserved profiles");
    }
    public static void main(String[] args)throws Exception{if(args.length==0)cpu();else if(args[0].equals("audit"))audit(args[1]);else gl(args[1]);}
}
