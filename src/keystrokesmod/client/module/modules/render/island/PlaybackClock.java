package keystrokesmod.client.module.modules.render.island;

/** Render-thread phase-locked playback clock. Jitter slews gently; real seeks snap once. */
public final class PlaybackClock {
    private MediaSnapshot sample;private long previous;private double position,correction;
    public long discontinuity;
    public double position(){return position;}
    public double update(MediaSnapshot next,long now){
        double dt=previous==0?0:Math.max(0,(now-previous)/1e9);previous=now;
        boolean identity=sample==null||!sample.session.equals(next.session)||!sample.id.equals(next.id)||!sample.title.equals(next.title)||!sample.artist.equals(next.artist);
        if(identity){sample=next;position=next.at(now);correction=0;discontinuity++;return position;}
        double before=position;boolean jumped=false;
        if(sample.playing&&!sample.buffering)position+=dt*sample.rate;
        if(next!=sample){
            double target=next.at(now),error=target-position;
            if(next.revision!=sample.revision||Math.abs(error)>1){position=target;correction=0;discontinuity++;jumped=true;}
            else if(!next.playing||next.buffering){position=target;correction=0;}
            else correction=error;
            sample=next;
        }
        if(sample.playing&&!sample.buffering){double amount=1-Math.exp(-dt/.24);position+=correction*amount;correction*=1-amount;
            // A small metadata correction must not run the lyrics backwards.
            if(!jumped&&position<before&&before-position<.5)position=before;
        }
        if(next.duration>0)position=Math.min(next.duration,position);position=Math.max(0,position);return position;
    }
    public void reset(){sample=null;previous=0;position=correction=0;}
}
