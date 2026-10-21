package keystrokesmod.client.module.modules.render.aurora;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Static sky VBOs; shader offsets animate them. Snow streams into one persistent buffer. */
public final class AuroraRenderer {
    private final AuroraGLState state = new AuroraGLState();
    private final int[] curtains = new int[AuroraSkyGeometry.MAX_CURTAINS];
    private final AuroraOptions defaults=new AuroraOptions();
    private final AuroraShootingStar meteor=new AuroraShootingStar();
    private final ConstellationRenderer constellation=new ConstellationRenderer();
    private final OvercastRenderer overcast=new OvercastRenderer();
    private final ArtSkyRenderer art=new ArtSkyRenderer();
    private final FloatBuffer snow = BufferUtils.createFloatBuffer(AuroraSimulation.MAX_SNOW * 8 * 3 * 7);
    private int stars, snowVbo, program, kind, clock, power, opacity, palette, theme, curtain;
    private int galaxy,galaxyVbo,wisps,moonVbo,folds,bloom,realClock,cloudMode,galaxySampler;
    private boolean ready;

    private int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source); GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 4096); GL20.glDeleteShader(shader);
            throw new IllegalStateException("Aurora shader: " + log);
        }
        return shader;
    }
    private int buffer(float[] data) {
        FloatBuffer staging = BufferUtils.createFloatBuffer(data.length); staging.put(data).flip();
        int id = GL15.glGenBuffers(); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, staging, GL15.GL_STATIC_DRAW); return id;
    }
    private void initialize() {
        if (ready) return;
        if (!GLContext.getCapabilities().OpenGL20) throw new IllegalStateException("Aurora needs OpenGL 2.0");
        int vertex = 0, fragment = 0;
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, AuroraShaders.VERTEX); fragment = compile(GL20.GL_FRAGMENT_SHADER, AuroraShaders.FRAGMENT);
            program = GL20.glCreateProgram(); GL20.glAttachShader(program, vertex); GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0)
                throw new IllegalStateException("Aurora link: " + GL20.glGetProgramInfoLog(program, 4096));
            kind = GL20.glGetUniformLocation(program, "kind"); clock = GL20.glGetUniformLocation(program, "clock");
            power = GL20.glGetUniformLocation(program, "power"); opacity = GL20.glGetUniformLocation(program, "opacity");
            palette = GL20.glGetUniformLocation(program, "palette"); theme = GL20.glGetUniformLocation(program, "theme");
            curtain = GL20.glGetUniformLocation(program, "curtain");
            folds=GL20.glGetUniformLocation(program,"folds");bloom=GL20.glGetUniformLocation(program,"bloom");
            realClock=GL20.glGetUniformLocation(program,"realClock");cloudMode=GL20.glGetUniformLocation(program,"cloudMode");
            galaxySampler=GL20.glGetUniformLocation(program,"galaxy");
            stars = buffer(AuroraSkyGeometry.stars());
            for (int i = 0; i < curtains.length; i++) curtains[i] = buffer(AuroraSkyGeometry.curtain(i));
            galaxyVbo=buffer(AuroraSkyGeometry.band(false));wisps=buffer(AuroraSkyGeometry.band(true));moonVbo=buffer(AuroraSkyGeometry.moon());
            galaxy=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,galaxy);
            java.nio.IntBuffer pixels=BufferUtils.createIntBuffer(AuroraSkyGeometry.MILKY_W*AuroraSkyGeometry.MILKY_H);
            pixels.put(AuroraSkyGeometry.milkyPixels()).flip();
            GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
            try{
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,4);GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH,0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS,0);GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS,0);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,AuroraSkyGeometry.MILKY_W,AuroraSkyGeometry.MILKY_H,0,GL12.GL_BGRA,GL12.GL_UNSIGNED_INT_8_8_8_8_REV,pixels);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
            }finally{GL11.glPopClientAttrib();}
            snowVbo = GL15.glGenBuffers(); GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, snowVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) snow.capacity() * 4, GL15.GL_STREAM_DRAW);
            ready = true;
        } catch (RuntimeException failure) { dispose(); throw failure; }
        finally { if (vertex != 0) GL20.glDeleteShader(vertex); if (fragment != 0) GL20.glDeleteShader(fragment); }
    }
    private void arrays(int vbo) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY); GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 28, 0L); GL11.glColorPointer(4, GL11.GL_FLOAT, 28, 12L);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    }
    private void sprites(int vbo){
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vbo);GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3,GL11.GL_FLOAT,36,0L);GL11.glColorPointer(4,GL11.GL_FLOAT,36,12L);
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0);GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);GL11.glTexCoordPointer(2,GL11.GL_FLOAT,36,28L);
    }
    public void sky(AuroraSimulation sim, float intensity, int count, int paletteIndex, int accent,
                    float density, boolean shooting, float fogR, float fogG, float fogB) {
        defaults.intensity=intensity;defaults.curtains=count;defaults.palette=paletteIndex;defaults.accent=accent;
        defaults.stars=density;defaults.shooting=shooting;
        sky(sim,defaults,fogR,fogG,fogB);
    }
    public void sky(AuroraSimulation sim,AuroraOptions opt,float fogR,float fogG,float fogB){
        state.begin();
        try {
            initialize(); GL11.glDisable(GL11.GL_DEPTH_TEST); GL20.glUseProgram(0);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // Cover the full sphere, blending into the exact FogColors event result at its equator.
            for (int ring = 0; ring < 32; ring++) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (int segment = 0; segment <= 48; segment++) {
                    double angle = segment * Math.PI * 2 / 48;
                    for (int edge = 0; edge < 2; edge++) {
                        double elevation = -Math.PI / 2 + (ring + edge) * Math.PI / 32;
                        float t = AuroraSimulation.smooth((float) Math.max(0,Math.sin(elevation)));
                        // Smooth full-height falloff rather than a near-horizon dark ring.
                        float dark=1-AuroraSimulation.clamp(opt.constellationMix,0,1);
                        GL11.glColor4f((fogR*(1-t)+.003F*t)*dark,(fogG*(1-t)+.004F*t)*dark,(fogB*(1-t)+.014F*t)*dark,sim.fade);
                        GL11.glVertex3d(Math.sin(angle)*Math.cos(elevation)*46, Math.sin(elevation)*46,
                                Math.cos(angle)*Math.cos(elevation)*46);
                    }
                }
                GL11.glEnd();
            }
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            if(opt.constellationMix+opt.duskMix+opt.art.mix()<.9999F){
            GL20.glUseProgram(program); GL20.glUniform1f(clock, (float) (sim.phase % 10000));
            GL20.glUniform1f(realClock,(float)(sim.time%10000));GL20.glUniform1f(opacity,sim.fade*(1-opt.constellationMix-opt.duskMix-opt.art.mix()));
            GL20.glUniform3f(theme,(opt.accent>>16&255)/255F,(opt.accent>>8&255)/255F,(opt.accent&255)/255F);
            if(opt.milkyWay){
                GL11.glBindTexture(GL11.GL_TEXTURE_2D,galaxy);GL20.glUniform1i(galaxySampler,0);
                GL20.glUniform1i(kind,3);sprites(galaxyVbo);GL11.glDrawArrays(GL11.GL_QUADS,0,96*8*4);
            }
            if(opt.starField){GL20.glUniform1i(kind,1);sprites(stars);
                GL11.glDrawArrays(GL11.GL_QUADS,0,Math.round(AuroraSimulation.MAX_STARS*AuroraSimulation.clamp(opt.stars,0,1))*4);}
            if(opt.moon){GL20.glUniform1i(kind,5);sprites(moonVbo);GL11.glDrawArrays(GL11.GL_QUADS,0,4);}
            GL20.glUniform1i(kind,0);GL20.glUniform1f(power,AuroraSimulation.clamp(opt.intensity,0,2));
            GL20.glUniform1f(folds,AuroraSimulation.clamp(opt.folds,0,2));GL20.glUniform1i(palette,opt.palette);
            int count=effectiveCurtains(opt);
            for (int i = 0; i < count; i++) {
                GL20.glUniform1f(curtain, i); arrays(curtains[i]);
                if(opt.bloom&&!opt.lowQuality){GL20.glUniform1f(bloom,1);GL11.glDrawArrays(GL11.GL_QUADS,0,AuroraSkyGeometry.COLUMNS*AuroraSkyGeometry.BANDS*4);}
                GL20.glUniform1f(bloom,0);GL11.glDrawArrays(GL11.GL_QUADS,0,AuroraSkyGeometry.COLUMNS*AuroraSkyGeometry.BANDS*4);
            }
            if(opt.cloudMode>0){GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL20.glUniform1i(kind,4);GL20.glUniform1i(cloudMode,opt.cloudMode);
                sprites(wisps);GL11.glDrawArrays(GL11.GL_QUADS,0,96*8*4);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);}
            }
            if(opt.constellationMix>0)constellation.draw(sim,opt,sim.fade*opt.constellationMix);
            if(opt.duskMix>0)overcast.draw(sim,opt,sim.fade*opt.duskMix);
            if (opt.shooting&&opt.duskMix+opt.art.mix()<.9999F) shooting(sim,opt);
            if(opt.art.mix()>0)art.draw(sim,opt);
        } finally { state.end(); }
    }
    public static int effectiveCurtains(AuroraOptions opt){int count=Math.max(4,Math.min(6,opt.curtains));return opt.lowQuality?(count+1)/2:count;}
    private void shooting(AuroraSimulation sim,AuroraOptions opt) {
        if(!meteor.sample(sim.time,opt.shootingChance,opt.shootingInterval))return;
        GL20.glUseProgram(0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
        try{
            GL11.glRotatef(meteor.yaw,0,1,0);
            float a=meteor.alpha*sim.fade*.85F*(1-opt.duskMix-opt.art.mix());
            for(int edge=0;edge<2;edge++){
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                for(int i=0;i<=12;i++)for(int side=0;side<2;side++){
                    float tail=i/12F;int lane=edge+side;
                    double x=(-12+meteor.progress*24-tail*6)*meteor.direction,y=meteor.elevation-meteor.progress*6+tail*1.5;
                    GL11.glColor4f(.75F+.25F*opt.constellationMix,.88F+.12F*opt.constellationMix,1,lane==1?a*(1-tail)*(1-tail):0);
                    GL11.glVertex3d(x,y+(lane-1)*.065,-25);
                }
                GL11.glEnd();
            }
        }finally{GL11.glPopMatrix();}
    }
    public void snow(AuroraSimulation sim, double cx, double cy, double cz,
                     float rx, float rz, float ux, float uy, float uz) {
        if (sim.snowFade <= 0 || sim.density <= 0) return;
        state.begin();
        try {
            initialize(); snow.clear();
            for (int i = 0; i < AuroraSimulation.MAX_SNOW; i++) {
                float a = sim.alpha[i] * sim.snowFade * AuroraSimulation.clamp(sim.density - i, 0, 1);
                float px = (float) (sim.x[i]-cx), py = (float) (sim.y[i]-cy), pz = (float) (sim.z[i]-cz);
                a *= AuroraSimulation.clamp((16 - Math.max(Math.abs(px), Math.abs(pz))) / 3, 0, 1);
                if (a <= .001 || !sim.exposed[i]) continue;
                float s = sim.size[i];
                for (int edge = 0; edge < 8; edge++) {
                    for (int corner = 0; corner < 3; corner++) {
                        double angle = (edge + (corner == 2 ? 1 : 0)) * Math.PI / 4;
                        float right = corner == 0 ? 0 : (float) Math.cos(angle) * s;
                        float up = corner == 0 ? 0 : (float) Math.sin(angle) * s;
                        snow.put(px + rx*right + ux*up).put(py + uy*up).put(pz + rz*right + uz*up);
                        snow.put(.84F).put(.92F).put(1).put(corner == 0 ? a*.72F : 0);
                    }
                }
            }
            snow.flip(); if (!snow.hasRemaining()) return;
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, snowVbo); GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, snow);
            GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glUseProgram(program); GL20.glUniform1i(kind, 2); GL20.glUniform1f(opacity, 1);
            arrays(snowVbo); GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, snow.limit()/7);
        } finally { state.end(); }
    }
    public void vignette(float amount, int width, int height) {
        if (amount <= 0) return;
        state.begin();
        try {
            GL20.glUseProgram(0); GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float w = width*.13F, h = height*.15F;
            for (int side = 0; side < 4; side++) {
                GL11.glBegin(GL11.GL_QUADS);
                for (int corner = 0; corner < 4; corner++) {
                    boolean outer = corner == 0 || corner == 1;
                    float x, y;
                    if (side < 2) { x = corner == 0 || corner == 3 ? 0 : width; y = side == 0 ? (outer ? 0 : h) : (outer ? height : height-h); }
                    else { y = corner == 0 || corner == 3 ? 0 : height; x = side == 2 ? (outer ? 0 : w) : (outer ? width : width-w); }
                    GL11.glColor4f(.13F,.33F,.57F,outer ? amount*.09F : 0); GL11.glVertex2f(x,y);
                }
                GL11.glEnd();
            }
        } finally { state.end(); }
    }
    public void dispose() {
        constellation.dispose();
        overcast.dispose();
        art.dispose();
        if (stars != 0) GL15.glDeleteBuffers(stars); if (snowVbo != 0) GL15.glDeleteBuffers(snowVbo);
        for (int i=0;i<curtains.length;i++) { if(curtains[i]!=0) GL15.glDeleteBuffers(curtains[i]); curtains[i]=0; }
        if(galaxy!=0)GL11.glDeleteTextures(galaxy);if(galaxyVbo!=0)GL15.glDeleteBuffers(galaxyVbo);
        if(wisps!=0)GL15.glDeleteBuffers(wisps);if(moonVbo!=0)GL15.glDeleteBuffers(moonVbo);
        if (program != 0) GL20.glDeleteProgram(program);
        ready=false;stars=snowVbo=program=0;
        galaxy=galaxyVbo=wisps=moonVbo=0;
    }
}
