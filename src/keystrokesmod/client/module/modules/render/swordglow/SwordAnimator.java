package keystrokesmod.client.module.modules.render.swordglow;

/** Two vanilla glint passes. Time is seconds from a single monotonic render-frame clock. */
public final class SwordAnimator {
    public static final class Frame {
        public final float[] scroll = new float[2], angle = new float[2];
        public float textureScale = 8, visibility, brightness, hue;
        public AnimationStyle style = AnimationStyle.Classic;
        public AnimationStyle previousStyle=AnimationStyle.Classic;
        public float transition=1,phase,swing,charge,burst,hitFlash,blocking,equipSweep=1;
        public int secondary=-1,previousSecondary=-1;
        public float secondaryTransition=1;
        public float swingEnergy,swingSweep;
        public boolean swinging;
        public int swingId;
    }
    private final Frame frame = new Frame();
    private double phase, previousTime = Double.NaN;
    private float visibility, fadeStart, target, fadeElapsed;
    private float duration=.2F,styleElapsed=1,equipElapsed=1;
    private boolean wasSwinging;
    private AnimationStyle lastStyle=AnimationStyle.Classic,fromStyle=AnimationStyle.Classic;
    private SwordOptions.Secondary lastSecondary=SwordOptions.Secondary.None;
    private float secondaryElapsed=1;
    private float previousSwing;

    public Frame frame() { return frame; }

    public Frame update(double time, float dt, boolean active, AnimationStyle style,
                        float intensity, float textureScale, float angle) {
        if (!Double.isFinite(time)) time = Double.isNaN(previousTime) ? 0 : previousTime;
        double elapsed = Double.isNaN(previousTime) ? 0 : Math.max(0, time - previousTime);
        previousTime = time;
        phase += elapsed * style.speed;
        dt = finiteClamp(dt, 0, .25F, 0);
        float nextTarget = active ? 1 : 0;
        if (nextTarget != target) {
            fadeStart = visibility; target = nextTarget; fadeElapsed = 0;
        }
        fadeElapsed = Math.min(duration, fadeElapsed + dt);
        float t = fadeElapsed / duration;
        t = t * t * (3 - 2 * t);
        visibility = fadeStart + (target - fadeStart) * t;
        frame.visibility = visibility;
        frame.style = style;
        frame.textureScale = finiteClamp(textureScale, 1, 32, 8) * style.scale;
        frame.angle[0] = finiteClamp(angle, -180, 180, -50);
        frame.angle[1] = frame.angle[0] + 60;
        frame.scroll[0] = (float) ((phase % 3) / 3 / frame.textureScale);
        frame.scroll[1] = (float) (-(phase % 4.873) / 4.873 / frame.textureScale);
        // SRC_COLOR, ONE squares source color. sqrt makes fade/intensity energy linear.
        frame.brightness = (float) Math.sqrt(visibility * finiteClamp(intensity, 0, 1, .75F));
        frame.hue = (float) ((phase * .20) % 1);
        frame.phase=(float)phase;
        return frame;
    }
    public void reset(){phase=0;previousTime=Double.NaN;visibility=fadeStart=target=fadeElapsed=0;equipElapsed=1;frame.visibility=frame.brightness=frame.charge=frame.burst=frame.hitFlash=frame.blocking=0;wasSwinging=false;previousSwing=frame.swingEnergy=frame.swingSweep=0;frame.swingId=0;frame.swinging=false;lastStyle=fromStyle=AnimationStyle.Classic;styleElapsed=secondaryElapsed=1;lastSecondary=SwordOptions.Secondary.None;frame.secondary=frame.previousSecondary=-1;}
    public void equipped(){visibility=fadeStart=target=fadeElapsed=0;equipElapsed=0;wasSwinging=false;previousSwing=frame.swingEnergy=0;frame.swinging=false;}
    public Frame advance(double time,float dt,boolean active,SwordOptions opt,float swing,boolean swinging,boolean hit,boolean blocking){
        duration=finiteClamp(opt.fade,.2F,.3F,.25F);
        update(time,dt,active,opt.style,opt.intensity,opt.scale,opt.angle);
        dt=finiteClamp(dt,0,.25F,0);
        if(lastStyle!=opt.style){fromStyle=lastStyle;lastStyle=opt.style;styleElapsed=0;}
        styleElapsed=Math.min(duration,styleElapsed+dt);float mix=styleElapsed/duration;
        frame.transition=mix*mix*(3-2*mix);frame.previousStyle=fromStyle;
        if(lastSecondary!=opt.secondary){frame.previousSecondary=frame.secondary;frame.secondary=opt.secondary.style==null?-1:opt.secondary.style.effect();lastSecondary=opt.secondary;secondaryElapsed=0;}
        secondaryElapsed=Math.min(duration,secondaryElapsed+dt);float secondaryMix=secondaryElapsed/duration;frame.secondaryTransition=secondaryMix*secondaryMix*(3-2*secondaryMix);
        frame.swing=finiteClamp(swing,0,1,0);
        frame.burst*=(float)Math.exp(-dt*8);frame.hitFlash*=(float)Math.exp(-dt*11);
        if(!swinging)frame.charge=Math.min(1,frame.charge+dt/2.2F);
        // Vanilla may keep isSwingInProgress true across consecutive attacks. A progress
        // wrap is a new stroke too; don't lose releases while holding the mouse down.
        boolean stroke=swinging&&(!wasSwinging||frame.swing+.15F<previousSwing);
        if(stroke){frame.swingId++;frame.burst=Math.max(.3F,frame.charge);frame.charge=0;}
        if(hit){frame.hitFlash=1;frame.burst=Math.max(frame.burst,1);frame.charge=0;}
        // Analytic asymmetric envelope: slow wind-up, bright cut, long soft recovery.
        // Uses interpolated vanilla progress, not animation speed or frame counts.
        frame.swinging=swinging;
        if(swinging){float p=frame.swing;
            float attack=smooth(p/.32F),recover=1-smooth((p-.32F)/.68F);
            frame.swingEnergy=attack*recover;
            frame.swingSweep=smooth((p-.06F)/.66F);
        }else frame.swingEnergy*=(float)Math.exp(-dt*18);
        wasSwinging=swinging;previousSwing=frame.swing;
        frame.blocking+=(blocking&&opt.block?1-frame.blocking:-frame.blocking)*(1-(float)Math.exp(-dt*12));
        equipElapsed=Math.min(.7F,equipElapsed+dt);frame.equipSweep=opt.equip?equipElapsed/.7F:1;
        return frame;
    }

    private static float smooth(float value){float t=Math.max(0,Math.min(1,value));return t*t*(3-2*t);}

    public static float finiteClamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
