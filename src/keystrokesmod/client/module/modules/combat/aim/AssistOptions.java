package keystrokesmod.client.module.modules.combat.aim;

public final class AssistOptions {
    public enum Hitbox {Head,Chest,Body}
    public enum Targets {Players,Players_and_mobs;@Override public String toString(){return this==Players?"Players":"Players + mobs";}}
    public double speed=4,yawStrength=.8,pitchStrength=.15,fov=60,range=4.5;
    public double trail=1,delay=.12,offset=.035,maxOffset=4,humanization=.2,fade=.15,switchDelay=.18,activation=1;
    public Hitbox hitbox=Hitbox.Chest;
    public Targets targets=Targets.Players;
    public boolean clicking,moving,weapon,noGui=true,teams=true,invisible=true,creative=true,mining=true,debug;
}
