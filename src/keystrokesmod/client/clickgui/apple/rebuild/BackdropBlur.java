package keystrokesmod.client.clickgui.apple.rebuild;

import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/** Cached quarter-resolution two-pass Gaussian frost; failure only disables this decoration. */
public final class BackdropBlur {
    private static final BackdropBlur INSTANCE = new BackdropBlur();
    private static final String VERTEX = "#version 120\n"
            + "varying vec2 uv; varying vec2 pos; void main(){pos=gl_Vertex.xy;uv=gl_MultiTexCoord0.xy;gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;}\n";
    private static final String FILTER = "#version 120\n"
            + "varying vec2 uv; uniform sampler2D uTexture; uniform vec2 uStep;"
            + "void main(){vec4 sum=vec4(0.0);float total=0.0;for(int i=-7;i<=7;i++){float r=float(i)*2.0;float w=exp(-r*r/98.0);sum+=texture2D(uTexture,uv+r*uStep)*w;total+=w;}gl_FragColor=sum/total;}\n";
    private static final String MASK = "#version 120\n"
            + "varying vec2 uv;varying vec2 pos;uniform sampler2D uTexture;uniform vec4 uRect;uniform float uRadius;uniform float uAlpha;"
            + "void main(){vec2 q=abs(pos-uRect.xy)-uRect.zw+vec2(uRadius);float d=length(max(q,vec2(0.0)))+min(max(q.x,q.y),0.0)-uRadius;if(d>=0.0)discard;float a=(1.0-smoothstep(-0.8,0.0,d))*uAlpha;vec3 c=texture2D(uTexture,uv).rgb;float gray=dot(c,vec3(0.2126,0.7152,0.0722));gl_FragColor=vec4(mix(vec3(gray),c,0.35)*a,a);}\n";
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private int source, textureA, textureB, framebuffer, filterProgram, maskProgram;
    private int filterTexture, filterStep, maskTexture, maskRect, maskRadius, maskAlpha;
    private int width, height, quarterW, quarterH;
    private boolean attempted, failed, ready, requested = true;
    private String failureReason = "";
    private BackdropBlur() { }
    public static BackdropBlur shared() { return INSTANCE; }
    public void invalidate() { requested = true; ready = false; }
    public boolean active() { return ready; }
    public String status() { return failed ? "fallback (" + failureReason + ")" : ready ? "active (GLSL 1.20)" : "pending capture"; }

    /** Called before the backdrop/window are painted, never once per ordinary GUI frame. */
    public void prepare() {
        if (failed) return;
        int w = GuiRenderContext.framebufferWidth(), h = GuiRenderContext.framebufferHeight();
        if (w <= 0 || h <= 0) return;
        if (w != width || h != height) { width = w; height = h; requested = true; ready = false; }
        if (!requested) return;
        if (!GLContext.getCapabilities().OpenGL20 || !GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            failureReason = "GLSL 1.20 or FBO unsupported"; failed = true; ready = false; return;
        }
        int oldUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int oldFramebuffer = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int oldMatrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        viewport.clear(); GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            if (!attempted) { attempted = true; filterProgram = program(FILTER); maskProgram = program(MASK);
                filterTexture = GL20.glGetUniformLocation(filterProgram, "uTexture"); filterStep = GL20.glGetUniformLocation(filterProgram, "uStep");
                maskTexture = GL20.glGetUniformLocation(maskProgram, "uTexture"); maskRect = GL20.glGetUniformLocation(maskProgram, "uRect");
                maskRadius = GL20.glGetUniformLocation(maskProgram, "uRadius"); maskAlpha = GL20.glGetUniformLocation(maskProgram, "uAlpha");
                framebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
                if (framebuffer == 0) throw new IllegalStateException("blur framebuffer allocation failed");
            }
            quarterW = Math.max(1, w / 4); quarterH = Math.max(1, h / 4);
            if (source != 0) { GL11.glDeleteTextures(source); GL11.glDeleteTextures(textureA); GL11.glDeleteTextures(textureB); }
            source = texture(w, h); textureA = texture(quarterW, quarterH); textureB = texture(quarterW, quarterH);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, source);
            // Copy the actual game framebuffer before the ClickGUI draws its first dim pixel.
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
            // A newly bound FBO can inherit GL_BACK as its draw/read buffer; that makes it
            // incomplete on drivers which do not automatically select COLOR_ATTACHMENT0.
            GL11.glDrawBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
            GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
            GL11.glViewport(0, 0, quarterW, quarterH);
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_SCISSOR_TEST); GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST); GL11.glDisable(GL11.GL_LIGHTING); GL11.glColorMask(true, true, true, true);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity(); GL11.glOrtho(0, 1, 0, 1, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
            try {
                attach(textureA); GL20.glUseProgram(0); GL11.glBindTexture(GL11.GL_TEXTURE_2D, source); quad();
                attach(textureB); filter(textureA, 1F / quarterW, 0F);
                attach(textureA); filter(textureB, 0F, 1F / quarterH);
                int error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) { GuiGlDebug.capturedError(error, "blur capture/filter"); throw new IllegalStateException("GL " + error + " during blur capture/filter"); }
            } finally {
                GL20.glUseProgram(0);
                GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
            }
            ready = true; requested = false;
        } catch (Throwable error) {
            String reason = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            failureReason = reason.length() > 70 ? reason.substring(0, 70) : reason;
            failed = true; ready = false;
        } finally {
            GL20.glUseProgram(oldProgram);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFramebuffer);
            GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            GL11.glMatrixMode(oldMatrix);
            GL11.glPopAttrib();
            GL13.glActiveTexture(oldUnit);
        }
    }

    /** Paints the same cached screen image with a rounded shader mask for each glass surface. */
    public void draw(float x, float y, float w, float h, float radius, float alpha) {
        if (!ready || alpha <= .001F || w <= 0F || h <= 0F) return;
        UiDraw.flushShapes();
        int oldUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);
        boolean quadOpen = false;
        try {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureA);
            GL20.glUseProgram(maskProgram);
            GL20.glUniform1i(maskTexture, 0);
            GL20.glUniform4f(maskRect, x + w * .5F, y + h * .5F, w * .5F, h * .5F);
            GL20.glUniform1f(maskRadius, radius); GL20.glUniform1f(maskAlpha, alpha * UiDraw.masterAlpha());
            float dw = GuiRenderContext.designWidth(), dh = GuiRenderContext.designHeight();
            GL11.glBegin(GL11.GL_QUADS); quadOpen = true;
            GL11.glTexCoord2f(x / dw, 1F - y / dh); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f((x + w) / dw, 1F - y / dh); GL11.glVertex2f(x + w, y);
            GL11.glTexCoord2f((x + w) / dw, 1F - (y + h) / dh); GL11.glVertex2f(x + w, y + h);
            GL11.glTexCoord2f(x / dw, 1F - (y + h) / dh); GL11.glVertex2f(x, y + h);
        } catch (Throwable error) {
            String reason = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            failureReason = reason.length() > 70 ? reason.substring(0, 70) : reason;
            failed = true; ready = false;
        } finally {
            if (quadOpen) GL11.glEnd();
            GL20.glUseProgram(0); GL11.glPopAttrib(); GL13.glActiveTexture(oldUnit);
        }
    }

    private void attach(int texture) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, texture, 0);
        if (EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT)
                != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) throw new IllegalStateException("blur framebuffer incomplete");
    }
    private void filter(int texture, float dx, float dy) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture); GL20.glUseProgram(filterProgram);
        GL20.glUniform1i(filterTexture, 0); GL20.glUniform2f(filterStep, dx, dy); quad();
    }
    private static void quad() {
        GL11.glColor4f(1F, 1F, 1F, 1F); GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0F, 0F); GL11.glVertex2f(0F, 0F);
        GL11.glTexCoord2f(1F, 0F); GL11.glVertex2f(1F, 0F);
        GL11.glTexCoord2f(1F, 1F); GL11.glVertex2f(1F, 1F);
        GL11.glTexCoord2f(0F, 1F); GL11.glVertex2f(0F, 1F); GL11.glEnd();
    }
    private static int texture(int w, int h) {
        int id = GL11.glGenTextures(); GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        return id;
    }
    private static int program(String fragment) {
        int vertex = compile(GL20.GL_VERTEX_SHADER, VERTEX), pixel = compile(GL20.GL_FRAGMENT_SHADER, fragment);
        int shader = GL20.glCreateProgram(); GL20.glAttachShader(shader, vertex); GL20.glAttachShader(shader, pixel);
        GL20.glLinkProgram(shader);
        if (GL20.glGetProgrami(shader, GL20.GL_LINK_STATUS) == 0) throw new IllegalStateException(GL20.glGetProgramInfoLog(shader, 4096));
        GL20.glDeleteShader(vertex); GL20.glDeleteShader(pixel); return shader;
    }
    private static int compile(int kind, String code) {
        int shader = GL20.glCreateShader(kind); GL20.glShaderSource(shader, code); GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) throw new IllegalStateException(GL20.glGetShaderInfoLog(shader, 4096));
        return shader;
    }
}
