package keystrokesmod.client.module.modules.render.swordglow;

public enum AnimationStyle {
    Theme(1,1), EnergyFlow(1,1), Shimmer(1,1), Pulse(1,1), Flame(1,1), Lightning(1,1),
    Galaxy(1,1), Aurora(1,1), RainbowWave(1,1), Charge(1,1), Glitch(1,1), Void(1,1),
    Classic(1, 1), Fast(1.75F, 1), Slow(.5F, 1), Sharp(1, 1.5F), Rainbow(1, 1);

    public final float speed, scale;
    AnimationStyle(float speed, float scale) { this.speed = speed; this.scale = scale; }
    public int effect(){switch(this){case Shimmer:return 1;case Pulse:return 2;case Flame:return 3;case Lightning:return 4;case Galaxy:return 5;case Aurora:return 6;case RainbowWave:case Rainbow:return 7;case Charge:return 8;case Glitch:return 9;case Void:return 10;default:return 0;}}
    @Override public String toString(){return this==EnergyFlow?"Energy Flow":this==RainbowWave?"Rainbow Wave":name();}
}
