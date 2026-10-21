package keystrokesmod.client.module.modules.combat.aim;

/** Bridges an active clicker's synthetic UP half-cycle, never the idle enabled state. */
public final class AttackGate {
    private double lastPress=Double.NEGATIVE_INFINITY;
    public void reset(){lastPress=Double.NEGATIVE_INFINITY;}
    public void press(double now){lastPress=now;}
    public boolean active(boolean mouseDown,boolean attackKeyDown,boolean clickerEnabled,double now){
        if(mouseDown||attackKeyDown){press(now);return true;}
        return clickerEnabled&&now>=lastPress&&now-lastPress<=.18;
    }
}
