package keystrokesmod.client.clickgui.apple.rebuild;

import java.nio.FloatBuffer;
import keystrokesmod.client.utils.font.PhantomDiagnostics;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/**
 * Batched GLSL 1.20 signed-distance shape renderer. A failed program is harmless: UiDraw keeps
 * its legacy path available for that session. Every submitted primitive is a single padded quad.
 */
final class SdfRenderer {
    private static final int QUADS = 2048, VERTICES_PER_QUAD = 4, FLOATS_PER_VERTEX = 26;
    private static final int STRIDE = FLOATS_PER_VERTEX * 4;
    private static final int TYPE_ROUND = 0, TYPE_CIRCLE = 1, TYPE_LINE = 2, TYPE_STAR = 3, TYPE_GLOW = 4, TYPE_HORIZONTAL = 5;
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);
    private static final String VERTEX =
            "#version 120\n"
            + "attribute vec4 aColorA; attribute vec4 aColorB; attribute vec4 aData0; attribute vec4 aData1; attribute vec4 aBorder; attribute vec4 aShadow;\n"
            + "varying vec2 vWorld; varying vec4 vColorA; varying vec4 vColorB; varying vec4 vData0; varying vec4 vData1; varying vec4 vBorder; varying vec4 vShadow;\n"
            + "void main(){ gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex; vWorld=gl_Vertex.xy; vColorA=aColorA; vColorB=aColorB; vData0=aData0; vData1=aData1; vBorder=aBorder; vShadow=aShadow; }\n";
    private static final String FRAGMENT =
            "#version 120\n"
            + "uniform float uPixel;\n"
            + "varying vec2 vWorld; varying vec4 vColorA; varying vec4 vColorB; varying vec4 vData0; varying vec4 vData1; varying vec4 vBorder; varying vec4 vShadow;\n"
            + "float sdRound(vec2 p, vec2 b, float r){ vec2 q=abs(p)-b+vec2(r); return length(max(q,vec2(0.0)))+min(max(q.x,q.y),0.0)-r; }\n"
            + "float sdSegment(vec2 p, vec2 a, vec2 b, float r){ vec2 ba=b-a; float h=clamp(dot(p-a,ba)/max(dot(ba,ba),0.0001),0.0,1.0); return length(p-a-ba*h)-r; }\n"
            // The prior analytic star used k2 with the wrong Y sign. That folds half of the
            // polygon into a triangle on some drivers. This fixed-ten-edge distance field is
            // deliberately explicit: point 0 is screen-up, the inner radius is exactly .42R,
            // and the small offset produces visually rounded 5-point corners.
            + "vec2 starPoint(float i,float ro,float ri){ float a=1.570796327+i*0.628318531; float rr=mod(i,2.0)<1.0?ro:ri; return vec2(cos(a),sin(a))*rr; }\n"
            + "float sdStar(vec2 p,float r,float ri){ float corner=r*0.08; float ro=max(0.01,r-corner); float rin=max(0.01,ri+corner*0.35); float md=1.0e20; float inside=0.0; for(int i=0;i<10;i++){ vec2 a=starPoint(float(i),ro,rin); vec2 b=starPoint(mod(float(i+1),10.0),ro,rin); vec2 e=b-a; vec2 q=p-a; float h=clamp(dot(q,e)/max(dot(e,e),0.0001),0.0,1.0); md=min(md,dot(q-e*h,q-e*h)); if((a.y>p.y)!=(b.y>p.y)){ float hit=(b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x; if(p.x<hit) inside=1.0-inside; } } float d=sqrt(md); return (inside>0.5?-d:d)-corner; }\n"
            + "float distAt(vec2 w){ float t=floor(vData1.w+0.5); if(t==1.0) return length(w-vData0.xy)-vData0.z; if(t==2.0) return sdSegment(w,vData0.xy,vData0.zw,vData1.x); if(t==3.0) return sdStar(vec2(w.x-vData0.x,vData0.y-w.y),vData0.z,vData0.w); return sdRound(w-vData0.xy,vData0.zw,vData1.x); }\n"
            + "void main(){ float t=floor(vData1.w+0.5); if(t==4.0){ float rr=max(vData0.z,0.001); float d=length(vWorld-vData0.xy); float a=exp(-2.7*d*d/(rr*rr))*vColorA.a; gl_FragColor=vec4(vColorA.rgb*a,a); return; } float d=distAt(vWorld); float aa=max(uPixel,0.35); float cover=1.0-smoothstep(-aa,aa,d); float insideStroke=smoothstep(-vData1.y-aa,-vData1.y+aa,d); float gradient=(t==5.0)?clamp((vWorld.x-(vData0.x-vData0.z))/(2.0*vData0.z),0.0,1.0):clamp((vWorld.y-(vData0.y-vData0.w))/(2.0*vData0.w),0.0,1.0); vec4 fill=mix(vColorA,vColorB,gradient); float edgeAlpha=mix(fill.a,vBorder.a,insideStroke)*cover; vec3 edgeRgb=mix(fill.rgb,vBorder.rgb,insideStroke); float sd=max(distAt(vWorld-vec2(0.0,vData1.z*0.2)),0.0); float outside=smoothstep(-aa,aa,sd); float shadow=(1.0-smoothstep(0.0,max(vData1.z,aa),sd))*outside*vShadow.a; float alpha=edgeAlpha+shadow*(1.0-edgeAlpha); vec3 rgb=edgeRgb*edgeAlpha+vShadow.rgb*shadow*(1.0-edgeAlpha); gl_FragColor=vec4(rgb,alpha); }\n";

    private static boolean attempted, active, inBatch;
    private static int program, vbo, pixelUniform;
    private static int colorA, colorB, data0, data1, border, shadow;
    private static int vertices;

    private SdfRenderer() { }

    static void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            if (!GLContext.getCapabilities().OpenGL20 || !GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
                PhantomDiagnostics.log("GUI SDF disabled: OpenGL 2.0/VBO unavailable; legacy AA fallback selected");
                return;
            }
            int vert = shader(GL20.GL_VERTEX_SHADER, VERTEX);
            int frag = shader(GL20.GL_FRAGMENT_SHADER, FRAGMENT);
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vert); GL20.glAttachShader(program, frag);
            // gl_Vertex occupies generic attribute zero in the compatibility profile. Leaving
            // these locations driver-assigned can overwrite that pointer (the old cause of
            // selectively invisible grid/header primitives on some Lunar drivers).
            GL20.glBindAttribLocation(program, 1, "aColorA");
            GL20.glBindAttribLocation(program, 2, "aColorB");
            GL20.glBindAttribLocation(program, 3, "aData0");
            GL20.glBindAttribLocation(program, 4, "aData1");
            GL20.glBindAttribLocation(program, 5, "aBorder");
            GL20.glBindAttribLocation(program, 6, "aShadow");
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) throw new IllegalStateException(GL20.glGetProgramInfoLog(program, 4096));
            GL20.glDeleteShader(vert); GL20.glDeleteShader(frag);
            colorA = attribute("aColorA"); colorB = attribute("aColorB"); data0 = attribute("aData0"); data1 = attribute("aData1"); border = attribute("aBorder"); shadow = attribute("aShadow");
            pixelUniform = GL20.glGetUniformLocation(program, "uPixel");
            vbo = GL15.glGenBuffers();
            active = vbo != 0;
            PhantomDiagnostics.log(active ? "GUI SDF active (GLSL 1.20, streamed VBO)" : "GUI SDF disabled: VBO allocation failed; legacy AA fallback selected");
        } catch (Throwable error) {
            active = false;
            if (program != 0) try { GL20.glDeleteProgram(program); } catch (Throwable ignored) { }
            PhantomDiagnostics.failure("GUI SDF initialization", error);
            PhantomDiagnostics.log("GUI SDF disabled after shader failure; legacy AA fallback selected");
        }
    }

    static boolean active() { return active; }
    static boolean pending() { return inBatch && vertices != 0; }
    static String status() { return active ? "SDF GLSL 1.20" : attempted ? "legacy AA fallback" : "initializing"; }
    static void beginFrame() { vertices = 0; inBatch = false; BUFFER.clear(); }
    static void endFrame() { flush(); }

    static void rounded(float left, float top, float right, float bottom, float radius, int topColor, int bottomColor,
                        int borderColor, float borderWidth, int shadowColor, float shadowBlur, boolean horizontal) {
        submit(left, top, right, bottom, TYPE_ROUND + (horizontal ? TYPE_HORIZONTAL : 0),
                (left + right) * .5F, (top + bottom) * .5F, Math.max(0F, (right - left) * .5F), Math.max(0F, (bottom - top) * .5F),
                radius, borderWidth, shadowBlur, topColor, bottomColor, borderColor, shadowColor);
    }

    static void circle(float x, float y, float radius, int topColor, int bottomColor, int borderColor, float borderWidth, int shadowColor, float shadowBlur) {
        submit(x - radius, y - radius, x + radius, y + radius, TYPE_CIRCLE, x, y, radius, radius,
                0F, borderWidth, shadowBlur, topColor, bottomColor, borderColor, shadowColor);
    }

    static void line(float x0, float y0, float x1, float y1, float width, int color) {
        float half = Math.max(.5F, width * .5F), left = Math.min(x0, x1) - half, top = Math.min(y0, y1) - half;
        float right = Math.max(x0, x1) + half, bottom = Math.max(y0, y1) + half;
        submit(left, top, right, bottom, TYPE_LINE, x0, y0, x1, y1, half, 0F, 0F, color, color, 0, 0);
    }

    static void star(float x, float y, float outer, float inner, int color, boolean filled) {
        submit(x - outer, y - outer, x + outer, y + outer, TYPE_STAR, x, y, outer, outer * .42F,
                 0F, filled ? 0F : Math.max(1.5F * GuiRenderContext.inverseUiScale(), 1.5F), 0F,
                filled ? color : 0, filled ? color : 0, color, 0);
    }

    static void glow(float x, float y, float radius, int color) {
        submit(x - radius, y - radius, x + radius, y + radius, TYPE_GLOW, x, y, radius, radius,
                0F, 0F, 0F, color, color, 0, 0);
    }

    private static void submit(float left, float top, float right, float bottom, int type,
                               float a0, float a1, float a2, float a3, float radius, float borderWidth, float shadowBlur,
                               int topColor, int bottomColor, int borderColor, int shadowColor) {
        if (!active || right <= left || bottom <= top) return;
        // An atlas GL_QUADS run may still be open. GL calls such as glBindBuffer,
        // glUseProgram and glDrawArrays are INVALID_OPERATION between glBegin/glEnd.
        UiDraw.flushText();
        float pad = Math.max(1F, shadowBlur + Math.max(borderWidth, GuiRenderContext.inverseUiScale()) * 2F);
        if (vertices + VERTICES_PER_QUAD > QUADS * VERTICES_PER_QUAD) flush();
        put(left - pad, top - pad, a0, a1, a2, a3, radius, borderWidth, shadowBlur, type, topColor, bottomColor, borderColor, shadowColor);
        put(right + pad, top - pad, a0, a1, a2, a3, radius, borderWidth, shadowBlur, type, topColor, bottomColor, borderColor, shadowColor);
        put(right + pad, bottom + pad, a0, a1, a2, a3, radius, borderWidth, shadowBlur, type, topColor, bottomColor, borderColor, shadowColor);
        put(left - pad, bottom + pad, a0, a1, a2, a3, radius, borderWidth, shadowBlur, type, topColor, bottomColor, borderColor, shadowColor);
        vertices += VERTICES_PER_QUAD;
        inBatch = true;
    }

    private static void put(float x, float y, float a0, float a1, float a2, float a3, float radius, float borderWidth, float shadowBlur, int type,
                            int topColor, int bottomColor, int borderColor, int shadowColor) {
        BUFFER.put(x).put(y);
        color(topColor); color(bottomColor);
        BUFFER.put(a0).put(a1).put(a2).put(a3);
        BUFFER.put(radius).put(borderWidth).put(shadowBlur).put(type);
        color(borderColor); color(shadowColor);
    }

    private static void color(int color) {
        BUFFER.put((color >>> 16 & 255) / 255F).put((color >>> 8 & 255) / 255F).put((color & 255) / 255F).put((color >>> 24 & 255) / 255F);
    }

    static void flush() {
        if (!active || !inBatch || vertices == 0) return;
        try {
            BUFFER.flip();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableTexture2D(); GlStateManager.disableDepth(); GlStateManager.disableCull();
            GL20.glUseProgram(program);
            GL20.glUniform1f(pixelUniform, GuiRenderContext.inverseUiScale() / Math.max(.01F, UiDraw.renderTransformScale()));
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, BUFFER, GL15.GL_STREAM_DRAW);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glVertexPointer(2, GL11.GL_FLOAT, STRIDE, 0L);
            pointer(colorA, 2 * 4L); pointer(colorB, 6 * 4L); pointer(data0, 10 * 4L); pointer(data1, 14 * 4L); pointer(border, 18 * 4L); pointer(shadow, 22 * 4L);
            GL11.glDrawArrays(GL11.GL_QUADS, 0, vertices);
            UiDraw.recordShapeDraw();
        } catch (Throwable error) {
            // A driver error must disable only the enhanced path, not the ClickGUI.
            active = false;
            PhantomDiagnostics.failure("GUI SDF draw", error);
        } finally {
            try {
                disable(colorA); disable(colorB); disable(data0); disable(data1); disable(border); disable(shadow);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL20.glUseProgram(0);
                GlStateManager.enableTexture2D(); GlStateManager.enableDepth(); GlStateManager.enableCull();
                GlStateManager.resetColor();
            } catch (Throwable ignored) { }
            BUFFER.clear(); vertices = 0; inBatch = false;
        }
    }

    private static void pointer(int attribute, long offset) { GL20.glEnableVertexAttribArray(attribute); GL20.glVertexAttribPointer(attribute, 4, GL11.GL_FLOAT, false, STRIDE, offset); }
    private static void disable(int attribute) { if (attribute >= 0) GL20.glDisableVertexAttribArray(attribute); }
    private static int attribute(String name) {
        int value = GL20.glGetAttribLocation(program, name);
        if (value < 0) throw new IllegalStateException("SDF attribute optimized out: " + name);
        return value;
    }
    private static int shader(int type, String source) {
        int value = GL20.glCreateShader(type);
        GL20.glShaderSource(value, source); GL20.glCompileShader(value);
        if (GL20.glGetShaderi(value, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) throw new IllegalStateException(GL20.glGetShaderInfoLog(value, 4096));
        return value;
    }
}
