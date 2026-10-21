package keystrokesmod.client.module.modules.render.island;

/** Continuous line position and explicit break detection. Never infer silence from line length. */
public final class LyricMotion {
    private final IslandSpring scroll=new IslandSpring(-1);
    private LyricTimeline timeline;private long transport=-1;private double lastPosition,fadeAge;
    public double position=-1,dots,opacity;public int active=-1;
    public void update(LyricTimeline next,double seconds,long discontinuity,double dt,float duration){
        int index=next.index(seconds);boolean reset=timeline!=next||transport!=discontinuity||Math.abs(seconds-lastPosition)>1.25;
        if(reset){scroll.value=index;scroll.velocity=0;fadeAge=0;dots=0;timeline=next;transport=discontinuity;}
        lastPosition=seconds;active=index;fadeAge=Math.min(.18,fadeAge+Math.max(0,dt));double f=fadeAge/.18;opacity=f*f*(3-2*f);
        position=scroll.update(index,dt,.36/Math.max(.2,duration),0);
        LyricTimeline.Line line=next.line(index),upcoming=next.line(index+1);
        boolean gap=false;
        if(line==null)gap=upcoming!=null&&upcoming.time>seconds;
        else gap=line.text.trim().isEmpty();
        dots+=( (gap?1:0)-dots )*(1-Math.exp(-Math.max(0,dt)*9));
    }
    public void reset(){timeline=null;transport=-1;position=-1;dots=opacity=fadeAge=0;scroll.value=-1;scroll.velocity=0;}
}
