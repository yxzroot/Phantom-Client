import keystrokesmod.client.clickgui.color.*;
import keystrokesmod.client.module.setting.impl.*;
import keystrokesmod.client.clickgui.apple.rebuild.*;
import keystrokesmod.client.module.modules.render.aurora.AuroraGLState;
import java.nio.*;import org.lwjgl.*;import org.lwjgl.opengl.*;

public final class SharedPickerTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void cpu(){
        RGBSetting rgb=new RGBSetting("Accent",110,168,254);rgb.setColors(new int[]{255,12,90});check(rgb.getRGB()==0xFFFF0C5A,"Bulk RGB cache remains stale");rgb.getColors()[1]=80;check(rgb.getGreen()==80&&(rgb.getRGB()>>8&255)==80,"Direct legacy channel edits not reflected");
        rgb.resetToDefaults();check(rgb.getRGB()==0xFF6EA8FE,"Reset overruns or fails");
        String old="{\"type\":\"rgbsetting\",\"red\":10,\"green\":220,\"blue\":99}";rgb.applyConfigFromJson(new com.google.gson.JsonParser().parse(old).getAsJsonObject());check(rgb.getRGB()==0xFF0ADC63,"Old color config incompatible");
        ColorPickerState s=new ColorPickerState(rgb);s.hsv(.76F,0,0);s.hsv(s.hue,1,1);check(Math.abs(s.hue-.76F)<.001,"Hue lost through black/gray");
        check(s.hex("#112233")&&rgb.getRGB()==0xFF112233,"HEX live preview failed");check(!s.hex("GGFFFF")&&!s.hex("11223300"),"Invalid/unsupported alpha accepted");s.cancel();check(rgb.getRGB()==0xFF0ADC63,"Cancel did not restore old config color");
        SliderSetting opacity=new SliderSetting("Opacity",65,0,100,1);PickerRGBSetting alpha=new PickerRGBSetting("Outline",20,30,40,opacity);s=new ColorPickerState(alpha);
        check(s.hex("AABBCC80")&&alpha.getRGB()==0xFFAABBCC&&Math.abs(opacity.getInput()-50)<=1,"RRGGBBAA order or opacity wrong");s.cancel();check(opacity.getInput()==65&&alpha.getRGB()==0xFF141E28,"Cancel opacity/color failed");
        s=new ColorPickerState(alpha);s.hex("00FF0080");s.select();s.cancel();check(alpha.getRGB()==0xFF00FF00,"Select not retained");
        RGBSetting restored=new RGBSetting("Restored",0,0,0);restored.applyConfigFromJson(alpha.getConfigAsJson());check(restored.getRGB()==alpha.getRGB(),"Config codec not preserved");
        ColorPicker p=new ColorPicker(new ColorPickerState(rgb));p.click(42+216,92,0);check(p.state.saturation==1&&p.state.value==1,"HSV drag start failed");p.drag(-100,700,false,false);check(p.state.saturation==0&&p.state.value==0,"Drag leaving control not clamped");p.release();
        p.click(250,55,0);check(p.tab==2,"HEX tab not selected");p.click(100,140,0);p.key('v',org.lwjgl.input.Keyboard.KEY_V,true,false,"#123456");check(rgb.getRGB()==0xFF123456,"Clipboard HEX edit failed");p.key('a',org.lwjgl.input.Keyboard.KEY_A,true,false,"");p.key('v',org.lwjgl.input.Keyboard.KEY_V,true,false,"#xyz");check(rgb.getRGB()==0xFF123456,"Invalid HEX mutated live color");p.key('\n',org.lwjgl.input.Keyboard.KEY_RETURN,false,false,"");check(p.action==0,"Invalid HEX confirmed");p.key((char)27,org.lwjgl.input.Keyboard.KEY_ESCAPE,false,false,"");check(p.action==-1,"Escape does not cancel");p.state.cancel();
        for(int w:new int[]{320,640,1920})for(int h:new int[]{240,360,1080}){p.layout(w,h,w-2,h-2);check(p.x>=0&&p.y>=0&&p.x+376*p.fit<=w&&p.y+400*p.fit<=h,"Popup outside scaled window");}
        check(rgb.getRavenComponentType().getName().endsWith("RGBComponent")&&rgb.getComponentType().getName().endsWith("KvRgbComponent"),"Future RGB setting not routed");
        System.out.println("PASS: real legacy RGB/PickerRGB codecs, cached-color repair, HSV hue retention, alpha/HEX validation, live Select/Cancel, drag outside, clipboard, keyboard, scaling/anchors and future setting adapters");
    }
    static void gl(String preview)throws Exception{
        Pbuffer context=new Pbuffer(800,440,new PixelFormat().withDepthBits(24),null);context.makeCurrent();
        try{
            java.lang.reflect.Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)f.get(null);
            net.minecraft.client.Minecraft mc=(net.minecraft.client.Minecraft)unsafe.allocateInstance(net.minecraft.client.Minecraft.class);mc.displayWidth=800;mc.displayHeight=440;mc.gameSettings=(net.minecraft.client.settings.GameSettings)unsafe.allocateInstance(net.minecraft.client.settings.GameSettings.class);mc.gameSettings.guiScale=1;
            java.lang.reflect.Field singleton=net.minecraft.client.Minecraft.class.getDeclaredField("theMinecraft");singleton.setAccessible(true);singleton.set(null,mc);
            net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit=GL13.GL_TEXTURE0;net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit=GL13.GL_TEXTURE1;GuiRenderContext.update(mc);
            GL11.glViewport(0,0,800,440);GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glLoadIdentity();GL11.glOrtho(0,800,440,0,-100,100);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();
            int tex=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(0,0,800,440);GL11.glShadeModel(GL11.GL_FLAT);
            AuroraGLState state=new AuroraGLState();ColorPicker p=new ColorPicker(new ColorPickerState(new PickerRGBSetting("Phantom accent",154,101,238,new SliderSetting("Opacity",80,0,100,1))));p.layout(800,440,395,40);
            java.lang.reflect.Field opened=ColorPicker.class.getDeclaredField("opened");opened.setAccessible(true);opened.setLong(p,System.nanoTime()-1000000000L);
            java.awt.image.BufferedImage sheet=new java.awt.image.BufferedImage(800*3,440,java.awt.image.BufferedImage.TYPE_INT_RGB);
            for(int tab=0;tab<3;tab++){
                p.tab=tab;java.lang.reflect.Field switched=ColorPicker.class.getDeclaredField("switched");switched.setAccessible(true);switched.setLong(p,System.nanoTime()-1000000000L);
                GL11.glClearColor(.035F,.03F,.05F,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);state.begin();try{UiDraw.beginFrame();try{p.draw(100,180);}finally{UiDraw.endFrame();}}finally{state.end();}
                check(GL11.glGetError()==0,"GL failure on tab "+tab);check(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)&&GL11.glIsEnabled(GL11.GL_LIGHTING)&&GL11.glIsEnabled(GL11.GL_DEPTH_TEST)&&GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)&&GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),"GL enables/depth/scissor leaked");check(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==tex&&GL11.glGetInteger(GL11.GL_SHADE_MODEL)==GL11.GL_FLAT,"Texture/shading leaked");
                IntBuffer data=BufferUtils.createIntBuffer(800*440);GL11.glReadPixels(0,0,800,440,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,data);int bright=0;for(int y=0;y<440;y++)for(int x=0;x<800;x++){int c=data.get(y*800+x);sheet.setRGB(tab*800+x,439-y,c);if((c>>16&255)>160&&(c>>8&255)>160&&(c&255)>160)bright++;}check(bright>100,"Missing labels/handles on tab "+tab);
            }
            javax.imageio.ImageIO.write(sheet,"png",new java.io.File(preview));
            for(int i=0;i<240;i++){state.begin();try{UiDraw.beginFrame();try{p.draw(200,200);}finally{UiDraw.endFrame();}}finally{state.end();}}check(GL11.glGetError()==0,"240-pass picker state failure");GL11.glDeleteTextures(tex);
            System.out.println("PASS: real three-tab production picker pixels, Poppins fonts, vertex gradients/rounded mask, alpha checkerboard, controls and 240 simulated draws with GL restore; preview "+preview);
        }finally{context.destroy();}
    }
    public static void main(String[] args)throws Exception{if(args.length>0)gl(args[1]);else cpu();}
}
