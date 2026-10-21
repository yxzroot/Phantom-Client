package keystrokesmod.client.module.modules.render.aurora;

/** Allocation-free animation/particle state; no Minecraft, GL or world mutation. */
public final class AuroraSimulation {
    public static final int MAX_SNOW = 300, MAX_STARS = 6000, SEGMENTS = 160, LEVELS = 9;
    public interface Roof { int height(int x, int z); }
    public final double[] x = new double[MAX_SNOW], y = new double[MAX_SNOW], z = new double[MAX_SNOW];
    public final float[] size = new float[MAX_SNOW], fall = new float[MAX_SNOW], alpha = new float[MAX_SNOW];
    public final boolean[] exposed = new boolean[MAX_SNOW];
    public double time, phase;
    public float fade, snowFade, density, gust, windX, windZ;
    private float progress, snowProgress;
    private int seed = 0x28AC7193;
    private boolean initialized;
    private double lastX, lastY, lastZ;
    private double roofTimer;

    public static float clamp(float value, float min, float max) {
        return Float.isNaN(value) || Float.isInfinite(value) ? min : Math.max(min, Math.min(max, value));
    }
    public static float smooth(float x) { x = clamp(x, 0, 1); return x * x * (3 - 2 * x); }
    private float random() { seed ^= seed << 13; seed ^= seed >>> 17; seed ^= seed << 5; return (seed & 0x7fffffff) / 2147483648F; }
    private static float approach(float from, float to, float amount) { return from < to ? Math.min(to, from + amount) : Math.max(to, from - amount); }
    public void reset() {
        initialized = false; progress = snowProgress = fade = snowFade = density = 0;
        roofTimer = 0;
        for (int i = 0; i < MAX_SNOW; i++) { alpha[i] = 0; exposed[i] = false; }
    }
    public void advance(double seconds, boolean active, int snowMode, float speed, float fadeSeconds,
                        float wind, double cameraX, double cameraY, double cameraZ, Roof roof) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) seconds = 0;
        time += seconds; phase += seconds * clamp(speed, .1F, 3);
        float dt = (float) Math.min(seconds, .25), step = dt / clamp(fadeSeconds, .05F, 2);
        progress = approach(progress, active ? 1 : 0, step); fade = smooth(progress);
        snowProgress = approach(snowProgress, active && snowMode > 0 ? 1 : 0, step); snowFade = smooth(snowProgress);
        density = approach(density, snowMode == 2 ? 300 : snowMode == 1 ? 90 : 0, step * 300);
        double cycle = time % 29;
        gust = smooth((float) ((cycle - 18) / 1.4)) * (1 - smooth((float) ((cycle - 23) / 2.2)));
        float windPower = clamp(wind, 0, 2);
        windX = (float) ((.28 + 2.3 * gust) * Math.cos(time * .025) * windPower);
        windZ = (float) ((.18 + 1.1 * gust) * Math.sin(.8 + time * .021) * windPower);
        if (snowFade <= 0 && (!active || snowMode == 0)) { initialized = false; return; }
        if (!initialized || Math.abs(cameraX - lastX) + Math.abs(cameraY - lastY) + Math.abs(cameraZ - lastZ) > 40) {
            for (int i = 0; i < MAX_SNOW; i++) {
                size[i] = .018F + random() * .036F; fall[i] = .65F + random() * 1.25F;
                spawn(i, cameraX, cameraY, cameraZ, false, roof);
            }
            initialized = true;
        }
        lastX = cameraX; lastY = cameraY; lastZ = cameraZ;
        roofTimer -= seconds;
        boolean checkRoof = roofTimer <= 0;
        if (checkRoof) roofTimer = .15;
        for (int i = 0; i < MAX_SNOW; i++) {
            x[i] += (windX + Math.sin(time * .7 + i * 1.37) * .18) * dt;
            z[i] += (windZ + Math.cos(time * .53 + i * .73) * .13) * dt;
            y[i] -= fall[i] * dt;
            if (y[i] < cameraY - 8 || Math.abs(x[i] - cameraX) > 16 || Math.abs(z[i] - cameraZ) > 16)
                spawn(i, cameraX, cameraY, cameraZ, true, roof);
            if (checkRoof) exposed[i] = y[i] >= roof.height((int) Math.floor(x[i]), (int) Math.floor(z[i]));
            float visible = exposed[i] ? 1 : 0;
            alpha[i] = approach(alpha[i], visible, dt * 7);
        }
    }
    private void spawn(int i, double cx, double cy, double cz, boolean top, Roof roof) {
        x[i] = cx + (random() - .5) * 30; z[i] = cz + (random() - .5) * 30;
        y[i] = cy + (top ? 7 + random() * 5 : (random() - .5) * 20);
        exposed[i] = y[i] >= roof.height((int) Math.floor(x[i]), (int) Math.floor(z[i]));
        alpha[i] = 0;
    }
    /** Original cached geometry, interleaved xyz + rgba (seven floats per vertex). */
    public static float[] curtainMesh(int index) {
        float[] levels = {0, .018F, .055F, .14F, .32F, .54F, .74F, .9F, 1};
        float[] data = new float[SEGMENTS * (LEVELS - 1) * 4 * 7];
        int at = 0;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            for (int band = 0; band < LEVELS - 1; band++) {
                for (int corner = 0; corner < 4; corner++) {
                    float u = (segment + (corner == 1 || corner == 2 ? 1 : 0)) / (float) SEGMENTS;
                    float v = levels[band + (corner >= 2 ? 1 : 0)];
                    double angle = (u - .5) * 2.65 + index * 1.17;
                    double radius = 25 + index * 2 + Math.sin(u * 16 + index) * 1.4;
                    data[at++] = (float) (Math.sin(angle) * radius);
                    data[at++] = 2.4F + index * 1.2F + v * (20 + index * 1.8F);
                    data[at++] = (float) (Math.cos(angle) * radius);
                    // Store u/v/index in color channels; GLSL produces layered spectral color.
                    data[at++] = u; data[at++] = v; data[at++] = index;
                    data[at++] = smooth(u * 12) * smooth((1 - u) * 12);
                }
            }
        }
        return data;
    }
    public static float[] starMesh() {
        java.util.Random random = new java.util.Random(0xA1707A);
        float[] data = new float[MAX_STARS * 4 * 7]; int at = 0;
        for (int i = 0; i < MAX_STARS; i++) {
            double az = random.nextDouble() * Math.PI * 2, up = .015 + random.nextDouble() * .985;
            double radial = Math.sqrt(1 - up * up), nx = Math.sin(az) * radial, nz = Math.cos(az) * radial;
            float size = .012F + random.nextFloat() * random.nextFloat() * .042F;
            float warmth = random.nextFloat(), alpha = .36F + random.nextFloat() * .64F;
            for (int corner = 0; corner < 4; corner++) {
                float right = corner == 0 || corner == 3 ? -size : size;
                float vertical = corner < 2 ? -size : size;
                data[at++] = (float) (nx * 46 + Math.cos(az) * right - Math.sin(az) * up * vertical);
                data[at++] = (float) (up * 46 + radial * vertical);
                data[at++] = (float) (nz * 46 - Math.sin(az) * right - Math.cos(az) * up * vertical);
                data[at++] = .76F + warmth * .24F; data[at++] = .84F + warmth * .13F;
                data[at++] = 1 - warmth * .18F; data[at++] = alpha;
            }
        }
        return data;
    }
    public static float lockedAngle(float ticks, float partial) {
        float angle = ((ticks + partial) / 24000F - .25F) % 1;
        if (angle < 0) angle += 1;
        float original = angle;
        angle = 1 - (float) ((Math.cos(angle * Math.PI) + 1) / 2);
        return original + (angle - original) / 3;
    }
}
