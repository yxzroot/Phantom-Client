package keystrokesmod.client.clickgui.color;

import keystrokesmod.client.clickgui.apple.rebuild.GuiRenderContext;
import keystrokesmod.client.clickgui.apple.rebuild.UiDraw;
import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.main.Raven;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.*;
import org.lwjgl.input.*;
import org.lwjgl.opengl.*;

/** Modal overlay: never closes/recreates the parent GUI or loses its drawer/scroll position. */
public final class ColorPickerPopup {
    private static ColorPickerPopup active;private static boolean logged;
    private final GuiScreen owner;private final ColorPicker picker;private final AuroraGLState gl=new AuroraGLState();
    private final float anchorX,anchorY;private final boolean repeat;
    private ColorPickerPopup(GuiScreen owner,RGBSetting setting,float x,float y){this.owner=owner;picker=new ColorPicker(new ColorPickerState(setting));anchorX=x;anchorY=y;repeat=Keyboard.areRepeatEventsEnabled();}
    public static void open(GuiScreen owner,RGBSetting color,float x,float y){
        if(owner==null||color==null)return;if(active!=null)active.close(false);
        try{active=new ColorPickerPopup(owner,color,x,y);MinecraftForge.EVENT_BUS.register(active);Keyboard.enableRepeatEvents(true);}
        catch(RuntimeException|LinkageError error){if(active!=null)active.close(false);failure(error);}
    }
    private static void failure(Throwable e){if(!logged){logged=true;java.util.logging.Logger.getLogger("Phantom/ColorPicker").log(java.util.logging.Level.WARNING,"Color picker closed safely; original color restored",e);}}
    private void close(boolean select){
        if(active!=this)return;
        try{if(select)picker.state.select();else picker.state.cancel();}
        finally{active=null;MinecraftForge.EVENT_BUS.unregister(this);Keyboard.enableRepeatEvents(repeat);}
        try{if(Raven.configManager!=null)Raven.configManager.save();if(Raven.clientConfig!=null)Raven.clientConfig.saveConfig();}catch(RuntimeException|LinkageError e){failure(e);}
    }
    private void action(){if(picker.action!=0)close(picker.action>0);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void draw(GuiScreenEvent.DrawScreenEvent.Post event){
        if(event.gui!=owner){close(false);return;}Minecraft mc=Minecraft.getMinecraft();
        try{
            picker.layout(owner.width,owner.height,anchorX,anchorY);GuiRenderContext.update(mc);gl.begin();
            try{
                GL20.glUseProgram(0);GL13.glActiveTexture(GL13.GL_TEXTURE0);GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);UiDraw.beginFrame();
                try{
                    float sx=mc.displayWidth/(float)Math.max(1,owner.width),sy=mc.displayHeight/(float)Math.max(1,owner.height);
                    float left=Math.max(0,picker.x-24*picker.fit),top=Math.max(0,picker.y-24*picker.fit),right=Math.min(owner.width,picker.x+400*picker.fit),bottom=Math.min(owner.height,picker.y+424*picker.fit);
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor((int)Math.floor(left*sx),Math.max(0,(int)Math.floor(mc.displayHeight-bottom*sy)),Math.max(0,(int)Math.ceil((right-left)*sx)),Math.max(0,(int)Math.ceil((bottom-top)*sy)));
                    picker.draw(event.mouseX,event.mouseY);
                }finally{UiDraw.endFrame();}
            }finally{gl.end();GL11.glColor4f(1,1,1,1);GlStateManager.color(1,1,1,1);}
        }catch(RuntimeException|LinkageError e){close(false);failure(e);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void mouse(GuiScreenEvent.MouseInputEvent.Pre event){
        if(event.gui!=owner)return;event.setCanceled(true);
        try{
            Minecraft mc=Minecraft.getMinecraft();float x=Mouse.getEventX()*owner.width/(float)Math.max(1,mc.displayWidth),y=owner.height-Mouse.getEventY()*owner.height/(float)Math.max(1,mc.displayHeight)-1;
            int button=Mouse.getEventButton();
            if(button>=0){if(Mouse.getEventButtonState())picker.click(x,y,button);else picker.release();}
            else picker.drag(x,y,Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT),false);
            action();
        }catch(RuntimeException|LinkageError e){close(false);failure(e);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void key(GuiScreenEvent.KeyboardInputEvent.Pre event){
        if(event.gui!=owner)return;event.setCanceled(true);if(!Keyboard.getEventKeyState())return;
        try{boolean ctrl=GuiScreen.isCtrlKeyDown(),shift=GuiScreen.isShiftKeyDown();picker.key(Keyboard.getEventCharacter(),Keyboard.getEventKey(),ctrl,shift,ctrl&&Keyboard.getEventKey()==Keyboard.KEY_V?GuiScreen.getClipboardString():"");action();}
        catch(RuntimeException|LinkageError e){close(false);failure(e);}
    }
    @SubscribeEvent public void screen(GuiOpenEvent event){if(event.gui!=owner)close(false);}
}
