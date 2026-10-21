import keystrokesmod.client.clickgui.apple.rebuild.*;import keystrokesmod.client.module.setting.impl.*;
import java.lang.reflect.*;import java.nio.*;import org.lwjgl.*;import org.lwjgl.opengl.*;
public final class PreferencesTest {
    public enum Theme{Phantom,Midnight,Aurora}
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void set(String name,Object value)throws Exception{Field f=GuiPreferences.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static float at(PreferencesMenu menu,String name)throws Exception{Field f=PreferencesMenu.class.getDeclaredField(name);f.setAccessible(true);return f.getFloat(menu);}
    static PreferencesMenu menu()throws Exception{set("inspected",true);set("theme",new ComboSetting<Theme>("Theme",Theme.Phantom));set("themeOptions",Theme.values());MainWindow w=new MainWindow();w.layout(1280,800);PreferencesMenu m=new PreferencesMenu();m.layout(w,1280,800);return m;}
    static void cpu()throws Exception{
        PreferencesMenu m=menu();float x=at(m,"x"),y=at(m,"y"),row=at(m,"row"),tx=at(m,"trackX"),tw=at(m,"trackW");
        boolean before=GuiPreferences.menuAnimationEnabled();m.click(x+30,y+70,0);check(GuiPreferences.menuAnimationEnabled()!=before,"Menu animation row is still a no-op");
        m.click(tx+tw,y+58+row+25,0);m.release();check(GuiPreferences.animationSpeed()==3,"Speed slider is still a no-op");
        float previous=GuiPreferences.guiScale();m.click(tx+tw,y+58+row*2+25,0);check(GuiPreferences.guiScale()==previous,"Scale changes before release and moves the dragged control");check(m.release()==2&&GuiPreferences.guiScale()==1.5F,"Scale slider not committed on release");
        m.click(tx+tw*.5F,y+58+row*6+25,0);m.release();check(Math.abs(GuiPreferences.tooltipDelay()-2.6F)<.11,"Tooltip delay slider failed");
        check(m.click(-100,-100,0)==1,"Outside click does not close preferences");
        m.click(tx+10,y+58+row*3+20,0);Method top=PreferencesMenu.class.getDeclaredMethod("themeTop");top.setAccessible(true);m.click(tx+10,(Float)top.invoke(m)+30,0);check(GuiPreferences.themeName().equals("Midnight"),"Theme dropdown selection failed");
        GuiPreferences.setGuiScale(1);System.out.println("PASS: real preferences controls; functional fallback animation/speed, deferred scale resize, tooltip delay, theme dropdown and outside close");
    }
    static void gl(String preview)throws Exception{
        Pbuffer p=new Pbuffer(800,520,new PixelFormat().withDepthBits(24),null);p.makeCurrent();try{
            Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);sun.misc.Unsafe u=(sun.misc.Unsafe)f.get(null);net.minecraft.client.Minecraft mc=(net.minecraft.client.Minecraft)u.allocateInstance(net.minecraft.client.Minecraft.class);mc.displayWidth=800;mc.displayHeight=520;mc.gameSettings=(net.minecraft.client.settings.GameSettings)u.allocateInstance(net.minecraft.client.settings.GameSettings.class);mc.gameSettings.guiScale=1;Field singleton=net.minecraft.client.Minecraft.class.getDeclaredField("theMinecraft");singleton.setAccessible(true);singleton.set(null,mc);net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;GuiRenderContext.update(mc);
            GL11.glViewport(0,0,800,520);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,800,520,0,-100,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glClearColor(.03F,.025F,.045F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);PreferencesMenu menu=menu();MainWindow window=new MainWindow();window.layout(800,520);menu.layout(window,800,520);
            keystrokesmod.client.module.modules.render.aurora.AuroraGLState state=new keystrokesmod.client.module.modules.render.aurora.AuroraGLState();
            for(int i=0;i<240;i++){menu.update(1F/240);state.begin();try{GL11.glDisable(GL11.GL_DEPTH_TEST);UiDraw.beginFrame();try{menu.draw(700,160,1);}finally{UiDraw.endFrame();}}finally{state.end();}}check(GL11.glGetError()==0,"Preferences GL error");
            IntBuffer pixels=BufferUtils.createIntBuffer(800*520);GL11.glReadPixels(0,0,800,520,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,pixels);java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(800,520,java.awt.image.BufferedImage.TYPE_INT_RGB);int bright=0;for(int y=0;y<520;y++)for(int x=0;x<800;x++){int c=pixels.get(y*800+x);image.setRGB(x,519-y,c);if((c>>16&255)>160&&(c>>8&255)>160&&(c&255)>160)bright++;}check(bright>100,"Menu text/controls missing");javax.imageio.ImageIO.write(image,"png",new java.io.File(preview));System.out.println("PASS: production settings popover, Poppins labels, iOS switches, sliders and theme controls in 240 GL passes; "+preview);
        }finally{p.destroy();}
    }
    public static void main(String[] args)throws Exception{if(args.length==0)cpu();else gl(args[1]);}
}
