package keystrokesmod.client.module.modules.render.island;

/** Five pending events plus one displayed event. Safe for asynchronous API callers. */
public final class IslandQueue {
    private final IslandEvent[] queue=new IslandEvent[5];private int count;
    private IslandEvent active;private long until;
    public synchronized void clear(){java.util.Arrays.fill(queue,null);count=0;active=null;until=0;}
    public synchronized int size(){return count;}
    public synchronized void post(IslandEvent event,long now){
        if(active!=null&&active.key.equals(event.key)){active=event;until=now+event.duration;return;}
        if(active!=null&&event.priority>active.priority){active=event;until=now+event.duration;return;}
        for(int i=0;i<count;i++)if(queue[i].key.equals(event.key)){queue[i]=event;return;}
        if(count==5){int lowest=0;for(int i=1;i<count;i++)if(queue[i].priority<queue[lowest].priority)lowest=i;
            if(event.priority<queue[lowest].priority)return;remove(lowest);}
        queue[count++]=event;
    }
    private IslandEvent remove(int index){IslandEvent e=queue[index];for(int i=index;i<count-1;i++)queue[i]=queue[i+1];queue[--count]=null;return e;}
    public synchronized IslandEvent current(long now){
        if(active!=null&&now>=until)active=null;
        if(active==null&&count>0){int best=0;for(int i=1;i<count;i++)if(queue[i].priority>queue[best].priority)best=i;active=remove(best);until=now+active.duration;}
        return active;
    }
}
