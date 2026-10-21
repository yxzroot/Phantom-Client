package keystrokesmod.client.module.modules.movement;

/** Time-based eased rotation with a low-pass filtered angular velocity. */
final class RotationSmoother {
    private float yawVelocity;
    private float pitchVelocity;
    float yawStep;
    float pitchStep;

    void reset() {
        yawVelocity = 0.0F;
        pitchVelocity = 0.0F;
        yawStep = 0.0F;
        pitchStep = 0.0F;
    }

    void step(float yawDelta, float pitchDelta, float deltaSeconds, float speed, float maxTurnPerTick) {
        float response = 1.0F - (float) Math.exp(-12.0F * deltaSeconds);
        float velocityResponse = 1.0F - (float) Math.exp(-18.0F * deltaSeconds);
        float maxStep = Math.min(speed * deltaSeconds, maxTurnPerTick);
        float desiredYawVelocity = clamp(yawDelta * response, -maxStep, maxStep) / deltaSeconds;
        float desiredPitchVelocity = clamp(pitchDelta * response, -maxStep, maxStep) / deltaSeconds;
        float previousYawVelocity = yawVelocity;
        float previousPitchVelocity = pitchVelocity;
        yawVelocity += (desiredYawVelocity - yawVelocity) * velocityResponse;
        pitchVelocity += (desiredPitchVelocity - pitchVelocity) * velocityResponse;
        yawStep = clamp(integratedStep(previousYawVelocity, desiredYawVelocity, velocityResponse,
                18.0F, deltaSeconds), -maxStep, maxStep);
        pitchStep = clamp(integratedStep(previousPitchVelocity, desiredPitchVelocity, velocityResponse,
                18.0F, deltaSeconds), -maxStep, maxStep);
    }

    void release(float deltaSeconds) {
        float decay = (float) Math.exp(-14.0F * deltaSeconds);
        yawStep = yawVelocity * (1.0F - decay) / 14.0F;
        pitchStep = pitchVelocity * (1.0F - decay) / 14.0F;
        yawVelocity *= decay;
        pitchVelocity *= decay;
        if (Math.abs(yawVelocity) < 0.01F) yawVelocity = yawStep = 0.0F;
        if (Math.abs(pitchVelocity) < 0.01F) pitchVelocity = pitchStep = 0.0F;
    }

    boolean isStill() { return yawVelocity == 0.0F && pitchVelocity == 0.0F; }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float integratedStep(float previousVelocity, float targetVelocity, float response,
                                        float filterRate, float deltaSeconds) {
        return targetVelocity * deltaSeconds
                + (previousVelocity - targetVelocity) * response / filterRate;
    }
}
