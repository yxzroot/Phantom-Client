package keystrokesmod.client.module.modules.movement;

/** Preserves the discrete rotation increments Minecraft generates from real mouse input. */
final class GcdFix {
    private float yawRemainder;
    private float pitchRemainder;

    void reset() {
        yawRemainder = 0.0F;
        pitchRemainder = 0.0F;
    }

    float yaw(float delta, float sensitivity) {
        float value = quantize(delta, yawRemainder, sensitivity);
        yawRemainder += delta - value;
        if (Math.signum(yawRemainder) != Math.signum(delta) && Math.abs(delta) > 0.0001F)
            yawRemainder = 0.0F;
        return value;
    }

    float pitch(float delta, float sensitivity) {
        float value = quantize(delta, pitchRemainder, sensitivity);
        pitchRemainder += delta - value;
        if (Math.signum(pitchRemainder) != Math.signum(delta) && Math.abs(delta) > 0.0001F)
            pitchRemainder = 0.0F;
        return value;
    }

    private static float quantize(float delta, float remainder, float sensitivity) {
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;
        if (gcd <= 0.000001F) return delta;
        float combined = delta + remainder;
        float amount = (float) Math.floor(Math.abs(combined) / gcd) * gcd;
        return Math.copySign(amount, combined);
    }
}
