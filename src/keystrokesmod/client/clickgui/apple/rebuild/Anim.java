package keystrokesmod.client.clickgui.apple.rebuild;

/** Small allocation-free scalar animation. */
public final class Anim {
    public enum Mode { EASE_OUT_CUBIC, SPRING }

    private static final float EPSILON = .0005F;
    private final Mode mode;
    private float duration;
    private float current, target, velocity, start, startVelocity, elapsed;

    public Anim(float initial, Mode mode, float duration) {
        current = target = start = initial;
        this.mode = mode;
        this.duration = duration <= 0 ? .001F : duration;
    }

    public void setTarget(float value) {
        if (target != value) { start = current; startVelocity = velocity; elapsed = 0F; target = value; }
        if (!GuiTokens.animationsEnabled()) snapTo(value);
    }

    public void snapTo(float value) { current = target = start = value; velocity = startVelocity = elapsed = 0F; }

    public float update(float dt) {
        if (isDone()) return current;
        if (!GuiTokens.animationsEnabled()) { snapTo(target); return current; }
        float speed = GuiTokens.animationSpeed();
        dt = Math.min(.05F, Math.max(0F, dt)) * speed;
        if (mode == Mode.SPRING) {
            // Critically damped; substeps avoid bounce on a long Minecraft frame.
            float omega = 5.8F / duration;
            while (dt > 0F) {
                float step = Math.min(.012F, dt);
                velocity += (omega * omega * (target - current) - 2F * omega * velocity) * step;
                current += velocity * step;
                float bound = Math.max(.0001F, Math.abs(start - target) * .02F);
                if (current < Math.min(start, target) - bound || current > Math.max(start, target) + bound) {
                    current = Math.max(Math.min(start, target) - bound, Math.min(Math.max(start, target) + bound, current));
                    velocity = 0F;
                }
                dt -= step;
            }
        } else {
            float before = current;
            elapsed = Math.min(duration, elapsed + dt);
            float u = elapsed / duration;
            float carried = Math.max(-Math.abs(target - start) * .5F, Math.min(Math.abs(target - start) * .5F, startVelocity * duration * .14F));
            current = start + (target - start) * MotionTokens.ease(u) + carried * u * (1F - u) * (1F - u);
            current = Math.max(Math.min(start, target), Math.min(Math.max(start, target), current));
            velocity = (current - before) / Math.max(.001F, dt);
            if (elapsed >= duration) snapTo(target);
        }
        if (Math.abs(target - current) < EPSILON && Math.abs(velocity) < .01F) snapTo(target);
        return current;
    }

    public float get() { return current; }
    public void setDuration(float seconds) { duration = Math.max(.001F, seconds); }
    public float target() { return target; }
    public boolean isDone() { return Math.abs(target - current) < EPSILON && Math.abs(velocity) < .01F; }
}
