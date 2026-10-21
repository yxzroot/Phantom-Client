package keystrokesmod.client.module.modules.render.killeffect;

/** Bounded identity-based death deduplication and recent-hit attribution, independently testable. */
public final class KillCredit {
    public static final int CAP=128,NONE=0,LOCAL=1,OTHER=2,BLOCKED=3;
    private final Object[] identity=new Object[CAP];
    private final boolean[] alive=new boolean[CAP],fired=new boolean[CAP];
    private final double[] seen=new double[CAP];
    private final double[] damagedAt=new double[CAP];
    private final int[] known=new int[CAP];
    private Object lastHit;
    private double hitAt=-100;
    public void clear(){java.util.Arrays.fill(identity,null);lastHit=null;hitAt=-100;}
    private int find(Object entity){for(int i=0;i<CAP;i++)if(identity[i]==entity)return i;return -1;}
    public boolean contains(Object entity){return find(entity)>=0;}
    public void watch(Object entity,boolean living,double now){
        if(entity==null)return;int slot=find(entity);if(slot>=0){
            seen[slot]=now;
            if(living&&fired[slot]){alive[slot]=true;fired[slot]=false;known[slot]=NONE;damagedAt[slot]=-100;}
            return;
        }
        slot=0;for(int i=0;i<CAP;i++){if(identity[i]==null){slot=i;break;}if(seen[i]<seen[slot])slot=i;}
        identity[slot]=entity;alive[slot]=living;fired[slot]=false;known[slot]=NONE;seen[slot]=now;damagedAt[slot]=-100;
    }
    public void attack(Object entity,double now){watch(entity,true,now);lastHit=entity;hitAt=now;int slot=find(entity);if(slot>=0)known[slot]=LOCAL;}
    public void credit(Object entity,int owner){int slot=find(entity);if(slot>=0&&owner!=NONE)known[slot]=owner;}
    public void damage(Object entity,double now){int slot=find(entity);if(slot>=0)damagedAt[slot]=now;}
    /** Returns LOCAL/OTHER exactly once, or NONE. Entity removal without death evidence is ignored. */
    public int observe(Object entity,boolean dead,int owner,double now,boolean allowOthers){
        int slot=find(entity);if(slot<0)return NONE;seen[slot]=now;
        if(!dead){alive[slot]=true;return NONE;}
        if(fired[slot]||!alive[slot])return NONE;
        fired[slot]=true;if(owner!=NONE)known[slot]=owner;
        boolean mine=entity==lastHit&&now>=hitAt&&now-hitAt<=3&&known[slot]!=OTHER&&known[slot]!=BLOCKED;
        if(entity==lastHit)lastHit=null;
        // When a server omits attacker metadata, the optional observer mode may use
        // a recent visible hurt animation followed by death. Never increment local stats.
        return mine?LOCAL:allowOthers&&known[slot]!=BLOCKED&&(known[slot]==OTHER||now-damagedAt[slot]<=3)?OTHER:NONE;
    }
}
